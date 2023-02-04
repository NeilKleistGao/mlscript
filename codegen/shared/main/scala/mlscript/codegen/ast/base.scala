package mlscript.codegen.ast

import mlscript.codegen.ast._
import mlscript.codegen.{Position => SourcePosition, Location => SourceLocation}

enum SourceType:
  case Script
  case Module

case class Program(
  body: List[Node with Statement],
  sourceType: SourceType = SourceType.Script,
  sourceFile: String
)(val start: Option[Int], val end: Option[Int], val location: Option[SourceLocation]) extends Node with Standardized with Scopable with BlockParent with Block

case class File(
  program: Program
)(val start: Option[Int], val end: Option[Int], val location: Option[SourceLocation]) extends Node with Standardized

case class BlockStatement(
  body: List[Node with Statement]
)(val start: Option[Int], val end: Option[Int], val location: Option[SourceLocation]) extends Node with Standardized with Scopable with BlockParent with Block with Statement

