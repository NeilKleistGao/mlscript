import { run, bench, boxplot, summary } from 'mitata';

import NaiveStateMachine from "../../NaiveStateMachine.mjs"
import StateMachine from "../out/StateMachine.mjs"

// ---------------------------------------------------------------------------
// The stepping-state-machine benchmark.
//
// Each step resolves the next state by name against the state list, scans the
// state's guarded edges, applies a virtual action and allocates a pair plus a
// triple.  The guards and actions share virtual call sites.  When the
// machine is statically known, the table collapses into code.
// ---------------------------------------------------------------------------

const size = Number(process.env.FSM_SIZE ?? 1000000);
// Two passes, so that one execution of the naive machine stays under a second.
const staticReps = 2;
const input = Array.from({ length: size }, (_, i) => (i * 101 + 7) % 256);

// Guard against measuring different computations.
{
  const small = Array.from({ length: 2000 }, (_, i) => (i * 101 + 7) % 256);
  const a = NaiveStateMachine.run0(small);
  if (StateMachine.run0(small) !== a)
    throw new Error("run0: implementations disagree");
}

// ---------------------------------------------------------------------------
// The machine named at the call site.  `run0` is statically known, so the plain
// staged module compiles it into state-specific loops.
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

await run();
