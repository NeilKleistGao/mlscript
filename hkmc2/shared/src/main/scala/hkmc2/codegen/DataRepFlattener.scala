package hkmc2
package codegen

import hkmc2.utils.*, shorthands.*
import utils.*

import semantics.*
import flowAnalysis.*

import hkmc2.semantics.Elaborator.State

class DataRepFlattener()(using State) extends BlockTransformer(SymbolSubst.Id)


object DataRepFlattener:
  def apply(p: Program)(using
    cfg: Config,
    tl: TL,
    raise: Raise,
    eState: State,
    symbolPrinter: SymbolPrinter,
  ): Program =
    cfg.dataRepFlatten match
      case N => p
      case S(dCfg) =>
        val flowCfg = Config.FlowAnalysisConfig(
          debug = false,
          mono = dCfg.mono,
          trackNonAffine = false,
          trackAccumulator = false,
          logNonAffine = false,
          logAccumulator = false,
        )
        val flowAnalysisRes =
          FlowAnalysis.mkTraceLogger(flowCfg, "data-rep-flatten > ", tl).givenIn:
            FlowAnalysis(
              p,
              mono = flowCfg.mono,
              nonAffineTracking = false,
              accumulatorTracking = false,
            )
        // TODO: build web
        new DataRepFlattener().applyProgram(p)
