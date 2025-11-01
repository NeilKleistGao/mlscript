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
    val tmp = new TempSymbol(N, name)
    Assign(tmp, res, k(tmp.asPath))

  def extractResult(b: Block)(k: Path => Block): Block =
    b.mapTail match
      case Return(r, _) => assign(r)(k)
      case _ => ??? // impossible

  def stagedBlock(nme: String, args: Ls[Path | Symbol])(k: Path => Block): Block =
    val s = summon[State].blockSymbol.asPath.selSN(nme)
    assign(Instantiate(false, s, args.map(toArg)))(k)
  def stagedShape(nme: String, args: Ls[Path | Symbol])(k: Path => Block): Block =
    val s = summon[State].shapeSymbol.asPath.selSN(nme)
    assign(Instantiate(false, s, args.map(toArg)))(k)

  // helpers corresponding to constructors

  def stagedSymbol(nme: String)(k: Path => Block): Block =
    stagedBlock("Symbol", Ls(toValue(nme)))(k)

  def stagedIdent(nme: String)(k: Path => Block): Block =
    stagedBlock("Ident", Ls(toValue(nme)))(k)

  def stagedRef(l: Symbol)(k: Path => Block): Block =
    stagedSymbol(l.nme): l =>
      stagedBlock("ValueRef", Ls(l))(k)

  // note that this is for Block.ValueLit, not Shape.Lit
  def stagedBLit(l: Literal)(k: Path => Block): Block =
    stagedBlock("ValueLit", Ls(Value.Lit(l)))(k)

  def stagedSelect(qual: Path, name: Str)(k: Path => Block): Block =
    stagedPath(qual): p =>
      stagedIdent(name): i =>
        stagedBlock("Select", Ls(p, i))(k)

  def stagedDynSelect(qual: Path, fld: Path, arrayIdx: Bool)(k: Path => Block): Block =
    stagedPath(qual): q =>
      stagedPath(fld): f =>
        stagedBlock("DynSelect", Ls(q, f, toValue(arrayIdx)))(k)

  def stagedPath(p: Path)(k: Path => Block): Block = p match
    case Select(qual, tree) => stagedSelect(qual, tree.name)(k)
    case DynSelect(qual, fld, arrayIdx) => stagedDynSelect(qual, fld, arrayIdx)(k)
    case Value.Ref(l) => stagedRef(l)(k)
    case Value.Lit(lit) => stagedBLit(lit)(k)
    case _ => ???

  def stagedTuple(elems: Ls[Symbol | Path])(k: Path => Block): Block =
    // is this the same as "Ls of"?
    assign(Tuple(false, elems.map(toArg))): tup =>
      stagedBlock("Tuple", Ls(tup))(k)

  // helpers to create and access the components of a staged value
  def returnPair(shape: Path, value: Path): Block =
    assign(Tuple(true, Ls(shape, value).map(toArg)))(end)
  def getShape(p: Path)(k: Path => Block): Block = assign(getShape2(p))(k)
  def getCode(p: Path)(k: Path => Block): Block = assign(getCode2(p))(k)
  def getShape2(p: Path): Path = DynSelect(p, toValue(0), false)
  def getCode2(p: Path): Path = DynSelect(p, toValue(1), false)

  // todo functions that fills out the holes in the functions above

  def stagedResult(res: Result)(k: Block => Block): Block = res match
    case Call(fun, args) => ???
    case res: Instantiate => ???
    case Lambda(params, body) => ???
    case Tuple(mut, elems) => ???
    case Record(mut, elems) => ???
    case p: Path => stagedPath(p)

  // probably wrong signature
  def stagedPath(p: Path): Block = ???

  def stagedArg(arg: Arg)(k: Path => Block): Block =
    val stagedSpread = arg.spread match
      case Some(value) => stagedBlock("Some", Ls(toValue(value)))
      case None => stagedBlock("None", Ls())
    stagedSpread: s =>
      stagedPath(arg.value): v =>
        stagedTuple(Ls(s, v))(k)

  // functions that perform the instrumentation

  def ruleLit(l: Literal): Block =
    stagedShape("Lit", Ls(Value.Lit(l))): sp =>
      stagedBlock("Lit", Ls(Value.Lit(l))): cde =>
        returnPair(sp, cde)

  def ruleVar(r: Value.Ref): Block =
    // why not just use getShape2?
    getShape(r): sp =>
      // why not just use r?
      stagedBlock("ValueRef", Ls(toValue(r.l.nme))): cde =>
        returnPair(sp, cde)

  def ruleSel(s: Select): Block =
    val Select(p, Tree.Ident(a)) = s
    transformPath(p): b =>
      extractResult(b): x =>
        // TODO: how to format a?
        val sel = ???
        val n = ???
        assign(Call(sel, Ls(getShape2(x), n).map(toArg))(true, false)): sp =>
          stagedSelect(getCode2(x), a): cde =>
            returnPair(sp, cde)

  def ruleReturn(r: Return): Block =
    transformResult(r.res): b =>
      extractResult(b): tmp =>
        getShape(tmp): sp =>
          stagedBlock("Return", Ls(getCode2(tmp))): cde =>
            returnPair(sp, cde)

  def ruleInst(i: Instantiate): Block =
    val Instantiate(mut, cls, args) = i
    assert(!mut)

    // collect (shape, code) pair for each arg
    def rec(f: List[(Path, Path)] => Block, a: Arg, rest: Ls[(Path, Path)]) =
      transformArg(a): b =>
        extractResult(b): p =>
          getShape(p): sh =>
            getCode(p): cde =>
              f((sh, cde) :: rest)

    // // can you collect element wise?
    // val paths = args.map(a =>
    //   (k: Path => Block) =>
    //     transformArg(a): b =>
    //       extractResult(b)(k)
    // )
    // collect up path for all args into a list
    args.foldRight((f: List[(Path, Path)] => Block) => f(Nil))((a, acc) =>
      f => acc(rest => rec(f, a, rest))
    ): ps =>
      val (shapes, codes) = ps.unzip
      stagedTuple(shapes): shapes =>
        stagedTuple(codes): codes =>
          stagedShape("Class", Ls(cls, shapes)): sp =>
            stagedBlock("Instantiate", Ls(cls, codes)): cde =>
              returnPair(sp, cde)

  def ruleVal(defn: ValDefn, rest: Block): Block =
    val ValDefn(_, sym, rhs) = defn
    transformPath(rhs): b =>
      extractResult(b): y =>
        transformBlock(rest): b =>
          extractResult(b): z =>
            // TODO: valdefn needs to be before code blocks?
            Define(
              ValDefn(???, sym, y),
              getShape(z): sp =>
                stagedSymbol("x"): x =>
                  stagedBlock("ValDefn", Ls(x, getCode2(y))): df =>
                    stagedBlock("Define", Ls(df, getCode2(z))): cde =>
                      returnPair(sp, cde)
            )

  def transformPath(p: Path)(k: Block => Block): Block =
    p match
      // case Select(p, ident) => ???
      // case DynSelect(qual, fld, arrayIdx) => ???
      case r: Value.Ref => ruleVar(r)
      case Value.Lit(lit) => ruleLit(lit)
      case _ => ???

  def transformResult(r: Result)(k: Block => Block): Block =
    ???

  def transformArg(p: Arg)(k: Block => Block): Block =
    ???

  def transformBlock(b: Block)(k: Block => Block): Block = ???

