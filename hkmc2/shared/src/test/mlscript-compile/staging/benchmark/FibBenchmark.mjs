import { run, bench, boxplot, summary } from 'mitata';

import SimpleFib from "../../SimpleFib.mjs"
import InterpreterFib from "../out/InterpreterFib.mjs"

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

await run();
