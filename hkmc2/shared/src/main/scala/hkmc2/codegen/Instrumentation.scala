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

  // helper for staging the constructors

  // could use `using` to allow passthrough of names
  def assign(res: Result, name: String = "tmp")(k: Path => Block): Assign =
    // TODO: skip assignment if res: Path?
    val tmp = new TempSymbol(N, name)
    Assign(tmp, res, k(tmp.asPath))

  def mlsBlockMod(nme: String, args: Ls[PathLike])(k: Path => Block): Block =
    val s = summon[State].blockSymbol.asPath.selSN(nme)
    assign(Instantiate(false, s, args.map(asArg)))(k)
  def mlsShapeMod(nme: String, args: Ls[PathLike])(k: Path => Block): Block =
    val s = summon[State].shapeSymbol.asPath.selSN(nme)
    assign(Instantiate(false, s, args.map(asArg)))(k)
  def mlsShape(nme: String, args: Ls[PathLike])(k: Shape => Block): Block =
    mlsShapeMod(nme, args)(p => k(Shape(p)))

  // helpers corresponding to constructors

  def mlsSelect(qual: Path, ident: Tree.Ident)(k: Path => Block): Block =
    // unnecessary assignment?
    assign(Select(qual, ident)(N))(k)

  def mlsTuple(elems: Ls[PathLike])(k: Path => Block): Block =
    // is this the same as "Ls of"?
    assign(Tuple(false, elems.map(asArg)))(k)

  def mlsCall(fun: Path, args: Ls[PathLike], isMlsFun: Bool)(k: Path => Block): Block =
    assign(Call(fun, args.map(asArg))(isMlsFun, false))(k)

  // helpers to create and access the components of a staged value
  case class Shape(p: Path)

  // A StagedPath is a path that points to a (shape, code) tuple
  class StagedPath(val p: Path):
    def shape: Shape = Shape(DynSelect(p, toValue(0), false))
    def code: Path = DynSelect(p, toValue(1), false)
    def end: Block = Return(p, false)

  object StagedPath:
    def mk(shape: Shape, code: Path)(k: StagedPath => Block): Block =
      mlsTuple(Ls(shape.p, code))(p => k(StagedPath(p)))
    // in some cases this can reduce the indentation level
    def mk(shapeCont: (Shape => Block) => Block, codeCont: (Path => Block) => Block)(k: StagedPath => Block): Block =
      shapeCont: shape =>
        codeCont: code =>
          mk(shape, code)(k)

  // linking functions defined in MLscipt

  def fnMrg(shapes: Ls[Shape])(k: Shape => Block): Block =
    mlsShapeMod("mrg", shapes)(s => k(Shape(s)))
  // TODO: make fnSilh take in a wrapped Path type
  def fnSilh(pattern: Path)(k: Shape => Block) =
    mlsShapeMod("silh", Ls(pattern))(s => k(Shape(s)))
  def fnMatch(s: Shape, pat: Path)(k: Path => Block) =
    mlsShapeMod("match", Ls(s, pat))(k)
  def fnSel(s1: Shape, s2: Shape)(k: Shape => Block): Block =
    mlsShapeMod("sel", Ls(s1, s2))(s => k(Shape(s)))
  def fnStatic(s: Shape)(k: Path => Block) =
    mlsShapeMod("static", Ls(s))(k)
  def fnCompile(x: Path)(k: StagedPath => Block): Block =
    mlsShapeMod("compile", Ls(x))(p => k(StagedPath(p)))
  def fnDet(s: Shape, ps: Ls[PathLike])(k: Path => Block): Block =
    mlsTuple(ps): tup =>
      mlsShapeMod("det", Ls(s, tup))(k)

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
    if isFunction(f.p) then
      k(inst(???, args))
    else 
      mlsShape("Dyn", Ls()): sp =>
        mlsCall(f.p, args.map(_.p), ???): cde =>
          StagedPath.mk(sp, cde)(k)
  
  // instrumentation rules

  def ruleLit(l: Literal)(k: StagedPath => Block): Block =
    mlsShape("Lit", Ls(Value.Lit(l))): sp =>
      mlsBlockMod("Lit", Ls(Value.Lit(l))): cde =>
        StagedPath.mk(sp, cde)(k)

  def ruleVar(r: Value.Ref)(k: StagedPath => Block): Block =
    // why assume it is already staged?
    val sp = StagedPath(r)
    // why not just use sp.code?
    mlsBlockMod("ValueRef", Ls(toValue(r.l.nme))): cde =>
      StagedPath.mk(sp.shape, cde)(k)

  def ruleTup(t: Tuple)(using Context)(k: StagedPath => Block): Block =
    val Tuple(mut, elems) = t
    assert(!mut)

    transformArgs(elems): xs =>
      mlsTuple(xs.map(_.shape)): shapes =>
        mlsShape("Arr", Ls(shapes)): sp =>
          mlsTuple(xs.map(_.code)): cde => // is Tuple quotes as well?
            StagedPath.mk(sp, cde)(k)

  def ruleSel(s: Select)(using Context)(k: StagedPath => Block): Block =
    val Select(p, i @ Tree.Ident(name)) = s
    transformPath(p): x =>
      // stage? there isn't a correct constructor for it though
      // val n = Shape(Value.Ref(new TempSymbol(N, name)))
      val n = ???
      fnSel(x.shape, n): sp =>
        mlsSelect(x.code, i): cde =>
          StagedPath.mk(sp, cde)(k)

  def ruleDynSel(d: DynSelect)(using Context)(k: StagedPath => Block): Block =
    val DynSelect(qual, path, arrayIdx) = d
    transformPath(qual): x =>
      transformPath(path): y =>
        fnSel(x.shape, y.shape): sp =>
          mlsBlockMod("DynSelect", Ls(x.code, y.code, toValue(arrayIdx))): cde =>
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
      mlsTuple(xs.map(_.shape)): shapes =>
        mlsTuple(xs.map(_.code)): codes =>
          mlsShape("Class", Ls(cls, shapes)): sp =>
            mlsBlockMod("Instantiate", Ls(cls, codes)): cde =>
              StagedPath.mk(sp, cde)(k)

  def ruleReturn(r: Return)(using Context)(k: StagedPath => Block): Block =
    transformResult(r.res): x =>
      mlsBlockMod("Return", Ls(x.code)): cde =>
        StagedPath.mk(x.shape, cde)(k)

  def ruleMatch(m: Match)(using Context)(k: StagedPath => Block): Block =
    val Match(p, arms, dflt, b) = m
    transformPath(p): x =>
      ???

  def ruleAssign(a: Assign)(using Context)(k: StagedPath => Block): Block = 
    val Assign(x, r, b) = a
    transformResult(r): y =>
      (Assign(x, y.p, _)):
        transformBlock(b): z =>
          // need to wrap x with Symbol?
          mlsBlockMod("Assign", Ls(x, y.code, z.code)): cde =>
            StagedPath.mk(z.shape, cde)(k)

  def ruleEnd()(k: StagedPath => Block): Block =
    mlsShape("Unit", Ls()): sp =>
      mlsBlockMod("End", Ls()): cde =>
        StagedPath.mk(sp, cde)(k)

  def ruleVal(defn: ValDefn, b: Block)(using Context)(k: StagedPath => Block): Block =
    val ValDefn(tsym, x, p) = defn
    transformPath(p): y =>
      transformBlock(b): z =>
        // TODO: valdefn needs to be before code blocks somehow?
        // y is StagedPath, not Path?
        (Define(ValDefn(tsym, x, y.p), _)):
            mlsBlockMod("ValDefn", Ls(x, y.code)): df =>
              mlsBlockMod("Define", Ls(df, z.code)): cde =>
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
      case s: Select => ruleSel(s)(k)
      case d: DynSelect => ruleDynSel(d)(k)
      case r: Value.Ref => ruleVar(r)(k)
      case Value.Lit(lit) => ruleLit(lit)(k)
      case _ => ??? // not supported

  def transformResult(r: Result)(using Context)(k: StagedPath => Block): Block =
    r match
      case c: Call => ruleApp(c)(k)
      case i: Instantiate => ruleInst(i)(k)
      case t: Tuple => ruleTup(t)(k)
      case p: Path => transformPath(p)(k)
      case _ : Lambda | _: Record => ??? // not supported

  def transformArg(a: Arg)(using Context)(k: StagedPath => Block): Block =
    val Arg(spread, value) = a
    ??? // arg has no shape of its own? it's just a wrapper for Path

    transformPath(value)(k)

  // provides list of shapes and list of codes to continuation
  def transformArgs(args: Ls[Arg])(using Context)(k: Ls[StagedPath] => Block): Block =
    // TODO: use BlockTransformer.applyListOf?
    args
      .map(transformArg)
      // defer applying k while prepending new paths to the list
      .foldRight((_: Ls[StagedPath] => Block)(Nil))((pathCont, restCont) =>
        k =>
          pathCont: p =>
            restCont: rest =>
              k(p :: rest)
      )(k)
  
  def transformDefine(d: Define)(using Context)(k: StagedPath => Block): Block = 
    d.defn match
      case f: FunDefn => ???
      case v: ValDefn => ruleVal(v, d.rest)(k)
      case c: ClsLikeDefn => ???

  def transformBlock(b: Block)(using Context)(k: StagedPath => Block): Block =
    b match
      case m: Match => ruleMatch(m)(k)
      case r: Return => ruleReturn(r)(k)
      case a: Assign => ruleAssign(a)(k)
      case d: Define => transformDefine(d)(k)
      case End(_) => ruleEnd()(k)
      case l: Label => ???
      case _ => ??? // not supported
