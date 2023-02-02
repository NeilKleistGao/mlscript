package mlscript.codegen.ast

import mlscript.codegen.ast._
import mlscript.codegen.{Position => SourcePosition, Location => SourceLocation}

case class TaggedTemplateExpression(val tag: Node with Expression, val quasi: TemplateLiteral)(val start: Option[Int], val end: Option[Int], val location: Option[SourceLocation])
  extends Node with Standardized with Expression:
  var typeParameters: Option[TSTypeParameterInstantiation] = None

case class TemplateElement(value: String, tail: Boolean = false)(val start: Option[Int], val end: Option[Int], val location: Option[SourceLocation])
  extends Node with Standardized

case class TemplateLiteral(
  quasis: List[TemplateElement],
  expressions: List[Node with Expression | Node with TSType]
)(val start: Option[Int], val end: Option[Int], val location: Option[SourceLocation]) extends Node with Standardized with Expression with Literal
