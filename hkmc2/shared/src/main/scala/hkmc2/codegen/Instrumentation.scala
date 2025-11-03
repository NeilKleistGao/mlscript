package hkmc2
package codegen

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

class Instrumentation(using State):
  // A PathLike type is a type that can be turned into an Arg
  type PathLike = Path | Symbol | Shape

  // is Elaborator.Ctx relevant?
  type Context = HashMap[Path, Shape]

  def asArg(x: PathLike): Arg =
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

  // could use `using` to allow passthrough of names
  def assign(res: Result, name: String = "tmp")(k: Path => Block): Assign =
    // TODO: skip assignment if res: Path?
    val tmp = new TempSymbol(N, name)
    Assign(tmp, res, k(tmp.asPath))

  def select(qual: Path, ident: Tree.Ident): Path =
    // do we need to call assign here? it's already a path anyways
    Select(qual, ident)(N)

  def tuple(elems: Ls[PathLike])(k: Path => Block): Block =
    // is this the same as "Ls of"?
    assign(Tuple(false, elems.map(asArg)))(k)

  def ctor(cls: Path, args: Ls[PathLike])(k: Path => Block): Block =
    assign(Instantiate(false, cls, args.map(asArg)))(k)

  // isMlsFun is probably always true?
  def call(fun: Path, args: Ls[PathLike], isMlsFun: Bool = true)(k: Path => Block): Block =
    assign(Call(fun, args.map(asArg))(isMlsFun, false))(k)

  // helper for staging the constructors

  def blockMod(name: String) = summon[State].blockSymbol.asPath.selSN(name)
  def shapeMod(name: String) = summon[State].shapeSymbol.asPath.selSN(name)

  def blockCtor(name: String, args: Ls[PathLike])(k: Path => Block): Block =
    ctor(blockMod(name), args)(k)
  def shapeCtor(name: String, args: Ls[PathLike])(k: Shape => Block): Block =
    // ctor(shapeMod(name), args)(p => k(Shape(p)))
    // override handling shape
    if name == "Lit" then ctor(shapeMod("Lit"), args)(p => k(Shape(p)))
    else ctor(shapeMod("Dyn"), Ls())(p => k(Shape(p)))

  def blockCall(name: String, args: Ls[PathLike])(k: Path => Block): Block =
    call(blockMod(name), args)(k)
  def shapeCall(name: String, args: Ls[PathLike])(k: Path => Block): Block =
    call(shapeMod(name), args)(k)

  // helpers to create and access the components of a staged value
  case class Shape(p: Path)

  // A StagedPath is a path that points to a (shape, code) tuple
  class StagedPath(val p: Path):
    def shape: Shape = Shape(DynSelect(p, toValue(0), false))
    def code: Path = DynSelect(p, toValue(1), false)
    def end: Block = Return(p, false)

  object StagedPath:
    def mk(shape: Shape, code: Path)(k: StagedPath => Block): Block =
      tuple(Ls(shape.p, code))(p => k(StagedPath(p)))
    // in some cases this can reduce the indentation level
    def mk(shapeCont: (Shape => Block) => Block, codeCont: (Path => Block) => Block)(k: StagedPath => Block): Block =
      shapeCont: shape =>
        codeCont: code =>
          mk(shape, code)(k)

  // linking functions defined in MLscipt

  def fnPrintCode(p: Path)(k: Path => Block): Block =
    blockCall("printCode", Ls(p))(k)
  def applyCompile(r: Path)(k: Path => Block): Block =
    blockCall("compile", Ls(r))(k)
  def fnMrg(shapes: Ls[Shape])(k: Shape => Block): Block =
    shapeCall("mrg", shapes)(s => k(Shape(s)))
  // TODO: make fnSilh take in a wrapped Path type
  def fnSilh(pattern: Path)(k: Shape => Block) =
    shapeCall("silh", Ls(pattern))(s => k(Shape(s)))
  def fnMatch(s: Shape, pat: Path)(k: Path => Block) =
    shapeCall("match", Ls(s, pat))(k)
  def fnSel(s1: Shape, s2: Shape)(k: Shape => Block): Block =
    shapeCall("sel", Ls(s1, s2))(s => k(Shape(s)))
  def fnStatic(s: Shape)(k: Path => Block) =
    shapeCall("static", Ls(s))(k)
  def fnCompile(x: Path)(k: StagedPath => Block): Block =
    shapeCall("compile", Ls(x))(p => k(StagedPath(p)))
  def fnDet(s: Shape, ps: Ls[PathLike])(k: Path => Block): Block =
    tuple(ps): tup =>
      shapeCall("det", Ls(s, tup))(k)

  // helpers for instrumenting functions

  def inst(f: StagedPath, args: Ls[StagedPath]): StagedPath =
    if ??? then
      // non-staged function
      ???
    else
      // staged function
      ???

  def instGlobal(f: StagedPath, args: Ls[StagedPath])(k: StagedPath => Block): Block =
    def isFunction(p: Path) = ???
    if isFunction(f.p) then k(inst(???, args))
    else
      shapeCtor("Dyn", Ls()): sp =>
        call(f.p, args.map(_.p), ???): cde =>
          StagedPath.mk(sp, cde)(k)

  // instrumentation rules

  def ruleLit(l: Literal)(k: StagedPath => Block): Block =
    shapeCtor("Lit", Ls(Value.Lit(l))): sp =>
      blockCtor("ValueLit", Ls(Value.Lit(l))): cde =>
        StagedPath.mk(sp, cde)(k)

  def ruleVar(r: Value.Ref)(k: StagedPath => Block): Block =
    // why assume it is already staged?
    val sp = StagedPath(r)
    // why not just use sp.code?
    blockCtor("ValueRef", Ls(toValue(r.l.nme))): cde =>
      StagedPath.mk(sp.shape, cde)(k)

  def ruleTup(t: Tuple)(using Context)(k: StagedPath => Block): Block =
    val Tuple(mut, elems) = t
    assert(!mut)

    transformArgs(elems): xs =>
      tuple(xs.map(_.shape)): shapes =>
        shapeCtor("Arr", Ls(shapes)): sp =>
          blockCtor("Tuple", xs.map(_.code)): cde =>
            StagedPath.mk(sp, cde)(k)

  def ruleSel(s: Select)(using Context)(k: StagedPath => Block): Block =
    val Select(p, i @ Tree.Ident(name)) = s
    transformPath(p): x =>
      // stage? there isn't a correct constructor for it though
      // val n = Shape(Value.Ref(new TempSymbol(N, name)))
      val n = ???
      fnSel(x.shape, n): sp =>
        select(x.code, i): cde =>
          StagedPath.mk(sp, cde)(k)

  def ruleDynSel(d: DynSelect)(using Context)(k: StagedPath => Block): Block =
    val DynSelect(qual, path, arrayIdx) = d
    transformPath(qual): x =>
      transformPath(path): y =>
        fnSel(x.shape, y.shape): sp =>
          blockCtor("DynSelect", Ls(x.code, y.code, toValue(arrayIdx))): cde =>
            StagedPath.mk(sp, cde)(k)

  def ruleRefinedPath(p: Path)(using ctx: Context)(k: StagedPath => Block): Block = ???

  // .apply is Call?
  def ruleApp(c: Call)(using Context)(k: StagedPath => Block): Block =
    val Call(fun, args) = c
    transformPath(fun): f =>
      transformArgs(args): xs =>
        instGlobal(f, xs)(k)

  def ruleInst(i: Instantiate)(using Context)(k: StagedPath => Block): Block =
    val Instantiate(mut, cls, args) = i
    assert(!mut)

    transformArgs(args): xs =>
      tuple(xs.map(_.shape)): shapes =>
        tuple(xs.map(_.code)): codes =>
          shapeCtor("Class", Ls(cls, shapes)): sp =>
            blockCtor("Instantiate", Ls(cls, codes)): cde =>
              StagedPath.mk(sp, cde)(k)

  def ruleReturn(r: Return)(using Context)(k: StagedPath => Block): Block =
    transformResult(r.res): x =>
      blockCtor("Return", Ls(x.code)): cde =>
        StagedPath.mk(x.shape, cde)(k)

  def ruleMatch(m: Match)(using ctx: Context)(k: StagedPath => Block): Block =
    def concat(b1: Block, b2: Block) = b1.mapTail {
      case r: Return => r
      case _: End => b2
      case _ => ???
    }
    def transformCase(cse: Opt[Case])(k: Path => Block): Block =
      cse match
        case S(Case.Lit(lit)) => blockCtor("Lit", Ls(Value.Lit(lit)))(k)
        case S(Case.Cls(cls, path)) => blockCtor("Cls", Ls(cls, path))(k)
        case S(Case.Tup(len, inf)) => blockCtor("Tup", Ls(len, inf).map(toValue))(k)
        case S(Case.Field(name, safe)) => blockCtor("Field", Ls(toValue(name.name)))(k)
        case N => blockCtor("Wildcard", Ls())(k)

    val Match(p, arms, dflt, rest) = m

    transformPath(p): x =>
      (arms.map((c, b) => (S(c), b)) ++ (dflt.map((N, _))))
        .map: (c, b) =>
          val concatBlock = concat(b, rest)
          (k: (((Path, (StagedPath => Block) => Block)) => Block)) =>
            transformCase(c): patt =>
              fnSilh(patt): sp =>
                val newCtx = ctx.clone() += (p -> sp)
                val blockCont = transformBlock(concatBlock)(using newCtx)
                k(patt, blockCont)
        .collectApply: arms =>
          // we need to duplicate the blocks anyways, so it's fine that blocksCont gets evaluated twice
          val (patts, blocksCont) = arms.unzip
          (arms.zipWithIndex.foldRight(_: Block) { case (((patt, blockCont), i), rest) =>
            val slice = arms.slice(0, i + 1).map(_._1)
            fnDet(x.shape, slice): scrut =>
              blockCont: block =>
                val cse = Case.Lit(Tree.BoolLit(false)) -> k(block)
                Match(scrut, Ls(cse), S(rest), End())
          }):
            // staged block
            blocksCont.collectApply: xs =>
              val (shapes, codes) = xs.map(xi => (xi.shape, xi.code)).unzip
              fnMrg(shapes): s =>
                (patts
                  .zip(codes)
                  .foldRight(blockCtor("End", Ls())) { case ((patt, cde), restCont) =>
                    (k: Path => Block) =>
                      restCont: rest =>
                        blockCtor("Match", Ls(x.code, patt, cde, rest))(k)
                  })(StagedPath.mk(s, _)(k))

  def ruleAssign(a: Assign)(using Context)(k: StagedPath => Block): Block =
    val Assign(x, r, b) = a
    transformResult(r): y =>
      (Assign(x, y.p, _)):
        transformBlock(b): z =>
          // need to wrap x with Symbol?
          blockCtor("Assign", Ls(x, y.code, z.code)): cde =>
            StagedPath.mk(z.shape, cde)(k)

  def ruleEnd()(k: StagedPath => Block): Block =
    shapeCtor("Unit", Ls()): sp =>
      blockCtor("End", Ls()): cde =>
        StagedPath.mk(sp, cde)(k)

  def ruleVal(defn: ValDefn, b: Block)(using Context)(k: StagedPath => Block): Block =
    val ValDefn(tsym, x, p) = defn
    transformPath(p): y =>
      transformBlock(b): z =>
        // TODO: valdefn needs to be before code blocks somehow?
        // y is StagedPath, not Path?
        (Define(ValDefn(tsym, x, y.p), _)):
          blockCtor("ValDefn", Ls(x, y.code)): df =>
            blockCtor("Define", Ls(df, z.code)): cde =>
              StagedPath.mk(z.shape, cde)(k)

  def ruleBlk(b: Block)(using Context)(k: StagedPath => Block): Block =
    transformBlock(b): x =>
      fnCompile(x.code)(k)

  // g is Program?
  def ruleCls(c: Program, rest: Block)(using Context)(k: StagedPath => Block): Block =
    // val ClsLikeDefn(_, _, )
    ???

  // transformations of Block

  def transformPath(p: Path)(using Context)(k: StagedPath => Block): Block =
    p match
      // case s: Select => ruleSel(s)(ruleRefinedPath(_)(k))
      // case d: DynSelect => ruleDynSel(d)(ruleRefinedPath(_)(k))
      // case r: Value.Ref => ruleVar(r)(ruleRefinedPath(_)(k))
      case r: Value.Ref => ruleVar(r)(k)
      case Value.Lit(lit) => ruleLit(lit)(k)
      case _ => ??? // not supported

  def transformResult(r: Result)(using Context)(k: StagedPath => Block): Block =
    r match
      // case c: Call => ruleApp(c)(k)
      // case i: Instantiate => ruleInst(i)(k)
      // case t: Tuple => ruleTup(t)(k)
      case p: Path => transformPath(p)(k)
      case _: Lambda | _: Record => ??? // not supported

  def transformArg(a: Arg)(using Context)(k: StagedPath => Block): Block =
    val Arg(spread, value) = a
    transformPath(value)(k)

  // provides list of shapes and list of codes to continuation
  def transformArgs(args: Ls[Arg])(using Context)(k: Ls[StagedPath] => Block): Block =
    args.map(transformArg).collectApply(k)

  def transformDefine(d: Define)(using Context)(k: StagedPath => Block): Block =
    d.defn match
      case f @ FunDefn(owner, sym, parameters, body) =>
        val genSym = BlockMemberSymbol("gen", Nil, true) // TODO: reuse original function name?
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

  def transformBlock(b: Block)(using Context)(k: StagedPath => Block): Block =
    b match
      // case m: Match => ruleMatch(m)(k)
      case r: Return => ruleReturn(r)(k)
      // case a: Assign => ruleAssign(a)(k)
      case d: Define => transformDefine(d)(k)
      case End(_) => ruleEnd()(k)
      case _ => ??? // not supported

  def transformProgram(prog: Program)(): Program =
    // TODO imports
    ??? // use ruleCls and ruleBlock here
    Program(prog.imports, transformBlock(prog.main)(using new Context())(_.end))
