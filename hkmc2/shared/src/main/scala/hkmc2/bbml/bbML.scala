package hkmc2
package bbml

import scala.collection.mutable.{LinkedHashSet, HashMap, ListBuffer}
import scala.annotation.tailrec

import mlscript.utils.*, shorthands.*
import Message.MessageContext
import semantics.*, semantics.Term.*
import syntax.*
import Tree.*

object InfVarUid extends Uid.Handler[Type.InfVar]

final case class Ctx(
  parent: Option[Ctx],
  lvl: Int,
  clsDefs: HashMap[Str, ClassDef],
  env: HashMap[Uid[Symbol], GeneralType],
  quoteSkolemEnv: HashMap[Uid[Symbol], Type.InfVar], // * SkolemTag for variables in quasiquotes
):
  def +=(p: Symbol -> GeneralType): Unit = env += p._1.uid -> p._2
  def get(sym: Symbol): Option[GeneralType] = env.get(sym.uid) orElse parent.dlof(_.get(sym))(None)
  def *=(cls: ClassDef): Unit = clsDefs += cls.sym.id.name -> cls
  def getDef(name: Str): Option[ClassDef] = clsDefs.get(name) orElse parent.dlof(_.getDef(name))(None)
  def &=(p: Symbol -> Type.InfVar): Unit =
    env += p._1.uid -> Ctx.varTy(p._2)(using this)
    quoteSkolemEnv += p._1.uid -> p._2
  def getSk(sym: Symbol): Option[Type] = quoteSkolemEnv.get(sym.uid) orElse parent.dlof(_.getSk(sym))(None)
  def nest: Ctx = Ctx(Some(this), lvl, HashMap.empty, HashMap.empty, quoteSkolemEnv)
  def nextLevel: Ctx = Ctx(Some(this), lvl + 1, HashMap.empty, HashMap.empty, quoteSkolemEnv)

object Ctx:
  def intTy(using ctx: Ctx): Type = Type.ClassType(ctx.getDef("Int").get.sym, Nil)
  def numTy(using ctx: Ctx): Type = Type.ClassType(ctx.getDef("Num").get.sym, Nil)
  def strTy(using ctx: Ctx): Type = Type.ClassType(ctx.getDef("Str").get.sym, Nil)
  def boolTy(using ctx: Ctx): Type = Type.ClassType(ctx.getDef("Bool").get.sym, Nil)
  private def codeBaseTy(cr: TypeArg, isVar: TypeArg)(using ctx: Ctx): Type = Type.ClassType(ctx.getDef("CodeBase").get.sym, cr :: isVar :: Nil)
  def codeTy(cr: Type)(using ctx: Ctx): Type = codeBaseTy(Wildcard.out(cr), Wildcard.out(Type.Top))
  def varTy(cr: Type)(using ctx: Ctx): Type = codeBaseTy(Wildcard(cr, cr), Wildcard.out(Type.Bot))
  def regionTy(sk: Type)(using ctx: Ctx): Type = Type.ClassType(ctx.getDef("Region").get.sym, Wildcard(sk, sk) :: Nil)
  def refTy(ct: Type, sk: Type)(using ctx: Ctx): Type = Type.ClassType(ctx.getDef("Ref").get.sym, Wildcard(ct, ct) :: Wildcard.out(sk) :: Nil)
  private val builtinClasses = Ls(
    "Any", "Int", "Num", "Str", "Bool", "Nothing", "CodeBase", "Region", "Ref"
  )
  private val infVarState = new InfVarUid.State()
  private val int2IntBinTy =
    (ctx: Ctx) => Type.FunType(intTy(using ctx) :: intTy(using ctx) :: Nil, intTy(using ctx), Type.Bot)
  private val int2BoolBinTy =
    (ctx: Ctx) => Type.FunType(intTy(using ctx) :: intTy(using ctx) :: Nil, boolTy(using ctx), Type.Bot)
  private val int2NumBinTy =
    (ctx: Ctx) => Type.FunType(intTy(using ctx) :: intTy(using ctx) :: Nil, numTy(using ctx), Type.Bot)
  private val num2NumBinTy =
    (ctx: Ctx) => Type.FunType(numTy(using ctx) :: numTy(using ctx) :: Nil, numTy(using ctx), Type.Bot)
  private val builtinOps = Map(
    "+" -> int2IntBinTy,
    "-" -> int2IntBinTy,
    "*" -> int2IntBinTy,
    "/" -> int2NumBinTy,
    "<" -> int2BoolBinTy,
    ">" -> int2BoolBinTy,
    "+." -> num2NumBinTy,
    "-." -> num2NumBinTy,
    "*." -> num2NumBinTy,
    "/." -> num2NumBinTy,
    "==" -> ((ctx: Ctx) => {
      val tv: Type.InfVar = Type.InfVar(1, infVarState.nextUid, new VarState(), false)
      PolyType(tv :: Nil, Type.FunType(tv :: tv :: Nil, boolTy(using ctx), Type.Bot))
    })
  )
  private val builtinVals = Map(
    "run" -> ((ctx: Ctx) => Type.FunType(codeTy(Type.Bot)(using ctx) :: Nil, Type.Top, Type.Bot)),
    "error" -> ((ctx: Ctx) => Type.Bot),
    "log" -> ((ctx: Ctx) => Type.FunType(strTy(using ctx) :: Nil, Type.Top, Type.Bot)),
  )
  def isOp(name: Str): Bool = builtinOps.contains(name)
  def init(predefs: Map[Str, Symbol]): Ctx =
    val ctx = new Ctx(None, 1, HashMap.empty, HashMap.empty, HashMap.empty)
    builtinClasses.foreach(
      cls =>
        predefs.get(cls) match
          case Some(cls: ClassSymbol) => ctx *= ClassDef.Plain(cls, Nil, ObjBody(Term.Blk(Nil, Term.Lit(Tree.UnitLit(true)))), None)
          case _ => ???
    )
    (builtinOps ++ builtinVals).foreach(
      p =>
        predefs.get(p._1) match
          case Some(v: Symbol) => ctx += v -> p._2(ctx)
          case _ => ???
    )
    ctx

class BBTyper(raise: Raise, val initCtx: Ctx, tl: TraceLogger):
  import tl.{trace, log}
  
  private val infVarState = new InfVarUid.State()
  private val solver = new ConstraintSolver(raise, infVarState, tl)

  private def freshSkolem(using ctx: Ctx): Type.InfVar = Type.InfVar(ctx.lvl, infVarState.nextUid, new VarState(), true)
  private def freshVar(using ctx: Ctx): Type.InfVar = Type.InfVar(ctx.lvl, infVarState.nextUid, new VarState(), false)
  private def freshWildcard(using ctx: Ctx) = Wildcard(freshVar, freshVar)

  // * always extruded
  private val allocSkolem: Type.InfVar = Type.InfVar(Int.MaxValue, infVarState.nextUid, new VarState(), true)

  private def error(msg: Ls[Message -> Opt[Loc]]) =
    raise(ErrorReport(msg))
    Type.Bot // TODO: error type?

  private def extract(asc: Term)(using map: Map[Uid[Symbol], Wildcard], pol: Bool)(using ctx: Ctx): GeneralType = asc match
    case Ref(cls: ClassSymbol) =>
      ctx.getDef(cls.nme) match
        case S(_) =>
          if cls.nme == "Any" then
            Type.Top
          else if cls.nme == "Nothing" then
            Type.Bot
          else
            Type.ClassType(cls, Nil)
        case N => 
          error(msg"Definition not found: ${cls.nme}" -> asc.toLoc :: Nil)
    case Ref(sym: VarSymbol) =>
      map.get(sym.uid) match
        case Some(Wildcard(in, out)) => if pol then out else in
        case _ => ctx.get(sym).getOrElse(error(msg"Type variable not found: ${sym.name}" -> asc.toLoc :: Nil))
    case FunTy(Term.Tup(params), ret, eff) =>
      PolyFunType(params.map {
        case Fld(_, p, _) => extract(p)(using map, !pol)
      }, extract(ret), eff.map(e => extract(e) match {
        case t: Type => t
        case _ => error(msg"Effect cannot be polymorphic." -> asc.toLoc :: Nil)
      }).getOrElse(Type.Bot))
    case Term.Forall(tvs, body) =>
      val nestCtx = ctx.nextLevel
      given Ctx = nestCtx
      val bd = tvs.map:
        case sym: VarSymbol =>
          val tv = freshVar
          nestCtx += sym -> tv // TODO: a type var symbol may be better...
          tv
      PolyType(bd, extract(body))
    case _ => error(msg"${asc.toString} is not a valid class member type" -> asc.toLoc :: Nil) // TODO

  private def typeType(ty: Term)(using ctx: Ctx, allowPoly: Bool): GeneralType = ty match
    case Ref(sym: VarSymbol) =>
      ctx.get(sym) match
        case Some(ty) => ty
        case _ =>
          if sym.nme == "Alloc" then
            allocSkolem
          else
            error(msg"Variable not found: ${sym.name}" -> ty.toLoc :: Nil)
    case Ref(cls: ClassSymbol) =>
      ctx.getDef(cls.nme) match
        case S(_) =>
          if cls.nme == "Any" then
            Type.Top
          else if cls.nme == "Nothing" then
            Type.Bot
          else
            Type.ClassType(cls, Nil) // TODO: tparams?
        case N => 
          error(msg"Definition not found: ${cls.nme}" -> ty.toLoc :: Nil)
    case FunTy(Term.Tup(params), ret, eff) =>
      PolyFunType(params.map {
        case Fld(_, p, _) => typeType(p)
      }, typeType(ret), eff.map(typeType).getOrElse(Type.Bot) match {
        case t: Type => t
        case _ => error(msg"Effect cannot be polymorphic." -> ty.toLoc :: Nil)
      })
    case Term.Forall(tvs, body) if allowPoly =>
      val nestCtx = ctx.nextLevel
      given Ctx = nestCtx
      val bd = tvs.map:
        case sym: VarSymbol =>
          val tv = freshVar
          nestCtx += sym -> tv // TODO: a type var symbol may be better...
          tv
      PolyType(bd, typeType(body))
    case _: Term.Forall =>
      error(msg"Polymorphic type is not allowed here." -> ty.toLoc :: Nil)
    case Term.TyApp(lhs, targs) => typeType(lhs) match
      case Type.ClassType(cls, _) => Type.ClassType(cls, targs.map {
        case Term.WildcardTy(in, out) =>
          Wildcard(
            in.map(t => typeType(t)(using ctx, false)).getOrElse(Type.Bot).monoOr(error(msg"Polymorphic type is not allowed here." -> ty.toLoc :: Nil)),
            out.map(t => typeType(t)(using ctx, false)).getOrElse(Type.Top).monoOr(error(msg"Polymorphic type is not allowed here." -> ty.toLoc :: Nil))
          )
        case t => typeType(t)(using ctx, false).monoOr(error(msg"Polymorphic type is not allowed here." -> ty.toLoc :: Nil))
      })
      case _ => error(msg"${lhs.toString} is not a class" -> ty.toLoc :: Nil)
    case CompType(lhs, rhs, pol) =>
      Type.mkComposedType(
        typeType(lhs).monoOr(error(msg"Polymorphic type is not allowed here." -> lhs.toLoc :: Nil)),
        typeType(rhs).monoOr(error(msg"Polymorphic type is not allowed here." -> rhs.toLoc :: Nil)),
        pol
      )
    case _ => error(msg"${ty.toString} is not a valid type annotation" -> ty.toLoc :: Nil) // TODO

  private def subst(ty: GeneralType)(using map: Map[Uid[Type.InfVar], Type.InfVar]): GeneralType = ty match
    case Type.ClassType(name, targs) =>
      Type.ClassType(name, targs.map {
        case Wildcard(in, out) =>
          Wildcard(
            subst(in).monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)),
            subst(out).monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil))
          )
        case ty: Type => subst(ty).monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil))
      })
    case v @ Type.InfVar(_, uid, _, _) =>
      map.get(uid).getOrElse(v)
    case PolyFunType(args, ret, eff) =>
      PolyFunType(args.map(subst), subst(ret), subst(eff).monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)))
    case Type.FunType(args, ret, eff) =>
      PolyFunType(args.map(subst), subst(ret), subst(eff).monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)))
    case Type.ComposedType(lhs, rhs, pol) =>
      Type.mkComposedType(
        subst(lhs).monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)),
        subst(rhs).monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)),
        pol
      )
    case Type.NegType(ty) => Type.mkNegType(subst(ty).monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)))
    case PolyType(tvs, body) =>
      PolyType(tvs, subst(body))
    case Type.Top | Type.Bot => ty

  private def instantiate(ty: PolyType)(using ctx: Ctx): GeneralType =
    subst(ty.body)(using (ty.tv.map {
      case Type.InfVar(_, uid, _, _) =>
        val nv = freshVar
        uid -> nv
    }).toMap)
  
  // * check if a poly lhs is equivalent to a poly rhs
  // TODO: refactor
  private def checkPoly(lhs: GeneralType, rhs: GeneralType)(using ctx: Ctx): Bool = (lhs, rhs) match
    case (Type.ClassType(name1, targs1), Type.ClassType(name2, targs2)) if name1.uid == name2.uid && targs1.length == targs2.length =>
      targs1.zip(targs2).foldLeft(true)((res, p) => p match {
        case (Wildcard(in1, out1), Wildcard(in2, out2)) =>
          res && checkPoly(in1, in2) && checkPoly(out1, out2)
        case (ty: Type, Wildcard(in2, out2)) =>
          res && checkPoly(ty, in2) && checkPoly(ty, out2)
        case (Wildcard(in1, out1), ty: Type) =>
          res && checkPoly(in1, ty) && checkPoly(out1, ty)
        case (ty1: Type, ty2: Type) => res && checkPoly(ty1, ty2)
      })
    case (Type.InfVar(_, uid1, _, _), Type.InfVar(_, uid2, _, _)) => uid1 == uid2
    case (PolyFunType(args1, ret1, eff1), PolyFunType(args2, ret2, eff2)) if args1.length == args2.length =>
      args1.zip(args2).foldLeft(checkPoly(ret1, ret2) && checkPoly(eff1, eff2))((res, p) => res && checkPoly(p._1, p._2))
    case (Type.FunType(args1, ret1, eff1), Type.FunType(args2, ret2, eff2)) if args1.length == args2.length =>
      args1.zip(args2).foldLeft(checkPoly(ret1, ret2) && checkPoly(eff1, eff2))((res, p) => res && checkPoly(p._1, p._2))
    case (Type.ComposedType(lhs1, rhs1, pol1), Type.ComposedType(lhs2, rhs2, pol2)) if pol1 == pol2 =>
      checkPoly(lhs1, lhs2) && checkPoly(rhs1, rhs2)
    case (Type.NegType(ty1), Type.NegType(ty2)) => checkPoly(ty1, ty2)
    case (PolyType(tv1, body1), PolyType(tv2, body2)) if tv1.length == tv2.length =>
      val maps = (tv1.zip(tv2).flatMap{
        case (Type.InfVar(_, uid1, _, _), Type.InfVar(_, uid2, _, _)) =>
          val nv = freshVar
          (uid1 -> nv) :: (uid2 -> nv) :: Nil
      }).toMap
      checkPoly(subst(body1)(using maps), subst(body2)(using maps))
    case (Type.Top, Type.Top) => true
    case (Type.Bot, Type.Bot) => true
    case _ =>
      false

  private def constrain(lhs: Type, rhs: Type)(using ctx: Ctx): Unit = solver.constrain(lhs, rhs)

  private def typeCode(code: Term)(using ctx: Ctx): (Type, Type) = code match
    case Lit(_) => (Type.Bot, Type.Bot)
    case Ref(sym: Symbol) if sym.nme == "error" => (Type.Bot, Type.Bot)
    case Lam(params, body) =>
      val nestCtx = ctx.nextLevel
      given Ctx = nestCtx
      val bds = params.map:
        case Param(_, sym, _) =>
          val sk = freshSkolem
          nestCtx &= sym -> sk
          sk
      val (bodyTy, eff) = typeCode(body)
      val res = freshVar(using ctx)
      val uni = Type.mkComposedType(bds.foldLeft[Type](Type.Bot)((res, bd) => Type.mkComposedType(res, bd, true)), res, true)
      constrain(bodyTy, uni)
      (res, eff)
    case Term.App(Ref(sym: TermSymbol), Term.Tup(rhs)) if Ctx.isOp(sym.nme) =>
      rhs.foldLeft[(Type, Type)]((Type.Bot, Type.Bot))((res, p) =>
        val (ty, eff) = typeCode(p.value)
        (Type.mkComposedType(res._1, ty, true), Type.mkComposedType(res._2, eff, true))
      )
    case Term.App(lhs, Term.Tup(rhs)) =>
      val (ty1, eff1) = typeCode(lhs)
      val (ty2, eff2) = rhs.foldLeft[(Type, Type)]((Type.Bot, Type.Bot))((res, p) =>
        val (ty, eff) = typeCode(p.value)
        (Type.mkComposedType(res._1, ty, true), Type.mkComposedType(res._2, eff, true))
      )
      (Type.mkComposedType(ty1, ty2, true), Type.mkComposedType(eff1, eff2, true))
    case Term.Unquoted(body) =>
      val (ty, eff) = typeCheck(body)
      val tv = freshVar
      constrain(ty.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), Ctx.codeTy(tv))
      (tv, eff)
    case Term.Blk(LetBinding(pat, rhs) :: Nil, body) => // TODO: more than one?
      val (rhsTy, rhsEff) = typeCode(rhs)(using ctx)
      val nestCtx = ctx.nextLevel
      given Ctx = nestCtx
      val bd = pat match
        case Pattern.Var(sym) =>
          val sk = freshSkolem
          nestCtx &= sym -> sk
          sk
        case _ => ???
      val (bodyTy, bodyEff) = typeCode(body)
      val res = freshVar(using ctx)
      constrain(bodyTy, Type.mkComposedType(bd, res, true))
      (Type.mkComposedType(rhsTy, res, true), Type.mkComposedType(rhsEff, bodyEff, true))
    case Term.If(Split.Cons(TermBranch.Boolean(cond, Split.Else(cons)), Split.Else(alts))) =>
      val (condTy, condEff) = typeCode(cond)
      val (consTy, consEff) = typeCode(cons)
      val (altsTy, altsEff) = typeCode(alts)
      (Type.mkComposedType(condTy, Type.mkComposedType(consTy, altsTy, true), true), Type.mkComposedType(condEff, Type.mkComposedType(consEff, altsEff, true), true))
    case _ =>
      (error(msg"Cannot quote ${code.toString}" -> code.toLoc :: Nil), Type.Bot)

  private def typeFunDef(sym: Symbol, lam: Term, sig: Opt[Term], t: Term, pctx: Ctx)(using ctx: Ctx) = lam match
    case Term.Lam(params, body) => sig match
      case S(sig) =>
        val sigTy = typeType(sig)(using ctx, true)
        pctx += sym -> sigTy
        ascribe(lam, sigTy)
        ()
      case N =>
        val funTy = freshVar
        pctx += sym -> funTy // for recursive types
        val (res, _) = typeCheck(lam)
        constrain(res.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), funTy)(using ctx)
    // case _ => ???

  private def typeSplit(split: TermSplit, sign: Opt[GeneralType])(using ctx: Ctx): (GeneralType, Type) = split match
    case Split.Cons(TermBranch.Boolean(cond, Split.Else(cons)), alts) =>
      val (condTy, condEff) = typeCheck(cond)
      val (consTy, consEff) = sign match
        case S(sign) => ascribe(cons, sign)
        case _=> typeCheck(cons)
      val (altsTy, altsEff) = typeSplit(alts, sign)
      val allEff = Type.mkComposedType(condEff, Type.mkComposedType(consEff, altsEff, true), true)
      constrain(condTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), Ctx.boolTy)
      (sign.getOrElse(Type.mkComposedType(consTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), altsTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), true)), allEff)
    case Split.Cons(TermBranch.Match(scrutinee, Split.Cons(PatternBranch(Pattern.Class(sym, _, _), cons), Split.NoSplit)), alts) =>
      val (clsTy, tv, emptyTy) = ctx.getDef(sym.nme) match
        case S(ClassDef.Parameterized(_, tparams, _, _, _)) =>
          (Type.ClassType(sym, tparams.map(_ => freshWildcard)), freshVar, Type.ClassType(sym, tparams.map(_ => Wildcard.empty)))
        case S(ClassDef.Plain(_, tparams, _, _)) =>
          (Type.ClassType(sym, tparams.map(_ => freshWildcard)), freshVar, Type.ClassType(sym, tparams.map(_ => Wildcard.empty)))
        case _ =>
          error(msg"Cannot match ${scrutinee.toString} as ${sym.toString}" -> split.toLoc :: Nil)
          (Type.Bot, Type.Bot, Type.Bot)
      val (scrutineeTy, scrutineeEff) = typeCheck(scrutinee)
      constrain(scrutineeTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), Type.mkComposedType(clsTy, Type.mkComposedType(tv, Type.mkNegType(emptyTy), false), true))
      val nestCtx1 = ctx.nest
      val nestCtx2 = ctx.nest
      scrutinee match // * refine
        case Ref(sym: VarSymbol) =>
          nestCtx1 += sym -> clsTy
          nestCtx2 += sym -> tv
        case _ => ()
      val (consTy, consEff) = typeSplit(cons, sign)(using nestCtx1)
      val (altsTy, altsEff) = typeSplit(alts, sign)(using nestCtx2)
      val allEff = Type.mkComposedType(scrutineeEff, Type.mkComposedType(consEff, altsEff, true), true)
      (sign.getOrElse(Type.mkComposedType(consTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), altsTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), true)), allEff)
    case Split.Else(alts) => sign match
      case S(sign) => ascribe(alts, sign)
      case _=> typeCheck(alts)

  private def ascribe(lhs: Term, rhs: GeneralType)(using ctx: Ctx): (GeneralType, Type) = (lhs, rhs) match
    case (Term.Lam(params, body), ft @ Type.FunType(args, ret, eff)) => // * annoted functions
      if params.length != args.length then
         (error(msg"Cannot type function ${lhs.toString} as ${rhs.toString}" -> lhs.toLoc :: Nil), Type.Bot)
      else
        val nestCtx = ctx.nest
        val argsTy = params.zip(args).map:
          case (Param(_, sym, _), ty) =>
            nestCtx += sym -> ty
            ty
        given Ctx = nestCtx
        val (bodyTy, effTy) = typeCheck(body)
        if ret.isPoly && !checkPoly(bodyTy, ret) then
          (error(msg"Cannot type function ${lhs.toString} as ${rhs.toString}" -> lhs.toLoc :: Nil), Type.Bot)
        else
          constrain(effTy, eff)
          if !ret.isPoly then constrain(bodyTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), ret)
          (ft, Type.Bot)
    case (Term.Lam(params, body), ft @ PolyFunType(args, ret, eff)) => // * annoted functions
      if params.length != args.length then
         (error(msg"Cannot type function ${lhs.toString} as ${rhs.toString}" -> lhs.toLoc :: Nil), Type.Bot)
      else
        val nestCtx = ctx.nest
        val argsTy = params.zip(args).map:
          case (Param(_, sym, _), ty) =>
            nestCtx += sym -> ty
            ty
        given Ctx = nestCtx
        val (bodyTy, effTy) = typeCheck(body)
        if ret.isPoly && !checkPoly(bodyTy, ret) then
          (error(msg"Cannot type function ${lhs.toString} as ${rhs.toString}" -> lhs.toLoc :: Nil), Type.Bot)
        else
          constrain(effTy, eff)
          if !ret.isPoly then constrain(bodyTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), ret.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)))
          (ft, Type.Bot)
    case (Term.Blk(LetBinding(Pattern.Var(sym), rhs) :: Nil, body), ty) => // * propagate
      val nestCtx = ctx.nest
      given Ctx = nestCtx
      val (rhsTy, eff) = typeCheck(rhs)
      nestCtx += sym -> rhsTy
      val (resTy, resEff) = ascribe(body, ty)
      (resTy, Type.mkComposedType(eff, resEff, true))
    case (Term.If(branches), ty) => // * propagate
      typeSplit(branches, S(ty))
    case (term, pt @ PolyType(tvs, body)) => // * generalize
      val nestCtx = ctx.nextLevel
      given Ctx = nestCtx
      constrain(ascribe(term, skolemize(tvs, body)(using false))._2.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), Type.Bot) // * never generalize terms with effects
      (pt, Type.Bot)
    case (term, ft: Type.FunType) if ft.isPoly =>
      val (ty, eff) = typeCheck(term)
      if !checkPoly(ty, ft) then
        (error(msg"Cannot type function ${lhs.toString} as ${rhs.toString}" -> lhs.toLoc :: Nil), Type.Bot)
      else
        (ty, eff)
    case _ =>
      val (lhsTy, eff) = typeCheck(lhs)
      (lhsTy, rhs) match
        case (lhs: PolyType, rhs: PolyType) => ???
        case (lhs: PolyType, rhs) => constrain(instantiate(lhs).monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), rhs.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)))
        case (lhs, rhs: PolyType) => ???
        case _ =>
          constrain(lhsTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), rhs.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)))
      (rhs, eff)

  // TODO: t -> loc when toLoc is implemented
  private def app(lhs: (GeneralType, Type), rhs: Ls[Fld], t: Term)(using ctx: Ctx): (GeneralType, Type) = lhs match
    case (Type.FunType(args, ret, eff), lhsEff) => // * if the function type is known, we can directly use it
      if args.length != rhs.length then
        (error(msg"The number of parameters is incorrect" -> t.toLoc :: Nil), Type.Bot)
      else
        val (argTy, argEff) = rhs.zip(args).flatMap{
          case (f, t) =>
            val (ty, eff) = ascribe(f.value, t)
            Left(ty) :: Right(eff) :: Nil
          }.partitionMap(x => x)
        (ret, argEff.foldLeft[Type](Type.mkComposedType(eff, lhsEff, true))((res, e) => Type.mkComposedType(res, e, true)))
    case (PolyFunType(args, ret, eff), lhsEff) => // * if the function type is known, we can directly use it
      if args.length != rhs.length then
        (error(msg"The number of parameters is incorrect" -> t.toLoc :: Nil), Type.Bot)
      else
        val (argTy, argEff) = rhs.zip(args).flatMap{
          case (f, t) =>
            val (ty, eff) = ascribe(f.value, t)
            Left(ty) :: Right(eff) :: Nil
          }.partitionMap(x => x)
        (ret, argEff.foldLeft[Type](Type.mkComposedType(eff, lhsEff, true))((res, e) => Type.mkComposedType(res, e, true)))
    case (funTy, lhsEff) =>
      val (argTy, argEff) = rhs.flatMap(f =>
        val (ty, eff) = typeCheck(f.value)
        Left(ty) :: Right(eff) :: Nil
      ).partitionMap(x => x)
      val effVar = freshVar
      val retVar = freshVar
      constrain(funTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), Type.FunType(argTy.map {
        case pt: PolyType => instantiate(pt).monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil))
        case ty: Type => ty
        case pf: PolyFunType => pf.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil))
      }, retVar, effVar))
      (retVar, argEff.foldLeft[Type](Type.mkComposedType(effVar, lhsEff, true))((res, e) => Type.mkComposedType(res, e, true)))

  private def skolemize(tv: Ls[Type.InfVar], body: GeneralType)(using inv: Bool) =
    val bds = tv.map(_.uid).toSet
    def rec(ty: GeneralType): GeneralType = ty match
      case Type.ClassType(name, targs) => Type.ClassType(name, targs.map(_.map(a => rec(a).monoOr(???))))
      case v @ Type.InfVar(vlvl, uid, state, isSkolem) =>
        if bds(uid) then Type.InfVar(vlvl, uid, state, !inv)
        else v
      case Type.FunType(args, ret, eff) => Type.FunType(args.map(a => rec(a).monoOr(???)), rec(ret).monoOr(???), rec(eff).monoOr(???))
      case Type.ComposedType(lhs, rhs, pol) => Type.ComposedType(rec(lhs).monoOr(???), rec(rhs).monoOr(???), pol)
      case Type.NegType(ty) => Type.NegType(rec(ty).monoOr(???))
      case Type.Top | Type.Bot => ty
      case PolyType(tv, body) => PolyType(tv, rec(body))
      case PolyFunType(args, ret, eff) => PolyFunType(args.map(rec), rec(ret), rec(eff).monoOr(???))
    rec(body)

  private def typeCheck(t: Term)(using ctx: Ctx): (GeneralType, Type) = trace[(GeneralType, Type)](s"Typing ${t.showDbg}", res => s": $res"):
    t match
    case Ref(sym: VarSymbol) =>
      ctx.get(sym) match
        case Some(ty) => (ty, Type.Bot)
        case _ =>
          (error(msg"Variable not found: ${sym.name}" -> t.toLoc :: Nil), Type.Bot)
    case Ref(sym: TermSymbol) =>
      ctx.get(sym) match
        case Some(ty) => (ty, Type.Bot)
        case _ =>
          (error(msg"Definition not found: ${sym.nme}" -> t.toLoc :: Nil), Type.Bot)
    case Blk(stats, res) =>
      val nestCtx = ctx.nest
      given Ctx = nestCtx
      val effBuff = ListBuffer.empty[Type]
      stats.foreach:
        case term: Term => typeCheck(term)
        case LetBinding(Pattern.Var(sym), rhs) =>
          val (rhsTy, eff) = typeCheck(rhs)
          effBuff += eff
          nestCtx += sym -> rhsTy
        case TermDefinition(Fun, sym, params, sig, Some(body), _) =>
          typeFunDef(sym, params match {
            case S(params) => Term.Lam(params, body)
            case _ => body // * via case expressions
          }, sig, t, ctx)
        case clsDef: ClassDef => ctx *= clsDef
        case _ => () // TODO
      val (ty, eff) = typeCheck(res)
      (ty, effBuff.foldLeft(eff)((res, e) => Type.mkComposedType(res, e, true)))
    case Lit(lit) => ((lit match
      case _: IntLit => Ctx.intTy
      case _: DecLit => Ctx.numTy
      case _: StrLit => Ctx.strTy
      case _: UnitLit => Type.Top
      case _: BoolLit => Ctx.boolTy), Type.Bot)
    case Lam(params, body) =>
      val nestCtx = ctx.nest
      given Ctx = nestCtx
      val tvs = params.map:
        case Param(_, sym, sign) =>
          val ty = sign.map(s => typeType(s)(using nestCtx, true)).getOrElse(freshVar)
          nestCtx += sym -> ty
          ty
      val (bodyTy, eff) = typeCheck(body)
      (PolyFunType(tvs, bodyTy, eff), Type.Bot)
    case Term.SelProj(term, Term.Ref(cls: ClassSymbol), field) =>
      val (ty, eff) = typeCheck(term)
      ctx.getDef(cls.nme) match
        case S(ClassDef.Parameterized(_, tparams, params, _, _)) =>
          val map = HashMap[Uid[Symbol], Wildcard]()
          val targs = tparams.map {
            case TyParam(_, targ) =>
              val ty = freshWildcard
                map += targ.uid -> ty
                ty
          }
          constrain(ty.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), Type.ClassType(cls, targs))
          (params.map {
            case Param(_, sym, sign) =>
              if sym.nme == field.name then sign else N
          }.filter(_.isDefined)) match
            case S(res) :: Nil => (extract(res)(using map.toMap, true), eff)
            case _ => (error(msg"${field.name} is not a valid member in class ${cls.nme}" -> t.toLoc :: Nil), Type.Bot)
        case S(ClassDef.Plain(_, tparams, _, _)) =>
          ???
        case N => 
          (error(msg"Definition not found: ${cls.nme}" -> t.toLoc :: Nil), Type.Bot)
    case Term.App(lhs, Term.Tup(rhs)) => typeCheck(lhs) match
      case (pt: PolyType, lhsEff) =>
        app((instantiate(pt), lhsEff), rhs, t)
      case (funTy, lhsEff) => app((funTy, lhsEff), rhs, t)
    case Term.New(cls, args) =>
      ctx.getDef(cls.nme) match
        case S(ClassDef.Parameterized(_, tparams, params, _, _)) =>
          if args.length != params.length then
            (error(msg"The number of parameters is incorrect" -> t.toLoc :: Nil), Type.Bot)
          else
            val map = HashMap[Uid[Symbol], Wildcard]()
            val targs = tparams.map {
              case TyParam(_, targ) =>
                val ty = freshWildcard
                map += targ.uid -> ty
                ty
            }
            val effBuff = ListBuffer.empty[Type]
            args.iterator.zip(params).foreach {
              case (arg, Param(_, _, S(sign))) =>
                val (ty, eff) = ascribe(arg, extract(sign)(using map.toMap, true))
                effBuff += eff
              case _ => ???
            }
            (Type.ClassType(cls, targs), effBuff.foldLeft[Type](Type.Bot)((res, e) => Type.mkComposedType(res, e, true)))
        case S(ClassDef.Plain(_, tparams, _, _)) =>
          ???
        case N => 
          (error(msg"Definition not found: ${cls.nme}" -> t.toLoc :: Nil), Type.Bot)
    case Term.Asc(term, ty) =>
      val res = typeType(ty)(using ctx, true)
      ascribe(term, res)
    case Term.Forall(tvs, body) => // * e.g. [A] => (x: A) => ...
      val nestCtx = ctx.nextLevel
      val bds = tvs.map:
        case sym: VarSymbol =>
          val tv = freshSkolem(using nestCtx)
          nestCtx += sym -> tv // TODO: a type var symbol may be better...
          tv
      val (bodyTy, eff) = typeCheck(body)(using nestCtx)
      constrain(eff, Type.Bot) // * never generalize terms with effects
      (PolyType(bds.map {
        case Type.InfVar(lvl, uid, st, _) => Type.InfVar(lvl, uid, st, false)
      }, skolemize(bds, bodyTy)(using true)), Type.Bot)
    case Term.If(branches) => typeSplit(branches, N)
    case Term.Region(sym, body) =>
      val nestCtx = ctx.nextLevel
      given Ctx = nestCtx
      val sk = freshSkolem
      nestCtx += sym -> Ctx.regionTy(sk)
      val (res, eff) = typeCheck(body)
      val tv = freshVar(using ctx)
      constrain(eff, Type.mkComposedType(tv, sk, true))
      (res, Type.mkComposedType(tv, allocSkolem, true))
    case Term.RegRef(reg, value) =>
      val (regTy, regEff) = typeCheck(reg)
      val (valTy, valEff) = typeCheck(value)
      val sk = freshVar
      constrain(regTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), Ctx.regionTy(sk))
      (Ctx.refTy(valTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), sk), Type.mkComposedType(sk, Type.mkComposedType(regEff, valEff, true), true))
    case Term.Set(lhs, rhs) =>
      val (lhsTy, lhsEff) = typeCheck(lhs)
      val (rhsTy, rhsEff) = typeCheck(rhs)
      val sk = freshVar
      constrain(lhsTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), Ctx.refTy(rhsTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), sk))
      (rhsTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), Type.mkComposedType(sk, Type.mkComposedType(lhsEff, rhsEff, true), true))
    case Term.Deref(ref) =>
      val (refTy, refEff) = typeCheck(ref)
      val sk = freshVar
      val ctnt = freshVar
      constrain(refTy.monoOr(error(msg"Polymorphic type is not allowed here." -> N :: Nil)), Ctx.refTy(ctnt, sk))
      (ctnt, Type.mkComposedType(sk, refEff, true))
    case Term.Quoted(body) =>
      val nestCtx = ctx.nextLevel
      given Ctx = nestCtx
      val (ctxTy, eff) = typeCode(body)
      (Ctx.codeTy(ctxTy), eff)
    case _: Term.Unquoted =>
      (error(msg"Unquote should nest in quasiquote" -> t.toLoc :: Nil), Type.Bot)
    case Term.Error =>
      (Type.Bot, Type.Bot) // TODO: error type?

  def typePurely(t: Term): GeneralType =
    val (ty, eff) = typeCheck(t)(using initCtx)
    constrain(eff, allocSkolem)(using initCtx)
    ty

