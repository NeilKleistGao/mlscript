import { run, bench, boxplot, summary } from 'mitata';

import SimpleFib from "../../SimpleFib.mjs"
import InterpreterFib from "../out/InterpreterFib.mjs"

// Both implementations run the same fixed Fibonacci instruction list.
const programArg = 25;

for (const n of [0, 1, 7, 14]) {
  const a = SimpleFib.mkFib(n).x;
  const b = InterpreterFib.mkFib(n).x;
  if (a !== b) throw new Error(`mkFib(${n}): implementations disagree ${a}, ${b}`);
}

boxplot(() => {
  summary(() => {
    bench(`SimpleFib.mkFib(${programArg})`, () => { return SimpleFib.mkFib(programArg).x; });
    bench(`InterpreterFib.mkFib(${programArg})`, () => { return InterpreterFib.mkFib(programArg).x; });
  });
});

await run();
