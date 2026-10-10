package hkmc2

import org.scalatest.{Args, Status, funsuite, ParallelTestExecution}
import org.scalatest.time._
import org.scalatest.concurrent.{TimeLimitedTests, Signaler}

import hkmc2.utils.*, shorthands.*
import io.PlatformPath.given


// Reusable base class for compile test runners. Subclasses provide the
// directories to scan and the compiler context to use, and this base class
// handles walking directories, filtering for mlscript-compile files,
// and registering ScalaTest tests.
abstract class CompileTestRunnerBase(
  compileDirs: Ls[os.Path],
  excludedDirs: Ls[os.Path] = Nil,
) extends TimeOutTests, ParallelTestExecution:
  
  
  val timeLimit = Span(600, Seconds)
  
  
  given CompilerCtx = cctx
  
  protected def cctx: CompilerCtx
  
  private val inParallel = isInstanceOf[ParallelTestExecution]
  
  val workingDir: os.Path = os.pwd
  
  val mainTestDir: os.Path = TestFolders.mainTestDir(workingDir)
  
  val validExt = Set("mls")
  
  // Resolve ctest's path against registered tests so discovery and exclusions
  // stay centralized here. Reject ambiguous basenames instead of compiling
  // both a staging source and its generated out/ copy.
  override def run(testName: Option[String], args: Args): Status =
    (testName, args.configMap.get("compileTestPath")) match
      case (None, Some(path: String)) =>
        val selector = path.stripSuffix(".mls").stripPrefix("mlscript-compile/")
        val matches = testNames.filter: name =>
          val relative = name.stripPrefix("mlscript-compile/")
          relative == selector || relative.endsWith("/" + selector)
        matches.toList.sorted match
          case selected :: Nil => super.run(Some(selected), args)
          case Nil =>
            throw new IllegalArgumentException(
              s"No compilation test matches '$path'. Use an exact file path, or ctest -z $path for substring matching.")
          case candidates =>
            throw new IllegalArgumentException(
              s"Ambiguous compilation test '$path'. Specify one of: ${candidates.mkString(", ")}")
      case _ => super.run(testName, args)

  for dir <- compileDirs do {
    val allFiles = os.walk(dir)
      .filter(_.toIO.isFile)
      .filter(_.ext in validExt)
    
    lazy val compileTestFiles = allFiles.filter: file =>
      file.segments.contains("mlscript-compile")
        && !TestFolders.isExcluded(file, excludedDirs)
    
    compileTestFiles.foreach: file =>
      
      val basePath = file.segments.drop(dir.segmentCount).toList.init
      val relativeName = basePath.map(_ + "/").mkString + file.baseName
      
      test(relativeName):
        
        this.synchronized:
          println(s"Compiling: $relativeName")
        
        // Synchronize diagnostic output to avoid interleaving since the compiler tests run in parallel.
        val wrap: (=> Unit) => Unit = body => this.synchronized(body)
        val report = ReportFormatter(System.out.println, mainTestDir, colorize = true, wrap = Some(wrap))
        val compiler = MLsCompiler(mkRaise = report.mkRaise)
        val startTime = System.nanoTime()
        // Report elapsed time even when compilation throws, as diff tests do.
        try compiler.compileModule(file) finally
          val duration = (System.nanoTime() - startTime) / 1000000
          this.synchronized:
            println(s"${fansi.Color.Cyan.escape}Compiled in ${Console.BOLD}${
                f"$duration%5d"
              } ms${Console.RESET}  ${
                Console.YELLOW + relativeName + "." + file.ext + Console.RESET
              }")
        
        if report.badLines.nonEmpty then
          fail(s"Unexpected diagnostic at: " +
            report.badLines.distinct.sorted
              .map("\n\t"+relativeName+"."+file.ext+":"+_).mkString(", "))
  }

end CompileTestRunnerBase
