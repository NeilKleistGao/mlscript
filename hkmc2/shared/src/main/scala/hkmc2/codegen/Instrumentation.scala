package hkmc2
package codegen

import utils.*
import hkmc2.Message.MessageContext

import scala.collection.mutable.HashMap

import mlscript.utils.*, shorthands.*

import semantics.*
import semantics.Elaborator.State

import syntax.{Literal, Tree}

// TODO: I didn't use BlockTransformer here, because in some cases it constrains the type of the continuation
// but it seems some logic should be deferred to it to dedup code

// it should be possible to convert to the BlockTransformer signatures,
// but it would require re-extracting and re-assigning StagedPath from the output.

// the continuation would basically be solely dedicated to staging then?
// like, we do a transformation on DynSelect where we keep the fields inteact, then perform staging in the DynSelect => Block continuation?
// the previous blocks created by the fields are handled by BlockTransformer's continuation code

class InstrumentationImpl(using State):
  type ArgWrappable = Path | Symbol | Shape

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

  // helpers corresponding to constructors

  def assign(res: Result, symName: String = "tmp")(k: Path => Block): Assign =
    // TODO: skip assignment if res: Path?
    val tmp = new TempSymbol(N, symName)
    Assign(tmp, res, k(tmp.asPath))

  def select(qual: Path, ident: Tree.Ident): Path =
    Select(qual, ident)(N)

  def tuple(elems: Ls[ArgWrappable], symName: String = "tmp")(k: Path => Block): Block =
    // is this the same as "Ls of"?
    assign(Tuple(false, elems.map(asArg)), symName)(k)

  def ctor(cls: Path, args: Ls[ArgWrappable], symName: String = "tmp")(k: Path => Block): Block =
    assign(Instantiate(false, cls, args.map(asArg)), symName)(k)

  // isMlsFun is probably always true?
  def call(fun: Path, args: Ls[ArgWrappable], isMlsFun: Bool = true, symName: String = "tmp")(k: Path => Block): Block =
    assign(Call(fun, args.map(asArg))(isMlsFun, false), symName)(k)

  // helper for staging the constructors

  def blockMod(name: String) = summon[State].blockSymbol.asPath.selSN(name)
  def shapeMod(name: String) = summon[State].shapeSymbol.asPath.selSN(name)

  def blockCtor(name: String, args: Ls[ArgWrappable], symName: String = "tmp")(k: Path => Block): Block =
    ctor(blockMod(name), args, symName = symName)(k)
  def shapeCtor(name: String, args: Ls[ArgWrappable], symName: String = "tmp")(k: Shape => Block): Block =
    ctor(shapeMod(name), args, symName = symName)(p => k(Shape(p)))

  def blockCall(name: String, args: Ls[ArgWrappable], symName: String = "tmp")(k: Path => Block): Block =
    call(blockMod(name), args, symName = symName)(k)
  def shapeCall(name: String, args: Ls[ArgWrappable], symName: String = "tmp")(k: Path => Block): Block =
    call(shapeMod(name), args, symName = symName)(k)

  // helpers to create and access the components of a staged value
  case class Shape(p: Path)

  // A StagedPath is a path that points to a (shape, code) tuple
  class StagedPath(val p: Path):
    def shape: Shape = Shape(DynSelect(p, toValue(0), false))
    def code: Path = DynSelect(p, toValue(1), false)
    def end: Block = Return(p, false)

  object StagedPath:
    def mk(shape: Shape, code: Path, symName: String = "tmp")(k: StagedPath => Block): Block =
      tuple(Ls(shape.p, code), symName)(p => k(StagedPath(p)))

  // linking functions defined in MLscipt

  def fnPrintCode(p: Path)(k: Path => Block): Block =
    blockCall("printCode", Ls(p))(k)

  // instrumentation rules

  def ruleLit(l: Literal)(k: StagedPath => Block): Block =
    shapeCtor("Lit", Ls(Value.Lit(l))): sp =>
      blockCtor("ValueLit", Ls(Value.Lit(l))): cde =>
        StagedPath.mk(sp, cde, "lit")(k)

  def ruleVar(r: Value.Ref)(k: StagedPath => Block): Block =
    // why assume it is already staged?
    val sp = StagedPath(r)
    // why not just use sp.code?
    blockCtor("Symbol", Ls(toValue(r.l.nme))): sym =>
      blockCtor("ValueRef", Ls(sym)): cde =>
        StagedPath.mk(sp.shape, cde, "var")(k)

  def ruleReturn(r: Return)(k: StagedPath => Block): Block =
    transformResult(r.res): x =>
      blockCtor("Return", Ls(x.code)): cde =>
        StagedPath.mk(x.shape, cde, "ret")(k)

  def ruleAssign(a: Assign)(k: StagedPath => Block): Block =
    val Assign(x, r, b) = a
    transformResult(r): y =>
      (Assign(x, y.p, _)):
        transformBlock(b): z =>
          blockCtor("Symbol", Ls(toValue(x.nme))): x =>
            // need to wrap x with Symbol?
            blockCtor("Assign", Ls(x, y.code, z.code)): cde =>
              StagedPath.mk(z.shape, cde, "ass")(k)

  def ruleEnd()(k: StagedPath => Block): Block =
    shapeCtor("Unit", Ls()): sp =>
      blockCtor("End", Ls()): cde =>
        StagedPath.mk(sp, cde, "end")(k)

  def ruleVal(defn: ValDefn, b: Block)(k: StagedPath => Block): Block =
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

  def transformPath(p: Path)(k: StagedPath => Block): Block =
    p match
      case r: Value.Ref => ruleVar(r)(k)
      case Value.Lit(lit) => ruleLit(lit)(k)
      case _ => ??? // not supported

  def transformResult(r: Result)(k: StagedPath => Block): Block =
    r match
      case p: Path =>
        transformPath(p): p =>
          blockCtor("TrivialResult", Ls(p.code)): cde =>
            StagedPath.mk(p.shape, cde)(k)
      case _ => ??? // not supported

  def transformArg(a: Arg)(k: StagedPath => Block): Block =
    val Arg(spread, value) = a
    transformPath(value)(k)

  def transformFunDefn(f: FunDefn): FunDefn =
    val FunDefn(owner, sym, parameters, body) = f
    val genSym = BlockMemberSymbol(sym.nme + "_gen", Nil, true)
    // TODO: remove it. only for test
    // TODO: put correct parameters instead of Nil
    val b = call(genSym.asPath, Nil): ret =>
      blockCall("printCode", Ls(StagedPath(ret).code)): _ => // discard result, we only care about side effect
        transformBlock(body)(_.end)
    FunDefn(owner, genSym, parameters, transformBlock(body)(_.end))

  def transformDefine(d: Define)(k: StagedPath => Block): Block =
    d.defn match
      // duplicated because we need a reference to genSym here
      case f @ FunDefn(owner, sym, parameters, body) =>
        val genSym = BlockMemberSymbol(sym.nme + "_gen", Nil, true)
        val b = transformBlock(d.rest): res =>
          // TODO: remove it. only for test
          // TODO: put correct parameters instead of Nil
          call(genSym.asPath, Nil): ret =>
            blockCall("printCode", Ls(StagedPath(ret).code)): _ => // discard result, we only care about side effect
              res.end
        val rest = Define(FunDefn(owner, genSym, parameters, transformBlock(body)(_.end)), b)
        Define(f, rest)
      case v: ValDefn => ruleVal(v, d.rest)(k)
      case c: ClsLikeDefn => ??? // nested class?

  def transformBlock(b: Block)(k: StagedPath => Block): Block =
    b match
      case r: Return => ruleReturn(r)(k)
      case a: Assign => ruleAssign(a)(k)
      case d: Define => transformDefine(d)(k)
      case End(_) => ruleEnd()(k)
      case _ => ??? // not supported

// TODO: rename as InstrumentationTransformer?
class Instrumentation(using State) extends BlockTransformer(new SymbolSubst()):
  val impl = new InstrumentationImpl

  // This stages any function definition,
  // instead of staging functions within staged modules.
  override def applyBlock(b: Block): Block = b match
    case Define(defn, rest) =>
      defn match
        // case f @ FunDefn(owner, sym, parameters, body) =>
        //   val genSym = BlockMemberSymbol("gen", Nil, true) // TODO: reuse original function name?
        //   val staged = FunDefn(owner, genSym, parameters, impl.transformBlock(body)(_.end))
        //   // TODO: remove it. only for test
        //   // TODO: put correct parameters instead of Nil
        //   val b = impl.call(genSym.asPath, Nil): ret =>
        //     // discard result, we only care about side effect of printCode
        //     impl.blockCall("printCode", Ls(impl.StagedPath(ret).code)): _ =>
        //       applyBlock(rest)
        //   Define(f, Define(staged, b))
        // find modules with staged annotation
        case c: ClsLikeDefn if c.sym.defn.exists(_.hasStagedModifier.isDefined) && c.companion.isDefined =>
          val companion = c.companion.get
          val (stagedMethods, debugPrintCode) = companion.methods.map { case f @ FunDefn(owner, sym, parameters, body) =>
            val genSym = BlockMemberSymbol(sym.nme + "_gen", Nil, true) // TODO: reuse original function name?
            // TODO: remove it. only for test
            // TODO: put correct parameters instead of Nil
            val b: Block = impl.call(c.sym.asPath.selSN(genSym.nme), Nil): ret =>
              impl.blockCall("printCode", Ls(impl.StagedPath(ret).code)): _ => // discard result, we only care about side effect
                End()
            (f.copy(sym = genSym, body = impl.transformBlock(body)(_.end)), b)
          }.unzip
          val newCompanion = companion.copy(methods = companion.methods ++ stagedMethods)
          val newModule = c.copy(sym = c.sym, companion = Some(newCompanion))
          val debugBlock: Block = debugPrintCode.foldRight(rest)((b1, b2) => b1.mapTail { case _ => b2 })
          Define(newModule, debugBlock)
        case _ => b
    case _ => b
