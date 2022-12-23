package mlscript.codegen.generator

import scala.util.matching.Regex
import scala.collection.mutable.{ArrayBuffer, HashSet}
import mlscript.codegen.babel._
import mlscript.codegen.{Position, Location, LocationType}

class NewLineState(var printed: Boolean)

case class AddNewlineOptions(
  val nextNodeStartLine: Int,
  val addNewlines: (Boolean, BaseNode) => Int
)

case class PrintSequenceOptions(
  val statement: Option[Boolean],
  val indent: Option[Boolean],
  val trailingCommentsLineOffset: Option[Int],
  val nextNodeStartLine: Option[Int],
  val addNewlines: Option[(Boolean, BaseNode)=>Int]
)

case class PrintListOptions(
  val separator: Option[(Printer) => Unit],
  val iterator: Option[(BaseNode, Int) => Unit],
  val statement: Option[Boolean],
  val indent: Option[Boolean]
)

class Printer(format: Format, map: SourceMapBuilder) {
  private val PURE_ANNOTATION_RE = "^\\s*[@#]__PURE__\\s*$".r
  private val ZERO_DECIMAL_INTEGER = "\\.0+$".r
  private val NON_DECIMAL_LITERAL = "^0[box]".r
  private val HAS_NEWLINE = "[\n\r\u2028\u2029]".r
  private val HAS_BlOCK_COMMENT_END = "\\*\\/".r
  private val SCIENTIFIC_NOTATION = "e\\d".r

  private var indentLevel: Int = 0
  private val buf = new Buffer(Some(map))
  private var _noLineTerminator = false
  private var _endsWithWord = false
  private var _endWithInteger = false
  private var _indentChar = '\u0000'
  private var _indentRepeat = 0
  private var _endsWithInteger = false
  private var _endWithInnerRaw = false
  private var _indentInnerComments = true
  private var _parenPushNewlineState: Option[NewLineState] = None
  private var _printAuxAfterOnNextUserNode = false
  private var _printStack = new ArrayBuffer[BaseNode]()
  private var _printedComments = new HashSet[BaseComment]()
  private var _insideAux = false
  private var _lastCommentLine = 0

  def generate(ast: BaseNode): BufferOutput = {
    print(Some(ast))
    _maybeAddAuxComment()

    buf.get()
  }

  def indent(): Unit =
    if (!format.compact && !format.concise) indentLevel += 1

  def dedent(): Unit =
    if (!format.compact && !format.concise) indentLevel -= 1

  def semicolon(force: Boolean = false): Unit = {
    _maybeAddAuxComment()
    if (force) _appendChar(';')
    else _queue(';')

    _noLineTerminator = false
  }

  def rightBrace: Unit = {
    if (format.minified) buf.removeLastSemicolon()
    token("}")
  }

  def space(force: Boolean = false): Unit =
    if (!format.compact) {
      if (force) _space()
      else if (buf.hasContent) {
        val lastCp = getLastChar()
        if (lastCp != ' ' && lastCp != '\n')
          _space()
      }
    }

  def word(str: String, noLineTerminatorAfter: Boolean = false): Unit = {
    _maybePrintInnerComments()
    if (_endsWithWord || (str.charAt(0) == '/' && endsWith('/')))
      space()

    _maybeAddAuxComment()
    _append(str, false)

    _endsWithWord = true
    _noLineTerminator = noLineTerminatorAfter
  }

  def number(str: String): Unit = {
    word(str)

    _endsWithInteger =
      (try { java.lang.Double.parseDouble(str); true } catch {case _ => false}) &&
      (NON_DECIMAL_LITERAL findFirstIn str).isEmpty &&
      (SCIENTIFIC_NOTATION findFirstIn str).isEmpty &&
      (ZERO_DECIMAL_INTEGER findFirstIn str).isEmpty &&
      str.last != '.'
  }

  def token(str: String, maybeNewline: Boolean = false): Unit = {
    _maybePrintInnerComments()

    val lastChar = getLastChar()
    val strFirst = str.charAt(0)
    
    if ((lastChar == '!' && str == "--") ||
        (strFirst == '+' && lastChar == '+') ||
        (strFirst == '-' && lastChar == '-') ||
        (strFirst == '.' && _endWithInteger))
      _space()

    _maybeAddAuxComment()
    _append(str, maybeNewline)
    _noLineTerminator = false
  }

  def tokenChar(char: Char): Unit = {
    _maybePrintInnerComments()
    val lastChar = getLastChar()

    if ((char == '+' && lastChar == '+') ||
        (char == '-' && lastChar == '-') ||
        (char == '.' && lastChar == '.'))
      _space()

    _maybeAddAuxComment()
    _appendChar(char)
    _noLineTerminator = false
  }

  def newline(i: Int = 1, force: Boolean = false): Unit =
    if (i > 0) {
      if (!force && !format.retainLines && !format.compact && format.concise)
        space()
      else if (force) {
        for (ii <- 0 until (if (i > 2) 2 else i) - buf.getNewlineCount)
          _newline()
      }
    }

  def endsWith(char: Char): Boolean =
    getLastChar() == char

  def getLastChar(): Char = buf.getLastChar

  def endsWithCharAndNewline: Option[Char] =
    buf.endsWithCharAndNewline

  def removeTrailingNewline: Unit = 
    buf.removeTrailingNewline()

  def exactSource(loc: Option[Location], node: BaseNode, parent: BaseNode): Unit = {
    if (loc.isEmpty) node.print(this, parent)
    else {
      _catchUp(LocationType.Start, loc)
      buf.exactSource(loc, node, parent, this)
    }
  }

  def source(prop: LocationType, loc: Option[Location]): Unit =
    if (!loc.isEmpty) {
      _catchUp(prop, loc)
      buf.source(prop, loc)
    }

  def sourceWithOffset(
    prop: LocationType, 
    loc: Option[Location],
    lineOffset: Int,
    columnOffset: Int
  ): Unit = if (!loc.isEmpty) {
    _catchUp(prop, loc)
    buf.sourceWithOffset(prop, loc, lineOffset, columnOffset)
  }

  def withSource(
    prop: LocationType, 
    loc: Option[Location],
    node: BaseNode,
    parent: BaseNode
  ): Unit = if (loc.isEmpty) node.print(this, parent)
      else {
        _catchUp(prop, loc)
        buf.withSource(prop, loc, node, parent, this)
      }

  private def _space() = _queue(' ')
  private def _newline() = _queue('\n')

  private def _append(str: String, maybeNewline: Boolean): Unit = {
    _maybeAddParen(str)
    _maybeIndent(str.charAt(0))

    buf.append(str, maybeNewline)

    _endsWithWord = false
    _endsWithInteger = false
  }

  private def _appendChar(char: Char): Unit = {
    _maybeAddParenChar(char)
    _maybeIndent(char)

    buf.appendChar(char)

    _endsWithWord = false
    _endWithInteger = false
  }

  private def _queue(char: Char): Unit = {
    _maybeAddParenChar(char)
    _maybeIndent(char)

    buf.queue(char)

    _endsWithWord = false
    _endWithInteger = false
  }

  private def _maybeIndent(firstChar: Char): Unit =
    if (indentLevel > 0 &&
        firstChar != '\n' &&
        endsWith('\n'))
      buf.queueIndentation(_indentChar, _getIndent)

  private def _shouldIndent(firstChar: Char) =
    indentLevel > 0 && firstChar != '\n' && endsWith('\n')

  private def _maybeAddParenChar(char: Char): Unit =
    if (!_parenPushNewlineState.isEmpty && char != ' ') {
      if (char != '\n')
        _parenPushNewlineState = None
      else {
        token("(")
        indent()
        _parenPushNewlineState.get.printed = true
      }
    }

  private def _maybeAddParen(str: String): Unit =
    if (!_parenPushNewlineState.isEmpty) {
      val len = str.length()
      val noSpaceIndex = str.indexWhere((c) => c != ' ')
      if (noSpaceIndex < len) {
        val char = str.charAt(noSpaceIndex)
        if (char == '\n') {
          if (char != '/' || noSpaceIndex + 1 == len)
            _parenPushNewlineState = None
          else {
            val charPost = str.charAt(noSpaceIndex + 1)
            if (charPost == '*') {
              (PURE_ANNOTATION_RE findFirstIn str.slice(noSpaceIndex + 2, len - 2)) match {
                case None => {
                  token("(")
                  indent()
                  _parenPushNewlineState.get.printed = true
                }
                case _ => ()
              }
            }
            else if (charPost != '/') {
              _parenPushNewlineState = None
            }
            else {
              token("(")
              indent()
              _parenPushNewlineState.get.printed = true
            }
          }
        }
        else {
          token("(")
          indent()
          _parenPushNewlineState.get.printed = true
        }
      }
    }

  def catchUp(line: Int): Unit =
    if (format.retainLines && line > buf.getCurrentLine) {
      _newline()
      catchUp(line)
    }

  private def _catchUp(prop: LocationType, optLoc: Option[Location]): Unit =
    if (format.retainLines)
      optLoc match
        case Some(loc) => 
          val pos = loc(prop)
          for (count <- 0 until (pos.get.line - buf.getCurrentLine)) {
            _newline()
          }
        case None => ()

  private def _getIndent: Int =
    _indentRepeat * indentLevel

  def printTerminatorless(node: BaseNode, parent: BaseNode, isLabel: Boolean): Unit =
    if (isLabel) {
      _noLineTerminator = true
      print(Some(node), Some(parent))
    }
    else {
      val terminatorState = new NewLineState(false)
      _parenPushNewlineState = Some(terminatorState)
      print(Some(node), Some(parent))

      if (terminatorState.printed) {
        dedent()
        newline()
        token(")")
      }
    }

  def print(
    node: Option[BaseNode],
    parent: Option[BaseNode] = None,
    noLineTerminatorAfter: Boolean = false,
    trailingCommentsLineOffset: Int = 0,
    forceParens: Boolean = false
  ): Unit = if (!node.isEmpty) {
    _endWithInnerRaw = false
    val oldConcise = format.concise
    if (node.get.compact) format.concise = true

    _printStack = _printStack :+ (node.get)
    val oldAux = _insideAux
    _insideAux = node.get.loc.isEmpty
    _maybeAddAuxComment(oldAux && _insideAux)

    val shouldPrintParens: Boolean =
      if (forceParens) true
      else if (format.retainFunctionParens)
        node.get match {
          case exp: BaseNode // TODO: FunctionExpression
            if (!exp.extra.isEmpty && exp.extra.get.contains("parenthesized")) => true 
          case _ => false
        }
      else Printer.needsParens(node, parent, Some(_printStack.toArray))

    if (shouldPrintParens) {
      token("(")
      _endWithInnerRaw = false
    }

    _lastCommentLine = 0
    _printLeadingComments(node.get, parent)

    val loc: Option[Location] = node.get match {
      case program: BaseNode => None // TODO: nodeType === "Program"
      case file: BaseNode => None // TODO: nodeType === "File"
      case _ => node.get.loc
    }

    exactSource(loc, node.get, parent.get)

    if (shouldPrintParens) {
      _printTrailingComments(node.get, parent)
      token(")")
      _noLineTerminator = noLineTerminatorAfter
    }
    else if (noLineTerminatorAfter && !_noLineTerminator) {
      _noLineTerminator = true
      _printTrailingComments(node.get, parent)
    }
    else {
      _printTrailingComments(node.get, parent, trailingCommentsLineOffset)
    }

    _printStack.dropRightInPlace(1)
    format.concise = oldConcise
    _insideAux = oldAux
    _endWithInnerRaw = false
  }

  private def _maybeAddAuxComment(enteredPositionlessNode: Boolean = false): Unit =
    if (enteredPositionlessNode) _printAuxBeforeComment()
    else _printAuxAfterComment()

  private def _printAuxBeforeComment(): Unit =
    if (!_printAuxAfterOnNextUserNode) {
      _printAuxAfterOnNextUserNode = true
      if (!format.auxiliaryCommentBefore.isEmpty) {
        _printComment(new CommentBlock(format.auxiliaryCommentBefore), CommentSkipNewLine.Default)
      }
    }

  private def _printAuxAfterComment(): Unit =
    if (!_printAuxAfterOnNextUserNode) {
      _printAuxAfterOnNextUserNode = true
      if (!format.auxiliaryCommentAfter.isEmpty) {
        _printComment(new CommentBlock(format.auxiliaryCommentAfter), CommentSkipNewLine.Default)
      }
    }

  // TODO: Exact node type
  def getPossibleRaw(node: BaseNode): Option[String] =
    if (!node.extra.isEmpty &&
        node.extra.get.contains("raw") &&
        node.extra.get.contains("rawValue")) node.extra.get.get("raw") // TODO: node.value === extra.rawValue
    else None

  def printJoin(nodes: Option[Array[BaseNode]], parent: BaseNode, opts: Printer.PrintJoinOptions): Unit =
    if (!nodes.isEmpty && nodes.get.length > 0) {
      if (!opts.indent.isEmpty) indent()

      val separator = if (opts.separator.isEmpty) None else () => opts.separator.get(this)

      nodes.get.zipWithIndex.foreach((node, i) => {
        // TODO: check is node is empty

        if (opts.statement.getOrElse(false))
          _printNewline(i == 0, AddNewlineOptions(0, opts.addNewlines.get))

        print(Some(node), Some(parent), false, opts.trailingCommentsLineOffset.getOrElse(0))

        if (!opts.iterator.isEmpty) opts.iterator.get(node, i)
        if (i < nodes.get.length && !opts.separator.isEmpty) opts.separator.get(this)

        if (opts.statement.getOrElse(false)) {
          if (i + 1 == nodes.get.length) newline(1)
          else {
            val nextNode = nodes.get(i + 1)
            val newlinesOpts = nextNode.loc match {
              case Some(loc) => AddNewlineOptions(loc.start.get.line, opts.addNewlines.get)
              case _ => AddNewlineOptions(0, opts.addNewlines.get)
            }

            _printNewline(true, newlinesOpts)
          }
        }
      })

      if (!opts.indent.isEmpty) dedent()
    }

  def printAndIndentOnComments(node: BaseNode, parent: BaseNode) = {
    val needIndent: Boolean =
      !node.leadingComments.isEmpty && node.leadingComments.get.length > 0
    if (needIndent) indent()
    print(Some(node), Some(parent))
    if (needIndent) dedent()
  }

  // TODO:
  // @see printer.ts line 789
  def printBlock(parent: BaseNode): Unit = ???

  private def _printTrailingComments(node: BaseNode, parent: Option[BaseNode], lineOffset: Int = 0) = {
    val innerComments = node.innerComments
    if (!innerComments.isEmpty && innerComments.get.length > 0)
      _printComments(CommentType.Trailing, innerComments.get, node, parent, lineOffset)

    val trailingComments = node.trailingComments
    if (!trailingComments.isEmpty && trailingComments.get.length > 0)
      _printComments(CommentType.Trailing, trailingComments.get, node, parent, lineOffset)
  }

  private def _printLeadingComments(node: BaseNode, parent: Option[BaseNode]) = {
    val comments = node.leadingComments
    if (!comments.isEmpty && comments.get.length > 0)
      _printComments(CommentType.Leading, comments.get, node, parent)
  }

  private def _maybePrintInnerComments(): Unit = {
    if (_endWithInnerRaw) printInnerComments()

    _endWithInnerRaw = true
    _indentInnerComments = true
  }

  def printInnerComments(): Unit = {
    val node = _printStack.last
    val comments = node.innerComments
    if (!comments.isEmpty & comments.get.length > 0) {
      val hasSpace = endsWith(' ')
      val printedCommentsCount = _printedComments.size
      if (_indentInnerComments) indent()

      _printComments(CommentType.Inner ,comments.get, node)
      if (hasSpace && printedCommentsCount != _printedComments.size)
        space()
      if (_indentInnerComments) dedent()
    }
  }

  def noIndentInneerCommentsHere(): Unit = _indentInnerComments = false

  def printSequence(nodes: Array[BaseNode], parent: BaseNode, opts: PrintSequenceOptions): Unit = {
    val newOpts = PrintSequenceOptions(Some(true), opts.indent,
      opts.trailingCommentsLineOffset, opts.nextNodeStartLine, opts.addNewlines)
    // printJoin(Some(nodes), parent, newOpts) TODO: type issue here
  }

  def printList(items: Array[BaseNode], parent: BaseNode, opts: PrintListOptions): Unit = {
    val newOpts = PrintListOptions(if (opts.separator.isEmpty) Some(Printer.commaSeparator) else opts.separator,
      opts.iterator, opts.statement, opts.indent)
    // printJoin(Some(items), parent, newOpts) TODO: type issue here
  }

  private def _printNewline(newline: Boolean, opts: AddNewlineOptions) = 
    if (!format.retainLines && !format.compact) {
      if (format.concise) space()
      else if (newline) {
        val startLine = opts.nextNodeStartLine
        val lastCommentLine = _lastCommentLine
        val offset = startLine - lastCommentLine

        if (startLine > 0 && lastCommentLine > 0 && offset >= 0)
          this.newline(if (offset > 0) offset else 1)
        else if (buf.hasContent) this.newline(1)
      }
    }

  private def _shouldPrintComment(comment: BaseComment): PrintCommentHint =
    if (comment.ignore.getOrElse(false)) PrintCommentHint.Skip
    else if (_printedComments.contains(comment)) PrintCommentHint.Skip
    else if (_noLineTerminator &&
      (!(HAS_NEWLINE findFirstIn comment.value).isEmpty || !(HAS_BlOCK_COMMENT_END findFirstIn comment.value).isEmpty))
      PrintCommentHint.Defer
    else {
      _printedComments.add(comment)
      if (!format.shouldPrintComment(comment.value)) PrintCommentHint.Skip
      else PrintCommentHint.Allow
    }

  private def _printComment(comment: BaseComment, skipNewLines: CommentSkipNewLine) = {
    val isBlockComment = comment match {
      case _: CommentBlock => true
      case _ => false
    }
    val printNewLines =
      isBlockComment && skipNewLines != CommentSkipNewLine.All && !_noLineTerminator
    
    if (printNewLines && buf.hasContent && skipNewLines != CommentSkipNewLine.Leading)
      newline(1)

    val value: String = if (isBlockComment) {
      if (format.adjustMultilineComment) {
        val offset = comment.loc match {
          case Some(loc) => loc.start.get.column
          case _ => 0
        }

        val newlineVal =
          if (offset > 0) (s"/*${comment.value}*/").replaceAll(s"\\n\\s{1,$offset}", "\n")
          else s"/*${comment.value}*/"
        val indentSize =
          (if (format.retainLines) 0 else buf.getCurrentColumn) +
          (if (_shouldIndent('/') || format.retainLines) _getIndent else 0)

        newlineVal.replaceAll("\n(?!$)", s"\n${" " * indentSize}")
      }
      else s"/*${comment.value}*/"
    }
    else if (!_noLineTerminator) s"//${comment.value}"
    else s"/*${comment.value}*/"

    if (endsWith('/')) _space()

    source(LocationType.Start, comment.loc)
    _append(value, isBlockComment)

    if (!isBlockComment && !_noLineTerminator) newline(1, true)
    if (printNewLines && skipNewLines != CommentSkipNewLine.Trailing)
      newline(1)
  }

  private def _printComments(
    ty: CommentType,
    comments: Array[BaseComment],
    node: BaseNode,
    parent: Option[BaseNode] = None,
    lineOffset: Int = 0
  ) = {
    val nodeLoc = node.loc
    val len = comments.length
    var hasLoc = !nodeLoc.isEmpty
    val nodeStartLine = if (hasLoc) nodeLoc.get.start.get.line else 0
    val nodeEndLine = if (hasLoc) nodeLoc.get.end.get.line else 0
    var lastLine = 0
    var leadingCommentNewline = 0
    var defered = false

    val maybeNewline =
      if (_noLineTerminator)
        (offset: Int) => {}
      else (offset: Int) => newline(offset)

    comments.zipWithIndex.foreach((comment, i) => if (!defered) {
      val shouldPrint = _shouldPrintComment(comment)
      if (shouldPrint == PrintCommentHint.Defer) {
        defered = true
        hasLoc = false
      }

      if (hasLoc && !comment.loc.isEmpty && shouldPrint == PrintCommentHint.Allow) {
        val commentStartLine = comment.loc.get.start.get.line
        val commentEndLine = comment.loc.get.end.get.line
        if (ty == CommentType.Leading) {
          val offset =
            if (i == 0) {
              if (buf.hasContent && ((comment match {
                case _: CommentLine => true
                case _ => false
              }) || commentStartLine != commentEndLine)) {
                leadingCommentNewline = 1
                1
              }
              else 0
            }
            else commentStartLine - lastLine

          lastLine = commentEndLine
          maybeNewline(offset)
          _printComment(comment, CommentSkipNewLine.All)

          if (i + 1 == len) {
            maybeNewline(scala.math.max(nodeStartLine - lastLine, leadingCommentNewline))
            lastLine = nodeStartLine
          }
        }
        else if (ty == CommentType.Inner) {
          val offset = commentStartLine -
            (if (i == 0) nodeStartLine else lastLine)
          lastLine = commentEndLine

          maybeNewline(offset)
          _printComment(comment, CommentSkipNewLine.All)
          if (i + 1 == len) {
            maybeNewline(scala.math.min(1, nodeEndLine - lastLine))
            lastLine = nodeEndLine
          }
        }
        else {
          val offset = commentStartLine -
            (if (i == 0) nodeEndLine - lineOffset else lastLine)
          lastLine = commentEndLine

          maybeNewline(offset)
          _printComment(comment, CommentSkipNewLine.All)
        }
      }
      else {
        hasLoc = false
        if (shouldPrint == PrintCommentHint.Allow) {
          if (len == 1) {
            val singleLine =
            if (!comment.loc.isEmpty)
              (comment.loc.get.start.get.line == comment.loc.get.end.get.line)
            else (HAS_NEWLINE findFirstIn comment.value).isEmpty

            val shouldSkipNewline = singleLine // TODO: Check node type
            if (ty == CommentType.Leading)
              _printComment(comment, // TODO: Check node type
                if (shouldSkipNewline || singleLine) CommentSkipNewLine.All
                else CommentSkipNewLine.Default)
            else if (shouldSkipNewline && ty == CommentType.Trailing)
              _printComment(comment, CommentSkipNewLine.All)
            else _printComment(comment, CommentSkipNewLine.Default)
          }
          else if (ty == CommentType.Inner) // TODO: Check node type
            _printComment(comment,
              if (i == 0) CommentSkipNewLine.Leading
              else if (i == len - 1) CommentSkipNewLine.Trailing
              else CommentSkipNewLine.Default)
          else _printComment(comment, CommentSkipNewLine.Default)
        }
      }
    })

    if (ty == CommentType.Trailing && hasLoc && lastLine > 0)
      _lastCommentLine = lastLine
  }
}

object Printer {
  def needsParens(
    node: Option[BaseNode],
    parent: Option[BaseNode],
    printStack: Option[Array[BaseNode]]
  ): Boolean = if (parent.isEmpty) false
      else {
        // TODO: check node type
        // @ see index.ts line 114
        ???
      }

  type PrintJoinOptions = PrintListOptions & PrintSequenceOptions

  def commaSeparator(printer: Printer): Unit = {
    printer.token(",")
    printer.space()
  }
}
