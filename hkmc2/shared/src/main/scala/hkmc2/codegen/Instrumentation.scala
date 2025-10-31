package hkmc2
package codegen

import mlscript.utils.*, shorthands.*

import semantics.*
import semantics.Elaborator.State

import syntax.{Literal, Tree}

type BlockRet = (Block => Block, Shape, Value)
class Instrumentation(using State):
  // use TempSymbol?
  lazy val block = new BlockMemberSymbol("Block", Nil)
  lazy val shape = new BlockMemberSymbol("Shape", Nil)

  def toArg(x: Path | Local): Arg =
    x match
      case p: Path => Arg(N, p)
      case l: Local => Arg(N, Value.Ref(l))

  // null and undefined are missing
  def toValue(lit: Str | BigInt | BigDecimal | Bool): Value =
    val l = lit match
      case i: BigInt => Tree.IntLit(i)
      case b: Bool => Tree.BoolLit(b)
      case s: Str => Tree.StrLit(s)
      case n: BigDecimal => Tree.DecLit(n)
    Value.Lit(l)
  
  // continuation-passing style allows symbols defined in current function to be referenced later
  
  def assign(res: Result)(k: Local => BlockRet): BlockRet = 
    val tmp = new TempSymbol(N, "tmp")
    val (rest, shape, value) = k(tmp)
    (b => Assign(tmp, res, rest(b)), shape, value)
  def assign2(res: Result)(k: Local => Block): Block = 
    val tmp = new TempSymbol(N, "tmp")
    Assign(tmp, res, k(tmp))
  
  // helper for staging the constructors in Block.scala to Block.mls
  // automatically convert Ls[Path] into Ls[Arg]?
  def stagedBlock(nme: String, args: Ls[Path | Local])(k: Local => BlockRet): BlockRet =
    val s = Select(Value.Ref(block), Tree.Ident(nme))(N)
    val res = args match
      case Nil => s
      case h :: t => Call(s, args.map(toArg))(false, false)
    assign(res)(k)
  def stagedBlock2(nme: String, args: Ls[Path | Local])(k: Local => Block): Block =
    val s = Select(Value.Ref(block), Tree.Ident(nme))(N)
    val res = args match
      case Nil => s
      case h :: t => Call(s, args.map(toArg))(false, false)
    assign2(res)(k)
  def stagedShape(nme: String, args: Ls[Arg])(k: Local => BlockRet): BlockRet = 
    val s = Select(Value.Ref(shape), Tree.Ident(nme))(N)
    val res = args match
      case Nil => s
      case h :: t => Call(s, args)(false, false)
    assign(res)(k)

  // helpers for staging constructors in Block.mls

  def stagedSymbol(nme: String)(k: Local => BlockRet): BlockRet =
    stagedBlock("Symbol", Ls(toValue(nme)))(k)
  
  def stagedIdent(nme: String)(k: Local => BlockRet): BlockRet = 
    stagedBlock("Ident", Ls(toValue(nme)))(k)
  
  def stagedRef(l: Symbol)(k: Local => BlockRet): BlockRet =
    stagedSymbol(l.nme): l =>
      stagedBlock("ValueRef", Ls(l))(k)
  
  // note that this is for Block.ValueLit, not Shape.Lit
  def stagedBLit(l: Literal)(k: Local => BlockRet): BlockRet =
    stagedBlock("ValueLit", Ls(Value.Lit(l)))(k)

  def stagedSelect(qual: Path, name: Str)(k: Local => BlockRet): BlockRet = 
    stagedPath(qual): p =>
      stagedIdent(name): i =>
        stagedBlock("Select", Ls(p, i))(k)

  def stagedDynSelect(qual: Path, fld: Path, arrayIdx: Bool)(k: Local => BlockRet): BlockRet =
    stagedPath(qual): q =>
      stagedPath(fld): f =>
        stagedBlock("DynSelect", Ls(q, f, toValue(arrayIdx)))(k)

  def stagedPath(p: Path)(k: Local => BlockRet): BlockRet = p match
    case Select(qual, tree) => stagedSelect(qual, tree.name)(k)
    case DynSelect(qual, fld, arrayIdx) => stagedDynSelect(qual, fld, arrayIdx)(k)
    case Value.Ref(l) => stagedRef(l)(k)
    case Value.Lit(lit) => stagedBLit(lit)(k)
    case _ => ???

  def stagedTuple(mut: Bool, elems: Ls[Arg])(k: Local => BlockRet): BlockRet =
    // TODO: staging array
      stagedBlock("Tuple", Ls(toValue(mut), ???))(k)
  
  // helpers to create and access the components of a staged value
  def returnStagedValue(shape: Local, value: Local): Block =
    stagedBlock2("Tuple", Ls(shape, value)): z =>
      stagedBlock2("Return", Ls(z, toValue(false))): _ =>
        End()
  def getShape(l: Local)(k: Local => BlockRet): BlockRet =
    stagedSelect(Value.Ref(l), "0")(k)
  def getBlock(l: Local)(k: Local => BlockRet): BlockRet =
    stagedSelect(Value.Ref(l), "1")(k)

  // functions that perform the instrumentation

  def ruleLit(l: Literal): BlockRet = 
    (id, SLit(l), Value.Lit(l))
  
  def ruleVar(x: (Shape, Value.Ref)): BlockRet =
    stagedBlock("ValueRef", Ls(x._2)): block =>
      (id, x._1, Value.Ref(block))
  
  def ruleArr(ps: Ls[Path]): BlockRet = 
    // ps match
    //   case head :: next => next.foldRight(stagedPath(head)(b))()
    //   case Nil => (id, )
    
    val (bs, ss, vs) = ps.map(stagedPath).unzip3
    (b => bs.foldRight(b)(_(_)), SArr(ps.length, ss), ???)
  
  def ruleReturn(r: Return): BlockRet =
    val (b, s, v) = stagedResult(r.res)
    stagedBlock("Return", Ls(v, toValue(r.implct))): ret =>
      (b, s, Value.Ref(ret))
  
  def ruleInst(i: Instantiate): BlockRet =
    val Instantiate(mut, cls, args) = i
    assert(!mut)
    // args.reduceRightOption[BlockRet]((arg, x) =>
    //   val (b, ss, vs) = x
    //   ???
    // ).getOrElse(???)
    val (bs, ss, vs) = args.map(stagedArg).unzip3
    // turn path to symbol somehow
    val sym: Symbol = ???
    val s = SClass(???, ss)
    stagedSelect(cls, "class"): sel =>
      stagedBlock("Tuple", Ls(toValue(false), ???)): _ =>
        ??? 
      // stagedBlock("Instantiate", Ls(toValue(false), sym, ???).map(toArg)): inst =>
      //   ???

  def ruleEnd(): BlockRet =
    stagedBlock("End", Ls()): end =>
      (id, Unit, Value.Ref(end))

  // todo functions that fills out the holes in the functions above
  
  def stagedResult(res: Result): BlockRet = res match
    case Call(fun, args) =>  ???
    case res: Instantiate => ruleInst(res)
    case Lambda(params, body) => ???
    case Tuple(mut, elems) => ???
    case Record(mut, elems) => ???
    case Select(qual, name) => ???
    case DynSelect(qual, fld, arrayIdx) => ???
    case Value.Ref(l) => ???
    case Value.Lit(lit) => ???
    case _ => ???
   
  def stagedPath(p: Path): BlockRet = p match
    case Select(qual, name) => ???
    case DynSelect(qual, fld, arrayIdx) => ???
    case Value.Ref(l) => ???
    case Value.Lit(lit) => ???
    case _ => ???
  
  def stagedArg(a: Arg): BlockRet = ???
  
  
  