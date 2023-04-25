package ts2mls

import scala.collection.mutable.{HashMap, ListBuffer}
import types._
import mlscript.utils._

class TSNamespace(name: String, parent: Option[TSNamespace]) {
  // name -> (namespace/type, export)
  private val subSpace = HashMap[String, (TSNamespace, Boolean)]()
  private val members = HashMap[String, (TSType, Boolean)]()

  // write down the order of members
  // easier to check the output one by one
  private val order = ListBuffer.empty[Either[String, String]]

  def derive(name: String, exported: Boolean): TSNamespace =
    if (subSpace.contains(name)) subSpace(name)._1 // if the namespace has appeared in another file, just return it
    else {
      val sub = new TSNamespace(name, Some(this))
      subSpace.put(name, (sub, exported))
      order += Left(name)
      sub
    }

  def put(name: String, tp: TSType, exported: Boolean): Unit =
    if (!members.contains(name)) {
      order += Right(name)
      members.put(name, (tp, exported))
    }
    else members.update(name, (tp, exported))

  def get(name: String): TSType =
    if (members.contains(name)) members(name)._1
    else if (!parent.isEmpty) parent.get.get(name)
    else throw new Exception(s"member $name not found.")

  def containsMember(name: String, searchParent: Boolean = true): Boolean =
    if (parent.isEmpty) members.contains(name) else (members.contains(name) || (searchParent && parent.get.containsMember(name)))

  def generate(writer: JSWriter, indent: String): Unit =
    order.toList.foreach((p) => p match {
      case Left(subName) => {
        val ss = subSpace(subName)
        val exp = if (ss._2) "export " else ""
        writer.writeln(s"${indent}${exp}declare module $subName {")
        ss._1.generate(writer, indent + "  ")
        writer.writeln(s"$indent}")
      }
      case Right(name) => {
        val (mem, exp) = members(name)
        mem match {
          case inter: TSIntersectionType => // overloaded functions
            writer.writeln(Converter.generateFunDeclaration(inter, name, exp)(indent))
          case f: TSFunctionType =>
            writer.writeln(Converter.generateFunDeclaration(f, name, exp)(indent))
          case overload: TSIgnoredOverload =>
            writer.writeln(Converter.generateFunDeclaration(overload, name, exp)(indent))
          case _: TSClassType => writer.writeln(Converter.convert(mem, exp)(indent))
          case TSInterfaceType(name, _, _, _) if (name =/= "") =>
            writer.writeln(Converter.convert(mem, exp)(indent))
          case _: TSTypeAlias => writer.writeln(Converter.convert(mem, exp)(indent))
          case _ =>
            if (exp) writer.writeln(s"${indent}export let $name: ${Converter.convert(mem)("")}")
            else writer.writeln(s"${indent}let $name: ${Converter.convert(mem)("")}")
        }
      }
    })
}

object TSNamespace {
  def apply() = new TSNamespace("", None) // global namespace
}
