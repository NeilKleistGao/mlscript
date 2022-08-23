package ts2mls.types

object Converter {
  private val primitiveName = Map[String, String](
    "boolean" -> "bool",
    "number" -> "number",
    "string" -> "string",
    "any" -> "anything",
    "unknown" -> "anything",
    "void" -> "unit",
    "null" -> "null",
    "undefined" -> "undefined",
    "never" -> "nothing",
    "object" -> "{}"
  )

  def convert(tsType: TSType): String = tsType match {
    case TSNamedType(typeName) => primitiveName.getOrElse(typeName, typeName)
    case TSFunctionType(params, res, constraint) => {
      val func = 
        if (params.length == 0) s"${primitiveName("void")} -> (${convert(res)})"
        else params.foldRight(convert(res))((tst, mlst) => s"(${convert(tst)}) -> (${mlst})")
      func
    }
    case TSUnionType(lhs, rhs) => s"(${convert(lhs)}) | (${convert(rhs)})"
    case TSIntersectionType(lhs, rhs) => s"(${convert(lhs)}) & (${convert(rhs)})"
    case TSTypeVariable(name, _) => name
    case TSTupleType(lst) => s"(${lst.foldLeft("")((p, t) => s"$p${convert(t)}, ")})"
    case TSArrayType(element) => s"MutArray[${convert(element)}]"
    case TSEnumType(_) => "int"
    case TSMemberType(base, modifier) => convert(base)
    case TSInterfaceType(name, members, typeVars, parents) => convertRecord(s"trait $name", members, typeVars, parents)
    case TSClassType(name, members, _, typeVars, parents) => convertRecord(s"class $name", members, typeVars, parents)
    case TSApplicationType(base, applied) => s"${base}[${applied.map((app) => convert(app)).reduceLeft((res, s) => s"$res, $s")}]"
    case _ => throw new Exception("Unknown TypeScript Type")
  }

  private def convertRecord(typeName: String, members: Map[String, TSMemberType], typeVars: List[TSTypeVariable], parents: List[TSType]) = {
    val allRecs = members.toList.map((m) => m._2.modifier match {
      case Public => {
        m._2.base match {
          case TSFunctionType(_, _, typeVars) if (typeVars.length > 0) =>
            s"  method ${m._1}[${typeVars.map((tv) => tv.name).reduceLeft((p, s) => s"$p, $s")}]: ${convert(m._2)}" // TODO: add constraints
          case inter: TSIntersectionType => {
            val lst = TSIntersectionType.getOverloadTypeVariables(inter)
            if (lst.isEmpty) s"${m._1}: ${convert(m._2)}"
            else
              s"  method ${m._1}[${lst.map((tv) => tv.name).reduceLeft((p, s) => s"$p, $s") }]: ${convert(m._2)}" // TODO: add constraints
          }
          case _ => s"${m._1}: ${convert(m._2)}"
        }
      }
      case _ => ""
    })

    val body = {
      val lst = allRecs.filter((s) => !s.startsWith("  ") && !s.equals(""))
      if (lst.isEmpty) "{}"
      else s"{ ${lst.reduceLeft((bd, m) => s"$bd; $m")} }"
    }
    val methods = {
      val lst = allRecs.filter(_.startsWith("  "))
      if (lst.isEmpty) ""
      else "\n" + lst.reduceLeft((bd, m) => s"$bd\n$m")
    }
    
    if (typeName.equals("trait ")) body
    else {
      val bodyWithParents = parents.foldLeft(body)((b, p) => s"$b & ${convert(p)}")
      if (typeVars.length == 0) s"$typeName: $bodyWithParents$methods"
      else
        s"$typeName[${typeVars.map((tv) => tv.name).reduceLeft((p, s) => s"$p, $s")}]: $bodyWithParents$methods" // TODO: add constraints
    }
  }
}