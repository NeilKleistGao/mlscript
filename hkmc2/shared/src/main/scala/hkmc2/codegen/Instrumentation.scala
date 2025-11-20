package hkmc2
package codegen

import utils.*
import hkmc2.Message.MessageContext

import scala.collection.mutable.HashMap

import mlscript.utils.*, shorthands.*

import semantics.*
import semantics.Elaborator.State

import syntax.{Literal, Tree}

// it seems some logic should be deferred to BlockTransformer to dedup code
// but it doesn't accept the current context, so applications seem limited

class InstrumentationImpl(using State):
  type ArgWrappable = Path | Symbol | Shape
  type Context = HashMap[Path, StagedPath]

  def asArg(x: ArgWrappable): Arg =
    x match
      case p: Path => p.asArg
      case l: Symbol => l.asPath.asArg
      case Shape(p) => p.asArg

  // null and undefined are missing
  def toValue(lit: Str | Int | BigDecimal | Bool): Value =
    val l = lit match
      case i: Int => Tree.IntLit(i)
      case b: Bool => Tree.BoolLit(b)
      case s: Str => Tree.StrLit(s)
      case n: BigDecimal => Tree.DecLit(n)
    Value.Lit(l)

  def concat(b1: Block, b2: Block): Block =
    b1.mapTail {
      case _: Return => b2
      case _: End => b2
      case _ => ???
    }

  // TODO: use BlockTransformer.applyListOf?
  extension [A](ls: Ls[(A => Block) => Block])
    def collectApply(f: Ls[A] => Block): Block =
      // defer applying k while prepending new paths to the list
      ls.foldRight((_: Ls[A] => Block)(Nil))((headCont, tailCont) =>
        k =>
          headCont: head =>
            tailCont: tail =>
              k(head :: tail)
      )(f)

  // helpers corresponding to constructors

  def assign(res: Result, symName: Str = "tmp")(k: Path => Block): Assign =
    // TODO: skip assignment if res: Path?
    val tmp = new TempSymbol(N, symName)
    Assign(tmp, res, k(tmp.asPath))

  def tuple(elems: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    assign(Tuple(false, elems.map(asArg)), symName)(k)

  def ctor(cls: Path, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    assign(Instantiate(false, cls, args.map(asArg)), symName)(k)

  // isMlsFun is probably always true?
  def call(fun: Path, args: Ls[ArgWrappable], isMlsFun: Bool = true, symName: Str = "tmp")(k: Path => Block): Block =
    assign(Call(fun, args.map(asArg))(isMlsFun, false), symName)(k)

  // helper for staging the constructors

  def blockMod(name: Str) = summon[State].blockSymbol.asPath.selSN(name)
  def shapeMod(name: Str) = summon[State].shapeSymbol.asPath.selSN(name)

  def blockCtor(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    ctor(blockMod(name), args, symName)(k)
  def shapeCtor(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Shape => Block): Block =
    ctor(shapeMod(name), args, symName)(p => k(Shape(p)))

  def blockCall(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    call(blockMod(name), args, symName = symName)(k)
  def shapeCall(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    call(shapeMod(name), args, symName = symName)(k)

  // helpers to create and access the components of a staged value
  case class Shape(p: Path)

  // A StagedPath is a path that points to a (shape, code) tuple
  class StagedPath(val p: Path):
    def shape: Shape = Shape(DynSelect(p, toValue(0), false))
    def code: Path = DynSelect(p, toValue(1), false)
    def end: Block = Return(p, false)

  object StagedPath:
    def mk(shape: Shape, code: Path, symName: Str = "tmp")(k: StagedPath => Block): Block =
      tuple(Ls(shape.p, code), symName)(p => k(StagedPath(p)))

  // linking functions defined in MLscipt

  def fnPrintCode(p: Path)(k: Path => Block): Block =
    blockCall("printCode", Ls(p))(k)
  def fnSel(s1: Shape, s2: Shape)(k: Shape => Block): Block =
    shapeCall("sel", Ls(s1, s2))(s => k(Shape(s)))

  // instrumentation rules

  def ruleLit(l: Value.Lit)(k: StagedPath => Block): Block =
    shapeCtor("Lit", Ls(l)): sp =>
      blockCtor("ValueLit", Ls(l)): cde =>
        StagedPath.mk(sp, cde, "lit")(k)

  // outdated
  def ruleVar(r: Value.Ref)(k: StagedPath => Block): Block =
    // why assume it is already staged?
    val sp = StagedPath(r)
    blockCtor("Symbol", Ls(toValue(r.l.nme))): sym =>
      blockCtor("ValueRef", Ls(sym)): cde =>
        StagedPath.mk(sp.shape, cde, "var")(k)

  def ruleTup(t: Tuple)(using ctx: Context)(k: StagedPath => Block): Block =
    assert(!t.mut)
    transformArgs(t.elems): xs =>
      tuple(xs.map(_.shape)): shapes =>
        shapeCtor("Arr", Ls(shapes)): sp =>
          tuple(xs.map(_.code)): codes =>
            blockCtor("Tuple", Ls(codes)): cde =>
              StagedPath.mk(sp, cde, "tup")(k)

  def ruleSel(s: Select)(using ctx: Context)(k: StagedPath => Block): Block =
    val Select(p, i @ Tree.Ident(name)) = s
    transformPath(p): x =>
      // TODO: figure out actual shape
      shapeCtor("Dyn", Ls()): n =>
        fnSel(x.shape, n): sp =>
          blockCtor("Symbol", Ls(toValue(name))): name =>
            blockCtor("Select", Ls(x.code, name)): cde =>
              StagedPath.mk(sp, cde, "sel")(k)

  def ruleInst(i: Instantiate)(using ctx: Context)(k: StagedPath => Block): Block =
    val Instantiate(mut, cls, args) = i
    assert(!mut)
    transformArgs(args): xs =>
      tuple(xs.map(_.shape)): shapes =>
        tuple(xs.map(_.code)): codes =>
          // NOTE: this was not needed in the formalization
          // but it seems to be necessary to stage the path?
          transformPath(cls): cls =>
            shapeCtor("Class", Ls(cls.code, shapes)): sp =>
              blockCtor("Instantiate", Ls(cls.code, codes)): cde =>
                StagedPath.mk(sp, cde, "inst")(k)

  def ruleReturn(r: Return)(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    transformResult(r.res): x =>
      blockCtor("Return", Ls(x.code)): cde =>
        StagedPath.mk(x.shape, cde, "return")(k(_, ctx))

  // outdated
  def ruleAssign(a: Assign)(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    val Assign(x, r, b) = a
    transformResult(r): y =>
      (Assign(x, y.p, _)):
        transformBlock(b): z => // have ctx here?
          blockCtor("Symbol", Ls(toValue(x.nme))): x =>
            blockCtor("Assign", Ls(x, y.code, z.code)): cde =>
              StagedPath.mk(z.shape, cde, "assign")(k(_, ctx))

  def ruleEnd()(k: StagedPath => Block): Block =
    assign(State.globalThisSymbol.asPath.selSN("Set")): newSet =>
      shapeCtor("Multiple", Ls(newSet)): sp =>
        blockCtor("End", Ls()): cde =>
          StagedPath.mk(sp, cde, "end")(k)

  // converted to ruleLet?
  // outdated
  def ruleVal(defn: ValDefn, b: Block)(using ctx: Context)(k: StagedPath => Block): Block =
    val ValDefn(tsym, x, p) = defn
    transformPath(p): y =>
      // y is StagedPath, not Path?
      (Define(ValDefn(tsym, x, y.p), _)):
        transformBlock(b): z =>
          blockCtor("Symbol", Ls(toValue(x.nme))): x =>
            blockCtor("ValDefn", Ls(x, y.code)): df =>
              blockCtor("Define", Ls(df, z.code)): cde =>
                StagedPath.mk(z.shape, cde, "val")(k)

  // transformations of Block

  def transformPath(p: Path)(using ctx: Context)(k: StagedPath => Block): Block =
    p match
      case r: Value.Ref => ruleVar(r)(k)
      case l: Value.Lit => ruleLit(l)(k)
      case s: Select => ruleSel(s)(k)
      case _ => ??? // not supporteda

  def transformResult(r: Result)(using ctx: Context)(k: StagedPath => Block): Block =
    r match
      case p: Path =>
        transformPath(p): p =>
          blockCtor("TrivialResult", Ls(p.code)): cde =>
            StagedPath.mk(p.shape, cde)(k)
      case t: Tuple => ruleTup(t)(k)
      case i: Instantiate => ruleInst(i)(k)
      case _ => ??? // not supported

  def transformArg(a: Arg)(using ctx: Context)(k: StagedPath => Block): Block =
    val Arg(spread, value) = a
    transformPath(value)(k)

  // provides list of shapes and list of codes to continuation
  def transformArgs(args: Ls[Arg])(using ctx: Context)(k: Ls[StagedPath] => Block): Block =
    args.map(transformArg).collectApply(k)

  // f.owner returns an InnerSymbol, but we need BlockMemberSymbol of the module to call the function
  // so we pass modSym instead
  def transformFunDefn(modSym: Symbol, f: FunDefn)(using ctx: Context): (FunDefn, Block) =
    val genSym = BlockMemberSymbol(f.sym.nme + "_gen", Nil, true)
    // TODO: remove it. only for test
    // TODO: put correct parameters instead of Nil
    val debug =
      call(modSym.asPath.selSN(genSym.nme), Nil): ret =>
        blockCall("printCode", Ls(StagedPath(ret).code)): _ => // discard result, we only care about side effect
          End()

    (f.copy(sym = genSym, body = transformBlock(f.body)(_.end)), debug)

  def transformDefine(d: Define)(using ctx: Context)(k: StagedPath => Block): Block =
    d.defn match
      case f: FunDefn => ???
      case v: ValDefn => ruleVal(v, d.rest)(k)
      case c: ClsLikeDefn => ??? // nested class?

  def transformBlock(b: Block)(using ctx: Context)(k: StagedPath => Block): Block =
    transformBlock(b)((p, _) => k(p))

  def transformBlock(b: Block)(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    // ruleBlk?
    val k2 = k(_, ctx)
    b match
      case r: Return => ruleReturn(r)(k)
      case a: Assign => ruleAssign(a)(k)
      case d: Define => transformDefine(d)(k2)
      case End(_) => ruleEnd()(k2)
      case _: Match => ???
      // temporary measure to accept returning an array
      // use BlockTransformer here?
      case Begin(b1, b2) => transformBlock(concat(b1, b2))(k)

// TODO: rename as InstrumentationTransformer?
class Instrumentation(using State) extends BlockTransformer(new SymbolSubst()):
  val impl = new InstrumentationImpl

  override def applyBlock(b: Block): Block = super.applyBlock(b) match
    case d @ Define(defn, rest) =>
      defn match
        // find modules with staged annotation
        case c: ClsLikeDefn if c.sym.defn.exists(_.hasStagedModifier.isDefined) && c.companion.isDefined =>
          val companion = c.companion.get
          val (stagedMethods, debugPrintCode) = companion.methods
            .map(impl.transformFunDefn(c.sym, _)(using new HashMap())) // fold instead to retain env?
            .unzip
          val newCompanion = companion.copy(methods = companion.methods ++ stagedMethods)
          val newModule = c.copy(sym = c.sym, companion = Some(newCompanion))
          val debugBlock = debugPrintCode.foldRight(rest)(impl.concat)
          Define(newModule, debugBlock)
        case _ => d
    case b => b
