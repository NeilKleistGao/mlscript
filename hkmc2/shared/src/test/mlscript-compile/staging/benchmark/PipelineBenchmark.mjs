import { run, bench, boxplot, summary } from 'mitata';

import NaivePipeline from "../../NaivePipeline.mjs"
import Pipeline from "../out/Pipeline.mjs"

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
// Three passes, so that one execution of the naive pipeline takes about 500ms.
const staticReps = 3;
const input = Array.from({ length: size }, (_, i) => (i * 37 + 11) % 999983);

// Guard against measuring different computations.
{
  const small = Array.from({ length: 1000 }, (_, i) => (i * 37 + 11) % 999983);
  const a = NaivePipeline.pipe0(small);
  if (Pipeline.pipe0(small) !== a)
    throw new Error("pipe0: implementations disagree");
}

// ---------------------------------------------------------------------------
// The pipeline named at the call site.  `pipe0` is statically known, so the
// staged module fuses it into direct arithmetic.
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

await run();
