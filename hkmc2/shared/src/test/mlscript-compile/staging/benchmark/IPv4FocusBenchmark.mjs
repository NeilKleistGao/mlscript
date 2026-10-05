// Focused A/B for the IPv4 pattern: plain staging vs `@special` staging.
// The full comparison lives in `RegExpIPV4Benchmark.mjs`; this one drops the
// other implementations so that an annotation or threshold change can be
// re-measured quickly.
import fs from "fs";
import { run, bench, boxplot, summary } from 'mitata';

import StagedRegExp from "../out/StagedRegExp.mjs"
import SpecialRegExpIPv4 from "../out/SpecialRegExpIPv4.mjs"

let text = fs.readFileSync("./input-text.txt", "utf8")

boxplot(() => {
  summary(() => {
    bench('StagedRegExp.matchAllIPv4', () => {
      StagedRegExp.matchAllIPv4(text);
    });

    bench('SpecialRegExpIPv4.matchAllIPv4', () => {
      SpecialRegExpIPv4.matchAllIPv4(text);
    });
  });
});

await run();
