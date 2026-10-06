import { run, bench, boxplot, summary } from 'mitata';

import SimpleFib from "../../SimpleFib.mjs"
import InterpreterFib from "../out/InterpreterFib.mjs"
import SpecialFib from "../out/SpecialFib.mjs"

const size = 1;
boxplot(() => {
  summary(() => {
    bench('Fib(25)', () => {
      for (let i = 0; i < size; ++i) {
        SimpleFib.mkFib(25);
      }
    });

    bench('Staged Fib(25)', () => {
      for (let i = 0; i < size; ++i) {
        InterpreterFib.mkFib(25);
      }
    });
  });
});

// ---------------------------------------------------------------------------
// The interpreted program chosen at run time.
//
// `mkFib` names one program, so `InterpreterFib` specializes the interpreter
// away against it.  `runProgram(k, n)` picks one of three programs instead,
// which leaves the instruction list an ordinary value -- `InterpreterFib` then
// has to interpret it, while `SpecialFib` marks the candidates `@special` and
// keeps one specialized interpreter per program.
// ---------------------------------------------------------------------------

const programArg = 21;

const runPrograms = (m) => {
  let acc = 0;
  for (let k = 0; k < 3; ++k) acc += m.runProgram(k, programArg).x;
  return acc;
};

// Guard against measuring three different computations, per program.
for (let k = 0; k < 3; ++k) {
  const a = SimpleFib.runProgram(k, 14).x;
  const b = InterpreterFib.runProgram(k, 14).x;
  const c = SpecialFib.runProgram(k, 14).x;
  if (a !== b || a !== c) throw new Error(`runProgram(${k}): implementations disagree`);
}
// ... and that the dispatch is not collapsed: the three programs compute
// different functions, so their results must differ.
{
  const outs = [0, 1, 2].map((k) => SpecialFib.runProgram(k, 14).x);
  if (new Set(outs).size !== 3) throw new Error("SpecialFib.runProgram: dispatch collapsed");
}

boxplot(() => {
  summary(() => {
    bench('SimpleFib.runProgram(3 programs)', () => { runPrograms(SimpleFib); });
    bench('InterpreterFib.runProgram(3 programs)', () => { runPrograms(InterpreterFib); });
    bench('SpecialFib.runProgram(3 programs)', () => { runPrograms(SpecialFib); });
  });
});

await run();
