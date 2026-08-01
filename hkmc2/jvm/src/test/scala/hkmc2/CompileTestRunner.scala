package hkmc2

import hkmc2.utils.*, shorthands.*
import io.PlatformPath.given


class CompileTestRunner extends CompileTestRunnerBase(
  compileDirs = TestFolders.mainCompileDirs(os.pwd),
  excludedDirs = TestFolders.mainExcludedCompileDirs(os.pwd),
):
  protected def cctx: CompilerCtx = CompileTestRunner.cctx

end CompileTestRunner


object CompileTestRunner:
  
  // * The root configuration is fixed for the whole run: the compilation units cached in this
  // * context are shared between tests, so they must not depend on which test reaches them first.
  // * Stack safety relies on the fact that runtime uses while loops for resumption
  // * and does not create extra stack depth. Hence, while loop rewriting should be disabled here.
  // * (It used to be on by default, but now is off by default, so nothing to do.)
  given cctx: CompilerCtx = CompilerCtx.fresh(io.FileSystem.default,
    TestFolders.compilerPaths(os.pwd), Config.default(TestFolders.mainTestDir(os.pwd)))
  
end CompileTestRunner

