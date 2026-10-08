import { run, bench, boxplot, summary } from 'mitata';

import NaivePipeline from "../../NaivePipeline.mjs"
import Pipeline from "../out/Pipeline.mjs"
import SpecialPipeline from "../out/SpecialPipeline.mjs"

// ---------------------------------------------------------------------------
// The workhorse `foreach`-in-generator benchmark.
//
// A pipeline is a tree of generator combinators ending in a sink, so each
// element pays one virtual `accept` per stage; each stage's payload is itself
// an interpreted expression tree, so each element also pays one virtual `eval`
// per node.  The driving loop counts down from a run-time length, so
// specializing the pipeline never means unrolling the data.
// ---------------------------------------------------------------------------

const size = Number(process.env.PIPE_SIZE ?? 1000000);
// Two passes, so the statically-known pipeline also lands in the 100ms-1s band.
const staticReps = 2;
const input = Array.from({ length: size }, (_, i) => (i * 37 + 11) % 999983);

// Guard against measuring three different computations.
{
  const small = Array.from({ length: 1000 }, (_, i) => (i * 37 + 11) % 999983);
  const a = NaivePipeline.pipe0(small);
  if (Pipeline.pipe0(small) !== a || SpecialPipeline.pipe0(small) !== a)
    throw new Error("pipe0: implementations disagree");
  for (let k = 0; k < 3; ++k) {
    const x = NaivePipeline.runPipe(k, small);
    if (Pipeline.runPipe(k, small) !== x || SpecialPipeline.runPipe(k, small) !== x)
      throw new Error(`runPipe(${k}): implementations disagree`);
  }
  // ... and that the dispatch is not collapsed: the three pipelines compute
  // different sums, so their results must differ.
  const outs = [0, 1, 2].map((k) => SpecialPipeline.runPipe(k, small));
  if (new Set(outs).size !== 3) throw new Error("SpecialPipeline.runPipe: dispatch collapsed");
}

// ---------------------------------------------------------------------------
// The pipeline named at the call site.  `pipe0` is statically known, so the
// plain staged module already fuses it; `@special` has nothing to add here.
// ---------------------------------------------------------------------------

boxplot(() => {
  summary(() => {
    bench(`NaivePipeline.pipe0(${staticReps}x${size})`, () => {
      for (let r = 0; r < staticReps; ++r) NaivePipeline.pipe0(input);
    });
    bench(`Pipeline.pipe0(${staticReps}x${size})`, () => {
      for (let r = 0; r < staticReps; ++r) Pipeline.pipe0(input);
    });
  });
});

// ---------------------------------------------------------------------------
// The pipeline chosen at run time.
//
// `runPipe(k, arr)` picks one of three pipelines.  `Pipeline` cannot specialize
// past that choice, so it walks the combinator tree and interprets every `Expr`
// node for every element; `SpecialPipeline` marks the pipeline nodes `@special`
// and gets one fused loop per pipeline plus a dispatch on `k`.
// ---------------------------------------------------------------------------

const runPipes = (m) => {
  let acc = 0;
  for (let k = 0; k < 3; ++k) acc += m.runPipe(k, input);
  return acc;
};

boxplot(() => {
  summary(() => {
    bench(`NaivePipeline.runPipe(3 pipelines, ${size})`, () => { runPipes(NaivePipeline); });
    bench(`Pipeline.runPipe(3 pipelines, ${size})`, () => { runPipes(Pipeline); });
    bench(`SpecialPipeline.runPipe(3 pipelines, ${size})`, () => { runPipes(SpecialPipeline); });
  });
});

await run();
