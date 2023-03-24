import scala.scalajs.js
import js.Dynamic.{global => g}
import js.DynamicImplicits._
import js.JSConverters._
import mlscript.utils._
import mlscript._
import mlscript.utils.shorthands._
import scala.collection.mutable.{ListBuffer,Map => MutMap}
import mlscript.codegen._

class Driver(options: DriverOptions) {
  import Driver._

  def execute: Unit =
    try {
      compile(options.filename)
      if (Driver.totalErrors > 0)
        js.Dynamic.global.process.exit(-1)
    }
    catch {
      case err: Diagnostic =>
        report(err)
        js.Dynamic.global.process.exit(-1)
    }
  
  def genPackageJson(): Unit = {
    val content = """{ "type": "module" }""" // TODO: more settings?
    writeFile(options.outputDir, "package.json", content)
  }

  private def compile(filename: String, exported: Boolean = false): Boolean = {
    val beginIndex = filename.lastIndexOf("/")
    val endIndex = filename.lastIndexOf(".")
    val prefixName = filename.substring(beginIndex + 1, endIndex)
    val path = filename.substring(0, beginIndex + 1)

    System.out.println(s"compiling $filename...")
    readFile(filename) match {
      case Some(content) => {
        import fastparse._
        import fastparse.Parsed.{Success, Failure}
        import mlscript.{NewLexer, NewParser, ErrorReport, Origin}

        val lines = content.splitSane('\n').toIndexedSeq
        val fph = new mlscript.FastParseHelpers(content, lines)
        val origin = Origin("<input>", 1, fph)
        val lexer = new NewLexer(origin, throw _, dbg = false)
        val tokens = lexer.bracketedTokens

        val parser = new NewParser(origin, tokens, throw _, dbg = false, None) {
          def doPrintDbg(msg: => String): Unit = if (dbg) println(msg)
        }
        parser.parseAll(parser.typingUnit) match {
          case tu => {
            val depList = tu.depList.map {
              case Import(path) => path
            }
            val needRecomp = depList.foldLeft(false)((nr, dp) => nr || compile(s"$path$dp", true))
            val mtime = getModificationTime(filename)
            val imtime = getModificationTime(s"${options.outputDir}/.temp/$prefixName.mlsi")

            if (options.force || needRecomp || imtime.isEmpty || mtime.compareTo(imtime) >= 0) {
              typeCheck(tu, prefixName, depList)
              generate(Pgrm(tu.entities), tu.depList, prefixName, exported)
              true
            }
            else false
          }
        }
      }
      case _ => report(s"can not open file $filename"); true
    }
  }

  private def typeCheck(tu: TypingUnit, filename: String, depList: List[String]): Unit = {
    val typer = new mlscript.Typer(
        dbg = false,
        verbose = false,
        explainErrors = false
      ) {
        newDefs = true
      }

    import typer._
    type ModuleType = DelayedTypeInfo

    var ctx: Ctx = Ctx.init
    implicit val raise: Raise = report

    def importModule(modulePath: String): Unit = {
      val filename = s"${options.outputDir}/.temp/$modulePath.mlsi"
      readFile(filename) match {
        case Some(content) => {
          val moduleName = modulePath.substring(modulePath.lastIndexOf("/") + 1)
          val wrapped = s"module $moduleName() {\n" +
            content.splitSane('\n').toIndexedSeq.map(line => s"  $line").reduceLeft(_ + "\n" + _) + "\n}"
          val lines = wrapped.splitSane('\n').toIndexedSeq
          val fph = new mlscript.FastParseHelpers(wrapped, lines)
          val origin = Origin("<input>", 1, fph)
          val lexer = new NewLexer(origin, throw _, dbg = false)
          val tokens = lexer.bracketedTokens

          val parser = new NewParser(origin, tokens, throw _, dbg = false, None) {
            def doPrintDbg(msg: => String): Unit = if (dbg) println(msg)
          }

          parser.parseAll(parser.typingUnit) match {
            case tu: TypingUnit => {
              val vars: Map[Str, typer.SimpleType] = Map.empty
              val tpd = typer.typeTypingUnit(tu, topLevel = true)(ctx.nest, raise, vars)
              object SimplifyPipeline extends typer.SimplifyPipeline {
                def debugOutput(msg: => Str): Unit =
                  // if (mode.dbgSimplif) output(msg)
                  println(msg)
              }
              val sim = SimplifyPipeline(tpd, all = false)(ctx)
              val exp = typer.expandType(sim)(ctx)
            }
          }
        }
        case _ => report(s"can not open file $filename")
      }
    }

    depList.foreach(d => importModule(d.substring(0, d.lastIndexOf("."))))
    
    implicit var nestedCtx: Ctx = ctx.nest
    implicit val extrCtx: Opt[typer.ExtrCtx] = N
    val vars: Map[Str, typer.SimpleType] = Map.empty
    val tpd = typer.typeTypingUnit(tu, topLevel = true)(nestedCtx.nest, raise, vars)
    object SimplifyPipeline extends typer.SimplifyPipeline {
      def debugOutput(msg: => Str): Unit =
        // if (mode.dbgSimplif) output(msg)
        println(msg)
    }
    val sim = SimplifyPipeline(tpd, all = false)(nestedCtx)
    val exp = typer.expandType(sim)(nestedCtx)
    val expStr = exp.showIn(ShowCtx.mk(exp :: Nil), 0)
    writeFile(s"${options.outputDir}/.temp", s"$filename.mlsi", expStr)
  }

  private def generate(program: Pgrm, imports: Ls[Import], filename: String, exported: Boolean): Unit = {
    val backend = new JSCompilerBackend()
    val lines = backend(program, imports, exported)
    val code = lines.mkString("", "\n", "\n")
    writeFile(options.outputDir, s"$filename.js", code)
  }
}

object Driver {
  def apply(options: DriverOptions) = new Driver(options)

  private val fs = g.require("fs") // must use fs module to manipulate files in JS

  private def readFile(filename: String) =
    if (!fs.existsSync(filename)) None
    else Some(fs.readFileSync(filename).toString)

  private def writeFile(dir: String, filename: String, content: String) = {
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, js.Dictionary("recursive" -> true))
    fs.writeFileSync(s"$dir/$filename", content)
  }

  private def getModificationTime(filename: String): String =
    if (!fs.existsSync(filename)) ""
    else {
      val state = fs.statSync(filename)
      state.mtimeMs.toString
    }

  private def report(msg: String): Unit =
    System.err.println(msg)

  private var totalErrors = 0
  
  // TODO factor with duplicated logic in DiffTests
  private def report(diag: Diagnostic): Unit = {
    val sctx = Message.mkCtx(diag.allMsgs.iterator.map(_._1), "?")
    val headStr = diag match {
      case ErrorReport(msg, loco, src) =>
        src match {
          case Diagnostic.Lexing =>
            s"╔══[LEXICAL ERROR] "
          case Diagnostic.Parsing =>
            s"╔══[PARSE ERROR] "
          case _ => // TODO customize too
              totalErrors += 1
            s"╔══[ERROR] "
        }
      case WarningReport(msg, loco, src) =>
        s"╔══[WARNING] "
    }
    val lastMsgNum = diag.allMsgs.size - 1
    diag.allMsgs.zipWithIndex.foreach { case ((msg, loco), msgNum) =>
      val isLast = msgNum =:= lastMsgNum
      val msgStr = msg.showIn(sctx)
      if (msgNum =:= 0) report(headStr + msgStr)
      else report(s"${if (isLast && loco.isEmpty) "╙──" else "╟──"} ${msgStr}")
      if (loco.isEmpty && diag.allMsgs.size =:= 1) report("╙──")
      loco.foreach { loc =>
        val (startLineNum, startLineStr, startLineCol) =
          loc.origin.fph.getLineColAt(loc.spanStart)
        val (endLineNum, endLineStr, endLineCol) =
          loc.origin.fph.getLineColAt(loc.spanEnd)
        var l = startLineNum
        var c = startLineCol
        while (l <= endLineNum) {
          val globalLineNum = loc.origin.startLineNum + l - 1
          val shownLineNum = "l." + globalLineNum
          val prepre = "║  "
          val pre = s"$shownLineNum: "
          val curLine = loc.origin.fph.lines(l - 1)
          report(prepre + pre + "\t" + curLine)
          val tickBuilder = new StringBuilder()
          tickBuilder ++= (
            (if (isLast && l =:= endLineNum) "╙──" else prepre)
            + " " * pre.length + "\t" + " " * (c - 1))
          val lastCol = if (l =:= endLineNum) endLineCol else curLine.length + 1
          while (c < lastCol) { tickBuilder += ('^'); c += 1 }
          if (c =:= startLineCol) tickBuilder += ('^')
          report(tickBuilder.toString)
          c = 1
          l += 1
        }
      }
    }
    if (diag.allMsgs.isEmpty) report("╙──")
    ()
  }
}
