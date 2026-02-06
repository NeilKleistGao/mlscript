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
    Scoped(Set(sym), Assign(sym, res, k(sym.asPath)))

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

  def ruleEnd(symName: String = "end")(k: Path => Block): Block =
    blockCtor("End", Ls(), symName)(k)

  def ruleBranches(x: Path, p: Path, arms: Ls[Case -> Block], dflt: Opt[Block], symName: String = "branches")(using Context)(k: (Path, Context) => Block): Block =
    def applyRuleBranch(cse: Case, block: Block)(f: Path => Context => Block)(ctx: Context): Block =
      transformCase(cse): cse =>
        transformBlock(block)(using ctx.clone() += p -> x): (y, ctx) =>
          // TODO: use Arm type instead of Tup
          tuple(Ls(cse, y), "branch"): cde =>
            f(cde)(ctx.clone() -= p)

    (arms.map(applyRuleBranch).collectApply(_: Ls[Path] => Context => Block)(summon)): arms =>
      ctx =>
        tuple(arms): arms =>
          ruleEnd(): e =>
            // TODO: use transformOption here
            def dfltStaged(k: (Path, Context) => Block) =
              dflt match
              case S(dflt) =>
                transformBlock(dflt)(using ctx.clone() += p -> x): (dflt, ctx) =>
                  optionSome(dflt)(k(_, ctx.clone() -= p))
              case N => optionNone()(k(_, ctx))
            dfltStaged: (dflt, ctx) =>
              blockCtor("Match", Ls(x, arms, dflt, e), symName)(k(_, ctx))

  // transformations of Block

  def transformPath(p: Path)(using ctx: Context)(k: Path => Block): Block =
    // rulePath
    ctx.get(p).map(k).getOrElse:
      p match
      case Value.Ref(l, disamb) =>
        transformSymbol(disamb.getOrElse(l)): sym =>
          blockCtor("ValueRef", Ls(sym), "var")(k)
      case l: Value.Lit =>
        blockCtor("ValueLit", Ls(l), "lit")(k)
      case s @ Select(p, i @ Tree.Ident(name)) =>
        transformPath(p): x =>
          val sym =
            if s.symbol.isDefined
            then transformSymbol(s.symbol.get)
            else blockCtor("Symbol", Ls(toValue(name)))
          sym: sym =>
            blockCtor("Select", Ls(x, sym), "sel")(k)
      case DynSelect(qual, fld, arrayIdx) =>
        transformPath(qual): x =>
          transformPath(fld): y =>
            blockCtor("DynSelect", Ls(x, y, toValue(arrayIdx)), "dynsel")(k)
      case _ => ??? // not supported

  def transformResult(r: Result)(using Context)(k: Path => Block): Block =
    r match
    case p: Path => transformPath(p)(k)
    case Tuple(mut, elems) =>
      assert(!mut, "mutable tuple not supported")
      transformArgs(elems): xs =>
        tuple(xs.map(_._1)): codes =>
          blockCtor("Tuple", Ls(codes), "tup")(k)
    case Instantiate(mut, cls, args) =>
      assert(!mut, "mutable instantiation not supported")
      transformArgs(args): xs =>
        transformPath(cls): cls =>
          tuple(xs.map(_._1)): codes =>
            blockCtor("Instantiate", Ls(cls, codes), "inst")(k)
    case Call(fun, args) =>
      val stagedFunPath =
        fun match
        case s @ Select(qual, Tree.Ident(name)) => s.symbol.flatMap({
            case t: TermSymbol => t.owner.flatMap({
                case sym: DefinitionSymbol[?] =>
                  sym.defn.get.hasStagedModifier.map(_ =>
                    Select(qual, Tree.Ident(name + "_gen"))(N)
                  )
              })
            case _ => N
          })
        case _ => N

      val newFun = stagedFunPath.getOrElse(fun)
      transformPath(newFun): fun =>
        transformArgs(args): args =>
          tuple(args.map(_._1)): tup =>
            blockCtor("Call", Ls(fun, tup), "app")(k)
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

  def transformBlock(b: Block)(using Context)(k: Path => Block): Block =
    transformBlock(b)((p, _) => k(p))

  def transformBlock(b: Block)(using ctx: Context)(k: (Path, Context) => Block): Block =
    b match
    case Return(res, implct) =>
      transformResult(res): x =>
        blockCtor("Return", Ls(x, toValue(implct)), "return")(k(_, ctx))
    case Assign(x, r, b) =>
      transformResult(r): y =>
        transformSymbol(x): xSym =>
          blockCtor("ValueRef", Ls(xSym)): xStaged =>
            (Assign(x, xStaged, _)):
              given Context = ctx.clone() += x.asPath -> xStaged
              transformBlock(b): (z, ctx) =>
                blockCtor("Assign", Ls(xSym, y, z), "assign")(k(_, ctx))
    case Define(cls: ClsLikeDefn, rest) =>
      assert(cls.companion.isEmpty, "nested module not supported")
      (Define(cls, _)):
        transformBlock(rest): p =>
          transformSymbol(cls.isym): c =>
            optionNone(): none => // TODO: handle companion object
              blockCtor("ClsLikeDefn", Ls(c, none)): cls =>
                blockCtor("Define", Ls(cls, p)): p =>
                  ruleEnd(): end =>
                    fnPrintCode(p)(k(end, ctx))
    case End(_) => ruleEnd()(k(_, ctx))
    case Match(p, ks, dflt, rest) =>
      transformPath(p): x =>
        ruleBranches(x, p, ks, dflt): (stagedMatch, ctx) =>
          transformBlock(rest)(using ctx): (z, ctx) =>
            fnConcat(stagedMatch, z, "match")(k(_, ctx))
    case Begin(sub, rest) =>
      // TODO: This is untested as there is no test case that generates the Begin block yet
      transformBlock(sub): (sub, ctx) =>
        transformBlock(rest)(using ctx): (rest, ctx) =>
          fnConcat(sub, rest)(k(_, ctx))
    case Scoped(syms, body) =>
      syms.toList.map(transformSymbol(_)).collectApply: symsStaged =>
        tuple(symsStaged): tup =>
          transformBlock(body): (body, ctx) =>
            blockCtor("Scoped", Ls(tup, body))(b => Scoped(syms, k(b, ctx)))
    case Label(labelSymbol, loop, body, rest) =>
      transformSymbol(labelSymbol): labelSymbol =>
        transformBlock(body): (body, ctx) =>
          transformBlock(rest)(using ctx): (rest, ctx) =>
            blockCtor("Label", Ls(labelSymbol, toValue(loop), body, rest))(k(_, ctx))
    case Break(labelSymbol) =>
      transformSymbol(labelSymbol): labelSymbol =>
        blockCtor("Break", Ls(labelSymbol))(k(_, ctx))
    case _ => ??? // not supported

  // f.owner returns an InnerSymbol, but we need BlockMemberSymbol of the module to call the function
  // so we pass modSym instead
  def transformFunDefn(modSym: BlockMemberSymbol, f: FunDefn): (FunDefn, Block) =
    val genSymName = f.sym.nme + "_instr"
    val genSym = BlockMemberSymbol(genSymName, Nil, false)
    val sym = modSym.asPath.selSN(genSymName)
    // NOTE: this debug printing only works for top-level modules, nested modules don't work

    // turn into fundefn
    val dSym = TermSymbol(f.dSym.k, f.dSym.owner, Tree.Ident(f.sym.nme + "_instr"))
    val argSyms = f.params.flatMap(_.params).map(_.sym)
    val newBody =
      val rest = transformBlock(f.body)(using new HashMap): body =>
        // maintain parameter names in instrumented code
        f.params.map(
          _.params.map(p => blockCtor("Symbol", Ls(toValue(p.sym.nme)))).collectApply
        ).collectApply: paramListSyms =>
          blockCtor("Symbol", Ls(toValue(f.sym.nme))): sym =>
            paramListSyms.map(tuple(_)).collectApply: tups =>
              tuple(tups): tup =>
                blockCtor("FunDefn", Ls(sym, tup, body, toValue(true))): block =>
                  Return(block, false)
      Scoped(Set(argSyms*), rest)

    // TODO: remove it. only for test
    val debug = call(sym, Nil)(fnPrintCode(_)(End()))
    val newFun = f.copy(sym = genSym, dSym = dSym, params = Ls(PlainParamList(Nil)), body = newBody)(false)
    (newFun, debug)

// TODO: rename as InstrumentationTransformer?
class Instrumentation(using State) extends BlockTransformer(new SymbolSubst()):
  val impl = new InstrumentationImpl

  def concat(b1: Block, b2: Block): Block =
    b1.mapTail {
      case _: End => b2
      case _ => ???
    }

  override def applyBlock(b: Block): Block =
    super.applyBlock(b) match
    // find modules with staged annotation
    case Define(c: ClsLikeDefn, rest) if c.companion.exists(_.isym.defn.exists(_.hasStagedModifier.isDefined)) =>
      val sym = c.sym.subst
      val companion = c.companion.get
      val (stagedMethods, debugPrintCode) = companion.methods
        .map(impl.transformFunDefn(sym, _))
        .unzip

      val unit = State.runtimeSymbol.asPath.selSN("Unit")
      val debugBlock = debugPrintCode.foldRight(Return(unit, true))(concat)
      def debugCont(rest: Block) =
        Begin(debugBlock, rest)
      val newCtor = impl.transformBlock(companion.ctor)(using new HashMap())(_ => debugCont(End()))
      val newCompanion = companion.copy(methods = companion.methods ++ stagedMethods, ctor = newCtor)
      val newModule = c.copy(sym = sym, companion = S(newCompanion))
      Define(newModule, rest)
    case b => b
