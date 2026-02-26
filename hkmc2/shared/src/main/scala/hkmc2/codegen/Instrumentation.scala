package hkmc2
package codegen

import utils.*
import hkmc2.Message.MessageContext

import scala.collection.mutable.{HashMap, HashSet}
import scala.util.chaining._

import mlscript.utils.*, shorthands.*

import semantics.*
import semantics.Elaborator.State

import syntax.{Literal, Tree}

// it should be possible to cache some common constructions (End, Option) into the context
// this avoids having to rebuild the same shapes everytime they are needed
case class Context(cache: HashMap[Path, Path], defs: HashMap[Path, (Path => Block) => Block]):
  def getCache(p: Path): Option[Path] = cache.get(p)
  def addCache(p: Path, v: Path): Context = Context(cache.clone() += (p -> v), defs)
  def delCache(p: Path): Context = Context(cache.clone() -= p, defs)
  // TODO: the paths for the definitions will be defined at the constructor of the module, is it possible to reference the value at the ctor, instead of rebuilding them in the function body?
  def addDef(p: Path, cont: (Path => Block) => Block): Context = Context(cache, defs.clone() += (p -> cont))

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

// removes Label and Break nodes
class LabelTransformer(using State, Raise) extends BlockTransformer(new SymbolSubst()):
  private def inlineLabelRestInDef(d: Defn)(using conts: Map[Symbol, Symbol | Block]): Defn = d match
  case fd @ FunDefn(owner, sym, dSym, params, body) =>
    val newBody = inlineLabelRest(body)
    if newBody is body then fd else FunDefn(owner, sym, dSym, params, newBody)(fd.forceTailRec)
  case ClsLikeDefn(owner, isym, sym, ctorSym, k, paramsOpt, auxParams, parentPath, methods, privateFields, publicFields, preCtor, ctor, companion, bufferable) =>
    val newMethods = methods.map(inlineLabelRestInDef).map {
      case fd: FunDefn => fd
    } // TODO: remove it
    val newPreCtor = inlineLabelRest(preCtor)
    val newCtor = inlineLabelRest(ctor)
    val newCompanion = companion.map {
      case ClsLikeBody(isym, methods, privateFields, publicFields, ctor) =>
        val newMethods = methods.map(inlineLabelRestInDef).map {
          case fd: FunDefn => fd
        } // TODO: remove it
        val newCtor = inlineLabelRest(ctor)
        ClsLikeBody(isym, newMethods, privateFields, publicFields, newCtor)
    }
    ClsLikeDefn(owner, isym, sym, ctorSym, k, paramsOpt, auxParams, parentPath, newMethods, privateFields, publicFields, newPreCtor, newCtor, newCompanion, bufferable)
  case _ => ??? // not supported yet

  private def inlineLabelRest(b: Block)(using conts: Map[Symbol, Symbol | Block]): Block = b match
  case Begin(body, rest) =>
    val newBody = inlineLabelRest(body)
    val newRest = inlineLabelRest(rest)
    if (newBody is body) && (newRest is rest) then b
    else Begin(newBody, newRest)
  case _: Return | _: End | _: Throw => b
  case Break(label) if label.nme.startsWith("split_root") => End()
  case Break(label) => conts.get(label) match
    case Some(sym: Symbol) => Return(Call(Value.Ref(sym, N), Nil)(true, false, false), true)
    case Some(rb: Block) => rb
    case _ => ??? // error
  case Assign(lhs, rhs, rest) =>
    val newRest = inlineLabelRest(rest)
    if newRest is rest then b
    else Assign(lhs, rhs, newRest)
  case Match(scrut, arms, dflt, rest) =>
    val newArms = arms.map(p => (p._1, inlineLabelRest(p._2)))
    val newDflt = dflt.map(inlineLabelRest)
    val newRest = inlineLabelRest(rest)
    Match(scrut, newArms, newDflt, newRest)
  case Label(label, false, body, rest) if label.nme.startsWith("split_root") =>
    Begin(inlineLabelRest(body), inlineLabelRest(rest))
  case Label(label, false, body, rest) =>
    val newRest = inlineLabelRest(rest)
    inlineLabelRest(body)(using conts + (label -> newRest))
  // TODO: create helper functions to remove duplications
  // if rest.size < 3 then
  //   val newRest = inlineLabelRest(rest)
  //   inlineLabelRest(body)(using conts + (label -> newRest))
  // else
  //   val contSym = new TempSymbol(N, "cont")
  //   Scoped(Set(contSym), Assign(contSym, Lambda(PlainParamList(Nil), rest), inlineLabelRest(body)(using conts + (label -> contSym))))
  case Scoped(syms, body) =>
    val newBody = inlineLabelRest(body)
    if newBody is body then b else Scoped(syms, newBody)
  case Define(defn, rest) =>
    val newDefn = inlineLabelRestInDef(defn)
    val newRest = inlineLabelRest(rest)
    if (newDefn is defn) && (newRest is rest) then b
    else Define(newDefn, newRest)
  case _ =>
    println(s"yydz: $b")
    ??? // not supported yet.

  override def applyBlock(b: Block): Block = inlineLabelRest(b)(using Map.empty)

// transform Block to Block IR so that it can be instrumented in mlscript
class Instrumentation(using State, Raise) extends BlockTransformer(new SymbolSubst()):
  // TODO: there could be a fresh scope per function body, instead of a single one for the entire program
  var scope = Scope.empty
  val inline = new LabelTransformer

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

  def concat(b1: Block, b2: Block): Block =
    b1.mapTail {
      case _: End => b2
      case _ => ???
    }

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

  def transformSymbol(sym: Symbol, symName: Str = "sym")(k: Path => Block): Block =
    sym match
    case clsSym: ClassSymbol =>
      val name = scope.allocateOrGetName(sym)
      transformParamsOpt(clsSym.defn.get.paramsOpt): paramsOpt =>
        blockCtor("ClassSymbol", Ls(toValue(name), paramsOpt), symName)(k)
    case t: TermSymbol if t.defn.exists(_.sym.asCls.isDefined) =>
      val name = scope.allocateOrGetName(sym)
      transformSymbol(t.defn.get.sym.asCls.get, symName)(k)
    case _: BuiltinSymbol =>
      // retain names to built-in functions
      blockCtor("Symbol", Ls(toValue(sym.nme)), symName)(k)
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

  def transformResult(r: Result)(using ctx: Context)(k: (Path, Context) => Block): Block =
    r match
    case p: Path => transformPath(p)(k(_, ctx))
    case Tuple(mut, elems) =>
      assert(!mut, "mutable tuple not supported")
      transformArgs(elems): xs =>
        tuple(xs.map(_._1)): codes =>
          blockCtor("Tuple", Ls(codes), "tup")(k(_, ctx))
    case Instantiate(mut, cls, args) =>
      assert(!mut, "mutable instantiation not supported")
      transformArgs(args): xs =>
        transformPath(cls): cls =>
          tuple(xs.map(_._1)): codes =>
            blockCtor("Instantiate", Ls(cls, codes), "inst")(k(_, ctx))
    case Call(fun, args) =>
      val isStagedFun: Boolean =
        fun match
        case s @ Select(qual, Tree.Ident(name)) => s.symbol.exists({
            case t: TermSymbol => t.owner.exists({
                case sym: DefinitionSymbol[?] =>
                  sym.defn.get.hasStagedModifier.isDefined
              })
            case _ => false
          })
        case _ => false
      // if staged, point to generator function instead of original function
      val transformer = new BlockTransformer(SymbolSubst()):
        override def applyPath(p: Path)(k: Path => Block) = p match
        case Select(qual, Tree.Ident(name)) => k(Select(qual, Tree.Ident(name + "_gen"))(N))
        case p => k(p)
      transformPath(fun): stagedFun =>
        val funPath = if isStagedFun then
          fun match
          case Select(qual, Tree.Ident(name)) => Select(qual, Tree.Ident(name + "_gen"))(N)
          case Value.Ref(l, _) => Value.Ref(TempSymbol(N, l.nme + "_gen"), N)
          case _ => fun
        else fun
        val cont = (k: Path => Block) =>
          transformPath(fun): stagedFun =>
            tuple(Ls(funPath, toValue(isStagedFun))): value =>
              call(stagedFun.selSN("hash"), Ls()): str =>
                tuple(Ls(str, value))(k)
        val newCtx = ctx.addDef(fun, cont)
        transformArgs(args): args =>
          tuple(args.map(_._1)): tup =>
            blockCtor("Call", Ls(stagedFun, tup), "app")(k(_, newCtx))
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

  // TODO: there is probably a better way to extract the function definitions...
  def transformBlockWithDefs(b: Block)(using Context)(k: Path => Block): (Block, HashMap[Path, (Path => Block) => Block]) =
    var defs = new HashMap[Path, (Path => Block) => Block]()
    val block = transformBlock(b)((p, ctx) =>
      defs = ctx.defs
      k(p)
    )
    (block, defs)

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
  def transformFunDefn(modSym: BlockMemberSymbol, f: FunDefn): (FunDefn, HashMap[Path, (Path => Block) => Block], (Path => Block) => Block) =
    val genSymName = f.sym.nme + "_instr"
    val genSym = BlockMemberSymbol(genSymName, Nil, false)

    // turn into fundefn
    val dSym = TermSymbol(f.dSym.k, f.dSym.owner, Tree.Ident(genSymName))
    val argSyms = f.params.flatMap(_.params).map(_.sym)
    val (newBody, defs) =
      val (rest, defs) = transformBlockWithDefs(f.body)(using Context(new HashMap(), new HashMap())): body =>
        if f.params.length != 1 then
          raise(WarningReport(msg"Multiple parameter lists are not supported in shape propagation yet." -> f.sym.toLoc :: Nil))
        // maintain parameter names in instrumented code
        f.params.map(
          _.params.map(p => blockCtor("Symbol", Ls(toValue(p.sym.nme)))).collectApply
        ).collectApply: paramListSyms =>
          paramListSyms.map(tuple(_)).collectApply: tups =>
            tuple(tups): tup =>
              blockCtor("Symbol", Ls(toValue(f.sym.nme))): sym =>
                blockCtor("FunDefn", Ls(sym, tup, body, toValue(true))): block =>
                  Return(block, false)
      (Scoped(Set(argSyms*), rest), defs)

    // TODO: replace modSym with f.owner.get?
    def pathCont(k: Path => Block) = call(modSym.asPath.selSN(genSymName), Ls())(instr => tuple(Ls(toValue(f.sym.nme), instr))(k))
    val newFun = f.copy(sym = genSym, dSym = dSym, params = Ls(PlainParamList(Nil)), body = newBody)(false)
    (newFun, defs, pathCont)

  override def applyBlock(b: Block): Block =
    super.applyBlock(inline.applyBlock(b)) match
    // find modules with staged annotation
    case Define(c: ClsLikeDefn, rest) if c.companion.exists(_.isym.defn.exists(_.hasStagedModifier.isDefined)) =>
      val sym = c.sym.subst
      val companion = c.companion.get
      val (stagedMethods, defsList, cacheTups) = companion.methods
        .map(transformFunDefn(sym, _))
        .unzip3

      val ctor = FunDefn.withFreshSymbol(S(companion.isym), BlockMemberSymbol("ctor$", Nil), Ls(PlainParamList(Nil)), companion.ctor)(false)
      val (stagedCtor, ctorPrint, ctorCache) = transformFunDefn(sym, ctor)

      // for storing specialized functions in each staged module
      val cacheSym = BlockMemberSymbol("cache", Nil, true)
      val cacheTsym = TermSymbol(syntax.ImmutVal, S(companion.isym), Tree.Ident("cache"))
      val cachePath = sym.asPath.selSN("cache")
      // initialize cache for the module
      def cacheDecl(rest: Block) =
        (ctorCache :: cacheTups).collectApply: cacheTups =>
          tuple(cacheTups): tup =>
            assign(Instantiate(mut = false, State.globalThisSymbol.asPath.selSN("Map"), Ls(Arg(N, tup)))): map =>
              assign(Instantiate(mut = false, State.specializeHelpersSymbol.asPath.selSN("FunCache"), Ls(Arg(N, map)))): mapInit =>
                Define(ValDefn(cacheTsym, cacheSym, mapInit), rest)

      def genMethod(f: FunDefn): FunDefn =
        val genSymName = f.sym.nme + "_gen"
        val sym = BlockMemberSymbol(genSymName, Nil, false)
        val dSym = TermSymbol(f.dSym.k, f.dSym.owner, Tree.Ident(genSymName))

        val body = call(cachePath.selSN("getFun"), Ls(toValue(f.sym.nme))): instr =>
          // TODO: enforce that the definition will only have one parameter list
          f.params.map(ps => tuple(ps.params.map(_.sym))).collectApply: tups =>
            tuple(tups): tups =>
              call(State.specializeHelpersSymbol.asPath.selSN("specialize"), Ls(cachePath, instr.selSN("value"), tups)): res =>
                Return(res, false)
        f.copy(sym = sym, dSym = dSym, body = body)(false)
      val genMethods = companion.methods.map(genMethod)

      // NOTE: this debug printing only works for top-level modules, nested modules don't work
      // TODO: remove this. only for testing
      def debugCont(rest: Block) =
        val printFun = State.globalThisSymbol.asPath.selSN("console").selSN("log")
        val renderFun = State.runtimeSymbol.asPath.selSN("render")
        val options = Record(false, Ls(RcdArg(S(toValue("indent")), toValue(true))))
        val tmp = TempSymbol(N, "tmp")
        val debug =
          assign(options): options =>
            // call(cachePath.selSN("toString"), Nil, false): str =>
            call(renderFun, Ls(cachePath, options), false): str =>
              call(printFun, Ls(str), false): _ =>
                // call(sym.asPath.selSN("defCtx").selSN("toString"), Nil, false): str =>
                call(printFun, Ls(sym.asPath.selSN("defCtx")), false): _ =>
                  rest

        Scoped(Set(tmp), debug)

      val (newCtor, defs) = transformBlockWithDefs(companion.ctor)(using Context(new HashMap(), new HashMap()))(_ => debugCont(End()))
      val allDefs = defsList.fold(defs)((l, r) => l ++ r)
      val defCtxSym = BlockMemberSymbol("defCtx", Nil, true)
      val defCtxTsym = TermSymbol(syntax.ImmutVal, S(companion.isym), Tree.Ident("defCtx"))
      def defCtxDecl(rest: Block) =
        allDefs.values.collectApply: defs =>
          tuple(defs): tup =>
            assign(Instantiate(mut = false, State.globalThisSymbol.asPath.selSN("Map"), Ls(Arg(N, tup)))): map =>
              Define(ValDefn(defCtxTsym, defCtxSym, map), rest)

      // used for staging classes inside modules
      val newCompanion = companion.copy(
        methods = stagedCtor :: companion.methods ++ stagedMethods ++ genMethods,
        ctor = defCtxDecl(cacheDecl(newCtor)),
        publicFields = cacheSym -> cacheTsym :: companion.publicFields
      )
      val newModule = c.copy(sym = sym, companion = S(newCompanion))
      Define(newModule, rest)
    case b => b
