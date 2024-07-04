package hkmc2.bbml

import mlscript.utils.*, shorthands.*

import Type.*

class TypeTraverser:
  def apply(pol: Bool)(ty: Type): Unit = ty match
    case Top | Bot =>
    case NegType(ty) => apply(!pol)(ty)
    case FunType(args, ret, eff) =>
      args.foreach(apply(!pol))
      apply(pol)(ret)
      apply(pol)(eff)
    case ClassType(name, targs) =>
      targs.foreach:
        case Wildcard(in, out) =>
          apply(!pol)(in)
          apply(pol)(out)
        case ty: Type =>
          apply(pol)(ty)
          apply(!pol)(ty)
    case InfVar(vlvl, uid, state) =>
      if pol then state.lowerBounds.foreach(apply(true))
      else state.upperBounds.foreach(apply(false))
    case ComposedType(lhs, rhs, _) =>
      apply(pol)(lhs)
      apply(pol)(rhs)
    case PolymorphicType(tv, body) =>
      apply(pol)(body)
  
