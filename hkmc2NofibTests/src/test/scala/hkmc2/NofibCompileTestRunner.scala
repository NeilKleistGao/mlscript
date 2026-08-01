package hkmc2

import hkmc2.utils.*, shorthands.*
import io.PlatformPath.given


class NofibCompileTestRunner extends CompileTestRunnerBase(
  compileDirs = TestFolders.nofibCompileDirs(os.pwd),
):
  protected def cctx: CompilerCtx = NofibCompileTestRunner.cctx

end NofibCompileTestRunner


object NofibCompileTestRunner:
  
  // * Fixed for the whole run: the compilation units cached in this context are shared
  // * between tests, so they must not depend on which test reaches them first.
  given cctx: CompilerCtx = CompilerCtx.fresh(io.FileSystem.default,
    TestFolders.compilerPaths(os.pwd), Config.default(TestFolders.mainTestDir(os.pwd)))

end NofibCompileTestRunner

