package mlscript.codegen

package object generator:
  // type SourcePos = {
  //   val identifierName: Option[String]
  //   val line: Option[Int]
  //   val column: Option[Int]
  //   val filename: Option[String]
  // }
  case class SourcePosition(
    val identifierName: Option[String],
    val line: Option[Int],
    val column: Option[Int],
    val fileName: Option[String]
  )

  object SourcePosition:
    def apply(): SourcePosition = SourcePosition(None, None, None, None)

  enum CommentSkipNewLine:
    case Default
    case All
    case Leading
    case Trailing

  enum PrintCommentHint:
    case Skip
    case Allow
    case Defer
