package driver

import scala.scalajs.js
import mlscript.utils._
import mlscript._
import mlscript.utils.shorthands._
import scala.collection.mutable.{ListBuffer,Map => MutMap, Set => MutSet}
import mlscript.codegen._
import mlscript.{NewLexer, NewParser, ErrorReport, Origin, Diagnostic}
import ts2mls.{TSProgram, TypeScript, TSPathResolver, JSFileSystem, JSWriter, FileInfo}
import ts2mls.IsUndefined

class Driver(options: DriverOptions) {
  import Driver._
  import JSFileSystem._
  import JSDriverBackend.ModuleType

  private var dbgWriter: Option[JSWriter] = None
  private def printDbg(msg: String) =
    dbgWriter.fold(println(msg))(writer => writer.writeDbg(msg))

  private val typer =
    new mlscript.Typer(
      dbg = false,
      verbose = false,
      explainErrors = false,
      newDefs = true
    ) {
      override def emitDbg(str: String): Unit = printDbg(str)
    }

  import typer._

  private object SimplifyPipeline extends typer.SimplifyPipeline {
    def debugOutput(msg: => Str): Unit =
      println(msg)
  }

  // errors in imported files should be printed in their own files to avoid redundancy
  private val noRedundantRaise = (diag: Diagnostic) => ()

  private val importedModule = MutSet[String]()
  private implicit val config = TypeScript.parseOption(options.path, options.tsconfig)

  import TSPathResolver.{normalize, isLocal, isMLScirpt, dirname}

  private def checkESModule(filename: String, from: String) =
    if (isMLScirpt(filename)) None
    else if (isLocal(filename)) // local files: check tsconfig.json
      Some(TypeScript.isESModule(config, false))
    else { // node_modules: find package.json to get the module type
      val fullname = TypeScript.resolveModuleName(filename, from, config)
      def find(path: String): Boolean = {
        val dir = dirname(path)
        val pack = s"$dir/package.json"
        if (JSFileSystem.exists(pack)) {
          val config = TypeScript.parsePackage(pack)
          TypeScript.isESModule(config, true)
        }
        else if (dir.isEmpty || dir === "." || dir === "/") false // not found: default is commonjs
        else find(dir)
      }
      Some(find(fullname))
    }

  // Return true if success
  def execute: Boolean =
    try {
      Driver.totalErrors = 0
      implicit var ctx: Ctx = Ctx.init
      implicit val raise: Raise = (diag: Diagnostic) => report(diag, options.expectTypeError, options.expectError)
      implicit val extrCtx: Opt[typer.ExtrCtx] = N
      implicit val vars: Map[Str, typer.SimpleType] = Map.empty
      implicit val stack = List[String]()
      initTyper
      compile(FileInfo(options.path, options.filename, options.interfaceDir), false)
      Driver.totalErrors == 0
    }
    catch {
      case err: Diagnostic => // we can not find a file to store the error message. print on the screen
        report(err, options.expectTypeError, options.expectError)
        false
    }

  def genPackageJson(): Unit = {
    val content = // TODO: more settings?
      if (!options.commonJS) "{ \"type\": \"module\" }\n"
      else "{ \"type\": \"commonjs\" }\n"
    saveToFile(s"${options.outputDir}/package.json", content)
  }

  type ParseResult = (List[Statement], List[NuDecl], List[Import], Origin)
  private def parse(filename: String, content: String): ParseResult = {
    import fastparse._
    import fastparse.Parsed.{Success, Failure}

    val lines = content.splitSane('\n').toIndexedSeq
    lines.headOption match {
      case S(head) => typer.dbg = head.startsWith("//") && head.endsWith(":d")
      case _ => typer.dbg = false
    }
    val fph = new mlscript.FastParseHelpers(content, lines)
    val origin = Origin(filename, 1, fph)
    val lexer = new NewLexer(origin, throw _, dbg = false)
    val tokens = lexer.bracketedTokens

    val parser = new NewParser(origin, tokens, throw _, dbg = false, None) {
      def doPrintDbg(msg: => String): Unit = if (dbg) println(msg)
    }

    val (tu, depList) = parser.parseAll(parser.tuWithImports)
    val (definitions, declarations) = tu.entities.partitionMap {
      case nt: NuTypeDef if (nt.isDecl) => Right(nt)
      case nf @ NuFunDef(_, _, _, Right(_)) => Right(nf)
      case t => Left(t)
    }

    (definitions, declarations, depList, origin)
  }

  private def isInterfaceOutdate(origin: String, inter: String): Boolean = {
    val mtime = getModificationTime(origin)
    val imtime = getModificationTime(inter)
    mtime >= imtime
  }

  private def packTopModule(moduleName: Option[String], content: String) =
    moduleName.fold(content)(moduleName =>
      s"declare module $moduleName() {\n" +
          content.splitSane('\n').toIndexedSeq.filter(!_.isEmpty()).map(line => s"  $line").reduceLeft(_ + "\n" + _) +
        "\n}\n"
    )

  private def parseAndRun[Res](filename: String, f: (ParseResult) => Res): Res = readFile(filename) match {
    case Some(content) => f(parse(filename, content))
    case _ =>
      throw
        ErrorReport(Ls((s"can not open file $filename", None)), Diagnostic.Compilation)
  }

  private def extractSig(filename: String, moduleName: String): TypingUnit =
    parseAndRun(filename, {
      case (_, declarations, _, origin) => TypingUnit(
        NuTypeDef(Mod, TypeName(moduleName), Nil, S(Tup(Nil)), N, N, Nil, N, N, TypingUnit(declarations))(S(Loc(0, 1, origin)), N, N) :: Nil)
    })

  // if the current file is es5.mlsi, we allow overriding builtin type(like String and Object)
  private def `type`(tu: TypingUnit, isES5: Boolean)(
    implicit ctx: Ctx,
    raise: Raise,
    extrCtx: Opt[typer.ExtrCtx],
    vars: Map[Str, typer.SimpleType]
  ) = {
    val tpd = typer.typeTypingUnit(tu, N, isES5)
    val sim = SimplifyPipeline(tpd, all = false)
    typer.expandType(sim)
  }

  private lazy val jsBuiltinDecs = Driver.jsBuiltinPaths.map(path => parseAndRun(path, {
    case (_, declarations, _, _) => declarations
  }))

  private def checkTSInterface(file: FileInfo, writer: JSWriter): Unit = parse(file.filename, writer.getContent) match {
    case (_, declarations, imports, origin) =>
      var ctx: Ctx = Ctx.init
      val extrCtx: Opt[typer.ExtrCtx] = N
      val vars: Map[Str, typer.SimpleType] = Map.empty
      initTyper(ctx, noRedundantRaise, extrCtx, vars)
      val reportRaise = (diag: Diagnostic) =>
        Diagnostic.report(diag, (s: String) => writer.writeErr(s), 0, false)
      val tu = TypingUnit(declarations)
      try {
        imports.foreach(d => importModule(file.`import`(d.path))(ctx, extrCtx, vars))
        `type`(tu, false)(ctx, reportRaise, extrCtx, vars)
      }
      catch {
        case t : Throwable =>
          if (!options.expectTypeError) totalErrors += 1
          writer.writeErr(t.toString())
          ()
      }
  }

  private def initTyper(
    implicit ctx: Ctx,
    raise: Raise,
    extrCtx: Opt[typer.ExtrCtx],
    vars: Map[Str, typer.SimpleType]
  ) = jsBuiltinDecs.foreach(lst => `type`(TypingUnit(lst), true))

  // translate mlscirpt import paths into js import paths
  private def resolveJSPath(file: FileInfo, imp: String) =
    if (isLocal(imp) && !isMLScirpt(imp)) { // local ts files: locate by checking tsconfig.json
      val tsPath = TypeScript.getOutputFileNames(s"${TSPathResolver.dirname(file.filename)}/$imp", config)
      val outputBase = TSPathResolver.dirname(TSPathResolver.normalize(s"${options.outputDir}${file.jsFilename}"))
      TSPathResolver.relative(outputBase, tsPath)
    }
    else imp // mlscript & node_module: use the original name

  private def importModule(file: FileInfo)(
    implicit ctx: Ctx,
    extrCtx: Opt[typer.ExtrCtx],
    vars: Map[Str, typer.SimpleType]
  ): Unit =
    parseAndRun(s"${options.path}/${file.interfaceFilename}", {
      case (_, declarations, imports, _) =>
        imports.foreach(d => importModule(file.`import`(d.path)))
        `type`(TypingUnit(declarations), false)(ctx, noRedundantRaise, extrCtx, vars)
  })

  // return true if this file is recompiled.
  private def compile(
    file: FileInfo,
    exported: Boolean
  )(
    implicit ctx: Ctx,
    extrCtx: Opt[typer.ExtrCtx],
    vars: Map[Str, typer.SimpleType],
    stack: List[String]
  ): Boolean = {
    if (!isMLScirpt(file.filename)) { // TypeScript
      System.out.println(s"generating interface for ${file.filename}...")
      val tsprog =
         TSProgram(file, true, options.tsconfig, checkTSInterface)
      return tsprog.generate
    }

    val mlsiFile = normalize(s"${file.workDir}/${file.interfaceFilename}")
    val mlsiWriter = JSWriter(mlsiFile)
    implicit val raise: Raise = (diag: Diagnostic) =>
      Diagnostic.report(diag, (s: String) => mlsiWriter.writeErr(s), 0, false)
    parseAndRun(file.filename, {
      case (definitions, declarations, imports, _) => {
        val depList = imports.map(_.path)

        val (cycleList, otherList) = depList.filter(dep => {
          val depFile = file.`import`(dep)
          if (depFile.filename === file.filename) {
            totalErrors += 1
            mlsiWriter.writeErr(s"can not import ${file.filename} itself")
            false
          }
          else true
        }).partitionMap { dep => {
          val depFile = file.`import`(dep)
          if (stack.contains(depFile.filename)) L(depFile)
          else R(dep)
        } }

        val (cycleSigs, cycleRecomp) = cycleList.foldLeft((Ls[TypingUnit](), false))((r, file) => r match {
          case (sigs, recomp) => {
            importedModule += file.filename
            (sigs :+ extractSig(file.filename, file.moduleName),
              recomp || isInterfaceOutdate(file.filename, s"${options.path}/${file.interfaceFilename}"))
          }
        })
        val needRecomp = otherList.foldLeft(cycleRecomp)((nr, dp) => {
          // We need to create another new context when compiling other files
          // e.g. A -> B, A -> C, B -> D, C -> D, -> means "depends on"
          // If we forget to add `import "D.mls"` in C, we need to raise an error
          // Keeping using the same environment would not.
          var newCtx: Ctx = Ctx.init
          val newExtrCtx: Opt[typer.ExtrCtx] = N
          val newVars: Map[Str, typer.SimpleType] = Map.empty
          initTyper
          val newFilename = file.`import`(dp)
          importedModule += newFilename.filename
          compile(newFilename, true)(newCtx, newExtrCtx, newVars, stack :+ file.filename)
        } || nr)

        if (options.force || needRecomp || isInterfaceOutdate(file.filename, mlsiFile)) {
          System.out.println(s"compiling ${file.filename}...")
          try { otherList.foreach(d => importModule(file.`import`(d))) }
          catch {
            case t : Throwable =>
              if (!options.expectTypeError) totalErrors += 1
              mlsiWriter.writeErr(t.toString())
          }
          if (file.filename.endsWith(".mls")) { // only generate js/mlsi files for mls files
            val expStr = try {
              cycleSigs.foldLeft("")((s, tu) => s"$s${`type`(tu, false).show}") + {
                dbgWriter = Some(mlsiWriter);
                val res = packTopModule(Some(file.moduleName), `type`(TypingUnit(definitions), false).show);
                dbgWriter = None
                res
              }
            }
            catch {
              case t : Throwable =>
                if (!options.expectTypeError) totalErrors += 1
                mlsiWriter.writeErr(t.toString())
                ""
            }
            val interfaces = otherList.map(s => Import(file.translateImportToInterface(s))).foldRight(expStr)((imp, itf) => s"$imp\n$itf")

            mlsiWriter.write(interfaces)
            mlsiWriter.close()
            if (totalErrors == 0)
              generate(Pgrm(definitions), s"${options.outputDir}/${file.jsFilename}", file.moduleName, imports.map(
                imp => new Import(resolveJSPath(file, imp.path)) with ModuleType {
                  val isESModule = checkESModule(path, TSPathResolver.resolve(file.filename))
                }
              ), exported || importedModule(file.filename))
          }
          else try {
            `type`(TypingUnit(declarations), false) // for ts/mlsi files, we only check interface files
          }
          catch {
            case t : Throwable =>
              if (!options.expectTypeError) totalErrors += 1
              mlsiWriter.writeErr(t.toString())
              ""
          }
          true
        }
        else false // no need to recompile
      }
    })
  }

  private def generate(
    program: Pgrm,
    filename: String,
    moduleName: String,
    imports: Ls[Import with ModuleType],
    exported: Boolean
  ): Unit = try {
    val backend = new JSDriverBackend()
    jsBuiltinDecs.foreach(lst => backend.declareJSBuiltin(Pgrm(lst)))
    val lines = backend(program, moduleName, imports, exported, options.commonJS)
    val code = lines.mkString("", "\n", "\n")
    saveToFile(filename, code)
  } catch {
      case CodeGenError(err) =>
        totalErrors += 1
        saveToFile(filename, s"//| codegen error: $err")
      case t : Throwable =>
        totalErrors += 1
        saveToFile(filename, s"//| unexpected error: ${t.toString()}")
    }
}

object Driver {
  def apply(options: DriverOptions) = new Driver(options)

  private val jsBuiltinPaths = List(
    "./ts2mls/js/src/test/diff/ES5.mlsi",
    "./ts2mls/js/src/test/diff/Dom.mlsi",
    "./driver/js/src/test/predefs/Predef.mlsi"
  )

  private def printErr(msg: String): Unit =
    System.err.println(msg)

  private var totalErrors = 0

  private def saveToFile(filename: String, content: String) = {
    val writer = JSWriter(filename)
    writer.write(content)
    writer.close()
  }

  private def report(diag: Diagnostic, expectTypeError: Boolean, expectError: Boolean): Unit = {
    diag match {
      case ErrorReport(msg, loco, src) =>
        src match {
          case Diagnostic.Lexing =>
            totalErrors += 1
          case Diagnostic.Parsing =>
            totalErrors += 1
          case _ =>
            if (!expectTypeError) totalErrors += 1
        }
      case WarningReport(msg, loco, src) => ()
    }
    Diagnostic.report(diag, printErr, 0, false)
  }
}
