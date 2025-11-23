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
  type ArgWrappable = Path | Symbol | ShapeSet
  type Context = HashMap[Path, StagedPath]

  def asArg(x: ArgWrappable): Arg =
    x match
      case p: Path => p.asArg
      case l: Symbol => l.asPath.asArg
      case ShapeSet(p) => p.asArg

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

  // possible to wrangle to the form above, but unweildy to do so in practice
  extension [A, B](ls: Ls[B => ((A, B) => Block) => Block])
    def collectApply(b: B)(f: (Ls[A], B) => Block): Block =
      ls.foldRight((b: B) => (f: (Ls[A], B) => Block) => f(Nil, b))((headCont, tailCont) =>
        b =>
          k =>
            headCont(b): (head, b) =>
              tailCont(b): (tail, b) =>
                k(head :: tail, b)
      )(b)(f)

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
  def optionMod(name: Str) = summon[State].optionSymbol.asPath.selSN(name)
  def patternMod(name: Str) = summon[State].shapeSetSymbol.asPath.selSN("Pattern").selSN(name)
  def shapeSetMod(name: Str) = summon[State].shapeSetSymbol.asPath.selSN(name)

  def blockCtor(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    ctor(blockMod(name), args, symName)(k)
  def patternCtor(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    ctor(patternMod(name), args, symName)(k)
  def optionSome(arg: ArgWrappable, symName: Str = "tmp")(k: Path => Block): Block =
    ctor(optionMod("Some"), Ls(arg), symName)(k)
  def optionNone(symName: Str = "tmp")(k: Path => Block): Block =
    assign(optionMod("None"), symName)(k)

  def blockCall(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    call(blockMod(name), args, symName = symName)(k)
  def shapeSetCall(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    call(shapeSetMod(name), args, symName = symName)(k)

  // helpers to create and access the components of a staged value
  case class ShapeSet(p: Path)

  // A StagedPath is a path that points to a (ShapeSet, code) tuple
  case class StagedPath(p: Path):
    def shapes: ShapeSet = ShapeSet(DynSelect(p, toValue(0), false))
    def code: Path = DynSelect(p, toValue(1), false)
    def end: Block = Return(p, false)

  object StagedPath:
    def mk(shapeSet: ShapeSet, code: Path, symName: Str = "tmp")(k: StagedPath => Block): Block =
      tuple(Ls(shapeSet.p, code), symName)(p => k(StagedPath(p)))

  // linking functions defined in MLscipt

  def fnPrintCode(p: Path)(k: Path => Block): Block =
    // discard result, we only care about side effect
    blockCall("printCode", Ls(p))(k)

  def shapeBot()(k: ShapeSet => Block): Block =
    shapeSetCall("mkBot", Ls())(s => k(ShapeSet(s)))
  def shapeDyn()(k: ShapeSet => Block): Block =
    shapeSetCall("mkDyn", Ls())(s => k(ShapeSet(s)))
  def shapeLit(p: Path)(k: ShapeSet => Block): Block =
    shapeSetCall("mkLit", Ls(p))(s => k(ShapeSet(s)))
  def shapeArr(ps: Ls[ShapeSet])(k: ShapeSet => Block): Block =
    tuple(ps, "test"): tup =>
      shapeSetCall("mkArr", Ls(tup))(s => k(ShapeSet(s)))
  def shapeClass(cls: Path, params: Ls[(Symbol, ShapeSet)])(k: ShapeSet => Block): Block =
    params.map((n, s) =>
      (k: Path => Block) =>
        transformSymbol(n): n =>
          tuple(Ls(n, s)): tup =>
            k(tup)
    ).collectApply: ls =>
      tuple(ls): params =>
        shapeSetCall("mkClass", Ls(cls, params))(s => k(ShapeSet(s)))

  def fnMrg(s1: ShapeSet, s2: ShapeSet)(k: ShapeSet => Block): Block =
    shapeSetCall("mrg", Ls(s1, s2))(s => k(ShapeSet(s)))
  def fnSel(s1: ShapeSet, s2: ShapeSet)(k: ShapeSet => Block): Block =
    shapeSetCall("sel", Ls(s1, s2))(s => k(ShapeSet(s)))
  def fnFilter(s1: ShapeSet, s2: Path)(k: ShapeSet => Block): Block =
    shapeSetCall("filter", Ls(s1, s2))(s => k(ShapeSet(s)))
  def fnUnion(s1: ShapeSet, s2: ShapeSet)(k: ShapeSet => Block): Block =
    shapeSetCall("union", Ls(s1, s2))(s => k(ShapeSet(s)))

  // transformation helpers

  def transformSymbol(sym: Symbol)(k: Path => Block) = blockCtor("Symbol", Ls(toValue(sym.nme)))(k)

  def transformOption[A](xOpt: Opt[A], f: A => (Path => Block) => Block)(k: Path => Block): Block =
    xOpt match
      case S(x) => f(x)(optionSome(_)(k))
      case N => optionNone()(k)

  // instrumentation rules

  def ruleLit(l: Value.Lit)(k: StagedPath => Block): Block =
    shapeLit(l): sp =>
      blockCtor("ValueLit", Ls(l)): cde =>
        StagedPath.mk(sp, cde, "lit")(k)

  // not in formalization
  def ruleVar(r: Value.Ref)(k: StagedPath => Block): Block =
    blockCtor("Symbol", Ls(toValue(r.l.nme))): sym =>
      blockCtor("ValueRef", Ls(sym)): cde =>
        // variable defined outside of scope, may be a reference to a class
        shapeDyn(): sp =>
          StagedPath.mk(sp, cde, "var")(k)

  def ruleTup(t: Tuple)(using Context)(k: StagedPath => Block): Block =
    assert(!t.mut)
    transformArgs(t.elems): xs =>
      shapeArr(xs.map(_.shapes)): sp =>
        tuple(xs.map(_.code)): codes =>
          blockCtor("Tuple", Ls(codes)): cde =>
            StagedPath.mk(sp, cde, "tup")(k)

  def ruleSel(s: Select)(using Context)(k: StagedPath => Block): Block =
    val Select(p, i @ Tree.Ident(name)) = s
    transformPath(p): x =>
      shapeLit(toValue(name)): n =>
        fnSel(x.shapes, n): sp =>
          blockCtor("Symbol", Ls(toValue(name))): name =>
            blockCtor("Select", Ls(x.code, name)): cde =>
              StagedPath.mk(sp, cde, "sel")(k)

  def ruleDynSel(d: DynSelect)(using Context)(k: StagedPath => Block): Block =
    val DynSelect(qual, fld, arrayIdx) = d
    transformPath(qual): x =>
      transformPath(fld): y =>
        fnSel(x.shapes, y.shapes): sp =>
          blockCtor("DynSelect", Ls(x.code, y.code, toValue(arrayIdx))): cde =>
            StagedPath.mk(sp, cde, "dynsel")(k)

  def ruleInst(i: Instantiate)(using Context)(k: StagedPath => Block): Block =
    val Instantiate(mut, cls, args) = i
    assert(!mut)
    transformArgs(args): xs =>
      tuple(xs.map(_.shapes)): shapes =>
        // reuse instrumentation logic, shape of cls is discarded
        // possible to skip this? this triggers unnecessary "out of context" Dyn shape thing
        val sym = cls match
          case Select(Value.Ref(l), _) => l
          case _ => ???
        transformSymbol(sym): sym =>
          // TODO: add back class names
          val fieldName = new TempSymbol(N, "TODO")
          shapeClass(sym, xs.map(x => (fieldName, x.shapes))): sp =>
            transformPath(cls): cls =>
              tuple(xs.map(_.code)): codes =>
                blockCtor("Instantiate", Ls(cls.code, codes)): cde =>
                  StagedPath.mk(sp, cde, "inst")(k)

  def ruleReturn(r: Return)(using Context)(k: (StagedPath, Context) => Block): Block =
    transformResult(r.res): x =>
      blockCtor("Return", Ls(x.code, toValue(false))): cde =>
        StagedPath.mk(x.shapes, cde, "return")(k(_, summon))

  def ruleMatch(m: Match)(using Context)(k: (StagedPath, Context) => Block): Block =
    val Match(p, ks, dflt, rest) = m
    transformPath(p): x =>
      ruleBranches(x, p, ks, dflt): (sp, scrut, arms, dflt, ctx1) =>
        transformBlock(rest)(using ctx1): (z, ctx2) =>
          fnMrg(sp, z.shapes): sp =>
            transformOption(dflt, p => (_(p))): dflt =>
              blockCtor("Match", Ls(scrut, arms, dflt, z.code)): cde =>
                StagedPath.mk(sp, cde)(k(_, ctx2))

  def ruleAssign(a: Assign)(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    val Assign(x, r, b) = a
    transformResult(r): y =>
      transformSymbol(x): xSym =>
        blockCtor("ValueRef", Ls(xSym)): xStaged =>
          // if ctx contains x, x was defined earlier
          // otherwise, x is defined here
          ctx.get(x.asPath) match
            case S(x1) =>
              fnUnion(y.shapes, x1.shapes): sp =>
                StagedPath.mk(sp, xStaged): x2 =>
                  (Assign(x, x2.p, _)):
                    given Context = ctx.clone() += x.asPath -> x2
                    transformBlock(b): (z, ctx) =>
                      blockCtor("Assign", Ls(xSym, y.code, z.code)): cde =>
                        StagedPath.mk(z.shapes, cde, "assign")(k(_, ctx))
            case N =>
              StagedPath.mk(y.shapes, xStaged): x2 =>
                // propagate shape information for future references to x
                (Assign(x, y.p, _)):
                  given Context = ctx.clone() += x.asPath -> x2
                  transformBlock(b): (z, ctx) =>
                    blockCtor("Assign", Ls(xSym, y.code, z.code)): cde =>
                      StagedPath.mk(z.shapes, cde, "assign")(k(_, ctx))

  def ruleLet(x: BlockMemberSymbol, b: Block)(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    shapeBot(): bot =>
      transformSymbol(x): xSym =>
        StagedPath.mk(bot, xSym): y =>
          (Assign(x, y.p, _)):
            given Context = ctx.clone() += x.asPath -> y
            transformBlock(b): (z, ctx) =>
              blockCtor("ValueLit", Ls(Value.Lit(Tree.UnitLit(false)))): undefined =>
                blockCtor("Assign", Ls(xSym, undefined, z.code)): cde =>
                  StagedPath.mk(z.shapes, cde, "_let")(k(_, summon))

  def ruleEnd()(k: StagedPath => Block): Block =
    shapeBot(): sp =>
      blockCtor("End", Ls()): cde =>
        StagedPath.mk(sp, cde, "end")(k)

  def ruleBlk(b: Block)(using Context)(k: Path => Block): Block =
    transformBlock(b): x =>
      k(x.code)

  def ruleCls(cls: ClsLikeDefn, rest: Block)(using Context)(k: Path => Block): Block =
    (Define(cls, _)):
      transformBlock(rest): p =>
        transformSymbol(cls.sym): c =>
          def stageParamList(ps: ParamList)(k: Path => Block) =
            ps.params.map(p => transformSymbol(p.sym)).collectApply(tuple(_)(k))
          transformOption(cls.paramsOpt, stageParamList): paramsOpt =>
            assert(cls.companion.isEmpty) // nested module not supported
            optionNone(): none =>
              blockCtor("ClsLikeDefn", Ls(c, paramsOpt, none)): cls =>
                blockCtor("Define", Ls(cls, p.code))(k)

  // horrible abstraction boundary
  def ruleBranches(x: StagedPath, p: Path, arms: Ls[Case -> Block], dflt: Opt[Block])(using
      ctx: Context
  )(k: (ShapeSet, Path, Path, Opt[Path], Context) => Block): Block =
    // TODO: do filtering
    def f(arm: Case -> Block)(ctx: Context)(k: (StagedPath, Context) => Block): Block =
      ruleBranch(x, p, arm._1, arm._2)(using ctx)(k)

    arms.map(f).collectApply(summon): (arms, ctx) =>
      shapeDyn(): sp => // TODO
        tuple(arms.map(_.code)): arms =>
          dflt match
            case S(dflt) =>
              transformBlock(dflt)(using ctx): (dflt, ctx) =>
                k(sp, x.code, arms, S(dflt.code), ctx)
            case N => k(sp, x.code, arms, N, ctx)

  def ruleBranch(x: StagedPath, p: Path, cse: Case, b: Block)(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    transformCase(cse): cse =>
      fnFilter(x.shapes, cse): sp =>
        StagedPath.mk(sp, x.code): x0 =>
          val arm = Case.Lit(Tree.BoolLit(true)) -> ruleEnd()(k(_, ctx))
          (Match(x0.shapes.p.selSN("isEmpty"), Ls(arm), N, _)):
            given Context = ctx.clone() += p -> x0
            transformBlock(b): (y1, ctx) =>
              blockCtor("End", Ls()): end =>
                // TODO: use Arm type instead of Tup
                blockCtor("Tup", Ls(cse, y1.code)): cde =>
                  StagedPath.mk(y1.shapes, cde)(k(_, ctx.clone() -= p))

  // transformations of Block

  def transformPath(p: Path)(using ctx: Context)(k: StagedPath => Block): Block =
    // rulePath
    ctx.get(p).map(k).getOrElse:
      p match
        case r: Value.Ref => ruleVar(r)(k)
        case l: Value.Lit => ruleLit(l)(k)
        case s: Select => ruleSel(s)(k)
        case d: DynSelect => ruleDynSel(d)(k)
        case _ => ??? // not supported

  def transformResult(r: Result)(using Context)(k: StagedPath => Block): Block =
    r match
      case p: Path => transformPath(p)(k)
      case t: Tuple => ruleTup(t)(k)
      case i: Instantiate => ruleInst(i)(k)
      case _ => ??? // not supported

  def transformArg(a: Arg)(using Context)(k: StagedPath => Block): Block =
    val Arg(spread, value) = a
    optionNone(): opt =>
      transformPath(value): value =>
        blockCtor("Arg", Ls(opt, value.code)): cde =>
          StagedPath.mk(value.shapes, cde)(k)

  // provides list of shapes and list of codes to continuation
  def transformArgs(args: Ls[Arg])(using Context)(k: Ls[StagedPath] => Block): Block =
    args.map(transformArg).collectApply(k)

  def transformCase(cse: Case)(k: Path => Block): Block =
    cse match
      case Case.Lit(lit) => patternCtor("Lit", Ls(Value.Lit(lit)))(k)
      // wrong 2nd argument
      case Case.Cls(cls, path) =>
        // TODO: retrieve argument names from symbol?
        transformSymbol(cls): cls =>
          patternCtor("Cls", Ls(cls, toValue(0)))(k)
      case Case.Tup(len, inf) => patternCtor("Tup", Ls(len, inf).map(toValue))(k)
      case Case.Field(name, safe) => ??? // not supported

  // f.owner returns an InnerSymbol, but we need BlockMemberSymbol of the module to call the function
  // so we pass modSym instead
  def transformFunDefn(modSym: BlockMemberSymbol, f: FunDefn)(using Context): (FunDefn, Block) =
    val genSym = BlockMemberSymbol(f.sym.nme + "_gen", Nil, true)
    // TODO: remove it. only for test
    // TODO: put correct parameters instead of Nil
    val sym = modSym.asPath.selSN(genSym.nme)
    val debug =
      call(sym, Nil): ret =>
        fnPrintCode(StagedPath(ret).code)(_ => End())

    // NOTE: this debug printing only works for top-level modules, nested modules don't work
    (f.copy(sym = genSym, body = transformBlock(f.body)(_.end)), debug)

  def transformDefine(d: Define)(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    d.defn match
      case f: FunDefn => ???
      case v: ValDefn =>
        val ValDefn(t, x, r) = v
        ruleLet(x, Assign(x, r, d.rest))(k)
      case c: ClsLikeDefn =>
        ruleCls(c, d.rest): p =>
          ruleEnd(): b =>
            fnPrintCode(p)(_ => k(b, ctx))

  def transformBlock(b: Block)(using Context)(k: StagedPath => Block): Block =
    transformBlock(b)((p, _) => k(p))

  def transformBlock(b: Block)(using Context)(k: (StagedPath, Context) => Block): Block =
    // ruleBlk?
    val k2 = k(_, summon)
    b match
      case r: Return => ruleReturn(r)(k)
      case a: Assign => ruleAssign(a)(k)
      case d: Define => transformDefine(d)(k)
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
          val sym = c.sym.subst
          val companion = c.companion.get
          val (stagedMethods, debugPrintCode) = companion.methods
            .map(impl.transformFunDefn(sym, _)(using new HashMap())) // fold instead to retain env?
            .unzip
          val newCtor = impl.transformBlock(companion.ctor)(using new HashMap())(_ => End())
          val newCompanion = companion.copy(methods = companion.methods ++ stagedMethods, ctor = newCtor)
          val newModule = c.copy(sym = sym, companion = S(newCompanion))
          // debug is printed without calling the instrumented function
          val debugBlock = debugPrintCode.foldRight(rest)(impl.concat)
          Define(newModule, debugBlock)
        case _ => d
    case b => b
