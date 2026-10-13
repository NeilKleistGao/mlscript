// Sparse matrix-vector product over a fixed 128x128 tridiagonal pattern.
// Each measured iteration does REPS products (382 nonzeros per product).
import { run, bench, boxplot, summary } from 'mitata';

import NaiveSparseTensor from "../../NaiveSparseTensor.mjs"
import StagedSparseTensor from "../out/StagedSparseTensor.mjs"

const REPS = 65000;

const iota = (n) => Array.from({ length: n }, (_, i) => i + 1);

// 2 + 126*3 + 2 = 382 nonzeros over 128 rows.
const valsTri128 = iota(382);
const x128 = iota(128);

// The naive kernel (`spmvTri128` in the naive module) rebuilds the pattern on
// every call and then interprets it, whereas the staged kernel has the same
// pattern resolved at specialization time.

// The whole result vector is summed, not just one entry, so that no part of
// the product can be dropped as dead.
function sum(v) {
  let t = 0;
  for (let i = 0; i < v.length; i++) t += v[i];
  return t;
}

function repeat(spmv) {
  let acc = 0;
  for (let i = 0; i < REPS; i++) acc += sum(spmv());
  return acc;
}

const naiveRebuilt = () => repeat(() => NaiveSparseTensor.spmvTri128(valsTri128, x128));
const staged = () => repeat(() => StagedSparseTensor.spmvTri128(valsTri128, x128));

// Guard against measuring different computations.
{
  const a = naiveRebuilt(), b = staged();
  if (a !== b) throw new Error(`spmvTri128: implementations disagree: ${a} ${b}`);
}

boxplot(() => {
  summary(() => {
    bench('NaiveSparseTensor.spmvTri128(pattern rebuilt per call)', naiveRebuilt);
    bench('StagedSparseTensor.spmvTri128', staged);
  });
});

await run();
