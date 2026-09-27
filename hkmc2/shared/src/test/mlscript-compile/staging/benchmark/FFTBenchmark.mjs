import { run, bench, boxplot, summary } from 'mitata';

import NaiveFFT from "../../NaiveFFT.mjs"
import StagedFFT from "../out/StagedFFT.mjs"

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

await run();
