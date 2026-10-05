// Sparse matrix-vector product over a known sparsity pattern.
//
// Two separate comparisons:
//
//   spmvTri128    -- the entry point names one pattern, so plain staging can
//                    already unroll it and `@special` has nothing to add: the
//                    functions reached from `spmv..._sp_*` are the same in
//                    both modules up to specialization numbering, so any gap
//                    between those two bars is noise, not a real difference.
//   spmvStencil   -- the pattern is picked at run time out of a fixed menu.
//                    Plain staging has to walk the spine here; the `@special`
//                    version still specializes each alternative.
//
// Each measured iteration does `REPS` products of a 128x128 tridiagonal matrix
// (382 nonzeros), which puts every bar in the few-hundred-millisecond range --
// long enough that per-iteration overhead and GC pauses average out instead of
// dominating the result.
import { run, bench, boxplot, summary } from 'mitata';

import NaiveSparseTensor from "../../NaiveSparseTensor.mjs"
import StagedSparseTensor from "../out/StagedSparseTensor.mjs"
import SpecialSparseTensor from "../out/SpecialSparseTensor.mjs"

const REPS = 65000;

const iota = (n) => Array.from({ length: n }, (_, i) => i + 1);

// 2 + 126*3 + 2 = 382 nonzeros over 128 rows.
const valsTri128 = iota(382);
const x128 = iota(128);

// 2 + 62*3 + 2 = 190 nonzeros over 64 rows.
const valsTri64 = iota(190);
const x64 = iota(64);

// `pat45`: 7 nonzeros over 4 rows of 5 columns.
const vals45 = [1, 2, 3, 4, 5, 6, 7];
const x5 = [1, 2, 3, 4, 5];

// A fixed, repeatable sequence of stencil choices, so that every
// implementation sees the same mix and the branch is not trivially predicted
// into one target.
const picks = Array.from({ length: REPS }, (_, i) => i % 3);
const stencilVals = [vals45, valsTri64, valsTri128];
const stencilX = [x5, x64, x128];

// The whole result vector is summed, not just one entry, so that no part of
// the product can be dropped as dead.
function sum(v) {
  let t = 0;
  for (let i = 0; i < v.length; i++) t += v[i];
  return t;
}

function fixed(m) {
  let acc = 0;
  for (let i = 0; i < REPS; i++) acc += sum(m.spmvTri128(valsTri128, x128));
  return acc;
}

function stencil(m) {
  let acc = 0;
  for (let i = 0; i < REPS; i++) {
    const k = picks[i];
    acc += sum(m.spmvStencil(k, stencilVals[k], stencilX[k]));
  }
  return acc;
}

// Guard against measuring three different computations.
for (const [name, f] of [["fixed", fixed], ["stencil", stencil]]) {
  const a = f(NaiveSparseTensor), b = f(StagedSparseTensor), c = f(SpecialSparseTensor);
  if (a !== b || a !== c)
    throw new Error(`${name}: implementations disagree: ${a} ${b} ${c}`);
}

boxplot(() => {
  summary(() => {
    bench('NaiveSparseTensor.spmvTri128', () => { fixed(NaiveSparseTensor); });
    bench('StagedSparseTensor.spmvTri128', () => { fixed(StagedSparseTensor); });
    bench('SpecialSparseTensor.spmvTri128', () => { fixed(SpecialSparseTensor); });
  });
});

boxplot(() => {
  summary(() => {
    bench('NaiveSparseTensor.spmvStencil', () => { stencil(NaiveSparseTensor); });
    bench('StagedSparseTensor.spmvStencil', () => { stencil(StagedSparseTensor); });
    bench('SpecialSparseTensor.spmvStencil', () => { stencil(SpecialSparseTensor); });
  });
});

await run();
