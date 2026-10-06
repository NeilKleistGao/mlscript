import { run, bench, boxplot, summary } from 'mitata';

import NaiveTransform3D from "../../NaiveTransform3D.mjs"
import Transform3D from "../out/Transform3D.mjs"
import SpecialTransform3D from "../out/SpecialTransform3D.mjs"

const coordsLength = 200000;
const minCoordValue = -1000;
const maxCoordValue = 1000;
const coordValueRange = maxCoordValue - minCoordValue;
const coords = Array.from({ length: coordsLength }, () => [
  Math.random() * coordValueRange + minCoordValue,
  Math.random() * coordValueRange + minCoordValue,
  Math.random() * coordValueRange + minCoordValue,
]);

boxplot(() => {
  summary(() => {
    bench('NaiveTransform3D.model(100000 coords)', () => {
      for (let i = 0; i < coords.length; ++i) {
        const coord = coords[i];
        NaiveTransform3D.model(coord, [11, 4, 51], [0.4, 0.19, 0.19], [0.8 * 3.14159265, 3.1415926535, 0.0])
      }
    });

    bench('StagedTransform3D.model(100000 coords)', () => {
      for (let i = 0; i < coords.length; ++i) {
        const coord = coords[i];
        Transform3D.model(coord, [11, 4, 51], [0.4, 0.19, 0.19], [0.8 * 3.14159265, 3.1415926535, 0.0])
      }
    });
  });
});

// ---------------------------------------------------------------------------
// The placement chosen at run time.
//
// `modelPreset(k, local)` picks between three placements.  `Transform3D`
// cannot specialize past that choice, so it still computes every sine, cosine
// and matrix entry; `SpecialTransform3D` marks the three presets `@special`
// and gets one collapsed transform per preset plus a dispatch on `k`.
// ---------------------------------------------------------------------------

// A fixed, repeatable sequence of choices, so every implementation sees the
// same mix and the dispatch is not trivially predicted into one target.
const presetPicks = Array.from({ length: coords.length }, (_, i) => i % 3);

const runPresets = (m) => {
  for (let i = 0; i < coords.length; ++i) {
    m.modelPreset(presetPicks[i], coords[i]);
  }
};

// Guard against measuring three different computations.
const round6 = (x) => { const y = Math.round(x * 1e6); return y === 0 ? 0 : y / 1e6; };
const showArr = (a) => a.map(round6).join(",");
for (let k = 0; k < 3; ++k) {
  const a = showArr(NaiveTransform3D.modelPreset(k, coords[0]));
  const b = showArr(Transform3D.modelPreset(k, coords[0]));
  const c = showArr(SpecialTransform3D.modelPreset(k, coords[0]));
  if (a !== b || a !== c) throw new Error(`modelPreset(${k}): implementations disagree`);
}
// ... and that the dispatch is not collapsed: the three presets are different
// transforms, so their outputs must differ.
{
  const outs = [0, 1, 2].map((k) => showArr(SpecialTransform3D.modelPreset(k, coords[0])));
  if (new Set(outs).size !== 3)
    throw new Error("SpecialTransform3D.modelPreset: dispatch collapsed");
}

boxplot(() => {
  summary(() => {
    bench('NaiveTransform3D.modelPreset(200000 coords)', () => { runPresets(NaiveTransform3D); });
    bench('Transform3D.modelPreset(200000 coords)', () => { runPresets(Transform3D); });
    bench('SpecialTransform3D.modelPreset(200000 coords)', () => { runPresets(SpecialTransform3D); });
  });
});

await run();
