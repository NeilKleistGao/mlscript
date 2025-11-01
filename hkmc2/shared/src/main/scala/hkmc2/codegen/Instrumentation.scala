package hkmc2
package codegen

import mlscript.utils.*, shorthands.*

import semantics.*
import semantics.Elaborator.State

import syntax.{Literal, Tree}

class Instrumentation(using State):
  def toArg(x: Path | Symbol): Arg =
    x match
      case p: Path => Arg(N, p)
      case l: Symbol => Arg(N, l.asPath)

  // null and undefined are missing
  def toValue(lit: Str | Int | BigDecimal | Bool): Value =
    val l = lit match
      case i: Int => Tree.IntLit(i)
      case b: Bool => Tree.BoolLit(b)
      case s: Str => Tree.StrLit(s)
      case n: BigDecimal => Tree.DecLit(n)
    Value.Lit(l)

  // helper for staging the constructors

  def end(l: Path): Block = Return(l, false)

  def assign(res: Result, name: String = "tmp")(k: Path => Block): Block =
    // TODO: skip assignment if res: Path?
    val tmp = new TempSymbol(N, name)
    Assign(tmp, res, k(tmp.asPath))

  def extractResult(b: Block)(k: Path => Block): Block =
    b.mapTail match
      case Return(r, _) => assign(r)(k)
      case _ => ??? // impossible

  def mlsBlock(nme: String, args: Ls[Path | Symbol])(k: Path => Block): Block =
    val s = summon[State].blockSymbol.asPath.selSN(nme)
    assign(Instantiate(false, s, args.map(toArg)))(k)
  def mlsShape(nme: String, args: Ls[Path | Symbol])(k: Path => Block): Block =
    val s = summon[State].shapeSymbol.asPath.selSN(nme)
    assign(Instantiate(false, s, args.map(toArg)))(k)

  // helpers corresponding to constructors

  def mlsSymbol(nme: String)(k: Path => Block): Block =
    mlsBlock("Symbol", Ls(toValue(nme)))(k)

  def mlsSelect(qual: Path, ident: Tree.Ident)(k: Path => Block): Block =
    // unnecessary assignment?
    assign(Select(qual, ident)(N))(k)

  def mlsTuple(elems: Ls[Symbol | Path])(k: Path => Block): Block =
    // is this the same as "Ls of"?
    assign(Tuple(false, elems.map(toArg)))(k)

  // helpers to create and access the components of a staged value
  // use getCode2 for spliced result, and getCode for code
  def returnPair(shape: Path, value: Path): Block =
    mlsTuple(Ls(shape, value))(end)
  def getShape(p: Path): Path = DynSelect(p, toValue(0), false)
  def getCode(p: Path): Path = DynSelect(p, toValue(1), false)

  // functions that perform the instrumentation

  def ruleLit(l: Literal): Block =
    mlsShape("Lit", Ls(Value.Lit(l))): sp =>
      mlsBlock("Lit", Ls(Value.Lit(l))): cde =>
        returnPair(sp, cde)

  def ruleVar(r: Value.Ref): Block =
    // why not just use r?
    mlsBlock("ValueRef", Ls(toValue(r.l.nme))): cde =>
      returnPair(getShape(r), cde)

  def ruleTup(t: Tuple): Block =
    val Tuple(mut, elems) = t
    assert(!mut)

    transformArgs(elems): (shapes, codes) =>
      mlsTuple(shapes): shapes =>
        mlsShape("Arr", Ls(shapes)): sp =>
          assign(Tuple(false, codes.map(toArg))): cde =>
            returnPair(sp, cde)

  def ruleSel(s: Select): Block =
    val Select(p, i) = s
    transformPath(p): x =>
      // TODO: how to format a?
      val sel = ???
      val n = ???
      assign(Call(sel, Ls(getShape(x), n).map(toArg))(true, false)): sp =>
        mlsSelect(getCode(x), i): cde =>
          returnPair(sp, cde)

  def ruleDynSel(d: DynSelect): Block = ???

  def ruleRefinedPath(p: Path): Block = ???

  def ruleApp(c: Call): Block = ???

  def ruleInst(i: Instantiate): Block =
    val Instantiate(mut, cls, args) = i
    assert(!mut)

    transformArgs(args): (shapes, codes) =>
      mlsTuple(shapes): shapes =>
        mlsTuple(codes): codes =>
          mlsShape("Class", Ls(cls, shapes)): sp =>
            mlsBlock("Instantiate", Ls(cls, codes)): cde =>
              returnPair(sp, cde)

  def ruleReturn(r: Return): Block =
    transformResult(r.res): x =>
      mlsBlock("Return", Ls(getCode(x))): cde =>
        returnPair(getShape(x), cde)

  def ruleMatch(m: Match): Block = ???

  def ruleAssign(a: Assign): Block = ???

  def ruleEnd(): Block =
    mlsShape("Unit", Ls()): sp =>
      mlsBlock("End", Ls()): cde =>
        returnPair(sp, cde)

  def ruleVal(defn: ValDefn, rest: Block): Block =
    val ValDefn(tsym, sym, rhs) = defn
    transformPath(rhs): y =>
      transformBlock(rest): z =>
        // TODO: valdefn needs to be before code blocks somehow?
        (Define(ValDefn(tsym, sym, y), _)):
          mlsSymbol("x"): x =>
            mlsBlock("ValDefn", Ls(x, getCode(y))): df =>
              mlsBlock("Define", Ls(df, getCode(z))): cde =>
                returnPair(getShape(z), cde)

  def ruleBlk(b: Block): Block = ???

  def ruleCls(c: ClassLikeDef, rest: Block): Block = ???

  // functions for instrumentation

  def transformPath(p: Path)(k: Path => Block): Block =
    p match
      // case Select(p, ident) => ???
      // case DynSelect(qual, fld, arrayIdx) => ???
      case r: Value.Ref => ruleVar(r)
      case Value.Lit(lit) => ruleLit(lit)
      case _ => ???

  def transformResult(r: Result)(k: Path => Block): Block =
    ???

  def transformArg(a: Arg)(k: Path => Block): Block =
    ???

  // provides list of shapes and list of codes to continuation
  def transformArgs(args: List[Arg])(k: (Ls[Path], Ls[Path]) => Block): Block =
    // collect (shape, code) pair for each arg
    def rec(f: List[(Path, Path)] => Block, a: Arg, rest: Ls[(Path, Path)]) =
      transformArg(a): p =>
        f((getShape(p), getCode(p)) :: rest)

    // collect up path for all args into a list
    // can you collect element wise instead?
    args.foldRight((f: List[(Path, Path)] => Block) => f(Nil))((a, acc) => f => acc(rec(f, a, _))):
      ps =>
        val (shapes, codes) = ps.unzip
        k(shapes, codes)

  def transformBlock(b: Block)(k: Path => Block): Block = ???
