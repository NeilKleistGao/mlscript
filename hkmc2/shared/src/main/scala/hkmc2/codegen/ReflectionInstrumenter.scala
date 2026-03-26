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

def asArg(x: ArgWrappable): Arg = x match
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
class ReflectionInstrumenter(using State, Raise, Ctx) extends BlockTransformer(SymbolSubst.Id):
  // TODO: there could be a fresh scope per function body, instead of a single one for the entire program
  val scope = Scope.empty(Scope.Cfg.default)
  val defnMap = HashMap[Symbol, ClsLikeDefn | ClsLikeBody]()
  val symbolMapSym: Symbol = TempSymbol(N, "symbolMap")
  // only create symbolMap if we create entries to avoid changing IR for unrelated tests
  var symbolMapUsed: Bool = false

  // helpers for constructing Block

  def assign(using State)(res: Result, symName: Str = "tmp")(k: Path => Block): Block =
    // TODO: skip assignment if res: Path?
    val sym = new TempSymbol(N, symName)
    Scoped(Set(sym), Assign(sym, res, k(sym.asPath)))

  def tuple(using State)(elems: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    assign(Tuple(false, elems.map(asArg)), symName)(k)

  def ctor(using State)(cls: Path, args: Ls[ArgWrappable], symName: Str = "tmp")(k: Path => Block): Block =
    assign(Instantiate(true, cls, args.map(asArg)), symName)(k)

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
    def checkMap(key: Path, p: Path) =
      symbolMapUsed = true
      blockCall("checkMap", Ls(symbolMapSym, key, p))(k)
    sym match
      case t: TermSymbol if t.defn.exists(_.sym.asClsOrMod.isDefined) =>
        transformSymbol(t.defn.get.sym.asClsOrMod.get, pOpt, symName)(k)
      // avoid name collision
      case _: TempSymbol | _: NoSymbol =>
        val name = scope.allocateOrGetName(sym)
        blockCtor("Symbol", Ls(toValue(name)), symName)(k)
      case clsSym: ClassSymbol if ctx.builtins.virtualClasses(clsSym) =>
        blockCtor("VirtualClassSymbol", Ls(toValue(sym.nme)), symName)(checkMap(toValue(sym.nme), _))
      case baseSym: BaseTypeSymbol =>
        val name = scope.allocateOrGetName(sym)
        // FIXME: we want the parent path for subtyping, but it is only available for ClsLikeDefn, not ClassDef
        val (owner, bsym, paramsOpt, auxParams) = (baseSym.defn, defnMap.get(baseSym)) match
          case (S(defn), _) => (defn.owner, defn.bsym, defn.paramsOpt, defn.auxParams)
          case (_, S(defn: ClsLikeDefn)) => (defn.owner, defn.sym, defn.paramsOpt, defn.auxParams)
          case _ =>
            raise(ErrorReport(msg"Unable to infer parameters from symbol in staged module, which are necessary to reconstruct class instances: ${sym.toString()}" -> sym.toLoc :: Nil))
            return End()

        val path = pOpt.getOrElse(owner match
          case S(owner) => owner.asPath.selSN(sym.nme)
          case N => bsym.asPath)
        baseSym match
          case _: ClassSymbol =>
            transformParamsOpt(paramsOpt): paramsOpt =>
              auxParams.map(transformParamList).collectApply: auxParams =>
                tuple(auxParams): auxParams =>
                  blockCtor("ClassSymbol", Ls(toValue(name), path, paramsOpt, auxParams), symName)(checkMap(path, _))
          case _: ModuleOrObjectSymbol =>
            blockCtor("ModuleSymbol", Ls(toValue(name), path), symName)(checkMap(path, _))
      case _ => blockCtor("Symbol", Ls(toValue(sym.nme)), symName)(k)

  def transformOption[A](xOpt: Opt[A], f: A => (Path => Block) => Block)(k: Path => Block): Block = xOpt match
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
            def dfltStaged(k: (Path, Context) => Block) = dflt match
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

  def transformResult(r: Result)(using ctx: Context)(k: (Path, Context) => Block): Block = r match
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

  // maintain parameter names in instrumented code
  def transformParamList(ps: ParamList)(k: Path => Block) =
    ps.params.map(p => transformSymbol(p.sym)).collectApply(tuple(_)(k))

  def transformParamsOpt(pOpt: Opt[ParamList])(k: Path => Block) =
    transformOption(pOpt, transformParamList)(k)

  def transformParams(params: Ls[ParamList])(k: Path => Block) =
    params.map(transformParamList).collectApply(tuple(_)(k))

  def transformCase(cse: Case)(using Context)(k: Path => Block): Block = cse match
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

  def transformBlock(b: Block)(using ctx: Context)(k: (Path, Context) => Block): Block = b match
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
    case Define(v: ValDefn, rest) =>
      // TODO: only allow ValDefn inside ctors
      transformBlock(rest): p =>
        transformOption(v.tsym.owner, transformSymbol(_, N, "test")): owner =>
          transformSymbol(v.sym): sym =>
            transformPath(v.rhs): rhs =>
              blockCtor("ValDefn", Ls(owner, sym, rhs)): v =>
                blockCtor("Define", Ls(v, p))(k(_, ctx))
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

  def transformFunDefn(f: FunDefn)(using Context)(k: ((Path, Context)) => Block): Block =
    transformBlock(f.body): (body, ctx) =>
      if f.params.length > 1 then
        raise(ErrorReport(msg":ftc must be enabled to desugar functions with multiple parameter lists." -> f.sym.toLoc :: Nil))
      // maintain parameter names in instrumented code
      transformParams(f.params): paramList =>
        transformSymbol(f.sym): sym =>
          blockCtor("FunDefn", Ls(sym, paramList, body))(k(_, ctx))

  def stageMethod(f: FunDefn): FunDefn =
    val stageSymName = f.sym.nme + "_instr"
    val stageSym = BlockMemberSymbol(stageSymName, Nil, false)

    // turn into fundefn
    val dSym = TermSymbol(f.dSym.k, f.dSym.owner, Tree.Ident(stageSymName))
    val argSyms = f.params.flatMap(_.params).map(_.sym)
    val newBody =
      val rest = transformFunDefn(f)(using Context(new HashMap()))((block, _) => Return(block, false))
      (Scoped(Set(argSyms*), rest))

    FunDefn.withFreshSymbol(f.dSym.owner, stageSym, Ls(PlainParamList(Nil)), newBody)(false)

  override def applyBlock(b: Block): Block = b match
    // TODO: assume staged classes have no companion module
    // find modules with staged annotation, or classes without companion module
    case Define(defn: ClsLikeDefn, rest)
        if defn.companion.exists(_.isym.defn.exists(_.hasStagedModifier.isDefined)) ||
          defn.companion.isEmpty && defn.isym.defn.exists(_.hasStagedModifier.isDefined) =>
      val (sym, companion, ctor, ctorParams, methods) = defn.companion match
        case S(companion) => (companion.isym, companion, companion.ctor, N, companion.methods)
        case N =>
          if !defn.privateFields.isEmpty then
            raise(ErrorReport(msg"Staged classes with private fields are not supported." -> defn.sym.toLoc :: Nil))
            return End()
          val companion = ClsLikeBody(ModuleOrObjectSymbol(Tree.TypeDef(syntax.Mod, Tree.Empty(), N), Tree.Ident(defn.sym.nme)), Nil, Nil, Nil, End())
          val ctor = Begin(defn.preCtor, defn.ctor)
          (defn.sym, companion, ctor, defn.paramsOpt, defn.methods)

      val modSym = companion.isym

      // avoid name clash of cache and generator map for derived staged classes
      val suffix = "$" + scope.allocateOrGetName(sym)
      // for storing specialized functions in each staged module
      val cacheNme = "cache" + suffix
      val cacheSym = BlockMemberSymbol(cacheNme, Nil, true)
      val cacheTsym = TermSymbol(syntax.ImmutVal, S(modSym), Tree.Ident(cacheNme))
      val cachePath = modSym.asPath.selSN(cacheNme)
      val generatorMapNme = "generatorMap" + suffix
      val generatorMapSym = BlockMemberSymbol(generatorMapNme, Nil, true)
      val generatorMapTsym = TermSymbol(syntax.ImmutVal, S(modSym), Tree.Ident(generatorMapNme))

      def genMethod(f: FunDefn, stagedPath: Path) =
        val genSymName = f.sym.nme + "_gen"
        val sym = BlockMemberSymbol(genSymName, Nil, false)
        val dSym = TermSymbol(f.dSym.k, f.dSym.owner, Tree.Ident(genSymName))
        
        val params = if defn.companion.isEmpty then PlainParamList(Param.simple(VarSymbol(Tree.Ident("cls"))) :: Nil) :: f.params else f.params
        val body = call(cachePath.selSN("getFun"), Ls(toValue(f.sym.nme))): instr =>
          params.map(ps => tuple(ps.params.map(_.sym))).collectApply: tups =>
            tuple(tups): args =>
              call(helperMod("specialize"), Ls(cachePath, toValue(f.sym.nme), stagedPath, args)): res =>
                Return(res, false)
        FunDefn.withFreshSymbol(f.dSym.owner, sym, params, body)(false)

      val ctorFun = FunDefn.withFreshSymbol(S(modSym), BlockMemberSymbol("ctor$", Nil, false), Ls(ctorParams.getOrElse(PlainParamList(Nil))), ctor)(false)
      val (helperMethods, cacheEntries, generatorEntries) = (ctorFun :: methods).map(f =>
        val staged = stageMethod(f)
        val stagedPath = modSym.asPath.selSN(staged.sym.nme)
        val gen = genMethod(f, stagedPath)
        def cacheDebug(k: Path => Block) =
          call(stagedPath, Nil): res =>
            // stub for the returned shape of the function
            tuple(Ls(res, toValue(1))): entry =>
              tuple(Ls(toValue(f.sym.nme), entry))(k)

        (
          Ls(staged, gen),
          cacheDebug,
          tuple(Ls(toValue(f.sym.nme), modSym.asPath.selSN(gen.sym.nme)))
        )
      ).unzip3

      // initialize cache for the module
      def cacheDecl(rest: Block) =
        cacheEntries.collectApply: cacheTups =>
          tuple(cacheTups): tup =>
            this.ctor(State.globalThisSymbol.asPath.selSN("Map"), Ls(tup)): map =>
              assign(Instantiate(false, helperMod("FunCache"), Ls(Arg(N, map)))): funCache =>
                Define(ValDefn(cacheTsym, cacheSym, funCache), rest)

      def generatorMapDecl(rest: Block) =
        generatorEntries.collectApply: defs =>
          tuple(defs): tup =>
            this.ctor(State.globalThisSymbol.asPath.selSN("Map"), Ls(tup)): map =>
              Define(ValDefn(generatorMapTsym, generatorMapSym, map), rest)

      // TODO: remove this. only for testing
      def debugCont(rest: Block) =
        val printFun = State.globalThisSymbol.asPath.selSN("console").selSN("log")
        // val renderFun = State.runtimeSymbol.asPath.selSN("render")
        // val options = Record(false, Ls(RcdArg(S(toValue("indent")), toValue(true))))

        // assign(options): options =>
        // call(cachePath.selSN("toString"), Nil, false): str =>
        // call(printFun, Ls(str), false): _ =>
        call(printFun, Ls(modSym.asPath.selSN(generatorMapNme)), false): _ =>
          if symbolMapUsed
          then call(printFun, Ls(symbolMapSym), false)(_ => rest)
          else rest

      // used for staging classes inside modules
      val newCompanion = companion.copy(
        methods = companion.methods ++ helperMethods.flatten,
        ctor = Begin(companion.ctor, cacheDecl(generatorMapDecl(debugCont(End())))),
        publicFields = companion.publicFields
      )
      val newClsLikeDefn = defn.copy(companion = S(newCompanion))
      Define(newClsLikeDefn, applyBlock(rest))
    case b => super.applyBlock(b)

  def mkDefnMap(b: Block): Unit =
    val transformer = new BlockTraverser:
      override def applyDefn(defn: Defn) = defn match
        case c: ClsLikeDefn =>
          defnMap.addOne(c.isym, c)
          super.applyDefn(defn)
        case _ => super.applyDefn(defn)
    transformer.applyBlock(b)

  def apply(b: Block) =
    mkDefnMap(b)
    val rest = applyBlock(b)
    if symbolMapUsed then
      Scoped(
        Set(symbolMapSym),
        Assign(
          symbolMapSym,
          Instantiate(false, State.globalThisSymbol.asPath.selSN("Map"), Nil),
          rest
        )
      )
    else rest
