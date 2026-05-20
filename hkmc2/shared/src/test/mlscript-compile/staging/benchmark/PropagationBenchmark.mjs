import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { run, bench, boxplot, summary } from 'mitata';

import TransformCompile from './TransformCompile.mjs';
import RegExpCompile from './RegExpCompile.mjs';
import WhereExpressionCompile from './WhereExpressionCompile.mjs';

const benchmarkDir = dirname(fileURLToPath(import.meta.url));
const generatedFiles = [
  '../out/Transform3D.mls',
  '../out/StagedRegExp.mls',
  '../out/StagedQuery.mls',
].map((file) => join(benchmarkDir, file));

const generatedSnapshots = generatedFiles.map((file) => [
  file,
  readFileSync(file, 'utf8'),
]);

try {
  boxplot(() => {
    summary(() => {
      bench('TransformCompile.generate', () => {
        void TransformCompile.generate();
      });

      bench('RegExpCompile.generate', () => {
        void RegExpCompile.generate();
      });

      bench('WhereExpressionCompile.generate', () => {
        void WhereExpressionCompile.generate();
      });
    });
  });

  await run();
} finally {
  for (const [file, content] of generatedSnapshots) {
    // writeFileSync(file, content);
  }
}
