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
const position = [11, 4, 51];
const scaling = [0.4, 0.19, 0.19];
const rotation = [0.8 * 3.14159265, 3.1415926535, 0.0];

// Check that both implementations apply the same fixed placement.
const round6 = (x) => { const y = Math.round(x * 1e6); return y === 0 ? 0 : y / 1e6; };
const showArr = (a) => a.map(round6).join(",");
for (const coord of [[1, 2, 3], [-4.5, 0.25, 7], [0, 0, 0]]) {
  if (showArr(NaiveTransform3D.model(coord, position, scaling, rotation)) !== showArr(Transform3D.model(coord, position, scaling, rotation)))
    throw new Error("model: implementations disagree");
}

boxplot(() => {
  summary(() => {
    bench(`NaiveTransform3D.model(${coordsLength} coords)`, () => {
      for (let i = 0; i < coords.length; ++i) {
        const coord = coords[i];
        NaiveTransform3D.model(coord, position, scaling, rotation)
      }
    });

    bench(`Transform3D.model(${coordsLength} coords)`, () => {
      for (let i = 0; i < coords.length; ++i) {
        const coord = coords[i];
        Transform3D.model(coord, position, scaling, rotation)
      }
    });
  });
});

await run();
