package hkmc2

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.*

import org.scalatest.{Args, ConfigMap, Reporter}
import org.scalatest.events.Event
import org.scalatest.funsuite.AnyFunSuite


class CompileTestSelectionTest extends AnyFunSuite:
  private val names = List(
    "mlscript-compile/NaiveFFT",
    "mlscript-compile/staging/StagedFFT",
    "mlscript-compile/staging/out/StagedFFT",
    "mlscript-compile/staging/SpecialFFT",
  )

  private def fixture(): (CompileTestRunnerBase, ConcurrentLinkedQueue[String]) =
    val executed = new ConcurrentLinkedQueue[String]
    // Register lightweight bodies so selecting the wrong file is observable
    // without compiling modules or modifying generated benchmark files.
    def makeRunner(): CompileTestRunnerBase =
      new CompileTestRunnerBase(Nil):
        protected def cctx: CompilerCtx = CompileTestRunner.cctx
        override def newInstance: CompileTestRunnerBase = makeRunner()
        names.foreach: name =>
          test(name):
            executed.add(name)
            succeed
    (makeRunner(), executed)

  private def runArgs(config: ConfigMap): Args =
    val reporter = new Reporter:
      def apply(event: Event): Unit = ()
    Args(reporter, configMap = config)

  for (selector, expected) <- List(
    "NaiveFFT" -> "mlscript-compile/NaiveFFT",
    "staging/StagedFFT" -> "mlscript-compile/staging/StagedFFT",
    "out/StagedFFT" -> "mlscript-compile/staging/out/StagedFFT",
    "mlscript-compile/staging/StagedFFT.mls" -> "mlscript-compile/staging/StagedFFT",
  ) do
    test(s"ctest $selector executes only the selected file"):
      val (runner, executed) = fixture()
      val status = runner.run(None, runArgs(ConfigMap("compileTestPath" -> selector)))
      status.waitUntilCompleted()
      assert(status.succeeds())
      assert(executed.asScala.toList == List(expected))

  for selector <- List("FFT", "StagedFFT", "staging") do
    test(s"ctest $selector rejects missing or ambiguous paths without executing tests"):
      val (runner, executed) = fixture()
      intercept[IllegalArgumentException]:
        runner.run(None, runArgs(ConfigMap("compileTestPath" -> selector)))
      assert(executed.isEmpty)

  test("ctest without a path executes all registered files"):
    val (runner, executed) = fixture()
    val status = runner.run(None, runArgs(ConfigMap.empty))
    status.waitUntilCompleted()
    assert(status.succeeds())
    assert(executed.asScala.toList.sorted == names.sorted)

end CompileTestSelectionTest
