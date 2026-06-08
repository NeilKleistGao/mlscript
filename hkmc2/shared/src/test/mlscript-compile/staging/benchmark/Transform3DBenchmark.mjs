import { run, bench, boxplot, summary } from 'mitata';

import NaiveTransform3D from "../../NaiveTransform3D.mjs"
import Transform3D from "../out/Transform3D.mjs"

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

await run();
