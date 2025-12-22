package hkmc2
package codegen

import utils.*
import hkmc2.Message.MessageContext

import scala.collection.mutable.HashMap
import scala.util.chaining._

import mlscript.utils.*, shorthands.*

import semantics.*
import semantics.Elaborator.State

import syntax.{Literal, Tree}

// it seems some logic should be deferred to BlockTransformer to dedup code
// but it doesn't accept the current context, so applications seem limited

// it should be possible to cache some common constructions (End, Option) into the context
// this avoids having to rebuild the same shapes everytime they are needed

// transform Block to Block IR so that it can be instrumented in mlscript
class InstrumentationImpl(using State):
  type ArgWrappable = Path | Symbol
  type Context = HashMap[Path, Path]

  def asArg(x: ArgWrappable): Arg =
    x match
      case p: Path => p.asArg
      case l: Symbol => l.asPath.asArg

  // null and undefined are missing
  def toValue(lit: Str | Int | BigDecimal | Bool): Value =
    val l = lit match
      case i: Int => Tree.IntLit(i)
      case b: Bool => Tree.BoolLit(b)
      case s: Str => Tree.StrLit(s)
      case n: BigDecimal => Tree.DecLit(n)
    Value.Lit(l)

  extension [A, B](ls: Ls[(A => B) => B])
    def collectApply(f: Ls[A] => B): B =
      // defer applying k while prepending new elements to the list
      ls.foldRight((_: Ls[A] => B)(Nil))((headCont, tailCont) =>
        k =>
          headCont: head =>
            tailCont: tail =>
              k(head :: tail)
      )(f)

  // helpers for constructing Block

  def assign(res: Result, symName: Str = "tmp")(k: Path => Block): Block =
    // TODO: skip assignment if res: Path?
    val sym = new TempSymbol(N, symName)
    Assign(sym, res, k(sym.asPath))

  def tuple(elems: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    assign(Tuple(false, elems.map(asArg)), symName)(k)

  def ctor(cls: Path, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    assign(Instantiate(false, cls, args.map(asArg)), symName)(k)

  // isMlsFun is probably always true?
  def call(fun: Path, args: Ls[ArgWrappable], isMlsFun: Bool = true, symName: Str = "tmp")(k: Path => Block): Block =
    assign(Call(fun, args.map(asArg))(isMlsFun, false, false), symName)(k)

  // helpers for instrumenting Block

  def blockMod(name: Str) = summon[State].blockSymbol.asPath.selSN(name)
  def optionMod(name: Str) = summon[State].optionSymbol.asPath.selSN(name)

  def blockCtor(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    ctor(blockMod(name), args, symName)(k)
  def optionSome(arg: ArgWrappable, symName: Str = "tmp")(k: Path => Block): Block =
    ctor(optionMod("Some"), Ls(arg), symName)(k)
  def optionNone(symName: Str = "tmp")(k: Path => Block): Block =
    assign(optionMod("None"), symName)(k)

  def blockCall(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    call(blockMod(name), args, symName = symName)(k)

  // linking functions defined in MLscipt

  def fnPrintCode(p: Path)(k: Block): Block =
    // discard result, we only care about side effect
    blockCall("printCode", Ls(p))(_ => k)

  def fnConcat(p1: Path, p2: Path, symName: String = "concat")(k: Path => Block): Block =
    blockCall("concat", Ls(p1, p2), symName)(k)

  // transformation helpers

  def transformSymbol(sym: Symbol, symName: Str = "sym")(k: Path => Block): Block =
    sym match
      case clsSym: ClassSymbol =>
        transformParamsOpt(clsSym.defn.get.paramsOpt): paramsOpt =>
          blockCtor("ClassSymbol", Ls(toValue(sym.nme), paramsOpt), symName)(k)
      case t: TermSymbol if t.defn.exists(_.sym.asCls.isDefined) =>
        transformSymbol(t.defn.get.sym.asCls.get, symName)(k)
      case _ => blockCtor("Symbol", Ls(toValue(sym.nme)), symName)(k)

  def transformOption[A](xOpt: Opt[A], f: A => (Path => Block) => Block)(k: Path => Block): Block =
    xOpt match
      case S(x) => f(x)(optionSome(_)(k))
      case N => optionNone()(k)

  // instrumentation rules

  def ruleLit(l: Value.Lit, symName: String = "lit")(k: Path => Block): Block =
    blockCtor("ValueLit", Ls(l), symName)(k)

  // not in formalization
  def ruleVar(r: Value.Ref, symName: String = "var")(k: Path => Block): Block =
    val Value.Ref(l, disamb) = r
    transformSymbol(disamb.getOrElse(l)): sym =>
      blockCtor("ValueRef", Ls(sym), symName)(k)

  def ruleTup(t: Tuple, symName: String = "tup")(using Context)(k: Path => Block): Block =
    assert(!t.mut, "mutable tuple not supported")
    transformArgs(t.elems): xs =>
      tuple(xs.map(_._1)): codes =>
        blockCtor("Tuple", Ls(codes), symName)(k)

  def ruleSel(s: Select, symName: String = "sel")(using Context)(k: Path => Block): Block =
    val Select(p, i @ Tree.Ident(name)) = s
    transformPath(p): x =>
      blockCtor("Symbol", Ls(toValue(name))): name =>
        blockCtor("Select", Ls(x, name), symName)(k)

  def ruleDynSel(d: DynSelect, symName: String = "dynsel")(using Context)(k: Path => Block): Block =
    transformPath(d.qual): x =>
      transformPath(d.fld): y =>
        blockCtor("DynSelect", Ls(x, y, toValue(d.arrayIdx)), symName)(k)

  def ruleApp(c: Call, symName: String = "app")(using Context)(k: Path => Block): Block =
    transformPath(c.fun): fun =>
      transformArgs(c.args): args =>
        tuple(args.map(_._1)): tup =>
          blockCtor("Call", Ls(fun, tup), symName)(k)

  def ruleInst(i: Instantiate, symName: String = "inst")(using Context)(k: Path => Block): Block =
    val Instantiate(mut, cls, args) = i
    assert(!mut, "mutable instantiation not supported")
    transformArgs(args): xs =>
      transformPath(cls): cls =>
        tuple(xs.map(_._1)): codes =>
          blockCtor("Instantiate", Ls(cls, codes), "inst")(k)

  def ruleReturn(r: Return, symName: String = "return")(using Context)(k: (Path, Context) => Block): Block =
    transformResult(r.res): x =>
      blockCtor("Return", Ls(x, toValue(false)), symName): cde =>
        k(cde, summon)

  def ruleMatch(m: Match, symName: String = "match")(using Context)(k: (Path, Context) => Block): Block =
    val Match(p, ks, dflt, rest) = m
    transformPath(p): x =>
      ruleBranches(x, p, ks, dflt): (stagedMatch, ctx1) =>
        transformBlock(rest)(using ctx1): (z, ctx2) =>
          fnConcat(stagedMatch, z, symName): cde =>
            k(cde, ctx2)

  def ruleAssign(a: Assign, symName: String = "assign")(using ctx: Context)(k: (Path, Context) => Block): Block =
    val Assign(x, r, b) = a
    transformResult(r): y =>
      transformSymbol(x): xSym =>
        blockCtor("ValueRef", Ls(xSym)): xStaged =>
          // x should always be defined, either as an argument to the function or in a Scope Block
          assert(ctx.get(x.asPath).isDefined)
          (Assign(x, xStaged, _)):
            given Context = ctx.clone() += x.asPath -> xStaged
            transformBlock(b): (z, ctx) =>
              blockCtor("Assign", Ls(xSym, y, z), symName)(k(_, ctx))

  def ruleEnd(symName: String = "end")(k: Path => Block): Block =
    blockCtor("End", Ls(), symName)(k)

  def ruleBlk(b: Block)(using Context)(k: Path => Block): Block =
    transformBlock(b)(k)

  def ruleCls(cls: ClsLikeDefn, rest: Block)(using Context)(k: Path => Block): Block =
    assert(cls.companion.isEmpty, "nested module not supported")
    (Define(cls, _)):
      transformBlock(rest): p =>
        transformParamsOpt(cls.paramsOpt): paramsOpt =>
          transformSymbol(cls.isym): c =>
            optionNone(): none => // TODO: handle companion object
              blockCtor("ClsLikeDefn", Ls(c, none)): cls =>
                blockCtor("Define", Ls(cls, p))(k)

  def ruleBranches(x: Path, p: Path, arms: Ls[Case -> Block], dflt: Opt[Block], symName: String = "branches")(using Context)(k: (Path, Context) => Block): Block =
    def applyRuleBranch(cse: Case, block: Block)(f: Path => (Context, Path) => Block)(ctx: Context, x: Path): Block =
      ruleBranch(x, p, cse, block)(using ctx)((y, ctx, x) => f(y)(ctx, x))

    val a = arms.map(applyRuleBranch).collectApply
    ((f: (Ls[Path], Context) => Block) => a(ys => (ctx, _) => f(ys, ctx))(summon, x)): (arms, ctx) =>
      tuple(arms): arms =>
        ruleEnd(): e =>
          // TODO: use transformOption here
          def dfltStaged(k: (Path, Context) => Block) = dflt match
            case S(dflt) => ruleWildCard(x, p, dflt)((dflt, ctx) => optionSome(dflt)(k(_, ctx)))
            case N => optionNone()(k(_, ctx))
          dfltStaged: (dflt, ctx) =>
            blockCtor("Match", Ls(x, arms, dflt, e), symName)(k(_, ctx))

  def ruleBranch(x: Path, p: Path, cse: Case, b: Block, symName: String = "branch")(using ctx: Context)(k: (Path, Context, Path) => Block): Block =
    transformCase(cse): cse =>
      transformBlock(b)(using ctx.clone() += p -> x): (y, ctx) =>
        // TODO: use Arm type instead of Tup
        tuple(Ls(cse, y), symName): cde =>
          k(cde, ctx.clone() -= p, x)

  def ruleWildCard(x: Path, p: Path, b: Block)(using ctx: Context)(k: (Path, Context) => Block): Block =
    given Context = ctx.clone() += p -> x
    transformBlock(b): (y, ctx) =>
      k(y, ctx.clone() -= p)

  // transformations of Block

  def transformPath(p: Path)(using ctx: Context)(k: Path => Block): Block =
    // rulePath
    ctx.get(p).map(k).getOrElse:
      p match
        case r: Value.Ref => ruleVar(r)(k)
        case l: Value.Lit => ruleLit(l)(k)
        case s: Select => ruleSel(s)(k)
        case d: DynSelect => ruleDynSel(d)(k)
        case _ => ??? // not supported

  def transformResult(r: Result)(using Context)(k: Path => Block): Block =
    r match
      case p: Path => transformPath(p)(k)
      case t: Tuple => ruleTup(t)(k)
      case i: Instantiate => ruleInst(i)(k)
      case c: Call => ruleApp(c)(k)
      case _ => ??? // not supported

  def transformArg(a: Arg)(using Context)(k: ((Path, Bool)) => Block): Block =
    val Arg(spread, value) = a
    transformOption(spread, bool => assign(toValue(bool))): spreadStaged =>
      transformPath(value): value =>
        blockCtor("Arg", Ls(spreadStaged, value)): cde =>
          k(cde, spread.isDefined)

  def transformArgs(args: Ls[Arg])(using Context)(k: Ls[(Path, Bool)] => Block): Block =
    args.map(transformArg).collectApply(k)

  def transformParamList(ps: ParamList)(k: Path => Block) =
    ps.params.map(p => transformSymbol(p.sym)).collectApply(tuple(_)(k))

  def transformParamsOpt(pOpt: Opt[ParamList])(k: Path => Block) =
    transformOption(pOpt, transformParamList)(k)

  def transformCase(cse: Case)(using Context)(k: Path => Block): Block =
    cse match
      case Case.Lit(lit) => blockCtor("Lit", Ls(Value.Lit(lit)))(k)
      case Case.Cls(cls, path) =>
        transformSymbol(cls): cls =>
          transformPath(path): path =>
            blockCtor("Cls", Ls(cls, path))(k)
      case Case.Tup(len, inf) => blockCtor("Tup", Ls(len, inf).map(toValue))(k)
      case Case.Field(name, safe) => ??? // not supported

  // f.owner returns an InnerSymbol, but we need BlockMemberSymbol of the module to call the function
  // so we pass modSym instead
  def transformFunDefn(modSym: BlockMemberSymbol, f: FunDefn): (FunDefn, Block) =
    val genSym = BlockMemberSymbol(f.sym.nme + "_gen", Nil, true)
    val sym = modSym.asPath.selSN(genSym.nme)
    // NOTE: this debug printing only works for top-level modules, nested modules don't work
    // TODO: remove it. only for test
    val debug =
      blockCtor("ValueLit", Ls(Value.Lit(Tree.UnitLit(false)))): undef =>
        // TODO: put correct parameters instead of End
        // TODO: handle curried arguments
        val argsList = f.params.map(ps => List.fill(ps.params.length)(undef))
        def makeCalls(k: Path => Block) =
          argsList.foldRight(k)((args, cont) => res => call(res, args)(cont))(sym)
        makeCalls: ret =>
          val p = ret
          fnPrintCode(p)(End())

    val dSym = TermSymbol(f.dSym.k, f.dSym.owner, Tree.Ident(f.sym.nme + "_gen"))
    val args = f.params.flatMap(_.params).map(_.sym)
    val newBody =
      ruleEnd(): end =>
        given Context = HashMap(args.map(s => Value.Ref(s, N) -> Value.Ref(s, N))*)
        transformBlock(f.body)(p => Return(p, false))
    val newFun = f.copy(sym = genSym, dSym = dSym, body = newBody)(false)
    (newFun, debug)

  def transformDefine(d: Define)(using Context)(k: (Path, Context) => Block): Block =
    d.defn match
      case c: ClsLikeDefn =>
        ruleCls(c, d.rest): p =>
          ruleEnd(): b =>
            fnPrintCode(p)(k(b, summon))
      case _: FunDefn | _: ValDefn => ???

  // TODO
  // discards result of sub
  def transformBegin(b: Begin)(using Context)(k: (Path, Context) => Block): Block =
    transformBlock(b.sub): (sub, ctx) =>
      transformBlock(b.rest)(using ctx): (rest, ctx) =>
        fnConcat(sub, rest): block =>
          k(block, ctx)

  def transformScoped(s: Scoped)(using ctx: Context)(k: (Path, Context) => Block): Block =
    val Scoped(syms, body) = s
    blockCtor("ValueLit", Ls(Value.Lit(Tree.UnitLit(false)))): undef =>
      val newCtx = ctx.clone() ++ syms.map(_.asPath -> undef)
      transformBlock(body)(using newCtx): (p, ctx) =>
        k(p, ctx)

  // ruleBlk?
  def transformBlock(b: Block)(using Context)(k: Path => Block): Block =
    transformBlock(b)((p, _) => k(p))

  def transformBlock(b: Block)(using Context)(k: (Path, Context) => Block): Block =
    b match
      case r: Return => ruleReturn(r)(k)
      case a: Assign => ruleAssign(a)(k)
      case d: Define => transformDefine(d)(k)
      case End(_) => ruleEnd()(k(_, summon))
      case m: Match => ruleMatch(m)(k)
      // temporary measure to accept returning an array
      // use BlockTransformer here?
      case b: Begin => ??? // transformBegin(b)(k)
      // case Begin(b1, b2) => transformBlock(concat(b1, b2))(k)
      case s: Scoped => transformScoped(s)(k)
      case _ => ??? // not supported

// TODO: rename as InstrumentationTransformer?
class Instrumentation(using State) extends BlockTransformer(new SymbolSubst()):
  val impl = new InstrumentationImpl

  def concat(b1: Block, b2: Block): Block =
    b1.mapTail {
      case _: End => b2
      case _ => ???
    }

  override def applyBlock(b: Block): Block = super.applyBlock(b) match
    case d @ Define(defn, rest) =>
      defn match
        // find modules with staged annotation
        case c: ClsLikeDefn if c.companion.exists(_.isym.defn.exists(_.hasStagedModifier.isDefined)) =>
          val sym = c.sym.subst
          val companion = c.companion.get
          val (stagedMethods, debugPrintCode) = companion.methods
            .map(impl.transformFunDefn(sym, _))
            .unzip
          val newCtor = impl.transformBlock(companion.ctor)(using new HashMap())(_ => End())
          val newCompanion = companion.copy(methods = companion.methods ++ stagedMethods, ctor = newCtor)
          val newModule = c.copy(sym = sym, companion = S(newCompanion))
          // debug is printed after definition
          val debugBlock = debugPrintCode.foldRight(rest)(concat)
          Define(newModule, debugBlock)
        case _ => d
    case b => b
