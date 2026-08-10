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
    /** Where the prelude and runtime of this compilation session live. Fixed alongside
      * `rootConfig`, and for the same reason: they take part in lowering — the lowered program
      * imports the runtime by path — so a unit lowered against different ones is a different
      * artifact, and the units cached here are shared. */
    val paths: MLsCompiler.Paths,
    /** The configuration of the whole compilation session, under which every compilation unit
      * reached through this context is elaborated. It is fixed when the context is created:
      * the units cached here are shared, so their elaboration — and hence the identity of the
      * symbols they define — must not depend on which of them is compiled or imported first. */
    val rootConfig: Config,
):
  
  def allFilesBeingImported: Ls[io.Path] =
    importing match
    case S((path, parent)) => path :: parent.allFilesBeingImported
    case N => Nil
  
  def derive(newFile: io.Path): CompilerCtx =
    CompilerCtx(S(newFile, this), beingCompiled + newFile, fs, cache, paths, rootConfig)
  
  /** Elaborate (and, when compiler paths are set, lower) a compilation unit, caching the result.
    *
    * Note that the importer's configuration is deliberately not a parameter: a compilation unit's
    * elaboration must depend only on the unit itself, or importers would keep invalidating each
    * other's cached artifact — and a single worksheet could then observe symbols from two different
    * elaborations of the same file (say, `State.tupleSymbol` from one and the symbols reached
    * through an `import` statement from another), which is silently inconsistent. */
  def getElaboratedBlock
        (file: io.Path, prelude: Ctx)
        (using TL, Raise)
        : Artifact =
    
    // println(s"Cache has: ${cache.elabCache.contains(file)} ${cache.elabCache.keys}")
    
    val lastMod = fs.getLastChangedTimestamp(file)
    
    def mk =
      val state = new Elaborator.State
      given Elaborator.State = state

      // * Later, we can draw this from a global root configuration,
      // * which is set for a whole application.
      given Config = rootConfig

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
      if file.toString === paths.runtimeSourceFile.toString then
        state.initRuntimeSymbolsFromBlock(blk0)
      else
        state.initRuntimeSymbolsFromFile(paths.runtimeSourceFile, prelude)(using tl, summon[Raise], this)

      val artifactConfig = Config.extractConfigFromStats(blk0)
      state.compilationUnitConfig = S(artifactConfig)
      artifactConfig.givenIn:
        given Elaborator.State = state
        val resolver = Resolver(rtl)
        resolver.traverseBlock(blk0)(using Resolver.ICtx.empty)

      // Runs the compilation pipeline on a freshly lowered compilation unit.
      // The unit's own top-level symbols are preserved as a private ABI, because other
      // compilation units may inline bodies of this one that still refer to them.
      // Note that this must run the *whole* pipeline (and not just the lowering proper),
      // as the `irDefn` fields the inliner reads are owned by the latest rewrite of each definition,
      // and only fully-transformed definitions are valid to splice into another unit.
      def optimize(lowered: codegen.Program): codegen.Program =
        given Config = artifactConfig
        val printer = (p: codegen.Program) => p.showAsTree // TODO: proper printing like in diff-tests
        val pipeline = new codegen.CompilationPipeline:
          override def extraSymbolsToPreserve(prog: codegen.Program): Set[codegen.BoundSymbol] =
            collectCompilationUnitSymbols(prog).toSet
        ltl.givenIn:
          pipeline.run(lowered, printer, exportedSymbol.toSet, dtl)

      // Every compilation unit is lowered, so that its symbols carry the IR definitions an
      // importer's inliner may splice in — and so that the importer never has to lower it itself,
      // which it could only do by elaborating it afresh, under symbols nobody else would share.
      val ir =
        artifactConfig.givenIn:
          given Elaborator.State = state
          def findQuote(t: semantics.Statement): Bool = t match
            case Term.Quoted(_) | Term.Unquoted(_) => true
            case Term.Ref(sym) => sym === State.termSymbol
            case _ => t.subTerms.exists(findQuote)
          val hasQuote = findQuote(blk0)
          val blk = new Term.Blk(
            Import(State.runtimeSymbol, paths.runtimeFile.toString, paths.runtimeFile) ::
              // Only import `Term.mls` when necessary.
              (if hasQuote then
                Import(State.termSymbol, paths.termFile.toString, paths.termFile) :: blk0.stats
              else
                blk0.stats),
            blk0.res
          )
          state.noteImportedModule(State.runtimeSymbol, paths.runtimeFile.toString)
          if hasQuote then state.noteImportedModule(State.termSymbol, paths.termFile.toString)
          val low = ltl.givenIn:
            new codegen.Lowering()(using artifactConfig, ltl, summon[Raise], state, artifactCtx, summon[SymbolPrinter])
          optimize(low.program(blk, Set.empty))

      Artifact(parsed, blk0, ir, artifactConfig, artifactCtx, state, rootConfig, lastMod)
    
    cache.upsert(file)(
      isCurrent = (cachedFile, art) =>
        // * The root configuration is fixed for a whole compilation session — nothing mutates it —
        // * so a cached artifact was necessarily built with the one we are asking for now.
        // * A mismatch means two contexts with different root configurations share a cache, which
        // * would give the same source file two elaborations and hence two sets of symbols.
        // * We keep the cached artifact rather than re-elaborating: reusing one identity is the
        // * lesser evil, and the diagnostic makes the misuse visible instead of silent.
        softAssert(art.rootConfig === rootConfig,
          s"Cached artifact for $cachedFile was elaborated under a different root configuration")
        try art.lastChangedTimestamp >= fs.getLastChangedTimestamp(cachedFile)
        catch case _: io.FileSystem.FileNotFoundException => false,
      create = mk,
    )

  def getPrelude
        (file: io.Path, dbgParsing: Bool)
        (using tl: TL, raise: Raise)
        : PreludeArtifact =
    // The prelude context is shared so every compilation unit sees the same prelude
    // symbols. Callers still elaborate their own files with a fresh State; the frozen
    // State remains the owner captured by the prelude symbols themselves.
    // The prelude is elaborated once per context, under its root configuration: were it
    // elaborated per requester, cached compilation units would keep referring to whichever
    // elaboration came first, and a body inlined across units would then carry prelude symbols
    // that the importing file does not recognize.
    val lastMod = fs.getLastChangedTimestamp(file)
    cache.upsertPrelude(file)(
      isCurrent = art =>
        // * See the corresponding assertion in `getElaboratedBlock` above.
        softAssert(art.config === rootConfig,
          s"Cached prelude for $file was elaborated under a different root configuration")
        !dbgParsing && art.lastChangedTimestamp >= lastMod,
      create =
        val state = new Elaborator.State
        given Elaborator.State = state
        given Config = rootConfig
        given CompilerCtx = this
        val parse = ParserSetup(file, dbgParsing)
        val elab = Elaborator(tl, file.up, Ctx.empty)
        val initCtx = State.init.nestLocal("prelude")
        val (blk, ctx) = elab.importFrom(parse.resultBlk)(using initCtx)
        PreludeArtifact(parse.resultBlk, blk, ctx, state, rootConfig, lastMod),
    )
  
  
object CompilerCtx:
  
  inline def get(using cctx: CompilerCtx) = cctx
  
  def fresh(fs: io.FileSystem, paths: MLsCompiler.Paths, rootConfig: Config): CompilerCtx =
    CompilerCtx(N, Set.empty, fs, new PlatformCompilerCache, paths, rootConfig)
  
end CompilerCtx



object CompilerCache:
  
  class Artifact(
    val tree: syntax.Tree.Block,
    val term: semantics.Term.Blk,
    val ir: codegen.Program,
    val config: Config,
    val ctx: Elaborator.Ctx,
    val state: Elaborator.State,
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
  
  /** Return the current artifact at `path`, or atomically invalidate and rebuild the cache.
    *
    * Synchronized for the same reason as `upsertPrelude` below, and additionally because the
    * artifact's symbols are shared: without atomicity, concurrent requesters each elaborate the
    * file and each walk away with a *different* set of symbols for it, only one of which is
    * retained in the cache. A later requester then disagrees with an earlier one about the
    * identity of the very same source-level definition. Elaboration is reentrant here — `create`
    * elaborates imports, which come back through this method on the same thread.
    *
    * Replacing one artifact invalidates every cached dependent: their terms and IR refer to the
    * old artifact's symbols, and private-ABI export names incorporate those symbol identities. */
  def upsert(path: io.Path)(isCurrent: (io.Path, Artifact) => Bool, create: => Artifact): Artifact =
    this.synchronized:
      // A requester may itself be unchanged while one of the imported artifacts captured in its
      // term/IR has changed. Check all published identities before returning any one of them.
      if elabCache.exists((cachedPath, art) => !isCurrent(cachedPath, art)) then
        elabCache.clear()
      elabCache.get(path) match
      case S(art) => art
      case N =>
        val art = create
        elabCache(path) = art
        art

  /** Synchronized because diff-test and compile-test runners compile files in parallel.
    *
    * Only the first requester elaborates the prelude; concurrent requesters block and reuse
    * the same frozen artifact once it is available. */
  def upsertPrelude(path: io.Path)(isCurrent: PreludeArtifact => Bool, create: => PreludeArtifact): PreludeArtifact =
    this.synchronized:
      preludeCache.get(path) match
      case S(art) if isCurrent(art) => art
      case stale =>
        // Every elaborated artifact captures symbols from this frozen prelude.
        if stale.nonEmpty then elabCache.clear()
        val art = create
        preludeCache(path) = art
        art
  
end CompilerCache
