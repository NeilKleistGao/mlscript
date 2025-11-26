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
      case _: End => b2
      case _ => ???
    }

  extension [A, B](ls: Ls[(A => B) => B])
    def collectApply(f: Ls[A] => B): B =
      // defer applying k while prepending new elements to the list
      ls.foldRight((_: Ls[A] => B)(Nil))((headCont, tailCont) =>
        k =>
          headCont: head =>
            tailCont: tail =>
              k(head :: tail)
      )(f)

  extension [A, B, C](f: A => B => C)
    def flip: B => A => C = b => f(_)(b)

  // helpers for constructing Block

  def assign(res: Result, symName: Str = "tmp")(k: Path => Block): Assign =
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
  def shapeSetMod(name: Str) = summon[State].shapeSetSymbol.asPath.selSN(name)

  def blockCtor(name: Str, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    ctor(blockMod(name), args, symName)(k)
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
    def apply(shapeSet: ShapeSet, code: Path, symName: Str = "tmp")(k: StagedPath => Block): Block =
      tuple(Ls(shapeSet.p, code), symName)(p => k(StagedPath(p)))

  // linking functions defined in MLscipt

  def fnPrintCode(p: Path)(k: Block): Block =
    // discard result, we only care about side effect
    blockCall("printCode", Ls(p))(_ => k)
  def fnPrintShapeSet(p: ShapeSet)(rest: Block): Block =
    shapeSetCall("printShapeSet", Ls(p.p))(_ => rest)

  def shapeBot()(k: ShapeSet => Block): Block =
    shapeSetCall("mkBot", Ls())(s => k(ShapeSet(s)))
  def shapeDyn()(k: ShapeSet => Block): Block =
    shapeSetCall("mkDyn", Ls())(s => k(ShapeSet(s)))
  def shapeLit(p: Path)(k: ShapeSet => Block): Block =
    shapeSetCall("mkLit", Ls(p))(s => k(ShapeSet(s)))
  def shapeArr(ps: Ls[ShapeSet], inf: Bool)(k: ShapeSet => Block): Block =
    tuple(ps, "test"): tup =>
      shapeSetCall("mkArr", Ls(tup, toValue(inf)))(s => k(ShapeSet(s)))
  def shapeClass(cls: Path, params: Ls[ShapeSet])(k: ShapeSet => Block): Block =
    tuple(params): params =>
      shapeSetCall("mkClass", Ls(cls, params))(s => k(ShapeSet(s)))

  def fnConcat(p1: Path, p2: Path)(k: Path => Block): Block =
    blockCall("concat", Ls(p1, p2))(k)
  def fnPruneBadArms(arms: Path)(k: Path => Block): Block =
    shapeSetCall("pruneBadArms", Ls(arms))(k)
  def fnMrg(s1: ShapeSet, s2: ShapeSet)(k: ShapeSet => Block): Block =
    shapeSetCall("mrg", Ls(s1, s2))(s => k(ShapeSet(s)))
  def fnSel(s1: ShapeSet, s2: ShapeSet)(k: ShapeSet => Block): Block =
    shapeSetCall("sel", Ls(s1, s2))(s => k(ShapeSet(s)))
  def fnFilter(s1: ShapeSet, s2: Path)(k: ShapeSet => Block): Block =
    shapeSetCall("filter", Ls(s1, s2))(s => k(ShapeSet(s)))
  def fnUnion(s1: ShapeSet, s2: ShapeSet)(k: ShapeSet => Block): Block =
    shapeSetCall("union", Ls(s1, s2))(s => k(ShapeSet(s)))

  // transformation helpers

  def transformSymbol[S <: Symbol](sym: S, symName: Str = "sym")(k: Path => Block): Block =
    sym match
      case clsSym: ClassSymbol =>
        transformParamsOpt(clsSym.defn.get.paramsOpt): paramsOpt =>
          blockCtor("ClassSymbol", Ls(toValue(sym.nme), paramsOpt), symName)(k)
      case _ => blockCtor("Symbol", Ls(toValue(sym.nme)), symName)(k)

  def transformOption[A](xOpt: Opt[A], f: A => (Path => Block) => Block)(k: Path => Block): Block =
    xOpt match
      case S(x) => f(x)(optionSome(_)(k))
      case N => optionNone()(k)

  // instrumentation rules

  def ruleLit(l: Value.Lit, symName: String = "lit")(k: StagedPath => Block): Block =
    shapeLit(l): sp =>
      blockCtor("ValueLit", Ls(l)): cde =>
        StagedPath(sp, cde, symName)(k)

  // not in formalization
  def ruleVar(r: Value.Ref, symName: String = "var")(k: StagedPath => Block): Block =
    val Value.Ref(l, disamb) = r
    transformSymbol(disamb.getOrElse(l)): sym =>
      blockCtor("ValueRef", Ls(sym)): cde =>
        // variable defined outside of scope, may be a reference to a class
        shapeDyn(): sp =>
          StagedPath(sp, cde, symName)(k)

  def ruleTup(t: Tuple, symName: String = "tup")(using Context)(k: StagedPath => Block): Block =
    assert(!t.mut, "mutable tuple not supported")
    transformArgs(t.elems): xs =>
      shapeArr(xs.map(_._1.shapes), xs.exists(_._2)): sp =>
        tuple(xs.map(_._1.code)): codes =>
          blockCtor("Tuple", Ls(codes)): cde =>
            StagedPath(sp, cde, symName)(k)

  def ruleSel(s: Select, symName: String = "sel")(using Context)(k: StagedPath => Block): Block =
    val Select(p, i @ Tree.Ident(name)) = s
    transformPath(p): x =>
      shapeLit(toValue(name)): n =>
        fnSel(x.shapes, n): sp =>
          blockCtor("Symbol", Ls(toValue(name))): name =>
            blockCtor("Select", Ls(x.code, name)): cde =>
              StagedPath(sp, cde, symName)(k)

  def ruleDynSel(d: DynSelect, symName: String = "dynsel")(using Context)(k: StagedPath => Block): Block =
    transformPath(d.qual): x =>
      transformPath(d.fld): y =>
        fnSel(x.shapes, y.shapes): sp =>
          blockCtor("DynSelect", Ls(x.code, y.code, toValue(d.arrayIdx))): cde =>
            StagedPath(sp, cde, symName)(k)

  def ruleInst(i: Instantiate, symName: String = "inst")(using Context)(k: StagedPath => Block): Block =
    val Instantiate(mut, cls, args) = i
    assert(!mut, "mutable instantiation not supported")
    transformArgs(args): xs =>
      val sym = cls match
        // TODO: if class is staged, we can just use Symbol without storing the arguments
        case Value.Ref(l, S(disamb)) => transformSymbol(disamb)
        case s: Select if s.symbol.isDefined => transformSymbol(s.symbol.get)
        case _ => transformSymbol(TempSymbol(N, "TODO"))
      sym: sym =>
        shapeClass(sym, xs.map(_._1.shapes)): sp =>
          // reuse instrumentation logic, shape of cls is discarded
          // possible to skip this? this uses ruleVar, which is not in formalization
          transformPath(cls): cls =>
            tuple(xs.map(_._1.code)): codes =>
              blockCtor("Instantiate", Ls(cls.code, codes)): cde =>
                StagedPath(sp, cde, symName)(k)

  def ruleReturn(r: Return, symName: String = "return")(using Context)(k: (StagedPath, Context) => Block): Block =
    transformResult(r.res): x =>
      blockCtor("Return", Ls(x.code, toValue(false))): cde =>
        StagedPath(x.shapes, cde, symName)(k(_, summon))

  def ruleMatch(m: Match, symName: String = "match")(using Context)(k: (StagedPath, Context) => Block): Block =
    val Match(p, ks, dflt, rest) = m
    transformPath(p): x =>
      ruleBranches(x, p, ks, dflt): (stagedMatch, ctx1) =>
        transformBlock(rest)(using ctx1): (z, ctx2) =>
          fnMrg(stagedMatch.shapes, z.shapes): sp =>
            fnConcat(stagedMatch.code, z.code): cde =>
              StagedPath(sp, cde, symName)(k(_, ctx2))

  def ruleAssign(a: Assign, symName: String = "assign")(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    val Assign(x, r, b) = a
    transformResult(r): y =>
      transformSymbol(x): xSym =>
        blockCtor("ValueRef", Ls(xSym)): xStaged =>
          // if ctx contains x, x was defined earlier
          // otherwise, x is defined here
          ctx.get(x.asPath) match
            case S(x1) =>
              fnUnion(y.shapes, x1.shapes): sp =>
                StagedPath(sp, xStaged): x2 =>
                  (Assign(x, x2.p, _)):
                    given Context = ctx.clone() += x.asPath -> x2
                    transformBlock(b): (z, ctx) =>
                      blockCtor("Assign", Ls(xSym, y.code, z.code)): cde =>
                        StagedPath(z.shapes, cde, symName)(k(_, ctx))
            case N =>
              StagedPath(y.shapes, xStaged): x2 =>
                // propagate shape information for future references to x
                (Assign(x, y.p, _)):
                  given Context = ctx.clone() += x.asPath -> x2
                  transformBlock(b): (z, ctx) =>
                    blockCtor("Assign", Ls(xSym, y.code, z.code)): cde =>
                      StagedPath(z.shapes, cde, symName)(k(_, ctx))

  def ruleLet(x: BlockMemberSymbol, b: Block, symName: String = "_let")(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    shapeBot(): bot =>
      transformSymbol(x): xSym =>
        StagedPath(bot, xSym): y =>
          (Assign(x, y.p, _)):
            given Context = ctx.clone() += x.asPath -> y
            transformBlock(b): (z, ctx) =>
              blockCtor("ValueLit", Ls(Value.Lit(Tree.UnitLit(false)))): undefined =>
                blockCtor("Assign", Ls(xSym, undefined, z.code)): cde =>
                  StagedPath(z.shapes, cde, symName)(k(_, summon))

  def ruleEnd(symName: String = "end")(k: StagedPath => Block): Block =
    shapeBot(): sp =>
      blockCtor("End", Ls()): cde =>
        StagedPath(sp, cde, symName)(k)

  def ruleBlk(b: Block)(using Context)(k: Path => Block): Block =
    transformBlock(b)(k apply _.code)

  def ruleCls(cls: ClsLikeDefn, rest: Block)(using Context)(k: Path => Block): Block =
    assert(cls.companion.isEmpty, "nested module not supported")
    (Define(cls, _)):
      transformBlock(rest): p =>
        transformParamsOpt(cls.paramsOpt): paramsOpt =>
          transformSymbol(cls.isym): c =>
            optionNone(): none => // TODO: handle companion object
              blockCtor("ClsLikeDefn", Ls(c, none)): cls =>
                blockCtor("Define", Ls(cls, p.code))(k)

  def ruleBranches(x: StagedPath, p: Path, arms: Ls[Case -> Block], dflt: Opt[Block], symName: String = "branches")(using Context)(k: (StagedPath, Context) => Block): Block =
    arms.map((cse, block) => (f: StagedPath => Context => Block) => (ruleBranch(x, p, cse, block)(using _)).flip(f(_)(_)))
      .collectApply
      .pipe(_.flip(summon)): arms =>
        ctx =>
          tuple(arms.map(_.p)): tup =>
            fnPruneBadArms(tup): res =>
              val result = StagedPath(res)
              val sp = result.shapes
              val arms = result.code
              blockCtor("End", Ls()): e =>
                // TODO: use transformOption here
                def dfltStaged(k: (Path, Context) => Block) = dflt match
                  case S(dflt) => ruleWildCard(x, p, dflt): (dflt, ctx) =>
                      optionSome(dflt.code)(k(_, ctx))
                  case N => optionNone()(k(_, ctx))
                dfltStaged: (dflt, ctx) =>
                  blockCtor("Match", Ls(x.code, arms, dflt, e)): m =>
                    StagedPath(sp, m, symName)(k(_, ctx))

  def ruleBranch(x: StagedPath, p: Path, cse: Case, b: Block, symName: String = "branch")(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    transformCase(cse): cse =>
      fnFilter(x.shapes, cse): sp =>
        call(sp.p.selSN("isEmpty"), Ls()): scrut =>
          ruleEnd(): e =>
            val res = new TempSymbol(N, "tmp")
            val arm = Case.Lit(Tree.BoolLit(true)) -> Assign(res, e.p, End())
            val dflt =
              StagedPath(sp, x.code): x0 =>
                given Context = ctx.clone() += p -> x0
                transformBlock(b): (y1, ctx) =>
                  // TODO: use Arm type instead of Tup
                  tuple(Ls(cse, y1.code)): cde =>
                    StagedPath(y1.shapes, cde, symName): ret =>
                      Assign(res, ret.p, End())
            (Match(scrut, Ls(arm), S(dflt), _)):
              k(StagedPath(Value.Ref(res)), ctx.clone() -= p)

  // this partially applies rules from filter to account for difference between Block.Case and Match pattern in the formalization
  // to avoid defining the `_` pattern in Block.Case, we apply filter(s, _) = s
  def ruleWildCard(x: StagedPath, p: Path, b: Block)(using ctx: Context)(k: (StagedPath, Context) => Block): Block =
    call(x.shapes.p.selSN("isEmpty"), Ls()): scrut =>
      val arm = Case.Lit(Tree.BoolLit(true)) -> ruleEnd()(p => Return(p.p, false))
      (Match(scrut, Ls(arm), N, _)):
        given Context = ctx.clone() += p -> x
        transformBlock(b): (y1, ctx) =>
          k(y1, ctx.clone() -= p)

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
      case c: Call => ???
      case _ => ??? // not supported

  def transformArg(a: Arg)(using Context)(k: ((StagedPath, Bool)) => Block): Block =
    val Arg(spread, value) = a
    transformOption(spread, bool => assign(toValue(bool))): spreadStaged =>
      transformPath(value): value =>
        blockCtor("Arg", Ls(spreadStaged, value.code)): cde =>
          StagedPath(value.shapes, cde): res =>
            k(res, spread.isDefined)

  def transformArgs(args: Ls[Arg])(using Context)(k: Ls[(StagedPath, Bool)] => Block): Block =
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
            blockCtor("Cls", Ls(cls, path.code))(k)
      case Case.Tup(len, inf) => blockCtor("Tup", Ls(len, inf).map(toValue))(k)
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
        val p = StagedPath(ret)
        (fnPrintShapeSet(p.shapes)(_)):
          fnPrintCode(p.code)(End())

    // NOTE: this debug printing only works for top-level modules, nested modules don't work
    (f.copy(sym = genSym, body = transformBlock(f.body)(_.end))(false), debug)

  def transformDefine(d: Define)(using Context)(k: (StagedPath, Context) => Block): Block =
    d.defn match
      case f: FunDefn => ???
      case v: ValDefn =>
        val ValDefn(_, x, r) = v
        ruleLet(x, Assign(x, r, d.rest))(k)
      case c: ClsLikeDefn =>
        ruleCls(c, d.rest): p =>
          ruleEnd(): b =>
            fnPrintCode(p)(k(b, summon))

  // ruleBlk?
  def transformBlock(b: Block)(using Context)(k: StagedPath => Block): Block =
    transformBlock(b)((p, _) => k(p))

  def transformBlock(b: Block)(using Context)(k: (StagedPath, Context) => Block): Block =
    val k2 = k(_, summon)
    b match
      case r: Return => ruleReturn(r)(k)
      case a: Assign => ruleAssign(a)(k)
      case d: Define => transformDefine(d)(k)
      case End(_) => ruleEnd()(k2)
      case m: Match => ruleMatch(m)(k)
      // temporary measure to accept returning an array
      // use BlockTransformer here?
      case Begin(b1, b2) => transformBlock(concat(b1, b2))(k)
      case _ => ??? // not supported

// TODO: rename as InstrumentationTransformer?
class Instrumentation(using State) extends BlockTransformer(new SymbolSubst()):
  val impl = new InstrumentationImpl

  override def applyBlock(b: Block): Block = super.applyBlock(b) match
    case d @ Define(defn, rest) =>
      defn match
        // find modules with staged annotation
        case c: ClsLikeDefn if c.companion.exists(_.isym.defn.exists(_.hasStagedModifier.isDefined)) =>
          val sym = c.sym.subst
          val companion = c.companion.get
          val (stagedMethods, debugPrintCode) = companion.methods
            .map(impl.transformFunDefn(sym, _)(using new HashMap())) // fold instead to retain env?
            .unzip
          val newCtor = impl.transformBlock(companion.ctor)(using new HashMap())(_ => End())
          val newCompanion = companion.copy(methods = companion.methods ++ stagedMethods, ctor = newCtor)
          val newModule = c.copy(sym = sym, companion = S(newCompanion))
          // debug is printed after definition
          val debugBlock = debugPrintCode.foldRight(rest)(impl.concat)
          Define(newModule, debugBlock)
        case _ => d
    case b => b
