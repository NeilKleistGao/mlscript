package hkmc2

import scala.collection.mutable
import scala.annotation.tailrec
import collection.mutable.Map as MutMap

import hkmc2.utils.*, shorthands.*
import hkmc2.utils.*
import hkmc2.Message.MessageContext
import hkmc2.io
import utils.TraceLogger

import semantics.*
import Elaborator.*
import hkmc2.syntax.LetBind


import CompilerCache.*


class CompilerCtx(
    val importing: Opt[(io.Path, CompilerCtx)],
    val beingCompiled: Set[io.Path],
    val fs: io.FileSystem,
    cache: CompilerCache,
    val paths: Opt[MLsCompiler.Paths],
    val rootConfig: Opt[Config],
):
  
  def allFilesBeingImported: Ls[io.Path] =
    importing match
    case S((path, parent)) => path :: parent.allFilesBeingImported
    case N => Nil
  
  def derive(newFile: io.Path): CompilerCtx =
    CompilerCtx(S(newFile, this), beingCompiled + newFile, fs, cache, paths, rootConfig)

  def withPaths(newPaths: MLsCompiler.Paths): CompilerCtx =
    CompilerCtx(importing, beingCompiled, fs, cache, S(newPaths), rootConfig)

  def withRootConfig(newRootConfig: Config): CompilerCtx =
    CompilerCtx(importing, beingCompiled, fs, cache, paths, S(newRootConfig))
  
  def getElaboratedBlock
        (file: io.Path, prelude: Ctx, importerCfg: Config)
        (using TL, Raise)
        : Artifact =
    
    // println(s"Cache has: ${cache.elabCache.contains(file)} ${cache.elabCache.keys}")
    
    val lastMod = fs.getLastChangedTimestamp(file)
    val compilationUnitConfig = rootConfig.getOrElse(Config.default(importerCfg.baseDir))
    
    def mk =
      val state = new Elaborator.State
      given Elaborator.State = state

      // * Later, we can draw this from a global root configuration,
      // * which is set for a whole application.
      given Config = compilationUnitConfig

      /*
      val parse =
        given CompilerCtx = this
        ParserSetup(file, dbgParsing = false)
      val resBlk = parse.resultBlk
      given Elaborator.Ctx = prelude.copy(mode = Mode.Light).nestLocal("prelude")
      val elab =
        given CompilerCtx = derive(parse.origin.fileName)
        Elaborator(tl, file.up, prelude)
      val elabbed = elab.importFrom(resBlk)

      // val
      */




      // TODO: !CLEANUP!
      // TODO adapt logic
      given SymbolPrinter = new SymbolPrinter(
        Scope.empty(Scope.Cfg.default.copy(
          escapeChars = false,
          useSuperscripts = true,
          includeZero = true,
        ))
      )
      val etl = new TraceLogger{override def doTrace: Bool = false}
      val ltl = new TraceLogger{override def doTrace: Bool = false}
      val dtl = new TraceLogger{override def doTrace: Bool = false}
      // val ltl = new TraceLogger{override def doTrace: Bool = true}
      val rtl = new TraceLogger{override def doTrace: Bool = false}


      val mainParse =
        given CompilerCtx = this
        ParserSetup(file, dbgParsing = false)
      // given Elaborator.Ctx = prelude.copy(mode = Mode.Light).nestLocal("prelude")
      val artifactCtx = prelude
      given Elaborator.Ctx = artifactCtx
      val elab =
        given CompilerCtx = derive(mainParse.origin.fileName)
        Elaborator(tl, file.up, prelude)

      // val elab = Elaborator(etl, wd, newCtx)
      val parsed = mainParse.resultBlk
      val nme = file.baseName
      val exportedSymbol = parsed.definedSymbols.find(_._1 === nme).map(_._2)
      state.initializeCompilationUnit(mainParse.origin, exportedSymbol)
      def collectCompilationUnitSymbols(program: codegen.Program): Set[BlockMemberSymbol] =
        program.main match
        case codegen.Scoped(syms, _) =>
          syms.iterator.collect:
            case sym: BlockMemberSymbol => sym
          .toSet
        case _ => Set.empty
      val (blk0, _) = elab.importFrom(parsed)
      paths.foreach: compilerPaths =>
        if file.toString === compilerPaths.runtimeSourceFile.toString then
          state.initRuntimeSymbolsFromBlock(blk0)
        else
          state.initRuntimeSymbolsFromFile(compilerPaths.runtimeSourceFile, prelude)(using tl, summon[Raise], compilationUnitConfig, this)

      val artifactConfig = Config.extractConfigFromStats(blk0)
      state.compilationUnitConfig = S(artifactConfig)
      artifactConfig.givenIn:
        given Elaborator.State = state
        val resolver = Resolver(rtl)
        resolver.traverseBlock(blk0)(using Resolver.ICtx.empty)

      // Imported compilation units are lowered even in worksheet mode so their
      // symbols carry IR definitions that the caller's inliner can inspect.
      if paths.isEmpty then
        artifactConfig.givenIn:
          given Elaborator.State = state
          val low = ltl.givenIn:
            new codegen.Lowering()(using artifactConfig, ltl, summon[Raise], state, artifactCtx, summon[SymbolPrinter])
              with codegen.LoweringSelSanityChecks
          low.program(blk0, Set.empty)
      val ir = paths.map: compilerPaths =>
        artifactConfig.givenIn:
          given Elaborator.State = state
          def findQuote(t: semantics.Statement): Bool = t match
            case Term.Quoted(_) | Term.Unquoted(_) => true
            case Term.Ref(sym) => sym === State.termSymbol
            case _ => t.subTerms.exists(findQuote)
          val hasQuote = findQuote(blk0)
          val blk = new Term.Blk(
            Import(State.runtimeSymbol, compilerPaths.runtimeFile.toString, compilerPaths.runtimeFile) ::
              // Only import `Term.mls` when necessary.
              (if hasQuote then
                Import(State.termSymbol, compilerPaths.termFile.toString, compilerPaths.termFile) :: blk0.stats
              else
                blk0.stats),
            blk0.res
          )
          state.noteImportedModule(State.runtimeSymbol, compilerPaths.runtimeFile.toString)
          if hasQuote then state.noteImportedModule(State.termSymbol, compilerPaths.termFile.toString)
          val low = ltl.givenIn:
            new codegen.Lowering()(using artifactConfig, ltl, summon[Raise], state, artifactCtx, summon[SymbolPrinter])
              with codegen.LoweringSelSanityChecks
          val jsb = ltl.givenIn:
            codegen.js.JSBuilder()
          val lowered = low.program(blk, Set.empty)
          val compilationUnitSymbols = collectCompilationUnitSymbols(lowered)
          var optimized = lowered
          val symbolsToPreserve: Set[codegen.BoundSymbol] =
            compilationUnitSymbols.toSet[codegen.BoundSymbol] ++ exportedSymbol.toSet
          optimized =
            val printer = (p: codegen.Program) => p.showAsTree // TODO: proper printing like in diff-tests
            optimized = codegen.WorkerWrapper(symbolsToPreserve, dtl, printer)(optimized)
            codegen.BlockSimplifier(symbolsToPreserve, dtl, printer)(optimized)
          ltl.givenIn:
            optimized = codegen.DeadParamElim(optimized)
          optimized

      val loweredPaths = paths.map(p => p.runtimeFile -> p.termFile)
      Artifact(parsed, blk0, ir, artifactConfig, artifactCtx, state, loweredPaths, compilationUnitConfig, lastMod)
    
    cache.upsert(file):
      case N => mk
      case cur @ S(art) =>
        val requestedLowering = paths.map(p => p.runtimeFile -> p.termFile)
        if art.lastChangedTimestamp < lastMod
          || art.rootConfig =/= compilationUnitConfig
          || requestedLowering.exists(rp => art.ir.isEmpty || art.loweredPaths =/= S(rp))
        then mk
        else art

  def getPrelude
        (file: io.Path, dbgParsing: Bool)
        (using tl: TL, raise: Raise, config: Config)
        : PreludeArtifact =
    // The prelude context is shared so every compilation unit sees the same prelude
    // symbols. Callers still elaborate their own files with a fresh State; the frozen
    // State remains the owner captured by the prelude symbols themselves.
    val lastMod = fs.getLastChangedTimestamp(file)
    cache.upsertPrelude(file):
      case cur @ S(art) if !dbgParsing && art.lastChangedTimestamp >= lastMod && art.config === config =>
        art
      case _ =>
        val state = new Elaborator.State
        given Elaborator.State = state
        given CompilerCtx = this
        val parse = ParserSetup(file, dbgParsing)
        val elab = Elaborator(tl, file.up, Ctx.empty)
        val initCtx = State.init.nestLocal("prelude")
        val (blk, ctx) = elab.importFrom(parse.resultBlk)(using initCtx)
        PreludeArtifact(parse.resultBlk, blk, ctx, state, config, lastMod)
  
  
object CompilerCtx:
  
  inline def get(using cctx: CompilerCtx) = cctx
  
  def fresh(fs: io.FileSystem): CompilerCtx = CompilerCtx(N, Set.empty, fs, new PlatformCompilerCache, N, N)
  
end CompilerCtx



object CompilerCache:
  
  class Artifact(
    val tree: syntax.Tree.Block,
    val term: semantics.Term.Blk,
    val ir: Opt[codegen.Program],
    val config: Config,
    val ctx: Elaborator.Ctx,
    val state: Elaborator.State,
    val loweredPaths: Opt[(io.Path, io.Path)],
    val rootConfig: Config,
    val lastChangedTimestamp: Long,
  )

  class PreludeArtifact(
    val tree: syntax.Tree.Block,
    val term: semantics.Term.Blk,
    val ctx: Elaborator.Ctx,
    val state: Elaborator.State,
    val config: Config,
    val lastChangedTimestamp: Long,
  )
  
end CompilerCache


trait CompilerCache:
  // TODO also use hash comparison to avoid needless re-parses?
  
  def elabCache: MutMap[io.Path, Artifact]

  private val preludeCache: MutMap[io.Path, PreludeArtifact] = MutMap.empty
  
  /** Create or update an artifact at the given path in the cache. */
  def upsert(path: io.Path)(update: Option[Artifact] => Artifact): Artifact =
    elabCache
      .updateWith(path):
        case N => S(update(N))
        case cur @ S(oldArt) =>
          val newArt = update(cur)
          if newArt is oldArt then cur else S(newArt)
      .get // * above, we always returns Some

  /** Synchronized because diff-test and compile-test runners compile files in parallel.
    *
    * Only the first requester elaborates the prelude; concurrent requesters block and reuse
    * the same frozen artifact once it is available. */
  def upsertPrelude(path: io.Path)(update: Option[PreludeArtifact] => PreludeArtifact): PreludeArtifact =
    this.synchronized:
      preludeCache
        .updateWith(path):
          case N => S(update(N))
          case cur @ S(oldArt) =>
            val newArt = update(cur)
            if newArt is oldArt then cur else S(newArt)
        .get
  
end CompilerCache
