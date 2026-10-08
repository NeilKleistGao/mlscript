import { run, bench, boxplot, summary } from 'mitata';

import NaiveStateMachine from "../../NaiveStateMachine.mjs"
import StateMachine from "../out/StateMachine.mjs"
import SpecialStateMachine from "../out/SpecialStateMachine.mjs"

// ---------------------------------------------------------------------------
// The stepping-state-machine benchmark.
//
// Each step resolves the next state by name against the state list, scans the
// state's guarded edges, applies a virtual action and allocates a pair plus a
// triple.  Nine `Guard` classes and eight `Act` classes reach the same two call
// sites, so neither fits in a polymorphic inline cache.  When the machine is
// statically known, the table collapses into code.
// ---------------------------------------------------------------------------

const size = Number(process.env.FSM_SIZE ?? 1000000);
// Three passes, so the statically-known machine also lands near the 100ms-1s band.
const staticReps = 3;
const input = Array.from({ length: size }, (_, i) => (i * 101 + 7) % 256);

// Guard against measuring three different computations.
{
  const small = Array.from({ length: 2000 }, (_, i) => (i * 101 + 7) % 256);
  const a = NaiveStateMachine.run0(small);
  if (StateMachine.run0(small) !== a || SpecialStateMachine.run0(small) !== a)
    throw new Error("run0: implementations disagree");
  for (let k = 0; k < 3; ++k) {
    const x = NaiveStateMachine.runSel(k, small);
    if (StateMachine.runSel(k, small) !== x || SpecialStateMachine.runSel(k, small) !== x)
      throw new Error(`runSel(${k}): implementations disagree`);
  }
  // ... and that the dispatch is not collapsed: the three machines compute
  // different checksums, so their results must differ.
  const outs = [0, 1, 2].map((k) => SpecialStateMachine.runSel(k, small));
  if (new Set(outs).size !== 3) throw new Error("SpecialStateMachine.runSel: dispatch collapsed");
}

// ---------------------------------------------------------------------------
// The machine named at the call site.  `run0` is statically known, so the plain
// staged module already compiles it; `@special` has nothing to add here.
// ---------------------------------------------------------------------------

boxplot(() => {
  summary(() => {
    bench(`NaiveStateMachine.run0(${staticReps}x${size})`, () => {
      for (let r = 0; r < staticReps; ++r) NaiveStateMachine.run0(input);
    });
    bench(`StateMachine.run0(${staticReps}x${size})`, () => {
      for (let r = 0; r < staticReps; ++r) StateMachine.run0(input);
    });
  });
});

// ---------------------------------------------------------------------------
// The machine chosen at run time.
//
// `runSel(k, arr)` picks one of three machines.  `StateMachine` cannot
// specialize past that choice, so it interprets the table; `SpecialStateMachine`
// marks the table nodes `@special` and gets one compiled machine per
// alternative plus a dispatch on `k`.
// ---------------------------------------------------------------------------

const runAll = (m) => {
  let acc = 0;
  for (let k = 0; k < 3; ++k) acc += m.runSel(k, input);
  return acc;
};

boxplot(() => {
  summary(() => {
    bench(`NaiveStateMachine.runSel(3 machines, ${size})`, () => { runAll(NaiveStateMachine); });
    bench(`StateMachine.runSel(3 machines, ${size})`, () => { runAll(StateMachine); });
    bench(`SpecialStateMachine.runSel(3 machines, ${size})`, () => { runAll(SpecialStateMachine); });
  });
});

await run();
