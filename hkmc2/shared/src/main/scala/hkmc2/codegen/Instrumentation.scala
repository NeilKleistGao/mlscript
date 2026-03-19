package hkmc2
package codegen

import utils.*
import hkmc2.Message.MessageContext

import scala.collection.mutable.HashMap
import scala.util.chaining.*

import mlscript.utils.*, shorthands.*

import semantics.*
import semantics.Elaborator.{State, Ctx, ctx}

import syntax.{Literal, Tree}

// it should be possible to cache some common constructions (End, Option) into the context
// this avoids having to rebuild the same shapes everytime they are needed
case class Context(cache: HashMap[Path, Path]):
  def getCache(p: Path): Option[Path] = cache.get(p)
  def addCache(p: Path, v: Path): Context = Context(cache.clone() += (p -> v))
  def delCache(p: Path): Context = Context(cache.clone() -= p)

extension [A, B](ls: Iterable[(A => B) => B])
  def collectApply(f: Ls[A] => B): B =
    // defer applying k while prepending new elements to the list
    ls.foldRight((_: Ls[A] => B)(Nil))((headCont, tailCont) =>
      k =>
        headCont: head =>
          tailCont: tail =>
            k(head :: tail)
    )(f)

type ArgWrappable = Path | Symbol

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

// transform Block to Block IR so that it can be instrumented in mlscript
class Instrumentation(using State, Raise, Ctx) extends BlockTransformer(new SymbolSubst()):
  // TODO: there could be a fresh scope per function body, instead of a single one for the entire program
  val scope = Scope.empty(Scope.Cfg.default)
  val defnMap = HashMap[Symbol, ClsLikeDefn | ClsLikeBody]()

  // helpers for constructing Block

  def assign(using State)(res: Result, symName: Str = "tmp")(k: Path => Block): Block =
    // TODO: skip assignment if res: Path?
    val sym = new TempSymbol(N, symName)
    Scoped(Set(sym), Assign(sym, res, k(sym.asPath)))

  def tuple(using State)(elems: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    assign(Tuple(false, elems.map(asArg)), symName)(k)

  def ctor(using State)(cls: Path, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    assign(Instantiate(false, cls, args.map(asArg)), symName)(k)

  def call(using State)(fun: Path, args: Ls[ArgWrappable], isMlsFun: Bool = true, symName: Str = "tmp")(k: Path => Block): Block =
    assign(Call(fun, args.map(asArg))(isMlsFun, false, false), symName)(k)

  // helpers for constructing Block IR

  def blockMod(name: Str) = summon[State].blockSymbol.asPath.selSN(name)
  def optionMod(name: Str) = summon[State].optionSymbol.asPath.selSN(name)
  def helperMod(name: Str) = summon[State].specializeHelpersSymbol.asPath.selSN(name)

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

  // if sym is ClassSymbol, we may need pOpt to link to the path pointing to the value of the symbol
  def transformSymbol(sym: Symbol, pOpt: Option[Path] = N, symName: Str = "sym")(k: Path => Block): Block =
    sym match
    case t: TermSymbol if t.defn.exists(_.sym.asClsOrMod.isDefined) =>
      transformSymbol(t.defn.get.sym.asClsOrMod.get, pOpt, symName)(k)
    // retain names to built-in functions or function definitions
    case t: TermSymbol if t.defn.exists(_.k == syntax.Fun) =>
      blockCtor("Symbol", Ls(toValue(sym.nme)), symName)(k)
    case _: BuiltinSymbol =>
      blockCtor("Symbol", Ls(toValue(sym.nme)), symName)(k)
    case clsSym: ClassSymbol if ctx.builtins.virtualClasses(clsSym) =>
      blockCtor("VirtualClassSymbol", Ls(toValue(sym.nme)), symName)(k)
    case baseSym: BaseTypeSymbol =>
      val name = scope.allocateOrGetName(sym)
      val (owner, bsym, paramsOpt, auxParams) = (baseSym.defn, defnMap.get(baseSym)) match
      case (S(defn), _) => (defn.owner, defn.bsym, defn.paramsOpt, defn.auxParams)
      case (_, S(defn: ClsLikeDefn)) => (defn.owner, defn.sym, defn.paramsOpt, defn.auxParams)
      case _ =>
        raise(ErrorReport(msg"Unable to infer parameters from symbol in staged module, which are necessary to reconstruct class instances: ${sym.toString()}" -> sym.toLoc :: Nil))
        return End()

      val path: ArgWrappable = pOpt.getOrElse(owner match
      case S(owner) => owner.asPath.selSN(sym.nme)
      case N => bsym)
      baseSym match
      case _: ClassSymbol =>
        transformParamsOpt(paramsOpt): paramsOpt =>
          auxParams.map(ps => transformParamList(ps)).collectApply: auxParams =>
            tuple(auxParams): auxParams =>
              blockCtor("ClassSymbol", Ls(toValue(name), path, paramsOpt, auxParams), symName)(k)
      case _: ModuleOrObjectSymbol =>
        blockCtor("ModuleSymbol", Ls(toValue(name), path), symName)(k)
    case _ =>
      val name = scope.allocateOrGetName(sym)
      blockCtor("Symbol", Ls(toValue(name)), symName)(k)

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
        transformBlock(block)(using ctx.addCache(p, x)): (y, ctx) =>
          blockCtor("Arm", Ls(cse, y)): cde =>
            f(cde)(ctx.delCache(p))

    (arms.map(applyRuleBranch).collectApply(_: Ls[Path] => Context => Block)(summon)): arms =>
      ctx =>
        tuple(arms): arms =>
          ruleEnd(): e =>
            // TODO: use transformOption here
            def dfltStaged(k: (Path, Context) => Block) =
              dflt match
              case S(dflt) =>
                transformBlock(dflt)(using ctx.addCache(p, x)): (dflt, ctx) =>
                  optionSome(dflt)(k(_, ctx.delCache(p)))
              case N => optionNone()(k(_, ctx))
            dfltStaged: (dflt, ctx) =>
              blockCtor("Match", Ls(x, arms, dflt, e), symName)(k(_, ctx))

  // transformations of Block

  def transformPath(p: Path)(using ctx: Context)(k: Path => Block): Block =
    // rulePath
    ctx.getCache(p).map(k).getOrElse:
      p match
      case Value.Ref(l, disamb) =>
        transformSymbol(disamb.getOrElse(l)): sym =>
          blockCtor("ValueRef", Ls(sym), "var")(k)
      case l: Value.Lit =>
        blockCtor("ValueLit", Ls(l), "lit")(k)
      case s @ Select(p, Tree.Ident(name)) =>
        transformPath(p): x =>
          val sym = s.symbol.map(transformSymbol(_, S(s)))
            .getOrElse(blockCtor("Symbol", Ls(toValue(name))))
          sym: sym =>
            blockCtor("Select", Ls(x, sym), "sel")(k)
      case DynSelect(qual, fld, arrayIdx) =>
        transformPath(qual): x =>
          transformPath(fld): y =>
            blockCtor("DynSelect", Ls(x, y, toValue(arrayIdx)), "dynsel")(k)
      case _: Value.This =>
        raise(ErrorReport(msg"Value.This not supported in staged module." -> p.toLoc :: Nil))
        End()

  def transformResult(r: Result)(using ctx: Context)(k: (Path, Context) => Block): Block =
    r match
    case p: Path => transformPath(p)(k(_, ctx))
    case Tuple(mut, elems) =>
      if mut then raise(ErrorReport(msg"Mutable tuples not supported in staged module." -> r.toLoc :: Nil))
      transformArgs(elems): xs =>
        tuple(xs.map(_._1)): codes =>
          blockCtor("Tuple", Ls(codes), "tup")(k(_, ctx))
    case Instantiate(mut, cls, args) =>
      if mut then raise(ErrorReport(msg"Mutable instantiations not supported in staged module." -> r.toLoc :: Nil))
      transformArgs(args): xs =>
        transformPath(cls): cls =>
          tuple(xs.map(_._1)): codes =>
            blockCtor("Instantiate", Ls(cls, codes), "inst")(k(_, ctx))
    case Call(fun, args) =>
      transformPath(fun): stagedFun =>
        transformArgs(args): args =>
          tuple(args.map(_._1)): tup =>
            blockCtor("Call", Ls(stagedFun, tup), "app")(k(_, ctx))
    case _ =>
      raise(ErrorReport(msg"Other Results not supported in staged module." -> r.toLoc :: Nil))
      End()

  def transformArg(a: Arg)(using Context)(k: ((Path, Bool)) => Block): Block =
    val Arg(spread, value) = a
    if spread.isDefined then raise(ErrorReport(msg"Spread parameters are not supported in staged module." -> value.toLoc :: Nil))
    transformPath(value): value =>
      blockCtor("Arg", Ls(value)): cde =>
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
    case Case.Tup(len, inf) =>
      if inf then raise(ErrorReport(msg"Spread parameters are not supported in staged module: ${cse.toString()}" -> N :: Nil))
      blockCtor("Tup", Ls(toValue(len)))(k)
    case Case.Field(name, safe) =>
      raise(ErrorReport(msg"Case.Field not supported in staged module." -> name.toLoc :: Nil))
      End()

  def transformBlock(b: Block)(using Context)(k: Path => Block): Block =
    transformBlock(b)((p, _) => k(p))

  def transformBlock(b: Block)(using ctx: Context)(k: (Path, Context) => Block): Block =
    b match
    case Return(res, implct) =>
      transformResult(res): (x, ctx) =>
        blockCtor("Return", Ls(x, toValue(implct)), "return")(k(_, ctx))
    case Assign(x, r, b) =>
      transformResult(r): (y, ctx) =>
        transformSymbol(x): xSym =>
          blockCtor("ValueRef", Ls(xSym)): xStaged =>
            (Assign(x, xStaged, _)):
              given Context = ctx.addCache(x.asPath, xStaged)
              transformBlock(b): (z, ctx) =>
                blockCtor("Assign", Ls(xSym, y, z), "assign")(k(_, ctx))
    case Define(cls: ClsLikeDefn, rest) =>
      assert(cls.companion.isEmpty, "nested module not supported")
      transformBlock(rest): p =>
        transformSymbol(cls.isym): c =>
          // staging the methods within the module
          cls.methods.map(transformFunDefn).collectApply: pairs =>
            // TODO: ignore ctx?
            val (methods, _) = pairs.unzip
            tuple(methods): methods =>
              optionNone(): none => // TODO: handle companion object
                blockCtor("ClsLikeDefn", Ls(c, methods, none)): cls =>
                  blockCtor("Define", Ls(cls, p))(k(_, ctx))
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
    case _ =>
      raise(ErrorReport(msg"Other Blocks not supported in staged module: ${b.toString()}" -> N :: Nil))
      End()

  // TODO: rename, this is the continuation version of the function
  def transformFunDefn(f: FunDefn)(using Context)(k: ((Path, Context)) => Block): Block =
    transformBlock(f.body): (body, ctx) =>
      if f.params.length != 1 then
        raise(ErrorReport(msg":ftc must be enabled to desugar functions with multiple parameter lists." -> f.sym.toLoc :: Nil))
      // maintain parameter names in instrumented code
      f.params.map(
        _.params.map(p => blockCtor("Symbol", Ls(toValue(p.sym.nme)))).collectApply
      ).collectApply: paramListSyms =>
        paramListSyms.map(tuple(_)).collectApply: tups =>
          tuple(tups): tup =>
            blockCtor("Symbol", Ls(toValue(f.sym.nme))): sym =>
              blockCtor("FunDefn", Ls(sym, tup, body, toValue(true)))(k(_, ctx))

  def stageMethod(f: FunDefn): FunDefn =
    val genSymName = f.sym.nme + "_instr"
    val genSym = BlockMemberSymbol(genSymName, Nil, false)

    // turn into fundefn
    val dSym = TermSymbol(f.dSym.k, f.dSym.owner, Tree.Ident(genSymName))
    val argSyms = f.params.flatMap(_.params).map(_.sym)
    val newBody =
      val rest = transformBlock(f.body)(using Context(new HashMap())): body =>
        if f.params.length != 1 then
          raise(WarningReport(msg":ftc must be enabled to desugar functions with multiple parameter lists." -> f.sym.toLoc :: Nil))
        // maintain parameter names in instrumented code
        f.params.map(
          _.params.map(p => blockCtor("Symbol", Ls(toValue(p.sym.nme)))).collectApply
        ).collectApply: paramListSyms =>
          paramListSyms.map(tuple(_)).collectApply: tups =>
            tuple(tups): tup =>
              transformSymbol(f.sym): sym =>
                blockCtor("FunDefn", Ls(sym, tup, body, toValue(true))): block =>
                  Return(block, false)
      (Scoped(Set(argSyms*), rest))

    f.copy(sym = genSym, dSym = dSym, params = Ls(PlainParamList(Nil)), body = newBody)(false)

  def cacheEntry(f: FunDefn)(k: Path => Block) =
    call(f.owner.get.asPath.selSN(f.sym.nme), Nil)(instr => tuple(Ls(toValue(f.sym.nme), instr))(k))

  def genMethod(f: FunDefn, cachePath: Path): FunDefn =
    val genSymName = f.sym.nme + "_gen"
    val sym = BlockMemberSymbol(genSymName, Nil, false)
    val dSym = TermSymbol(f.dSym.k, f.dSym.owner, Tree.Ident(genSymName))

    val body = call(cachePath.selSN("getFun"), Ls(toValue(f.sym.nme))): instr =>
      f.params.map(ps => tuple(ps.params.map(_.sym))).collectApply: tups =>
        tuple(tups): tups =>
          call(helperMod("specialize"), Ls(cachePath, instr.selSN("value"), tups)): res =>
            Return(res, false)
    f.copy(sym = sym, dSym = dSym, body = body)(false)

  override def applyBlock(b: Block): Block =
    super.applyBlock(b) match
    // find modules with staged annotation
    case Define(c: ClsLikeDefn, rest) if c.companion.exists(_.isym.defn.exists(_.hasStagedModifier.isDefined)) =>
      val companion = c.companion.get
      val isym = companion.isym
      val stagedMethods = companion.methods.map(stageMethod)

      val ctor = FunDefn.withFreshSymbol(S(isym), BlockMemberSymbol("ctor$", Nil), Ls(PlainParamList(Nil)), companion.ctor)(false)
      val stagedCtor = stageMethod(ctor)

      // for storing specialized functions in each staged module
      val cacheSym = BlockMemberSymbol("cache", Nil, true)
      val cacheTsym = TermSymbol(syntax.ImmutVal, S(isym), Tree.Ident("cache"))
      val cachePath = isym.asPath.selSN("cache")
      // initialize cache for the module
      def cacheDecl(rest: Block) =
        (stagedCtor :: stagedMethods).map(cacheEntry).collectApply: cacheTups =>
          tuple(cacheTups): tup =>
            assign(Instantiate(mut = false, State.globalThisSymbol.asPath.selSN("Map"), Ls(Arg(N, tup)))): map =>
              assign(Instantiate(mut = false, helperMod("FunCache"), Ls(Arg(N, map)))): mapInit =>
                Define(ValDefn(cacheTsym, cacheSym, mapInit), rest)

      // add generator functions for classes within the constructor
      val genCls = new BlockTransformer(new SymbolSubst()):
        override def applyBlock(b: Block): Block = super.applyBlock(b) match
        case Define(c: ClsLikeDefn, rest) if c.companion.isEmpty =>
          val genMethods = c.methods.map(genMethod(_, cachePath))
          val stagedMethods = c.methods.map(stageMethod)
          // FIXME: add the default staged block IR to the cache
          val newModule = c.copy(methods = c.methods ++ stagedMethods ++ genMethods)
          Define(newModule, rest)
        case b => b

      // NOTE: this debug printing only works for top-level modules, nested modules don't work
      // TODO: remove this. only for testing
      def debugCont(rest: Block) =
        val printFun = State.globalThisSymbol.asPath.selSN("console").selSN("log")
        val renderFun = State.runtimeSymbol.asPath.selSN("render")
        val options = Record(false, Ls(RcdArg(S(toValue("indent")), toValue(true))))

        assign(options): options =>
          call(cachePath.selSN("toString"), Nil, false): str =>
            call(printFun, Ls(str), false): _ =>
              call(printFun, Ls(isym.asPath.selSN("generatorMap")), false): _ =>
                rest

      val (genMethods, generatorEntries) = companion.methods.map(f => {
        val gen = genMethod(f, cachePath)
        (gen, tuple(Ls(toValue(f.sym.nme), isym.asPath.selSN(gen.sym.nme))))
      }).unzip

      val generatorMapSym = BlockMemberSymbol("generatorMap", Nil, true)
      val generatorMapTsym = TermSymbol(syntax.ImmutVal, S(isym), Tree.Ident("generatorMap"))
      def generatorMapDecl(rest: Block) =
        generatorEntries.collectApply: defs =>
          tuple(defs): tup =>
            assign(Instantiate(mut = false, State.globalThisSymbol.asPath.selSN("Map"), Ls(Arg(N, tup)))): map =>
              Define(ValDefn(generatorMapTsym, generatorMapSym, map), rest)

      // used for staging classes inside modules
      val newCompanion = companion.copy(
        methods = stagedCtor :: companion.methods ++ stagedMethods ++ genMethods,
        ctor = generatorMapDecl(cacheDecl(debugCont(genCls.applyBlock(companion.ctor)))),
        publicFields = cacheSym -> cacheTsym :: companion.publicFields
      )
      val newModule = c.copy(companion = S(newCompanion))
      Define(newModule, rest)
    case b => b

  def mkDefnMap(b: Block): Unit =
    val transformer = new BlockTraverser:
      override def applyDefn(defn: Defn) = defn match
      case c: ClsLikeDefn =>
        defnMap.addOne(c.isym, c)
        super.applyDefn(defn)
      case _ => super.applyDefn(defn)
    transformer.applyBlock(b)

  def applyBlockFinal(b: Block) =
    mkDefnMap(b)
    applyBlock(b)
