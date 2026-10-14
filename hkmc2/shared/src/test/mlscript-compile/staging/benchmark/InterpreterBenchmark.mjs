import { run, bench, boxplot, summary } from 'mitata';

import SimpleFib from "../../SimpleFib.mjs"
import InterpreterFib from "../out/InterpreterFib.mjs"
import SpecialFib from "../out/SpecialFib.mjs"

// Distinct seeds, so that mixing up the parameters changes the result.
const seedA = 2;
const seedB = 3;

// ---------------------------------------------------------------------------
// The function chosen at run time.
//
// `run(name, args)` defines five functions -- `fib(a, b, n)`, `fact(x)`,
// `square(x)`, `sumSquares(n)` (which calls `square`) and `pow(b, e)` -- and
// calls the one `name` picks on the argument list `args`.
// `InterpreterFib` merges the closures `Call(name)` may find; `SpecialFib`
// marks closures `@special`, so the call dispatches on the closure and each
// branch is specialized against one function's body.
// ---------------------------------------------------------------------------

// The arguments each function is called with.
const runArgs = {
  fib: [seedA, seedB, 15],
  fact: [20],
  square: [12345],
  sumSquares: [200],
  pow: [3, 30],
};
const names = Object.keys(runArgs);
const runs = 600;

// A fixed, repeatable sequence of choices, so every implementation sees the
// same mix and the dispatch is not trivially predicted into one target.
const picks = Array.from({ length: runs }, (_, i) => names[i % names.length]);

// Each module has its own `Cons`, so each gets its own argument lists; they are
// built once, outside the timed loop.
const toList = (m, xs) => xs.reduceRight((rest, x) => m.Cons(x, rest), m.Nil);
const argsFor = (m) =>
  Object.fromEntries(names.map((name) => [name, toList(m, runArgs[name])]));

const runPicks = (m, args = argsFor(m)) => {
  let acc = 0;
  for (let i = 0; i < runs; ++i) {
    const name = picks[i];
    acc += m.run(name, args[name]).x;
  }
  return acc;
};

// Guard against measuring different computations, on every function.
{
  const a = runPicks(SimpleFib), b = runPicks(InterpreterFib), c = runPicks(SpecialFib);
  if (a !== b || a !== c) throw new Error(`run: implementations disagree ${a}, ${b}, ${c}`);
  for (const name of names) {
    const outs = [SimpleFib, InterpreterFib, SpecialFib].map((m) => m.run(name, toList(m, runArgs[name])).x);
    if (new Set(outs).size !== 1 || typeof outs[0] !== "number")
      throw new Error(`run(${name}): implementations disagree ${outs}`);
  }
}

boxplot(() => {
  summary(() => {
    const simpleArgs = argsFor(SimpleFib), interpArgs = argsFor(InterpreterFib), specialArgs = argsFor(SpecialFib);
    bench(`SimpleFib.run(${runs} picks)`, () => runPicks(SimpleFib, simpleArgs));
    bench(`InterpreterFib.run(${runs} picks)`, () => runPicks(InterpreterFib, interpArgs));
    bench(`SpecialFib.run(${runs} picks)`, () => runPicks(SpecialFib, specialArgs));
  });
});

await run();
