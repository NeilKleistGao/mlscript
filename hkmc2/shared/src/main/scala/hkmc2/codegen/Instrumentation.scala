package hkmc2
package codegen

import mlscript.utils.*, shorthands.*

import semantics.*
import semantics.Elaborator.State

import syntax.{Literal, Tree}

class Instrumentation(using State):
  def toArg(x: Path | Symbol): Arg =
    x match
      case p: Path => Arg(N, p)
      case l: Symbol => Arg(N, l.asPath)

  // null and undefined are missing
  def toValue(lit: Str | Int | BigDecimal | Bool): Value =
    val l = lit match
      case i: Int => Tree.IntLit(i)
      case b: Bool => Tree.BoolLit(b)
      case s: Str => Tree.StrLit(s)
      case n: BigDecimal => Tree.DecLit(n)
    Value.Lit(l)
  
  def end(l: Path): Block = Return(l, false)
  def assign(res: Result, name: String = "tmp")(k: Path => Block): Block = 
    val tmp = new TempSymbol(N, name)
    Assign(tmp, res, k(tmp.asPath))
  
  // helper for staging the constructors in Block.scala to Block.mls
  
  def stagedBlock(nme: String, args: Ls[Path | Symbol])(k: Path => Block): Block =
    val s = summon[State].blockSymbol.asPath.selSN(nme)
    assign(Instantiate(false, s, args.map(toArg)))(k)
  def stagedShape(nme: String, args: Ls[Path | Symbol])(k: Path => Block): Block = 
    val s = summon[State].shapeSymbol.asPath.selSN(nme)
    assign(Instantiate(false, s, args.map(toArg)))(k)

  // helpers for staging constructors in Block.mls

  def stagedSymbol(nme: String)(k: Path => Block): Block =
    stagedBlock("Symbol", Ls(toValue(nme)))(k)
  
  def stagedIdent(nme: String)(k: Path => Block): Block = 
    stagedBlock("Ident", Ls(toValue(nme)))(k)
  
  def stagedRef(l: Symbol)(k: Path => Block): Block =
    stagedSymbol(l.nme): l =>
      stagedBlock("ValueRef", Ls(l))(k)
  
  // note that this is for Block.ValueLit, not Shape.Lit
  def stagedBLit(l: Literal)(k: Path => Block): Block =
    stagedBlock("ValueLit", Ls(Value.Lit(l)))(k)

  def stagedSelect(qual: Path, name: Str)(k: Path => Block): Block = 
    stagedPath(qual): p =>
      stagedIdent(name): i =>
        stagedBlock("Select", Ls(p, i))(k)

  def stagedDynSelect(qual: Path, fld: Path, arrayIdx: Bool)(k: Path => Block): Block =
    stagedPath(qual): q =>
      stagedPath(fld): f =>
        stagedBlock("DynSelect", Ls(q, f, toValue(arrayIdx)))(k)

  def stagedPath(p: Path)(k: Path => Block): Block = p match
    case Select(qual, tree) => stagedSelect(qual, tree.name)(k)
    case DynSelect(qual, fld, arrayIdx) => stagedDynSelect(qual, fld, arrayIdx)(k)
    case Value.Ref(l) => stagedRef(l)(k)
    case Value.Lit(lit) => stagedBLit(lit)(k)
    case _ => ???

  def stagedTuple(elems: Ls[Symbol | Path])(k: Path => Block): Block =
    // TODO: staging array
      stagedBlock("Tuple", Ls(toValue(false), ???))(k)
  
  // helpers to create and access the components of a staged value
  def returnStagedValue(shape: Path, value: Path)(k: Path => Block): Block =
    assign(Tuple(true, Ls(shape, value).map(toArg)))(k)
  def getShape(l: Symbol)(k: Path => Block): Block =
    assign(DynSelect(Value.Ref(l), toValue(0), false))(k)
  def getBlock(l: Symbol)(k: Path => Block): Block =
    assign(DynSelect(Value.Ref(l), toValue(1), false))(k)
  
  // todo functions that fills out the holes in the functions above
  
  def stagedResult(res: Result)(k: Block => Block): Block = res match
    case Call(fun, args) =>  ???
    case res: Instantiate => ???
    case Lambda(params, body) => ???
    case Tuple(mut, elems) => ???
    case Record(mut, elems) => ???
    case p: Path => stagedPath(p)
  
