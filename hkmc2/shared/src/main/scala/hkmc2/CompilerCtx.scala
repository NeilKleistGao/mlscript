package hkmc2

import scala.collection.mutable
import scala.annotation.tailrec
import collection.mutable.Map as MutMap

import mlscript.utils.*, shorthands.*
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
):
  
  def allFilesBeingImported: Ls[io.Path] =
    importing match
    case S((path, parent)) => path :: parent.allFilesBeingImported
    case N => Nil
  
  def derive(newFile: io.Path): CompilerCtx =
    CompilerCtx(S(newFile, this), beingCompiled + newFile, fs, cache, paths)

  def withPaths(newPaths: MLsCompiler.Paths): CompilerCtx =
    CompilerCtx(importing, beingCompiled, fs, cache, S(newPaths))
  
  def getElaboratedBlock
        (file: io.Path, prelude: Ctx, importerCfg: Config)
        (using TL, Raise)
        : Artifact =
    
    // println(s"Cache has: ${cache.elabCache.contains(file)} ${cache.elabCache.keys}")
    
    // * FIXME:
    // * This is not quite correct, but might be good enough for now
    // * (to be fixed when we overhaul the symbol and elaboration systems).
    // * The problem is that different modules will see different builtin symbols
    // * for things like `unitSymbol` and `termSymbol`, which could in theory cause problems
    // * if the compiler later wants to compare them as part of the type checking/compilation/optimization logic.
    // * Technically, we should also have the same problem with the symbols loaded from the prelude,
    // * which are passed on to imported modules from the first importer
    // * (and the "first importer" is nondeterministic, due to concurrent tests),
    // * and since the imported modules are cached,
    // * this means subsequent importers will not have see same prelude symbols.
    // * The correct approach should be to only cache a *single* State and prelude Ctx at the start,
    // * and reuse it for every compilation unit (each compilation unit duplicating the root State).
    val state = new Elaborator.State
    given Elaborator.State = state
    
    val lastMod = fs.getLastChangedTimestamp(file)
    
    def mk =
      // * Later, we can draw this from a global root configuration,
      // * which is set for a whole application.
      given Config = importerCfg
      
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
      val (blk0, _) = elab.importFrom(parsed)
      
      val artifactConfig = Config.extractConfigFromStats(blk0)
      val ir = paths.map: compilerPaths =>
        artifactConfig.givenIn:
          val resolver = Resolver(rtl)
          resolver.traverseBlock(blk0)(using Resolver.ICtx.empty)
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
          val low = ltl.givenIn:
            new codegen.Lowering()
              with codegen.LoweringSelSanityChecks
          val jsb = ltl.givenIn:
            codegen.js.JSBuilder()
          val lowered = low.program(blk)
          var optimized = lowered
          val nme = file.baseName
          val exportedSymbol = parsed.definedSymbols.find(_._1 === nme).map(_._2)
          optimized =
            val printer = (p: codegen.Program) => p.showAsTree // TODO: proper printing like in diff-tests
            optimized = codegen.WorkerWrapper(exportedSymbol.toSet, dtl, printer)(optimized)
            codegen.BlockSimplifier(exportedSymbol.toSet, dtl, printer)(optimized)
          ltl.givenIn:
            optimized = codegen.DeadParamElim(optimized)
          optimized

      val loweredPaths = paths.map(p => p.runtimeFile -> p.termFile)
      Artifact(parsed, blk0, ir, artifactConfig, artifactCtx, state, loweredPaths, lastMod)
    
    cache.upsert(file):
      case N => mk
      case cur @ S(art) =>
        val requestedLowering = paths.map(p => p.runtimeFile -> p.termFile)
        if art.lastChangedTimestamp < lastMod
          || requestedLowering.exists(rp => art.ir.isEmpty || art.loweredPaths =/= S(rp))
        then mk
        else art
  
  
object CompilerCtx:
  
  inline def get(using cctx: CompilerCtx) = cctx
  
  def fresh(fs: io.FileSystem): CompilerCtx = CompilerCtx(N, Set.empty, fs, new PlatformCompilerCache, N)
  
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
    val lastChangedTimestamp: Long,
  )
  
end CompilerCache


trait CompilerCache:
  // TODO also use hash comparison to avoid needless re-parses?
  
  def elabCache: MutMap[io.Path, Artifact]
  
  /** Create or update an artifact at the given path in the cache. */
  def upsert(path: io.Path)(update: Option[Artifact] => Artifact): Artifact =
    elabCache
      .updateWith(path):
        case N => S(update(N))
        case cur @ S(oldArt) =>
          val newArt = update(cur)
          if newArt is oldArt then cur else S(newArt)
      .get // * above, we always returns Some
  
end CompilerCache



