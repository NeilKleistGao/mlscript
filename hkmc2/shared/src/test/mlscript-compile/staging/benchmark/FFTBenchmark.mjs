import { run, bench, boxplot, summary } from 'mitata';

import NaiveFFT from "../../NaiveFFT.mjs"
import StagedFFT from "../out/StagedFFT.mjs"
import SpecialFFT from "../out/SpecialFFT.mjs"

// Each bench body transforms `signalCount * repeats` buffers, which keeps a
// single measured execution well under a second.
const signalCount = 1024;
const repeats = 16;

const randomSignal = (n) =>
  Array.from({ length: 2 * n }, () => Math.random() * 2 - 1);

const signals8 = Array.from({ length: signalCount }, () => randomSignal(8));
const signals12 = Array.from({ length: signalCount }, () => randomSignal(12));

// One boxplot and one summary over all three factorizations.  The summary ranks
// all six benches against the single fastest one, so only the ratios between a
// naive/staged pair of the same transform length mean anything; ratios across
// lengths also reflect a 12-point transform doing more work than an 8-point one.
boxplot(() => {
  summary(() => {
    // 8 = 2*2*2
    bench('NaiveFFT.fft8(16384 signals)', () => {
      for (let r = 0; r < repeats; ++r) {
        for (let i = 0; i < signals8.length; ++i) {
          NaiveFFT.fft8(signals8[i]);
        }
      }
    });

    bench('StagedFFT.fft8(16384 signals)', () => {
      for (let r = 0; r < repeats; ++r) {
        for (let i = 0; i < signals8.length; ++i) {
          StagedFFT.fft8(signals8[i]);
        }
      }
    });

    // 8 = 4*2
    bench('NaiveFFT.fft8_42(16384 signals)', () => {
      for (let r = 0; r < repeats; ++r) {
        for (let i = 0; i < signals8.length; ++i) {
          NaiveFFT.fft8_42(signals8[i]);
        }
      }
    });

    bench('StagedFFT.fft8_42(16384 signals)', () => {
      for (let r = 0; r < repeats; ++r) {
        for (let i = 0; i < signals8.length; ++i) {
          StagedFFT.fft8_42(signals8[i]);
        }
      }
    });

    // 12 = 3*2*2
    bench('NaiveFFT.fft12(16384 signals)', () => {
      for (let r = 0; r < repeats; ++r) {
        for (let i = 0; i < signals12.length; ++i) {
          NaiveFFT.fft12(signals12[i]);
        }
      }
    });

    bench('StagedFFT.fft12(16384 signals)', () => {
      for (let r = 0; r < repeats; ++r) {
        for (let i = 0; i < signals12.length; ++i) {
          StagedFFT.fft12(signals12[i]);
        }
      }
    });
  });
});

// ---------------------------------------------------------------------------
// The factorization chosen at run time.
//
// `fftPlan(k, xs)` picks between 8 = 2*2*2, 8 = 4*2 and 8 = 2*4.  `StagedFFT`
// cannot specialize past that choice -- the radix list it hands to `run` is an
// ordinary value -- so it falls back to interpreting the transform, which is
// roughly what `NaiveFFT` does.  `SpecialFFT` marks the three candidates
// `@special`, so it gets one unrolled butterfly network per factorization and
// a single dispatch on `k`.
//
// More repeats than above, since this group's fastest bar is a specialized
// transform and would otherwise be too short to measure reliably.
// ---------------------------------------------------------------------------

const planRepeats = 48;

// A fixed, repeatable sequence of choices, so every implementation sees the
// same mix and the dispatch is not trivially predicted into one target.
const planPicks = Array.from({ length: signals8.length }, (_, i) => i % 3);

const runPlans = (m) => {
  for (let r = 0; r < planRepeats; ++r) {
    for (let i = 0; i < signals8.length; ++i) {
      m.fftPlan(planPicks[i], signals8[i]);
    }
  }
};

// Guard against measuring three different computations: all three must agree
// with each other, to six decimals, on every plan.
const round6 = (x) => { const y = Math.round(x * 1e6); return y === 0 ? 0 : y / 1e6; };
const showArr = (a) => a.map(round6).join(",");
for (let k = 0; k < 3; ++k) {
  const a = showArr(NaiveFFT.fftPlan(k, signals8[0]));
  const b = showArr(StagedFFT.fftPlan(k, signals8[0]));
  const c = showArr(SpecialFFT.fftPlan(k, signals8[0]));
  if (a !== b || a !== c) throw new Error(`fftPlan(${k}): implementations disagree`);
}

boxplot(() => {
  summary(() => {
    bench('NaiveFFT.fftPlan(49152 signals)', () => { runPlans(NaiveFFT); });
    bench('StagedFFT.fftPlan(49152 signals)', () => { runPlans(StagedFFT); });
    bench('SpecialFFT.fftPlan(49152 signals)', () => { runPlans(SpecialFFT); });
  });
});

await run();
