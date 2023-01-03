package mlscript.codegen.generator

import mlscript.codegen.ast.Node

class BufferSuite extends munit.FunSuite:
  // TODO: Decouple Printer and Buffer.
  val printer = new Printer(new Format {
    val compact: Boolean = false
    val minified: Boolean = false
    var concise: Boolean = false
    val retainLines: Boolean = false
    val auxiliaryCommentBefore: String = ""
    val auxiliaryCommentAfter: String = ""
    val adjustMultilineComment: Boolean = false
    val retainFunctionParens: Boolean = false
    val indent = "  "
    
    def shouldPrintComment(comment: String): Boolean = false
  }, new SourceMapBuilder(None, None, Left(""))) {
    def print(node: Node): Unit = ()
  }
  test("Buffer Output - queue") {
    val buffer = new Buffer(None, printer)
    buffer.queue('H')
    buffer.queue('e')
    buffer.queue('l')
    buffer.queue('l')
    buffer.queue('o')
    buffer.queue(' ')
    buffer.queue('\t')
    buffer.queue('\n')
    buffer.queue('!')

    assertEquals(buffer.getLastChar, '!')

    val output = buffer.get()
    assertEquals(output.code, "Hello\n!")
  }

  test("Buffer Output - position") {
    val buffer = new Buffer(None, printer)
    assertEquals(buffer.getCurrentLine, 1)
    assertEquals(buffer.getCurrentColumn, 0)

    buffer.queue('f')
    buffer.queue('o')
    buffer.queue('o')
    assertEquals(buffer.getCurrentLine, 1)
    assertEquals(buffer.getCurrentColumn, 3)

    buffer.get() // flush
    buffer.appendChar('\n')
    buffer.appendChar('1')
    assertEquals(buffer.getCurrentLine, 2)
    assertEquals(buffer.getCurrentColumn, 1)
  }

  test("Buffer Output - others") {
    val buffer = new Buffer(None, printer)
    assertEquals(buffer.hasContent, false)

    buffer.appendChar('?')
    assertEquals(buffer.getLastChar, '?')

    assertEquals(buffer.getNewlineCount, 0)
    buffer.appendChar('\n')
    assertEquals(buffer.getNewlineCount, 1)
    buffer.queue('\n')
    assertEquals(buffer.getNewlineCount, 2)
    buffer.get() // flush

    buffer.appendChar(';') // not revertable!
    buffer.removeLastSemicolon()
    assertEquals(buffer.getLastChar, ';')
    buffer.appendChar('1')
    buffer.queue(';') // revertable!
    buffer.removeLastSemicolon()
    assertEquals(buffer.getLastChar, '1')

    buffer.queue('\n')
    buffer.removeTrailingNewline()
    assertEquals(buffer.getLastChar, '1')

    assertEquals(buffer.hasContent, true)

    buffer.queue('2')
    assertEquals(buffer.endsWithCharAndNewline, None)
    buffer.queue('\n')
    assertEquals(buffer.endsWithCharAndNewline, Some('2'))
  }

