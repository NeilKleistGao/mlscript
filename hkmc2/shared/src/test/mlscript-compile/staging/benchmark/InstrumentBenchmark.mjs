import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { run, bench, boxplot, summary } from 'mitata';

import MLscript from '../../../../../../js/target/scala-3.8.3/hkmc2-opt/MLscript.mjs';

const thisFile = fileURLToPath(import.meta.url);
const stackSizeFlag = '--stack-size=8192';
const hasStackSize = process.execArgv.some((arg) => arg.startsWith('--stack-size='));

if (!hasStackSize && process.env.HKMC2_INSTRUMENT_BENCHMARK_STACK !== '1') {
  const result = spawnSync(
    process.execPath,
    [stackSizeFlag, ...process.execArgv, thisFile],
    {
      stdio: 'inherit',
      env: {
        ...process.env,
        HKMC2_INSTRUMENT_BENCHMARK_STACK: '1',
      },
    },
  );

  process.exit(result.status ?? 1);
}

const benchmarkDir = dirname(thisFile);
const compileRoot = join(benchmarkDir, '../..');
const compileOptions = {
  traces: {
    codegen: false,
  },
};

const benchmarkFiles = [
  // ['Transform3D.mls', 'staging/Transform3D.mls'],
  // ['StagedRegExp.mls', 'staging/StagedRegExp.mls'],
  // ['StagedQuery.mls', 'staging/StagedQuery.mls'],
  ['out/Transform3D.mls', 'staging/out/Transform3D.mls'],
  ['out/StagedRegExp.mls', 'staging/out/StagedRegExp.mls'],
  // ['out/StagedQuery.mls', 'staging/out/StagedQuery.mls'],
  ['NaiveTransform3D.mls', 'NaiveTransform3D.mls'],
  ['SimpleRegExp.mls', 'SimpleRegExp.mls'],
  // ['NaiveQuery.mls', 'NaiveQuery.mls'],
];

const benchmarks = benchmarkFiles.map(([name, relativePath]) => [
  name,
  readFileSync(join(compileRoot, relativePath), 'utf8'),
]);

const compile = (source) => {
  const originalLog = console.log;
  console.log = () => {};
  try {
    return MLscript.compile(source, compileOptions);
  } finally {
    console.log = originalLog;
  }
};

boxplot(() => {
  summary(() => {
    for (const [name, source] of benchmarks) {
      bench(`compile ${name}`, () => {
        void compile(source);
      });
    }
  });
});

await run();
