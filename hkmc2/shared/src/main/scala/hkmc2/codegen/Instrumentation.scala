package hkmc2
package codegen

import mlscript.utils.*, shorthands.*

import semantics.*
import semantics.Elaborator.State

import syntax.{Literal, Tree}

class Instrumentation(using State):
  type PathLike = Path | Symbol | Shape

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

  def mlsBlock(nme: String, args: Ls[PathLike])(k: Path => Block): Block =
    val s = summon[State].blockSymbol.asPath.selSN(nme)
    assign(Instantiate(false, s, args.map(asArg)))(k)
  def mlsShape(nme: String, args: Ls[PathLike])(k: Shape => Block): Block =
    val s = summon[State].shapeSymbol.asPath.selSN(nme)
    assign(Instantiate(false, s, args.map(asArg)))(p => k(Shape(p)))

  // helpers corresponding to constructors

  def mlsSymbol(nme: String)(k: Path => Block): Block =
    mlsBlock("Symbol", Ls(toValue(nme)))(k)

  def mlsSelect(qual: Path, ident: Tree.Ident)(k: Path => Block): Block =
    // unnecessary assignment?
    assign(Select(qual, ident)(N))(k)

  def mlsTuple(elems: Ls[PathLike])(k: Path => Block): Block =
    // is this the same as "Ls of"?
    assign(Tuple(false, elems.map(asArg)))(k)

  def mlsCall(fun: Path, args: Ls[PathLike], isMlsFun: Bool)(k: Path => Block): Block =
    assign(Call(fun, args.map(asArg))(isMlsFun, false))(k)

  // helpers to create and access the components of a staged value
  case class Shape(val p: Path)

  class StagedPath(val p: Path):
    def shape: Shape = Shape(DynSelect(p, toValue(0), false))
    def code: Path = DynSelect(p, toValue(1), false)

  object StagedPath:
    def mk(shape: Shape, code: Path)(k: StagedPath => Block): Block =
      mlsTuple(Ls(shape.p, code))(p => k(StagedPath(p)))
    // in some cases this can reduce the indentation level
    def mk(shapeCont: (Shape => Block) => Block, codeCont: (Path => Block) => Block)(k: StagedPath => Block): Block =
      shapeCont: shape =>
        codeCont: code =>
          mk(shape, code)(k)
    def end(sp: StagedPath): Block = Return(sp.p, false)

  // linking functions defined in MLscipt
  val mrgSymbol = new TempSymbol(N, "mrg")
  val silhSymbol = new TempSymbol(N, "silh")
  val matchSymbol = new TempSymbol(N, "match")
  val selSymbol = new TempSymbol(N, "sel")
  val staticSymbol = new TempSymbol(N, "static")
  val compileSymbol = new TempSymbol(N, "compile")
  
  def fnMrg(shapes: Ls[Shape])(k: Shape => Block): Block = 
    mlsCall(mrgSymbol.asPath, shapes, true)(s => k(Shape(s)))
  // TODO: make fnSilh take in a wrapped Path type
  def fnSilh(pattern: Path)(k: Shape => Block) = 
    mlsCall(silhSymbol.asPath, Ls(pattern), true)(s => k(Shape(s)))
  def fnMatch(s: Shape, pat: Path)(k: Path => Block) = 
    mlsCall(matchSymbol.asPath, Ls(s, pat), true)(k)
  def fnSel(s1: Shape, s2: Shape)(k: Shape => Block): Block = 
    mlsCall(selSymbol.asPath, Ls(s1, s2), true)(s => k(Shape(s)))
  def fnStatic(s: Shape)(k: Path => Block) = 
    mlsCall(staticSymbol.asPath, Ls(s), true)(k)
  def fnCompile(x: Path)(k: StagedPath => Block): Block = 
    mlsCall(compileSymbol.asPath, Ls(x), true)(p => k(StagedPath(p)))

  // functions that perform the instrumentation

  def ruleLit(l: Literal)(k: StagedPath => Block): Block =
    mlsShape("Lit", Ls(Value.Lit(l))): sp =>
      mlsBlock("Lit", Ls(Value.Lit(l))): cde =>
        StagedPath.mk(sp, cde)(k)

  def ruleVar(r: Value.Ref)(k: StagedPath => Block): Block =
    // why assume it is already staged?
    val sp = StagedPath(r)
    // why not just use sp.code?
    mlsBlock("ValueRef", Ls(toValue(r.l.nme))): cde =>
      StagedPath.mk(sp.shape, cde)(k)

  def ruleTup(t: Tuple)(k: StagedPath => Block): Block =
    val Tuple(mut, elems) = t
    assert(!mut)

    transformArgs(elems): (shapes, codes) =>
      mlsTuple(shapes): shapes =>
        mlsShape("Arr", Ls(shapes)): sp =>
          mlsTuple(codes): cde => // is Tuple quotes as well?
            StagedPath.mk(sp, cde)(k)

  def ruleSel(s: Select)(k: StagedPath => Block): Block =
    val Select(p, i) = s
    transformPath(p): x =>

      // TODO: how to format a?
      val sel = ???
      val n = ???
      // can use shape.p?
      // assign(Call(sel, Ls(x.shape, n).map(toArg))(true, false)): sp =>
      //   mlsSelect(x.code, i): cde =>
      //     StagedPath.mk(sp, cde)(k)
      ???

  def ruleDynSel(d: DynSelect)(k: StagedPath => Block): Block = ???

  def ruleRefinedPath(p: Path)(k: StagedPath => Block): Block = ???

  def ruleApp(c: Call)(k: StagedPath => Block): Block = ???

  def ruleInst(i: Instantiate)(k: StagedPath => Block): Block =
    val Instantiate(mut, cls, args) = i
    assert(!mut)

    transformArgs(args): (shapes, codes) =>
      mlsTuple(shapes): shapes =>
        mlsTuple(codes): codes =>
          mlsShape("Class", Ls(cls, shapes)): sp =>
            mlsBlock("Instantiate", Ls(cls, codes)): cde =>
              StagedPath.mk(sp, cde)(k)

  def ruleReturn(r: Return)(k: StagedPath => Block): Block =
    transformResult(r.res): x =>
      mlsBlock("Return", Ls(x.code)): cde =>
        StagedPath.mk(x.shape, cde)(k)

  def ruleMatch(m: Match)(k: StagedPath => Block): Block = ???

  def ruleAssign(a: Assign)(k: StagedPath => Block): Block = ???

  def ruleEnd()(k: StagedPath => Block): Block =
    mlsShape("Unit", Ls()): sp =>
      mlsBlock("End", Ls()): cde =>
        StagedPath.mk(sp, cde)(k)

  def ruleVal(defn: ValDefn, rest: Block)(k: StagedPath => Block): Block =
    val ValDefn(tsym, sym, rhs) = defn
    transformPath(rhs): y =>
      transformBlock(rest): z =>
        // TODO: valdefn needs to be before code blocks somehow?
        // y is StagedPath, not Path?
        (Define(ValDefn(tsym, sym, y.p), _)):
          mlsSymbol("x"): x =>
            mlsBlock("ValDefn", Ls(x, y.code)): df =>
              mlsBlock("Define", Ls(df, z.code)): cde =>
                StagedPath.mk(z.shape, cde)(k)

  def ruleBlk(b: Block)(k: StagedPath => Block): Block = ???

  def ruleCls(c: ClassLikeDef, rest: Block)(k: StagedPath => Block): Block = ???

  // functions for instrumentation

  def transformPath(p: Path)(k: StagedPath => Block): Block =
    p match
      // case Select(p, ident) => ???
      // case DynSelect(qual, fld, arrayIdx) => ???
      case r: Value.Ref => ruleVar(r)(k)
      case Value.Lit(lit) => ruleLit(lit)(k)
      case _ => ???

  def transformResult(r: Result)(k: StagedPath => Block): Block =
    r match
      case Call(name, args) => ???
      case Instantiate(mut, cls, args) => ???
      case Lambda(params, body) => ???
      case Tuple(mut, elems) => ???
      case Record(mut, elems) => ???
      case p: Path => transformPath(p)(k)

  def transformArg(a: Arg)(k: StagedPath => Block): Block =
    val Arg(spread, value) = a
    ??? // arg has no shape of its own?

    transformPath(value)(k)

  // provides list of shapes and list of codes to continuation
  def transformArgs(args: Ls[Arg])(k: (Ls[Shape], Ls[Path]) => Block): Block =
    args
      .map(transformArg)
      // defer applying k while prepending new paths to the list
      .foldRight((_: Ls[StagedPath] => Block)(Nil))((pathCont, restCont) =>
        k =>
          pathCont: p =>
            restCont: rest =>
              k(p :: rest)
      ): ps =>
        // collect (shape, code) pair for each arg
        val (shapes, codes) = ps.map(p => (p.shape, p.code)).unzip
        k(shapes, codes)

  def transformBlock(b: Block)(k: StagedPath => Block): Block = ???
