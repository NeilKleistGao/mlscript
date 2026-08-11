package hkmc2

import collection.mutable.{Map as MutMap, Set as MutSet}

import hkmc2.utils.*, shorthands.*
import hkmc2.io
import utils.TraceLogger

import semantics.*
import Elaborator.*


import CompilerCache.*


class CompilerCtx(
    val importing: Opt[(io.Path, CompilerCtx)],
    val beingCompiled: Set[io.Path],
    val fs: io.FileSystem,
    cache: CompilerCache,
    /** Build-local sink for artifacts requested while elaborating the caller, if any. */
    private val dependencyRecorder: Opt[CompilerCtx.DependencyRecorder],
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
  
  private def derive(newFile: io.Path, recorder: CompilerCtx.DependencyRecorder): CompilerCtx =
    CompilerCtx(S(newFile, this), beingCompiled + newFile, fs, cache, S(recorder), paths, rootConfig)
  
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
    
    val lastMod = fs.getLastChangedTimestamp(file)
    
    def mk =
      val dependencies = new CompilerCtx.DependencyRecorder
      val state = new Elaborator.State
      given Elaborator.State = state

      given Config = rootConfig

      given SymbolPrinter = new SymbolPrinter(
        Scope.empty(Scope.Cfg.default.copy(
          escapeChars = false,
          useSuperscripts = true,
          includeZero = true,
        ))
      )
      val backendTL = new TraceLogger:
        override def doTrace: Bool = false

      val parse =
        given CompilerCtx = this
        ParserSetup(file)
      given Elaborator.Ctx = prelude
      val artifactCtx = derive(parse.origin.fileName, dependencies)
      val elab =
        given CompilerCtx = artifactCtx
        Elaborator(tl, file.up, prelude)

      val parsed = parse.resultBlk
      val nme = file.baseName
      val exportedSymbol = parsed.definedSymbols.find(_._1 === nme).map(_._2)
      state.initializeCompilationUnit(parse.origin, exportedSymbol)
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
        state.initRuntimeSymbolsFromFile(paths.runtimeSourceFile, prelude)(
          using tl, summon[Raise], artifactCtx)

      val artifactConfig = Config.extractConfigFromStats(blk0)
      state.compilationUnitConfig = S(artifactConfig)
      artifactConfig.givenIn:
        given Elaborator.State = state
        val resolver = Resolver(backendTL)
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
        backendTL.givenIn:
          pipeline.run(lowered, printer, exportedSymbol.toSet, backendTL)

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
          val low = backendTL.givenIn:
            new codegen.Lowering()(using artifactConfig, backendTL, summon[Raise], state, prelude, summon[SymbolPrinter])
          optimize(low.program(blk, Set.empty))

      state.initializeCompilationUnitPrivateNames:
        codegen.js.JSBuilder.allocateModulePrivateExportNames(ir)(using state, summon[Raise])
      Artifact(parsed, blk0, ir, artifactConfig, prelude, state, rootConfig, dependencies.result, lastMod)
    
    val artifact = cache.upsert(file)(
      isCurrent = (cachedFile, art) =>
        // * The root configuration is fixed for a whole compilation session — nothing mutates it —
        // * so a cached artifact was necessarily built with the one we are asking for now.
        // * A mismatch means two contexts with different root configurations share a cache, which
        // * would give the same source file two elaborations and hence two sets of symbols.
        // * We keep the cached artifact rather than re-elaborating: reusing one identity is the
        // * lesser evil, and the diagnostic makes the misuse visible instead of silent.
        softAssert(art.rootConfig === rootConfig,
          s"Cached artifact for $cachedFile was elaborated under a different root configuration")
        def sourceIsCurrent(path: io.Path, timestamp: Long): Bool =
          try timestamp >= fs.getLastChangedTimestamp(path)
          catch case _: io.FileSystem.FileNotFoundException => false
        (art.ctx is prelude)
          && sourceIsCurrent(cachedFile, art.lastChangedTimestamp)
          && art.dependencies.forall(dep => sourceIsCurrent(dep.path, dep.lastChangedTimestamp)),
      create = mk,
    )
    dependencyRecorder.foreach(_.note(file, artifact))
    artifact

  def getPrelude
        (file: io.Path)
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
        art.lastChangedTimestamp >= lastMod,
      create =
        val state = new Elaborator.State
        given Elaborator.State = state
        given Config = rootConfig
        given CompilerCtx = this
        val parse = ParserSetup(file)
        val elab = Elaborator(tl, file.up, Ctx.empty)
        val initCtx = State.init.nestLocal("prelude")
        val (blk, ctx) = elab.importFrom(parse.resultBlk)(using initCtx)
        PreludeArtifact(parse.resultBlk, blk, ctx, state, rootConfig, lastMod),
    )
  
  
object CompilerCtx:
  
  inline def get(using cctx: CompilerCtx) = cctx
  
  def fresh(fs: io.FileSystem, paths: MLsCompiler.Paths, rootConfig: Config): CompilerCtx =
    CompilerCtx(N, Set.empty, fs, new PlatformCompilerCache, N, paths, rootConfig)

  private[hkmc2] final class DependencyRecorder:
    // Keep the timestamp in the set element rather than mapping paths to timestamps. If a source
    // changes while one artifact is being built and two branches observe different versions, both
    // versions remain in the snapshot; the older one then makes the artifact immediately stale.
    private val dependencies = MutSet.empty[SourceDependency]

    def note(path: io.Path, artifact: Artifact): Unit =
      dependencies += SourceDependency(path, artifact.lastChangedTimestamp)
      dependencies ++= artifact.dependencies

    def result: Set[SourceDependency] = dependencies.toSet
  
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
    val dependencies: Set[SourceDependency],
    val lastChangedTimestamp: Long,
  )

  /** The version of one transitive source dependency observed while building an artifact. */
  final case class SourceDependency(path: io.Path, lastChangedTimestamp: Long)

  class PreludeArtifact(
    val tree: syntax.Tree.Block,
    val term: semantics.Term.Blk,
    val ctx: Elaborator.Ctx,
    val state: Elaborator.State,
    val config: Config,
    val lastChangedTimestamp: Long,
  )

  /** A cache whose expensive computations are serialized per path rather than globally.
    *
    * The supplied maps provide the platform's concurrency semantics: JVM uses `TrieMap`, while
    * JavaScript uses ordinary mutable maps on its single execution thread. Keeping a stable lock
    * per path makes every successful requester observe the same published artifact — and therefore
    * the same symbol identities — without a cache-wide monitor. */
  private[hkmc2] final class ArtifactCache[A <: AnyRef](
      entries: MutMap[io.Path, A],
      pathLocks: MutMap[io.Path, AnyRef],
  ):
    private def pathLock(path: io.Path): AnyRef =
      pathLocks.getOrElseUpdate(path, new Object)

    def getOrCreate(path: io.Path)(isCurrent: A => Bool, create: => A): A =
      pathLock(path).synchronized:
        entries.get(path) match
        case S(entry) if isCurrent(entry) => entry
        case _ =>
          val entry = create
          entries(path) = entry
          entry
  
end CompilerCache


trait CompilerCache:
  // TODO also use hash comparison to avoid needless re-parses?

  protected def elabCache: CompilerCache.ArtifactCache[Artifact]
  protected def preludeCache: CompilerCache.ArtifactCache[PreludeArtifact]
  
  /** Return the current artifact at `path`, or atomically rebuild that artifact.
    *
    * The artifact records all of its transitive source dependencies, so `isCurrent` can decide
    * whether this particular artifact must be rebuilt without scanning or clearing unrelated
    * cache entries. `create` runs under a lock dedicated to `path`: concurrent requesters reuse
    * one symbol identity for the same file, while unrelated files remain free to elaborate in
    * parallel. Imports recursively acquire their own path locks rather than holding a cache-wide
    * monitor through the entire rebuilding chain. */
  def upsert(path: io.Path)(isCurrent: (io.Path, Artifact) => Bool, create: => Artifact): Artifact =
    elabCache.getOrCreate(path)(isCurrent(path, _), create)

  /** Diff-test and compile-test runners request the Prelude in parallel.
    *
    * Only the first requester for this path elaborates it; concurrent requesters block on that
    * path alone and reuse the same frozen artifact once it is available. */
  def upsertPrelude(path: io.Path)(isCurrent: PreludeArtifact => Bool, create: => PreludeArtifact): PreludeArtifact =
    preludeCache.getOrCreate(path)(isCurrent, create)
  
end CompilerCache
