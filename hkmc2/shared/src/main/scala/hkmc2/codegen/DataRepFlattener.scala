package hkmc2
package codegen

import hkmc2.utils.*, shorthands.*
import utils.*

import semantics.*
import flowAnalysis.*

import hkmc2.semantics.Elaborator.State

import scala.collection.mutable.{Set as MutSet, Map as MutMap}
import scala.collection.mutable.ListBuffer

type Web = FlowWebComputation.Result[Ctor, ConcreteCtorConsumer]

private object DataRepFlattenDebug:
  private def ctorName(ctor: CtorCls): Str = ctor match
    case cls: ClassLikeSymbol => cls.nme
    case size: Int => s"tup(size $size)"

  private def fieldName(field: SelField): Str = field match
    case sym: TermSymbol => sym.nme
    case index: Int => index.toString

  def showProducer(producer: Ctor): Str =
    s"${ctorName(producer.ctor)}@${producer.exprId}"

  def showFieldAccess(access: FieldSel): Str =
    s"${fieldName(access.field)}@${access.exprId}"

  def showPatternMatch(patternMatch: Dtor): Str =
    s"match@${patternMatch.exprId}"

class EntryPointCollector(val flowRes: FlowConstraintSolver)(using val tl: TL) extends BlockTraverser:
  private given fState: FlowAnalysis.State = flowRes.fState
  private given eState: State = flowRes.eState

  private val entryPoints: ListBuffer[List[Ctor]] = ListBuffer.empty
  private val concreteCtorsByResultId = MutMap.empty[ResultId, ListBuffer[Ctor]]
  for ctor <- flowRes.ctorsWithDests do
    concreteCtorsByResultId.getOrElseUpdate(ctor.exprId, ListBuffer.empty) += ctor

  private def funName(fun: FunDefn): Str =
    fun.owner.fold(fun.dSym.nme)(owner => s"${owner.nme}.${fun.dSym.nme}")

  private class AllocationCollector extends BlockTraverserShallow:
    val allocations: ListBuffer[ResultId -> CtorCls] = ListBuffer.empty

    override def applyResult(r: Result): Unit =
      r match
        case CtorProducer(ctor, _, _) => allocations += r.uid -> ctor
        case _ => ()
      super.applyResult(r)
  end AllocationCollector

  override def applyFunDefn(fun: FunDefn): Unit =
    if fun.visibility is Visibility.Public then
      val currentEntryPoints = ListBuffer.empty[Ctor]
      val collector = new AllocationCollector()
      collector.applyBlock(fun.body)

      val seenEntryPoints = MutSet.empty[Ctor]
      for
        (allocationId, _) <- collector.allocations
        ctor <- concreteCtorsByResultId.getOrElse(allocationId, Nil)
        if !ctor.dests.contains(UnknownCons)
        if seenEntryPoints.add(ctor)
      do currentEntryPoints += ctor

      if currentEntryPoints.nonEmpty then
        tl.log(s"track construction of ${currentEntryPoints.map(DataRepFlattenDebug.showProducer).mkString(", ")} in ${funName(fun)}")

      entryPoints += currentEntryPoints.toList

  override def applyClsLikeDefn(defn: ClsLikeDefn): Unit =
    defn.companion.foreach(applyCompanionModule)

object EntryPointCollector:
  def apply(p: Program, flowRes: FlowConstraintSolver)(using TL): List[List[Ctor]] =
    val collector = new EntryPointCollector(flowRes)
    collector.applyProgram(p)
    collector.entryPoints.toList


class DataRepFlattener(val webs: List[Web])(using State) extends BlockTransformer(SymbolSubst.Id)


object DataRepFlattener:
  private def mkWeb(entries: List[Ctor]): Web =
    FlowWebComputation[Ctor, ConcreteCtorConsumer](
      producer => producer.dests.collect:
        case consumer: ConcreteCtorConsumer => consumer,
      consumer => consumer.srcs.collect:
        case producer: Ctor => producer,
      entries,
      Nil,
    )

  private def mkWebs(entryPoints: List[List[Ctor]]) =
    val coveredProducers = MutSet.empty[Ctor]
    val webs = ListBuffer.empty[Web]
    for entries <- entryPoints do
      if entries.nonEmpty && !entries.exists(coveredProducers) then
        val web = mkWeb(entries)
        coveredProducers ++= web.markedProducers
        webs += web
    webs.toList

  private def logWebs(webs: List[Web])(using tl: TL): Unit =
    if webs.nonEmpty then
      tl.emitDbg(">>> start data-rep-flatten web-computation-phase")
      for (web, index) <- webs.zipWithIndex do
        val producers = web.markedProducers.toList.sortBy(_.exprId.uid)
        val fieldAccesses = web.markedConsumers.collect:
          case access: FieldSel => access
        val patternMatches = web.markedConsumers.collect:
          case patternMatch: Dtor => patternMatch
        tl.emitDbg(s"data-rep-flatten web-computation-phase > web $index:")
        tl.emitDbg(s"data-rep-flatten web-computation-phase >   producers: ${producers.map(DataRepFlattenDebug.showProducer).mkString(", ")}")
        if fieldAccesses.nonEmpty then
          tl.emitDbg(s"data-rep-flatten web-computation-phase >   field accesses: ${fieldAccesses.toList.sortBy(_.exprId.uid).map(DataRepFlattenDebug.showFieldAccess).mkString(", ")}")
        if patternMatches.nonEmpty then
          tl.emitDbg(s"data-rep-flatten web-computation-phase >   pattern matches: ${patternMatches.toList.sortBy(_.exprId.uid).map(DataRepFlattenDebug.showPatternMatch).mkString(", ")}")
      tl.emitDbg("<<< end data-rep-flatten web-computation-phase")

  def apply(p: Program)(using
    cfg: Config,
    tl: TL,
    raise: Raise,
    eState: State,
    symbolPrinter: SymbolPrinter,
  ): Program =
    cfg.dataRepFlatten match
      case N => p
      case S(dCfg) =>
        val flowCfg = Config.FlowAnalysisConfig(
          debug = false,
          mono = dCfg.mono,
          trackNonAffine = false,
          trackAccumulator = false,
          logNonAffine = false,
          logAccumulator = false,
        )
        val flowAnalysisRes =
          FlowAnalysis.mkTraceLogger(flowCfg, "data-rep-flatten flow-analysis-phase > ", tl).givenIn:
            FlowAnalysis(
              p,
              mono = flowCfg.mono,
              nonAffineTracking = false,
              accumulatorTracking = false,
            )
        val collectorTl = new TraceLogger(using tl.debugPrinter):
          override def doTrace: Bool = dCfg.debug
          override def emitDbg(str: Str): Unit =
            tl.emitDbg(s"data-rep-flatten collection-phase > $str")
        val entryPoints = collectorTl.givenIn:
          if dCfg.debug then tl.emitDbg(">>> start data-rep-flatten collection-phase")
          val result = EntryPointCollector(p, flowAnalysisRes)
          if dCfg.debug then tl.emitDbg("<<< end data-rep-flatten collection-phase")
          result
        val webs = mkWebs(entryPoints)
        if dCfg.debug then logWebs(webs)
        new DataRepFlattener(webs).applyProgram(p)
