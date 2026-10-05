package hkmc2
package codegen

import hkmc2.utils.*, shorthands.*
import utils.*
import Message.MessageContext

import semantics.*
import flowAnalysis.*

import hkmc2.semantics.Elaborator.State

import scala.collection.mutable.{Set as MutSet, Map as MutMap, LinkedHashSet as MutLinkedHashSet}
import scala.collection.mutable.ListBuffer

type Web = FlowWebComputation.Result[Ctor, ConcreteCtorConsumer]

private object ClassTagsDebug:
  def showCtor(ctor: CtorCls): Str = ctor match
    case cls: ClassLikeSymbol => cls.nme
    case size: Int => s"tup(size $size)"

  def showField(field: SelField): Str = field match
    case sym: TermSymbol => sym.nme
    case index: Int => index.toString

  def showProducer(producer: Ctor): Str =
    s"${showCtor(producer.ctor)}@${producer.exprId}"

  def showFieldAccess(access: FieldSel): Str =
    s"${{showCtor(access.selectsFrom)}}.${showField(access.field)}@${access.exprId}"

  def showPatternMatch(patternMatch: Dtor): Str =
    s"match@${patternMatch.exprId}"

  def showConsumer(consumer: ConcreteCtorConsumer): Str = consumer match
    case access: FieldSel => showFieldAccess(access)
    case patternMatch: Dtor => showPatternMatch(patternMatch)

// * Collect all producers & consumers in the given function to build the web
class WebEntryCollector(val flowRes: FlowConstraintSolver)(using val tl: TL) extends BlockTraverser:
  private given fState: FlowAnalysis.State = flowRes.fState
  private given eState: State = flowRes.eState

  private val entryPoints = ListBuffer.empty[WebEntryCollector.EntryPoints]
  private val concreteCtorsByResultId = MutMap.empty[ResultId, ListBuffer[Ctor]]
  for ctor <- flowRes.ctorsWithDests do
    concreteCtorsByResultId.getOrElseUpdate(ctor.exprId, ListBuffer.empty) += ctor
  private val concreteConsumersByResultId = MutMap.empty[ResultId, ListBuffer[ConcreteCtorConsumer]]
  for consumer <- flowRes.consumersWithSrcs do
    concreteConsumersByResultId.getOrElseUpdate(consumer.exprId, ListBuffer.empty) += consumer

  private class ResultCollector extends BlockTraverserShallow:
    val resultIds: ListBuffer[ResultId] = ListBuffer.empty

    override def applyResult(r: Result): Unit =
      resultIds += r.uid
      super.applyResult(r)
  end ResultCollector

  override def applyFunDefn(fun: FunDefn): Unit =
    val funName = fun.owner.fold(fun.dSym.nme)(owner => s"${owner.nme}.${fun.dSym.nme}")
    val collector = new ResultCollector()
    collector.applyBlock(fun.body)

    val seenProducerEntryPoints = MutSet.empty[Ctor]
    for
      resultId <- collector.resultIds
      ctor <- concreteCtorsByResultId.getOrElse(resultId, Nil)
      if !ctor.dests.contains(UnknownCons) // does not leak out of the web
      if !ctor.dests.exists:
        case consumer: ConcreteCtorConsumer => consumer.srcs.contains(UnknownProd)
        case _ => false
    do seenProducerEntryPoints.add(ctor)

    if !seenProducerEntryPoints.isEmpty then
      tl.log(s"track construction of ${seenProducerEntryPoints.map(ClassTagsDebug.showProducer).mkString(", ")} in $funName")

    val seenConsumerEntryPoints = MutSet.empty[ConcreteCtorConsumer]
    for
      resultId <- collector.resultIds
      consumer <- concreteConsumersByResultId.getOrElse(resultId, Nil)
      if !consumer.srcs.contains(UnknownProd) // not allocated out of the web
      if !consumer.srcs.exists:
        case ctor: Ctor => ctor.dests.contains(UnknownCons)
        case _ => false
      if consumer.srcs.exists:
        case _: Ctor => true
        case _ => false
    do seenConsumerEntryPoints.add(consumer)

    if !seenConsumerEntryPoints.isEmpty then
      tl.log(s"track consumption at ${seenConsumerEntryPoints.map(ClassTagsDebug.showConsumer).mkString(", ")} in $funName")

    entryPoints += WebEntryCollector.EntryPoints(
      seenProducerEntryPoints.toList,
      seenConsumerEntryPoints.toList,
    )

  def result: List[WebEntryCollector.EntryPoints] = entryPoints.toList

object WebEntryCollector:
  case class EntryPoints(producers: List[Ctor], consumers: List[ConcreteCtorConsumer])

  def apply(p: Program, flowRes: FlowConstraintSolver)(using TL): List[EntryPoints] =
    val collector = new WebEntryCollector(flowRes)
    collector.applyProgram(p)
    collector.result


private sealed abstract class Shape:
  def show: Str

  /** Flatten into the distinct concrete alternatives this shape stands for. No budget is
    * needed: shapes are propagated within a single function body, so a union is as deep
    * as the code that builds it rather than as deep as the call graph. */
  def flattenShape: List[Shape]

  def containsUnion: Bool

  // * This shape subsumption is only used for wildcards (dynamic shapes) checking.
  // * i.e., if the pattern is a wildcard, it can accept any scrutinee
  // * We do not support a union shape for pattern
  // * and we flattern unions in Ctor to insert different tags
  // * We also track precise information so we do not need to check if a class is a subclass of another.
  // * i.e., if a variable has shape C, then it is impossible that the variable is instantiated to a subclass D in runtime.
  final infix def <=(that: Shape): Bool = (this, that) match
    case (_, DynamicShape) => true
    case (LitShape(left), LitShape(right)) => left === right
    case (ClassShape(leftCtor, leftFields), ClassShape(rightCtor, rightFields)) =>
      leftCtor === rightCtor
        && leftFields.keySet === rightFields.keySet
        && leftFields.forall: (field, shape) =>
          shape <= rightFields(field)
    case (TupleShape(leftLength, leftElements), TupleShape(rightLength, rightElements)) =>
      leftLength === rightLength
        && leftElements.zip(rightElements).forall((left, right) => left <= right)
    case _ => false

private object Shape:
  def mkShapeByPattern(pattern: Pattern)(using raise: Raise): Shape =
    pattern match
      case ctorPattern @ Pattern.Constructor(_, arguments) =>
        val ctor = ctorPattern.symbol.flatMap:
          case ctor: ClassCtorSymbol => S(ctor.associatedCls)
          case symbol => symbol.asClsLike
        ctor match
          case S(cls: ClassSymbol) =>
            ClassTagsTransformer.classFields(cls) match
              case S(fields) =>
                val argumentShapes = arguments match
                  case S(patterns) => patterns.map(mkShapeByPattern)
                  case N => Nil
                if argumentShapes.size =/= fields.size then
                  raise(ErrorReport(
                    msg"Expected constructor arity ${fields.size} in @matchShapes pattern for ${cls.nme}, but found ${argumentShapes.size}." -> pattern.toLoc :: Nil,
                    source = Diagnostic.Source.Compilation,
                  ))
                  DynamicShape
                else ClassShape(cls, fields.zip(argumentShapes).toMap)
              case N =>
                raise(ErrorReport(
                  msg"This pattern is not supported by @matchShapes yet." -> pattern.toLoc :: Nil,
                  source = Diagnostic.Source.Compilation,
                ))
                DynamicShape
          case S(obj: ModuleOrObjectSymbol) =>
            ClassShape(obj, Map.empty)
          case _ => DynamicShape
      case Pattern.Tuple(leading, N) =>
        TupleShape(leading.size, leading.map(mkShapeByPattern))
      case Pattern.Literal(literal) => LitShape(Value.Lit(literal))
      case Pattern.Wildcard() => DynamicShape
      case _ =>
        raise(ErrorReport(
          msg"This pattern is not supported by @matchShapes yet." -> pattern.toLoc :: Nil,
          source = Diagnostic.Source.Compilation,
        ))
        DynamicShape

private case class LitShape(lit: Value.Lit) extends Shape:
  // * `show` and `hashCode` are cached: both walk the whole shape, and both are hit
  // * over and over by sorting, deduplication and the tag map.
  override lazy val show: Str = lit match
    case Value.Lit(lit) => lit.idStr

  override lazy val hashCode: Int = scala.util.hashing.MurmurHash3.productHash(this)

  def flattenShape: List[Shape] = this :: Nil

  def containsUnion: Bool = false

private case class ClassShape(ctor: ClassLikeSymbol, fields: Map[TermSymbol, Shape]) extends Shape:
  override lazy val show: Str =
    if fields.isEmpty then ClassTagsDebug.showCtor(ctor)
    else
      val shownFields = fields.iterator
        .map((field, shape) => s"${ClassTagsDebug.showField(field)}: ${shape.show}")
      s"${ClassTagsDebug.showCtor(ctor)}${shownFields.mkString("(", ", ", ")")}"

  override lazy val hashCode: Int = scala.util.hashing.MurmurHash3.productHash(this)

  def flattenShape: List[Shape] =
    val alternatives = fields.iterator.foldLeft(MutLinkedHashSet(Map.empty[TermSymbol, Shape])):
      case (alternatives, (field, fieldShape)) =>
        val extended = MutLinkedHashSet.empty[Map[TermSymbol, Shape]]
        for alternative <- alternatives; concrete <- fieldShape.flattenShape do
          extended += alternative.updated(field, concrete)
        extended
    alternatives.iterator.map(ClassShape(ctor, _)).toList

  def containsUnion: Bool = fields.valuesIterator.exists(_.containsUnion)

private case class TupleShape(length: Int, elements: List[Shape]) extends Shape:
  require(elements.length === length)
  override lazy val show: Str =
    if elements.isEmpty then ClassTagsDebug.showCtor(length)
    else s"${ClassTagsDebug.showCtor(length)}${elements.map(_.show).mkString("(", ", ", ")")}"

  override lazy val hashCode: Int = scala.util.hashing.MurmurHash3.productHash(this)

  def flattenShape: List[Shape] =
    // * prefixes are built reversed and flipped once at the end
    val alternatives = elements.foldLeft(MutLinkedHashSet(List.empty[Shape])):
      case (alternatives, element) =>
        val extended = MutLinkedHashSet.empty[List[Shape]]
        for alternative <- alternatives; concrete <- element.flattenShape do
          extended += concrete :: alternative
        extended
    alternatives.iterator.map(prefix => TupleShape(length, prefix.reverse)).toList

  def containsUnion: Bool = elements.exists(_.containsUnion)

private case class UnionShape(subshapes: List[Shape]) extends Shape:
  override lazy val show: Str = subshapes.map(_.show).mkString("(", " | ", ")")

  override lazy val hashCode: Int = scala.util.hashing.MurmurHash3.productHash(this)

  def flattenShape: List[Shape] =
    val alternatives = MutLinkedHashSet.empty[Shape]
    for subshape <- subshapes do alternatives ++= subshape.flattenShape
    alternatives.toList

  def containsUnion: Bool = true

private object UnionShape:
  /** A union keeps at most this many top-level members of the same class apart; past
    * that they decay -- see `decayWideClasses`. */
  val classDecayThreshold: Int = 16

  /** Members are put in a canonical order so that unions with the same members are the
    * same shape. `rank` must be a stable total order that does not render the shape:
    * `show` on a wide shape can be large enough to exhaust the heap on its own.
    */
  def mkUnion(shapes: Iterable[Shape], rank: Shape => Int): Shape =
    val normalized = MutLinkedHashSet.empty[Shape]
    shapes.iterator.foreach:
      case UnionShape(subshapes) if subshapes.nonEmpty => normalized ++= subshapes
      case shape => normalized += shape
    val members = decayWideClasses(normalized)
    if members.isEmpty then DynamicShape
    else if members.size === 1 then members.head
    else UnionShape(members.toList.sortBy(rank))

  /** Collapse the members of every class that outgrew `classDecayThreshold` into a
    * single `C(_, ..., _)`: a scrutinee that gains one member per loop iteration would
    * otherwise give every one of them its own tag and its own candidate test. Members of
    * the other classes in the same union are left alone. `mkShapeSet` in `ShapeSet.mls`
    * bounds the specializer's unions the same way, so that what this pass computes still
    * satisfies the patterns printed from there. */
  private def decayWideClasses(members: MutLinkedHashSet[Shape]): MutLinkedHashSet[Shape] =
    val classMembers = members.toList.collect { case shape: ClassShape => shape }
    val wide = classMembers.groupBy(_.ctor).collect {
      case (ctor, group) if group.size > classDecayThreshold => ctor -> decayedShape(group)
    }
    if wide.isEmpty then members
    else
      val result = MutLinkedHashSet.empty[Shape]
      members.iterator.foreach:
        case shape: ClassShape if wide.contains(shape.ctor) => result += wide(shape.ctor)
        case shape => result += shape
      result

  /** What a decayed class stands for: the class is still known, its fields are not. The
    * field set comes from the widest member, since a shape built from a constructor's
    * `val` definitions need not mention every field. */
  private def decayedShape(group: List[ClassShape]): Shape =
    val widest = group.maxBy(_.fields.size)
    ClassShape(widest.ctor, widest.fields.map((field, _) => field -> (DynamicShape: Shape)))

private object DynamicShape extends Shape:
  def show: Str = "_"

  def flattenShape: List[Shape] = this :: Nil

  def containsUnion: Bool = false

/** Propagates shapes starting from the public members only -- the module's functions,
  * its classes' methods and their constructors -- and reaches everything else through
  * its call sites, so a private specialization is analysed with the argument shapes it
  * is actually called with. Walking each specialization on its own with unknown
  * parameters instead makes every shape collapse to `C(_, _)`, which no precise
  * `@matchShapes` pattern can ever accept.
  *
  * Recursion is cut per function rather than per argument shape: re-entering a function
  * already being analysed yields a dynamic shape. That bounds the depth of a shape by
  * the depth of the call graph, which is what keeps this from growing without limit.
  *
  * Tags are allocated on the fly, as each new class shape turns up at a construction.
  *
  * This has to agree with the specializer's own propagation (`prop` in
  * `SpecializeHelpers.mls`), because the tags come from here while the `@matchShapes`
  * patterns the tags are checked against come from there: a shape this pass computes
  * less precisely than the specializer did is a pattern no tag can satisfy. So `Ctx`,
  * `sop`, `Shape.filter`/`Shape.rest` and `ownerSelfShape` all have a counterpart below,
  * and the one place the two are deliberately allowed to differ is precision in our
  * favour -- a shape finer than the specializer's still satisfies its patterns.
  */
private class ShapeProp(
  funDefns: collection.Map[TermSymbol, FunDefn],
  patternShapesByCall: collection.Map[ResultId, List[Shape]],
)(using FlowAnalysis.State, State, Elaborator.Ctx):
  private type Ctx = Map[Path, Shape]

  val shapeByResultId: MutMap[ResultId, Shape] = MutMap.empty
  val shapeTags: MutMap[Shape, Int] = MutMap.empty

  private val shapeRanks = MutMap.empty[Shape, Int]
  def rank(shape: Shape): Int = shapeRanks.getOrElseUpdate(shape, shapeRanks.size)

  /** One entry per function: its result shape. Keyed on the function alone, which both
    * terminates and loses nothing -- a specialization has fixed parameter shapes, so
    * however it is re-entered the shapes are the same. */
  private val returnShapes = MutMap.empty[TermSymbol, Shape]
  private val entryPoints = ListBuffer.empty[FunDefn]
  /** Direct superclass of each class-like definition, so that a `Case.Cls` test can be
    * decided the way `Shape.isSubClassOf` decides it at run time. */
  private val superClasses = MutMap.empty[ClassLikeSymbol, ClassLikeSymbol]

  def applyProgram(program: Program): Unit =
    collectEntryPoints(program.main)
    for fun <- entryPoints.toList do
      val args = fun.params.flatMap(_.params).map(_ => DynamicShape: Shape)
      analyze(fun, selfShapeOf(fun), args)
    // * the top level itself, for constructions sitting directly in it
    walk(program.main, Map.empty)
    ()

  /** The receiver of a method is always an instance of the owner class; only its fields
    * are unknown. Decaying it all the way to a dynamic shape would lose that, and the
    * specializer does not either -- see `ownerSelfShape` in `SpecializeHelpers`. */
  private def selfShapeOf(fun: FunDefn): Shape = fun.owner match
    case S(cls: ClassSymbol) => silhouetteOf(cls)
    case _ => DynamicShape

  /** `Shape.silh` for a class: the class is known, its fields are not. */
  private def silhouetteOf(cls: ClassLikeSymbol): Shape = classFields(cls) match
    case S(fields) => ClassShape(cls, fields.map(_ -> (DynamicShape: Shape)).toMap)
    case N => DynamicShape

  private def silhouetteOf(cse: Case): Shape = cse match
    case Case.Lit(lit) => LitShape(Value.Lit(lit))
    case Case.Cls(cls, _) => silhouetteOf(cls)
    case Case.Tup(len, _) => TupleShape(len, List.fill(len)(DynamicShape))

  private def isSubclassOf(sub: ClassLikeSymbol, sup: ClassLikeSymbol): Bool =
    sub === sup || superClasses.get(sub).exists(isSubclassOf(_, sup))

  /** The public surface: anything a caller outside this module could reach. */
  private def collectEntryPoints(block: Block): Unit = block match
    case Define(defn, rest) =>
      defn match
        case defn: ClsLikeDefn =>
          val parent = defn.parentPath.flatMap(_.targetSymbol).flatMap(_.asClsLike).collect:
            case parent: ClassLikeSymbol => parent
          (defn.isym, parent) match
            case (cls: ClassLikeSymbol, S(parent)) => superClasses(cls) = parent
            case _ => ()
          entryPoints ++= defn.methods
          defn.companion.foreach(companion => entryPoints ++= companion.methods)
          collectEntryPoints(defn.preCtor)
          collectEntryPoints(defn.ctor)
          defn.companion.foreach(companion => collectEntryPoints(companion.ctor))
        case _ => ()
      collectEntryPoints(rest)
    case Match(_, arms, dflt, rest) =>
      arms.foreach((_, body) => collectEntryPoints(body))
      dflt.foreach(collectEntryPoints)
      collectEntryPoints(rest)
    case Begin(sub, rest) => collectEntryPoints(sub); collectEntryPoints(rest)
    case TryBlock(sub, finallyDo, rest) =>
      collectEntryPoints(sub); collectEntryPoints(finallyDo); collectEntryPoints(rest)
    case Label(_, _, body, rest) => collectEntryPoints(body); collectEntryPoints(rest)
    case Scoped(_, body) => collectEntryPoints(body)
    case Assign(_, _, rest) => collectEntryPoints(rest)
    case AssignField(_, _, _, rest) => collectEntryPoints(rest)
    case AssignDynField(_, _, _, _, rest) => collectEntryPoints(rest)
    case _ => ()

  private def unionOf(shapes: List[Shape]): Shape =
    if shapes.isEmpty || shapes.contains(DynamicShape) then DynamicShape
    else UnionShape.mkUnion(shapes, rank)

  /** The alternatives a shape stands for, as `ShapeSet.values` gives them: the top-level
    * members only, with a field's own union left alone. An empty list is the bottom
    * shape, which is how a branch the scrutinee can never take is told apart from one
    * whose scrutinee is merely unknown. */
  private def alternativesOf(shape: Shape): List[Shape] = shape match
    case UnionShape(subshapes) => subshapes
    case shape => shape :: Nil

  /** `Shape.filter`: the alternatives `cse` can match. */
  private def filterSet(set: List[Shape], cse: Case): List[Shape] = set.flatMap: alt =>
    (alt, cse) match
      case (LitShape(Value.Lit(left)), Case.Lit(right)) =>
        if left === right then alt :: Nil else Nil
      case (TupleShape(length, _), Case.Tup(len, _)) =>
        if length === len then alt :: Nil else Nil
      case (ClassShape(cls, _), Case.Cls(scrutCls, _)) =>
        if isSubclassOf(cls, scrutCls) then alt :: Nil else Nil
      case (DynamicShape, _) => silhouetteOf(cse) :: Nil
      // * a literal tested against a class is a primitive-type test, which we cannot
      // * decide from the symbol alone, so the alternative is kept
      case (LitShape(_), Case.Cls(_, _)) => alt :: Nil
      case _ => Nil

  /** `Shape.rest`: the alternatives left over once `cse` has taken the ones it matches. */
  private def restSet(set: List[Shape], cse: Case): List[Shape] = set.flatMap: alt =>
    (alt, cse) match
      case (LitShape(Value.Lit(left)), Case.Lit(right)) =>
        if left === right then Nil else alt :: Nil
      case (TupleShape(length, _), Case.Tup(len, _)) =>
        if length === len then Nil else alt :: Nil
      case (ClassShape(cls, _), Case.Cls(scrutCls, _)) =>
        if isSubclassOf(cls, scrutCls) then Nil else alt :: Nil
      case _ => alt :: Nil

  /** The part of `shape` a `@matchShapes` pattern accepts, or nothing when the pattern
    * rules it out entirely. This is `filterSet` against a shape rather than a `Case`:
    * fields are narrowed by the pattern's fields instead of being replaced by them. */
  private def narrowToPattern(shape: Shape, pattern: Shape): Opt[Shape] = pattern match
    case DynamicShape => S(shape)
    case _ => shape match
      case UnionShape(subshapes) =>
        val kept = subshapes.flatMap(narrowToPattern(_, pattern))
        if kept.isEmpty then N else S(unionOf(kept))
      case DynamicShape => N // * nothing is known, so there is nothing to narrow
      case LitShape(_) => if shape <= pattern then S(shape) else N
      case ClassShape(cls, fields) => pattern match
        case ClassShape(patternCls, patternFields)
            if cls === patternCls && fields.keySet === patternFields.keySet =>
          val narrowed = fields.toList.map: (field, fieldShape) =>
            field -> narrowToPattern(fieldShape, patternFields(field))
          if narrowed.exists(_._2.isEmpty) then N
          else S(ClassShape(cls, narrowed.map((field, kept) => field -> kept.get).toMap))
        case _ => N
      case TupleShape(length, elements) => pattern match
        case TupleShape(patternLength, patternElements) if length === patternLength =>
          val narrowed = elements.zip(patternElements).map(narrowToPattern)
          if narrowed.exists(_.isEmpty) then N
          else S(TupleShape(length, narrowed.map(_.get)))
        case _ => N

  private def analyze(fun: FunDefn, selfShape: Shape, argShapes: List[Shape]): Shape =
    val key = fun.dSym
    returnShapes.get(key) match
      // * Already analysed, or being analysed: reuse the result and leave the body
      // * alone. The dummy entry below is what a recursive call reads.
      case S(shape) => shape
      case N =>
        returnShapes(key) = DynamicShape
        val params = fun.params.flatMap(_.params)
        val bound = params.zip(argShapes).foldLeft(Map.empty[Path, Shape]):
          (acc, entry) => acc.updated(Value.SimpleRef(entry._1.sym), entry._2)
        val ctx = fun.owner match
          case S(owner) => bound.updated(Value.This(owner), selfShape)
          case N => bound
        val returned = unionOf(walk(fun.body, ctx)._1)
        returnShapes(key) = returned
        returned

  private def resolveCallee(fun: Path): Opt[FunDefn] =
    fun.targetSymbol.collect:
      case sym: TermSymbol => sym
    .flatMap(funDefns.get)

  private def classFields(cls: ClassLikeSymbol): Opt[List[TermSymbol]] =
    ClassTagsTransformer.classFields(cls)

  /** Fields are matched by name: the symbol on a selection is not necessarily the one
    * the class tree carries for that parameter. */
  private def projectField(shape: Shape, name: Str): Shape = shape match
    case ClassShape(_, fields) =>
      fields.collectFirst:
        case (field, fieldShape) if field.nme === name => fieldShape
      .getOrElse(DynamicShape)
    case UnionShape(subshapes) => unionOf(subshapes.map(projectField(_, name)))
    case _ => DynamicShape

  /** A reference that denotes an instance rather than a class: an object or module, or a
    * class with no parameter list at all, which `sop` also reads as an instance. */
  private def classValueOf(path: Path): Opt[ClassLikeSymbol] = path.targetSymbol match
    case S(target) => target.asClsLike match
      case S(obj: ModuleOrObjectSymbol) => S(obj)
      case S(cls: ClassSymbol) if cls.tree.clsParams.isEmpty => S(cls)
      case _ => N
    case N => N

  /** The length of a tuple shape, when every alternative agrees on it. */
  private def tupleLength(shape: Shape): Opt[Int] =
    val alternatives = alternativesOf(shape)
    val lengths = alternatives.collect:
      case TupleShape(length, _) => length
    .distinct
    if lengths.sizeCompare(1) === 0 && alternatives.sizeCompare(alternatives.count {
      case TupleShape(_, _) => true
      case _ => false
    }) === 0 then S(lengths.head) else N

  private def projectIndex(shape: Shape, index: Int): Shape = shape match
    case TupleShape(_, elements) => elements.lift(index).getOrElse(DynamicShape)
    case UnionShape(subshapes) => unionOf(subshapes.map(projectIndex(_, index)))
    case _ => DynamicShape

  private def shapeOfPath(path: Path, ctx: Ctx): Shape =
    ctx.get(path).orElse(classValueOf(path).map(ClassShape(_, Map.empty))) match
      case S(shape) => shape
      case N => path match
        case lit: Value.Lit => LitShape(lit)
        // * `sop` special-cases `.length` on an array shape
        case sel: Select if sel.name.name === "length" =>
          tupleLength(shapeOfPath(sel.qual, ctx)) match
            case S(length) => LitShape(Value.Lit(syntax.Tree.IntLit(length)))
            case N => DynamicShape
        case sel: Select => projectField(shapeOfPath(sel.qual, ctx), sel.name.name)
        case DynSelect(qual, fld, _) =>
          shapeOfPath(fld, ctx) match
            case LitShape(Value.Lit(syntax.Tree.IntLit(index))) =>
              projectIndex(shapeOfPath(qual, ctx), index.toInt)
            case LitShape(Value.Lit(syntax.Tree.StrLit(name))) =>
              projectField(shapeOfPath(qual, ctx), name)
            case _ => DynamicShape
        case Cast(value: Path, _, _) => shapeOfPath(value, ctx)
        case _ => DynamicShape

  private def shapeOfResult(result: Result, ctx: Ctx): Shape = result match
    case CtorProducer(ctor, args, _) =>
      val argShapes = args.map(arg => shapeOfPath(arg.value, ctx))
      ctor match
        case cls: ClassLikeSymbol =>
          classFields(cls) match
            case S(fields) if fields.size === argShapes.size =>
              ClassShape(cls, fields.zip(argShapes).toMap)
            case _ => DynamicShape
        case length: Int =>
          if argShapes.size === length then TupleShape(length, argShapes) else DynamicShape
    case path: Path => shapeOfPath(path, ctx)
    // * A `shape.match` is a pattern match, not an opaque call: each branch is analysed
    // * on its own with the scrutinee refined to that branch's pattern, and the branch
    // * results are unioned. Treating it as opaque made its result dynamic, which is
    // * self-defeating -- the shapes it needs are the ones computed through it.
    case call @ Call(fun, (Arg(N, scrutinee) :: branchArgs) :: Nil)
        if branchArgs.nonEmpty && ClassTagsTransformer.isShapeMatch(fun) =>
      val patternShapes = patternShapesByCall.getOrElse(call.uid, Nil)
      // * The branches were generated from the scrutinee's own flattened shapes, so the
      // * refinement is a *filter* on what we already know, not a replacement by the
      // * pattern. A pattern prints a field as `_` whenever the branches share it, so
      // * taking the pattern as the branch shape silently drops that field -- and then
      // * no construction is ever tagged precisely enough for the pattern to accept it.
      val known = shapeOfPath(scrutinee, ctx)
      val branchResults = branchArgs.zipWithIndex.flatMap: entry =>
        resolveCallee(entry._1.value) match
          case S(branch) =>
            val refined = patternShapes.lift(entry._2) match
              case S(patternShape) =>
                // * nothing known satisfies the pattern: fall back to the pattern itself,
                // * which is what `Shape.filter` does for a dynamic scrutinee
                ctx.updated(scrutinee, narrowToPattern(known, patternShape).getOrElse(patternShape))
              case N => ctx
            walk(branch.body, refined)._1
          case N => DynamicShape :: Nil
      unionOf(branchResults)
    case Call(fun, argss) =>
      // * follow the call, so a specialization is seen with the shapes it is given
      resolveCallee(fun) match
        case S(callee) =>
          val argShapes = argss.toList.flatten.map(arg => shapeOfPath(arg.value, ctx))
          val selfShape = fun match
            case sel: Select => shapeOfPath(sel.qual, ctx)
            case _ => DynamicShape
          analyze(callee, selfShape, argShapes)
        case N => DynamicShape
    case _ => DynamicShape

  /** Combines what two analyses of the same construction found. A dynamic shape is
    * dropped rather than swallowing the other: it means that pass learnt nothing here,
    * and at run time a value matching no candidate is simply left untagged. */
  private def mergeSiteShape(previous: Shape, next: Shape): Shape =
    if previous === next then previous
    else if previous === DynamicShape then next
    else if next === DynamicShape then previous
    else UnionShape.mkUnion(previous :: next :: Nil, rank)

  private def noteResult(result: Result, ctx: Ctx): Shape =
    val shape = shapeOfResult(result, ctx)
    result match
      case CtorProducer(_, _, _) =>
        val merged = shapeByResultId.get(result.uid) match
          case S(previous) => mergeSiteShape(previous, shape)
          case N => shape
        shapeByResultId(result.uid) = merged
        // * allocate as we go: every new class shape gets its own tag
        for concrete <- merged.flattenShape do concrete match
          case concrete: ClassShape => shapeTags.getOrElseUpdate(concrete, shapeTags.size)
          case _ => ()
      case _ => ()
    shape

  private def refine(scrut: Path, set: List[Shape], ctx: Ctx): Ctx =
    ctx.updated(scrut, unionOf(set))

  /** What the branches of a match taught us about variables the enclosing block had not
    * bound yet, as `Ctx.sub` and `mergeAssigned` carry it out of a branch: a variable a
    * branch assigns is visible to the code after the match, as the union over the
    * branches that assign it. Only bindings the enclosing context does not already have
    * are taken, which is what `otherIsBot` tests for there. */
  private def mergeBranches(ctx: Ctx, branches: List[Ctx]): Ctx =
    val learnt = MutMap.empty[Path, List[Shape]]
    for branch <- branches; (path, shape) <- branch if !ctx.contains(path) do
      learnt(path) = learnt.getOrElse(path, Nil) :+ shape
    learnt.foldLeft(ctx)((acc, entry) => acc.updated(entry._1, unionOf(entry._2.distinct)))

  /** Returns the shapes this block can return, and the context the block ends in. */
  private def walk(block: Block, ctx: Ctx): (List[Shape], Ctx) = block match
    case Assign(lhs, rhs, rest) =>
      val shape = noteResult(rhs, ctx)
      val next = lhs match
        case sym: LocalVarSymbol => ctx.updated(Value.SimpleRef(sym), shape)
        case _ => ctx
      walk(rest, next)
    case AssignField(_, _, rhs, rest) => noteResult(rhs, ctx); walk(rest, ctx)
    case AssignDynField(_, _, _, rhs, rest) => noteResult(rhs, ctx); walk(rest, ctx)
    case Define(defn, rest) =>
      defn match
        // * a nested function is analysed from its call sites, not here
        case defn: ValDefn => noteResult(defn.rhs, ctx)
        case _ => ()
      walk(rest, ctx)
    case Match(scrut, arms, dflt, rest) =>
      // * `prop` narrows the scrutinee arm by arm and skips the arms whose share of it is
      // * empty, so a branch that cannot be taken contributes no constructions and the
      // * default sees only what the arms left behind.
      val armShapes = ListBuffer.empty[Shape]
      val branchCtxs = ListBuffer.empty[Ctx]
      var remaining = alternativesOf(shapeOfPath(scrut, ctx))
      for (cse, body) <- arms do
        val matching = filterSet(remaining, cse)
        if matching.nonEmpty then
          val (shapes, branchCtx) = walk(body, refine(scrut, matching, ctx))
          armShapes ++= shapes
          branchCtxs += branchCtx
          remaining = restSet(remaining, cse)
      val dfltShapes = ListBuffer.empty[Shape]
      if remaining.nonEmpty then
        for body <- dflt do
          val (shapes, branchCtx) = walk(body, refine(scrut, remaining, ctx))
          dfltShapes ++= shapes
          branchCtxs += branchCtx
      val (restShapes, restCtx) = walk(rest, mergeBranches(ctx, branchCtxs.toList))
      (armShapes.toList ++ dfltShapes.toList ++ restShapes, restCtx)
    case Begin(sub, rest) =>
      val (subShapes, subCtx) = walk(sub, ctx)
      val (restShapes, restCtx) = walk(rest, subCtx)
      (subShapes ++ restShapes, restCtx)
    case TryBlock(sub, finallyDo, rest) =>
      val (subShapes, _) = walk(sub, ctx)
      val (finallyShapes, _) = walk(finallyDo, ctx)
      val (restShapes, restCtx) = walk(rest, ctx)
      (subShapes ++ finallyShapes ++ restShapes, restCtx)
    // * the body of a loop may run any number of times, so what it binds does not reach
    // * the code after it
    case Label(_, _, body, rest) =>
      val (bodyShapes, _) = walk(body, ctx)
      val (restShapes, restCtx) = walk(rest, ctx)
      (bodyShapes ++ restShapes, restCtx)
    case Scoped(_, body) => walk(body, ctx)
    case Return(res) => (noteResult(res, ctx) :: Nil, ctx)
    case Throw(exc) => noteResult(exc, ctx); (Nil, ctx)
    case _ => (Nil, ctx)
end ShapeProp


class ClassTagsTransformer private (
  taggedResultIds: Set[ResultId],
  shapeByResultId: collection.Map[ResultId, Shape],
  funSymToFunDefn: collection.Map[TermSymbol, FunDefn],
  shapeTags: collection.Map[Shape, Int],
  patternShapesByCall: collection.Map[ResultId, List[Shape]],
  debug: Bool,
)(using State, Elaborator.Ctx, TL, Raise, FlowAnalysis.State) extends BlockTransformer(SymbolSubst.Id):

  private val tagField = new syntax.Tree.Ident("__tag$")

  private enum ShapeMatchScope:
    case TopLevel, SupportedFunction, NestedFunction, ClassValue

  private def isShapeMatch(path: Path): Bool =
    ClassTagsTransformer.isShapeMatch(path)

  private def rejectUnsupportedShapeMatches(program: Program): Unit =
    class Checker(val scope: ShapeMatchScope) extends BlockTraverser:
      override def applyResult(result: Result): Unit =
        result match
          case call @ Call(fun, _) if isShapeMatch(fun) =>
            val errorMessage = scope match
              case ShapeMatchScope.TopLevel =>
                S(msg"shape.match is not supported at the top level.")
              case ShapeMatchScope.NestedFunction =>
                S(msg"shape.match is not supported in nested functions.")
              case ShapeMatchScope.ClassValue =>
                S(msg"shape.match is not supported in class value initializers.")
              case ShapeMatchScope.SupportedFunction => N
            errorMessage.foreach: message =>
              summon[Raise].apply(ErrorReport(
                message -> call.toLoc :: Nil,
                source = Diagnostic.Source.Compilation,
              ))
          case _ => ()
        super.applyResult(result)

      override def applyFunDefn(fun: FunDefn): Unit =
        // Anonymous functions originate from lambdas and are lifted later.
        val functionScope =
          if !fun.sym.nameIsMeaningful then ShapeMatchScope.SupportedFunction
          else scope match
            case ShapeMatchScope.TopLevel => ShapeMatchScope.SupportedFunction
            case _ => ShapeMatchScope.NestedFunction
        new Checker(functionScope).applyBlock(fun.body)

      override def applyClsLikeDefn(defn: ClsLikeDefn): Unit =
        defn.parentPath.foreach(applyPath)
        defn.methods.foreach: method =>
          new Checker(ShapeMatchScope.SupportedFunction).applyBlock(method.body)
        new Checker(ShapeMatchScope.ClassValue).applyBlock(defn.preCtor)
        new Checker(ShapeMatchScope.ClassValue).applyBlock(defn.ctor)
        defn.companion.foreach: companion =>
          companion.methods.foreach: method =>
            new Checker(ShapeMatchScope.SupportedFunction).applyBlock(method.body)
          new Checker(ShapeMatchScope.ClassValue).applyBlock(companion.ctor)

    new Checker(ShapeMatchScope.TopLevel).applyBlock(program.main)

  private lazy val taggedShapes: List[ClassShape -> Int] =
    val entries = shapeTags.toList.collect:
      case (shape: ClassShape, tag) => shape -> tag
    if debug then
      for (shape, tag) <- entries.sortBy(_._2) do
        summon[TL].emitDbg(s"class-tags transform-phase > allocated tag $tag for ${shape.show}")
    // * most precise first, so a construction matching several takes the precise one
    entries.sortBy(_._2).foldLeft(List.empty[ClassShape -> Int]): (acc, entry) =>
      val split = acc.span(other => !strictlyBelow(entry._1, other._1))
      split._1 ++ (entry :: split._2)

  private def strictlyBelow(left: Shape, right: Shape): Bool =
    left <= right && !(right <= left)

  private lazy val tagOfShape: Map[Shape, Int] =
    taggedShapes.iterator.map((shape, tag) => (shape: Shape) -> tag).toMap

  private def bindResult(result: Result)(k: Path => Block): Block = result match
    case path: Path => k(path)
    case result =>
      val symbol = new TempSymbol(N, erasedType = result.erasedValueType, "tmp")
      val reference = symbol.asSimpleRef.withLocOf(result)
      Scoped(Set.single(symbol), Assign(symbol, result, k(reference)))

  private def assignTag(instance: Path, tag: Int)(next: Block): Block = // TODO: make __tag$ a real field and fill the symbol for selections
    AssignField(instance, tagField, Value.Lit(syntax.Tree.IntLit(tag)), next)(N)

  private def mkEquals(left: Path, right: Path)(k: Path => Block): Block =
    bindResult(Call(State.builtinOpsMap("===").asSimpleRef,
      (left.asArg :: right.asArg :: Nil) ne_:: Nil)(CallMetadata.defaultMlsFun))(k)

  private def mkAnd(left: Path, right: Path)(k: Path => Block): Block =
    bindResult(Call(State.builtinOpsMap("&&").asSimpleRef,
      (left.asArg :: right.asArg :: Nil) ne_:: Nil)(CallMetadata.defaultMlsFun))(k)

  /** The runtime test for one value against one shape. A shape that carries a tag is
    * tested by comparing tags, which is what makes the test recursive without being
    * structural: the argument was itself tagged when it was built. */
  private def mkShapeCheck(argument: Path, shape: Shape)(k: Path => Block): Block =
    tagOfShape.get(shape) match
      case S(tag) => mkEquals(
        Select(argument, tagField)(N)(false).withLocOf(argument),
        Value.Lit(syntax.Tree.IntLit(tag)),
      )(k)
      case N => shape match
        case LitShape(lit) => mkEquals(argument, lit)(k)
        case _ => k(Value.Lit(syntax.Tree.BoolLit(true)))

  /** Conjunction of leaf checks. Every check is evaluated and the results are and-ed
    * together: no short-circuiting. */
  private def mkChecks(checks: List[Path -> Shape], acc: Opt[Path])(k: Path => Block): Block =
    checks match
      case Nil => k(acc.getOrElse(Value.Lit(syntax.Tree.BoolLit(true))))
      case (argument, shape) :: rest =>
        mkShapeCheck(argument, shape): condition =>
          acc match
            case N => mkChecks(rest, S(condition))(k)
            case S(previous) =>
              mkAnd(previous, condition): combined =>
                mkChecks(rest, S(combined))(k)

  /** The leaf checks `expected` calls for, each paired with what the candidates still in
    * the waitlist have at that same position so that one check can be weighed against
    * all of them at once. A field nothing is known about calls for no check, and a tuple
    * is tested element-wise -- a non-tuple fails those element tests anyway. */
  private def leafChecks(
    argument: Path, expected: Shape, waitlist: IndexedSeq[Shape],
  ): List[(Path, Shape, IndexedSeq[Shape])] = expected match
    case DynamicShape => Nil
    case TupleShape(_, elements) =>
      elements.zipWithIndex.flatMap: (element, index) =>
        val elementPath = DynSelect(argument, Value.Lit(syntax.Tree.IntLit(index)), true)
          .withLocOf(argument)
        val elementWaitlist = waitlist.map:
          case TupleShape(_, otherElements) => otherElements.lift(index).getOrElse(DynamicShape)
          case _ => DynamicShape
        leafChecks(elementPath, element, elementWaitlist)
    case _ => (argument, expected, waitlist) :: Nil

  /** Whether the check this pass emits for `expected` must fail for a value whose shape
    * is `actual`. Used only to drop checks, so it answers conservatively: `false` keeps a
    * candidate in the waitlist, and therefore keeps the check that would have ruled it
    * out. */
  private def testExcludes(expected: Shape, actual: Shape): Bool =
    if expected === actual then false
    else tagOfShape.get(expected) match
      // * the check is `argument.__tag$ === tag`, which any other tagged shape fails
      case S(tag) => tagOfShape.get(actual).exists(_ =/= tag)
      case N => (expected, actual) match
        // * the check is `argument === lit`
        case (LitShape(left), LitShape(right)) => left =/= right
        case _ => false

  /** The checks that tell `shape` apart from the candidates still in the waitlist, rather
    * than every check its fields call for. A value reaching this construction is one of
    * the candidates, so a check only has to discriminate: two candidates whose field is
    * `C(..)` and `D(..)` are told apart by that one field whatever it holds, and once a
    * check has ruled out the last of the others the remaining fields decide nothing.
    *
    * When a candidate survives every check -- which happens when one candidate's shape
    * subsumes another's -- the full conjunction is kept instead, so that the test still
    * validates rather than merely discriminating. */
  private def mkDiscriminatingChecks(
    arguments: List[TermSymbol -> Path], shape: ClassShape, waitlist: List[ClassShape],
  ): List[Path -> Shape] =
    def fieldOf(candidate: ClassShape, field: TermSymbol): Shape =
      candidate.fields.getOrElse(field, DynamicShape)
    val leaves = arguments.flatMap: (field, argument) =>
      leafChecks(argument, fieldOf(shape, field), waitlist.map(fieldOf(_, field)).toIndexedSeq)
    val chosen = ListBuffer.empty[Path -> Shape]
    var surviving = waitlist.indices.toSet
    for (argument, expected, others) <- leaves if surviving.nonEmpty do
      val excluded = surviving.filter(index => testExcludes(expected, others(index)))
      if excluded.nonEmpty then
        chosen += argument -> expected
        surviving --= excluded
    if surviving.isEmpty then chosen.toList
    else leaves.map((argument, expected, _) => argument -> expected)

  /** The condition for each candidate in turn, all bound before any of them is read.
    * Keeping them out of the branches below is what makes the generated test flat: a
    * candidate's checks no longer sit under the previous candidate's `else`. The last
    * candidate gets no condition at all -- it is the only one left in the waitlist by
    * then, so there is nothing to tell it apart from. */
  private def mkCandidateConditions(
    arguments: List[TermSymbol -> Path],
    candidates: List[ClassShape -> Int],
    acc: List[Opt[Path] -> Int],
  )(k: List[Opt[Path] -> Int] => Block): Block =
    candidates match
      case Nil => k(acc.reverse)
      case (_, tag) :: Nil => k((((N: Opt[Path]) -> tag) :: acc).reverse)
      case (shape, tag) :: remaining =>
        // * only the candidates still to come: the earlier ones have already been ruled
        // * out by their own failed conditions
        val checks = mkDiscriminatingChecks(arguments, shape, remaining.map(_._1))
        mkChecks(checks, N): condition =>
          mkCandidateConditions(arguments, remaining, (S(condition) -> tag) :: acc)(k)

  /** Try each candidate in turn, most precise first, and write the tag of the first one
    * whose test passes. Only the choice of tag is conditional: every test has already
    * been evaluated, so nothing is computed inside a branch. */
  private def mkTagChoice(
    instance: Path, arguments: List[TermSymbol -> Path], candidates: List[ClassShape -> Int],
  ): Block =
    def choose(conditions: List[Opt[Path] -> Int]): Block = conditions match
      case Nil => End()
      case (N, tag) :: _ => assignTag(instance, tag)(End())
      case (S(condition), tag) :: remaining =>
        new Match(
          condition,
          Case.Lit(syntax.Tree.BoolLit(true)) -> assignTag(instance, tag)(End()) :: Nil,
          if remaining.isEmpty then N else S(choose(remaining)),
          End(),
        )
    mkCandidateConditions(arguments, candidates, Nil)(choose)

  private def insertShapeTag(result: Result)(k: Path => Block): Block =
    result match
      case CtorProducer(ctor, args, _) =>
        // * the shapes this very construction was found to take, in tag order
        val candidates = shapeByResultId.get(result.uid).toList.flatMap: shape =>
          shape.flattenShape.collect:
            case concrete: ClassShape => concrete
        .flatMap(shape => tagOfShape.get(shape).map(shape -> _)).distinct
        candidates match
          case Nil =>
            if debug then summon[TL].emitDbg(s"class-tags transform-phase > no tag for ${
              ClassTagsDebug.showCtor(ctor)}@${result.uid} of shape ${
              shapeByResultId.get(result.uid).fold("<unknown>")(_.show.take(160))}")
            bindResult(result)(k)
          // * a single shape needs no test at all
          case (_, tag) :: Nil =>
            bindResult(result): instance =>
              assignTag(instance, tag)(k(instance))
          case _ =>
            val ordered = candidates.foldLeft(List.empty[ClassShape -> Int]): (acc, entry) =>
              val split = acc.span(other => !strictlyBelow(entry._1, other._1))
              split._1 ++ (entry :: split._2)
            val fields = ClassTagsTransformer.classFields(ordered.head._1.ctor) match
              case S(fields) if fields.size === args.size => fields.zip(args.map(_.value))
              case _ => Nil
            bindResult(result): instance =>
              Begin(mkTagChoice(instance, fields, ordered), k(instance))
      case _ => bindResult(result)(k)

  override def applyProgram(program: Program): Program =
    if debug then
      summon[TL].emitDbg(">>> start class-tags transform-phase")
    rejectUnsupportedShapeMatches(program)
    // * allocate every tag before any of them is looked up
    if tagOfShape.isEmpty && debug then
      summon[TL].emitDbg("class-tags transform-phase > no shapes to tag")
    val result = super.applyProgram(program)
    if debug then
      summon[TL].emitDbg("<<< end class-tags transform-phase")
    result

  override def applyFunDefn(fun: FunDefn): FunDefn =
    val transformer = new BlockTransformerShallow(SymbolSubst.Id):
      override def applyBlock(block: Block): Block =
        block match
          // * Assigning a variable captured from an enclosing scope is allowed:
          // * `shape.match` branches are anonymous functions that get inlined into
          // * their enclosing function, so such an assignment stays local.
          case AssignField(lhs, _, _, _) =>
            summon[Raise].apply(ErrorReport(
              msg"Class tags do not support set operations yet." -> lhs.toLoc :: Nil,
              source = Diagnostic.Source.Compilation,
            ))
          case AssignDynField(lhs, _, _, _, _) =>
            summon[Raise].apply(ErrorReport(
              msg"Class tags do not support set operations yet." -> lhs.toLoc :: Nil,
              source = Diagnostic.Source.Compilation,
            ))
          case _ => ()
        super.applyBlock(block)

      // * get the branch body defined as a FunDefn
      private def getBranch(path: Path): Opt[FunDefn] =
        path.targetSymbol.collect:
          case symbol: TermSymbol => symbol
        .flatMap(funSymToFunDefn.get)

      // * Generate branch based on the branch function
      private def mkBranch(branch: FunDefn, resultSymbol: TempSymbol): Block =
        SymbolRefresher(Map.empty).apply(applyFunBodyLikeBlock(branch.body)).mapReturn:
          case Return(result) => Assign(resultSymbol, result, End())

      private def rewriteShapeMatch(call: Call, scrutinee: Path, branchArgs: List[Arg])(k: Result => Block): Opt[Block] =
        val annotation = call.metadata.annotations.collectFirst:
          case Annot.MatchShapes(patterns) => patterns
        if annotation.isEmpty then
          summon[Raise].apply(ErrorReport(
            msg"shape.match must be annotated with @matchShapes." -> call.toLoc :: Nil,
            source = Diagnostic.Source.Compilation,
          ))
        annotation.flatMap: patterns =>
          val branches = branchArgs.map(arg => getBranch(arg.value))
          val malformedReasons =
            (if patterns.size =/= branchArgs.size then
              msg"The number of @matchShapes patterns (${patterns.size}) does not match the number of shape.match branches (${branchArgs.size})." -> call.toLoc :: Nil
            else Nil) ++
            branchArgs.zip(branches).collect:
              case (arg, N) =>
                msg"This shape.match branch does not resolve to a function." -> arg.value.toLoc
          if malformedReasons.nonEmpty then
            summon[Raise].apply(ErrorReport(
              msg"Malformed annotated shape.match call." -> call.toLoc :: malformedReasons,
              source = Diagnostic.Source.Compilation,
            ))
            N
          else
            val branchDefns = branches.flatten
            val namedBranches = branchArgs.zip(branchDefns).collect:
              case (arg, branch) if branch.sym.nameIsMeaningful => arg.value
            if namedBranches.nonEmpty then
              summon[Raise].apply(ErrorReport(
                msg"Annotated shape.match branches must be anonymous functions." -> call.toLoc ::
                namedBranches.map: branch =>
                  msg"This branch is a named function." -> branch.toLoc,
                source = Diagnostic.Source.Compilation,
              ))
              N
            else
              val branchesWithParams = branchArgs.zip(branchDefns).collect:
                case (arg, branch)
                    if branch.params.exists(paramList => paramList.params.nonEmpty || paramList.restParam.nonEmpty) =>
                  arg.value
              if branchesWithParams.nonEmpty then
                summon[Raise].apply(ErrorReport(
                  msg"Annotated shape.match branches must take no arguments." -> call.toLoc ::
                  branchesWithParams.map: branch =>
                    msg"This branch takes arguments." -> branch.toLoc,
                  source = Diagnostic.Source.Compilation,
                ))
                N
              else
                // * computed once up front, so the patterns are not elaborated twice
                val patternShapes = patternShapesByCall.getOrElse(call.uid, Nil)
                // * each tag goes to the first pattern that accepts it, matching the
                // * ordered semantics the specializer's branches assume
                val claimed = MutSet.empty[Int]
                val branchTags = patternShapes.map: patternShape =>
                  val tags = taggedShapes.collect:
                    case (shape, tag) if !claimed.contains(tag) && shape <= patternShape => tag
                  claimed ++= tags
                  tags
                if debug then
                  val shown = patternShapes.zip(branchTags).map: entry =>
                    val tags = if entry._2.isEmpty then "<none>" else entry._2.mkString("|")
                    s"${entry._1.show}->$tags"
                  summon[TL].emitDbg(
                    s"class-tags transform-phase > match shapes ${shown.mkString(", ")}")
                if patternShapes.exists(_.containsUnion) then
                  softAssert(false, "@matchShapes patterns must not contain union shapes.")
                  N
                else if branchTags.forall(_.isEmpty) then
                  summon[Raise].apply(ErrorReport(
                    msg"Annotated shape.match has no tagged class shapes for its scrutinee." -> call.toLoc :: Nil,
                    source = Diagnostic.Source.Compilation,
                  ))
                  N
                else
                  val resultSymbol = new TempSymbol(N, erasedType = call.erasedValueType, "shapeMatchResult")
                  val resultRef = resultSymbol.asSimpleRef.withLocOf(call)
                  val tagAccess = Select(scrutinee, tagField)(N)(false).withLocOf(scrutinee)
                  S(Scoped(Set.single(resultSymbol),
                    bindResult(tagAccess): tag =>
                      mkTagDispatch(tag, branchTags.zip(branchDefns), resultSymbol, k(resultRef))))

      /** Two matches rather than one: the tag picks a branch index, then the index
        * picks the body. A branch claiming several tags would otherwise have its body
        * duplicated once per tag. */
      private def mkTagDispatch(
        tag: Path, branches: List[(List[Int], FunDefn)], resultSymbol: TempSymbol, rest: Block,
      ): Block =
        val reachable = branches.zipWithIndex.filter(_._1._1.nonEmpty)
        val index = new TempSymbol(N, erasedType = S(ErasedType.Int), "shapeMatchBranch")
        val indexRef = index.asSimpleRef
        val arms = reachable.flatMap: entry =>
          entry._1._1.map: expected =>
            Case.Lit(syntax.Tree.IntLit(expected)) ->
              (Assign(index, Value.Lit(syntax.Tree.IntLit(entry._2)), End()): Block)
        val bodies = reachable.foldRight(
          Throw.error("Unhandled shape tag in shape.match"): Block
        ): (entry, otherwise) =>
          new Match(
            indexRef,
            Case.Lit(syntax.Tree.IntLit(entry._2)) -> mkBranch(entry._1._2, resultSymbol) :: Nil,
            S(otherwise),
            rest,
          )
        Scoped(Set.single(index), Begin(
          new Match(tag, arms, S(Assign(index, Value.Lit(syntax.Tree.IntLit(-1)), End())), End()),
          bodies))

      override def applyResult(result: Result)(k: Result => Block): Block =
        result match
          case call @ Call(fun, (Arg(N, scrutinee) :: branches) :: Nil) if branches.nonEmpty && isShapeMatch(fun) =>
            // Rewrite annotated shape.match calls
            rewriteShapeMatch(call, scrutinee, branches)(k).getOrElse:
              super.applyResult(result)(k)
          case CtorProducer(_, _, _) =>
            // Insert tags for instantiations
            // TODO: make the tag a real field?
            // * Only the constructions some web covers are tagged: a construction no
            // * `shape.match` can ever see has no tag to be given.
            if taggedResultIds.contains(result.uid) then
              super.applyResult(result): transformed =>
                insertShapeTag(transformed)(k)
            else
              if debug then summon[TL].emitDbg(
                s"class-tags transform-phase > outside every web: @${result.uid}")
              super.applyResult(result)(k)
          case _ => super.applyResult(result)(k)
    val body = transformer.applyFunBodyLikeBlock(fun.body)
    val transformed =
      if body is fun.body then fun
      else FunDefn(fun.owner, fun.sym, fun.dSym, fun.params, body)(fun.configOverride, fun.annotations)
    super.applyFunDefn(transformed)
end ClassTagsTransformer


object ClassTagsTransformer:
  private type WebProducer = ProdStrat | WebEntryCollector.EntryPoints
  private type WebConsumer = ConcreteCtorConsumer | ProdStrat | WebEntryCollector.EntryPoints

  def isShapeMatch(path: Path)(using ctx: Elaborator.Ctx): Bool =
    path.targetSymbol.flatMap(_.asBlkMember).contains(ctx.builtins.shape.`match`)

  private def getShapeMatchCalls(program: Program)(using Elaborator.Ctx): List[Call] =
    val calls = ListBuffer.empty[Call]
    val collector = new BlockTraverser:
      override def applyResult(result: Result): Unit =
        result match
          case call: Call if isShapeMatch(call.fun) => calls += call
          case _ => ()
        super.applyResult(result)
    collector.applyProgram(program)
    calls.toList

  private def mkWeb(
    entries: WebEntryCollector.EntryPoints,
    entriesByProducer: Map[Ctor, List[WebEntryCollector.EntryPoints]],
    entriesByConsumer: Map[ConcreteCtorConsumer, List[WebEntryCollector.EntryPoints]],
  ): Web =
    val result = FlowWebComputation[WebProducer, WebConsumer](
      producer => producer match
        case ctor: Ctor =>
          val consumers = ctor.dests.iterator.collect:
            case consumer: ConcreteCtorConsumer => consumer: WebConsumer
          consumers
            ++ ctor.args.iterator.map(arg => arg._2: WebConsumer)
            ++ entriesByProducer.getOrElse(ctor, Nil) // also connect other entries in the same function to the current web
        case variable: StratVar =>
          variable.lowerBounds.iterator.map(producer => producer: WebConsumer)
        case entries: WebEntryCollector.EntryPoints =>
          entries.producers.iterator.map(producer => producer: WebConsumer)
            ++ entries.consumers
        case _ => Nil,
      consumer => consumer match
        case consumer: ConcreteCtorConsumer =>
          consumer.srcs.iterator.map(producer => producer: WebProducer)
            ++ entriesByConsumer.getOrElse(consumer, Nil) // also connect other entries in the same function to the current web
        case variable: StratVar =>
          variable.lowerBounds.iterator.map(producer => producer: WebProducer)
        case producer: ProdStrat => (producer: WebProducer) :: Nil
        case entries: WebEntryCollector.EntryPoints => (entries: WebProducer) :: Nil,
      entries.producers,
      entries.consumers,
    )
    FlowWebComputation.Result[Ctor, ConcreteCtorConsumer](
      result.markedProducers.collect:
        case ctor: Ctor => ctor,
      result.markedConsumers.collect:
        case consumer: ConcreteCtorConsumer => consumer,
    )

  /** The web grown from the `shape.match` consumers themselves, rather than one web
    * per function that is then filtered. Its producers are exactly the instances that
    * can reach some match -- both the ones a match scrutinises and the ones a tag check
    * reads out of a field, since a constructor's arguments are edges of the web too.
    *
    * Partitioning per function and dropping the webs without a match, as this used to
    * do, is order-dependent: a producer claimed by a match-free web is never tagged
    * even when it does reach a match through another one.
    */
  private def mkMatchWebs(
    entryPoints: List[WebEntryCollector.EntryPoints],
    matchConsumers: List[ConcreteCtorConsumer],
  ): List[Web] =
    val entriesByProducer = entryPoints.iterator
      .flatMap(entries => entries.producers.map(_ -> entries))
      .toList.groupMap(_._1)(_._2)
    val entriesByConsumer = entryPoints.iterator
      .flatMap(entries => entries.consumers.map(_ -> entries))
      .toList.groupMap(_._1)(_._2)
    if matchConsumers.isEmpty then Nil
    else
      val seeds = WebEntryCollector.EntryPoints(Nil, matchConsumers)
      mkWeb(seeds, entriesByProducer, entriesByConsumer) :: Nil

  private def logWebs(webs: List[Web])(using tl: TL): Unit =
    if webs.nonEmpty then
      tl.emitDbg(">>> start class-tags web-computation-phase")
      for (web, index) <- webs.zipWithIndex do
        val producers = web.markedProducers.toList.sortBy(_.exprId.uid)
        val fieldAccesses = web.markedConsumers.collect:
          case access: FieldSel => access
        val patternMatches = web.markedConsumers.collect:
          case patternMatch: Dtor => patternMatch
        tl.emitDbg(s"class-tags web-computation-phase > web $index:")
        tl.emitDbg(s"class-tags web-computation-phase >   producers: ${producers.map(ClassTagsDebug.showProducer).mkString(", ")}")
        if fieldAccesses.nonEmpty then
          tl.emitDbg(s"class-tags web-computation-phase >   field accesses: ${fieldAccesses.toList.sortBy(_.exprId.uid).map(ClassTagsDebug.showFieldAccess).mkString(", ")}")
        if patternMatches.nonEmpty then
          tl.emitDbg(s"class-tags web-computation-phase >   pattern matches: ${patternMatches.toList.sortBy(_.exprId.uid).map(ClassTagsDebug.showPatternMatch).mkString(", ")}")
      tl.emitDbg("<<< end class-tags web-computation-phase")

  /** Ordered constructor parameters of a class-like symbol, if it has a single list. */
  def classFields(cls: ClassLikeSymbol): Opt[List[TermSymbol]] = cls match
    case cls: ClassSymbol => cls.tree.clsParams match
      case fields :: Nil => S(fields)
      // * `class Nil` has no parameter list at all, which is not the same as having
      // * unknown fields: it is a field-less class, and both `new Nil` and a `Nil`
      // * pattern must get its class shape.
      case Nil => S(Nil)
      case _ => N
    case _: ModuleOrObjectSymbol => S(Nil)
    case _ => N

  def apply(p: Program)(using
    cfg: Config,
    tl: TL,
    raise: Raise,
    eState: State,
    ctx: Elaborator.Ctx,
    symbolPrinter: SymbolPrinter,
  ): Program =
    cfg.classTags match
      case N => p
      case S(_) if !cfg.noFreeze => // TODO: make the tag a real field and remove this restriction.
        raise(ErrorReport(
          msg"Class tag insertion requires :noFreeze." -> N :: Nil,
          source = Diagnostic.Source.Compilation,
        ))
        p
      case S(dCfg) =>
        val matchCalls = getShapeMatchCalls(p)
        // * The flow analysis is only used to work out the webs, so the cheaper
        // * monomorphic version is always the right one here.
        val flowCfg = Config.FlowAnalysisConfig(
          debug = false,
          mono = true,
          trackNonAffine = false,
          trackAccumulator = false,
          logNonAffine = false,
          logAccumulator = false,
        )
        val flowAnalysisRes =
          FlowAnalysis.mkTraceLogger(flowCfg, "class-tags flow-analysis-phase > ", tl).givenIn:
            FlowAnalysis(p, mono = true, nonAffineTracking = false, accumulatorTracking = false)
        given fState: FlowAnalysis.State = flowAnalysisRes.fState
        val matchResultIds = matchCalls.iterator.map(_.uid).toSet
        val collectorTl = new TraceLogger(using tl.debugPrinter):
          override def doTrace: Bool = dCfg.debug
          override def emitDbg(str: Str): Unit =
            tl.emitDbg(s"class-tags collection-phase > $str")
        val entryPoints = collectorTl.givenIn:
          if dCfg.debug then tl.emitDbg(">>> start class-tags collection-phase")
          val result = WebEntryCollector(p, flowAnalysisRes)
          if dCfg.debug then tl.emitDbg("<<< end class-tags collection-phase")
          result
        // * Every `shape.match`, not only the ones `WebEntryCollector` vetted: a match
        // * whose scrutinee can also come from an unknown producer is still a match, and
        // * dropping it as a seed leaves the constructions that feed it untagged while
        // * its branches still dispatch on tags -- which is an unhandled tag at run time.
        val matchConsumers = flowAnalysisRes.consumersWithSrcs.iterator.collect:
          case patternMatch: Dtor if matchResultIds.contains(patternMatch.exprId) => patternMatch
        .toList.distinct
        val webs = mkMatchWebs(entryPoints, matchConsumers)
        if dCfg.debug then logWebs(webs)
        // * All that is kept from the webs: which constructions may be tagged. Holding
        // * on to the flow analysis itself, or to shapes derived from it, is what used
        // * to exhaust the heap on the larger staged programs.
        // * Every producer the web reaches is tagged. There is no need to leave out the
        // * ones that escape the webs' functions: the pass requires `:noFreeze`, so an
        // * object this module builds is never frozen and can always carry the property.
        val taggedResultIds = webs.iterator
          .flatMap(_.markedProducers.iterator)
          .map(_.exprId).toSet
        val funSymToFunDefn = flowAnalysisRes.preAnalyzer.res.funSymToFunDefn
        val patternShapesByCall = matchCalls.iterator.flatMap: call =>
          call.metadata.annotations.collectFirst:
            case Annot.MatchShapes(patterns) => call.uid -> patterns.map(Shape.mkShapeByPattern)
        .toMap
        val shapeProp = new ShapeProp(funSymToFunDefn, patternShapesByCall)
        shapeProp.applyProgram(p)
        new ClassTagsTransformer(
          taggedResultIds,
          shapeProp.shapeByResultId,
          funSymToFunDefn,
          shapeProp.shapeTags,
          patternShapesByCall,
          dCfg.debug,
        ).applyProgram(p)
