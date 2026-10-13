import { run, bench, boxplot, summary } from 'mitata';

import NaiveFFT from "../../NaiveFFT.mjs"
import StagedFFT from "../out/StagedFFT.mjs"

// Each bench body transforms `signalCount * repeats` buffers; `repeats` is
// chosen so that one execution of the naive transform takes about 500ms.
const signalCount = 1024;
const repeats = 128;
const total = signalCount * repeats;

const randomSignal = (n) =>
  Array.from({ length: 2 * n }, () => Math.random() * 2 - 1);

const signals8 = Array.from({ length: signalCount }, () => randomSignal(8));

// Guard against measuring different computations: the staged transform must
// agree with the naive one, to six decimals, on every signal.
const round6 = (x) => { const y = Math.round(x * 1e6); return y === 0 ? 0 : y / 1e6; };
const showArr = (a) => a.map(round6).join(",");
for (const signal of signals8) {
  if (showArr(NaiveFFT.fft8(signal)) !== showArr(StagedFFT.fft8(signal)))
    throw new Error(`fft8: implementations disagree`);
}

boxplot(() => {
  summary(() => {
    // 8 = 2*2*2
    bench(`NaiveFFT.fft8(${total} signals)`, () => {
      for (let r = 0; r < repeats; ++r) {
        for (let i = 0; i < signals8.length; ++i) {
          NaiveFFT.fft8(signals8[i]);
        }
      }
    });

    bench(`StagedFFT.fft8(${total} signals)`, () => {
      for (let r = 0; r < repeats; ++r) {
        for (let i = 0; i < signals8.length; ++i) {
          StagedFFT.fft8(signals8[i]);
        }
      }
    });
  });
});

await run();
