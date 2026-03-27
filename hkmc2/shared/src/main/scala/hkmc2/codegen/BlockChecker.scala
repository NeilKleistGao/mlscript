package hkmc2
package codegen

import scala.collection.mutable.{Map => MutMap, Set => MutSet}
import sourcecode.Line

import mlscript.utils.*, shorthands.*
import hkmc2.utils.*
import hkmc2.Message.MessageContext

import semantics.*
import semantics.Elaborator.State


/** An invariant of the IR is that each symbol should be bound at most once
  * (this simplifies various analyses and transformations).
  * This class checks this invariant. */
class BlockChecker()(using DebugPrinter, State, Raise) extends BlockTraverser:
  
  val definedSyms = MutSet.empty[Local]
  
  private def checkSymbol(sym: Local, info: => Any): Unit =
    if !definedSyms.add(sym) then
      raise:
        InternalError(
          msg"[BlockChecker] Invalid IR: symbol ${sym.showAsPlain} is bound more than once" -> sym.toLoc
            :: Nil,
          extraInfo = S(info)
        )
  
  override def applyBlock(b: Block): Unit =
    b match
    case Scoped(syms, body) =>
      syms.foreach(checkSymbol(_, b))
    case _ => ()
    super.applyBlock(b)
  
  override def applyDefn(defn: Defn): Unit =
    defn match
    case cls: ClsLikeDefn =>
      cls.paramsOpt.foreach:
        _.paramSyms.foreach(checkSymbol(_, cls))
    case _ =>
    super.applyDefn(defn)
  
  override def applyLam(lam: Lambda): Unit =
    lam.params.paramSyms.foreach(checkSymbol(_, lam))
    super.applyLam(lam)
  
end BlockChecker


