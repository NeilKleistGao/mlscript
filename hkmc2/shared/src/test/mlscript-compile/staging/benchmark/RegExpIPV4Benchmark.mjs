import fs from "fs";
import { run, bench, boxplot, summary } from 'mitata';

import SimpleRegExp from "../../SimpleRegExp.mjs"
import StagedRegExp from "../out/StagedRegExp.mjs"

let text = fs.readFileSync("./input-text.txt", "utf8")

boxplot(() => {
  summary(() => {
    bench('SimpleRegExp.matchAllIPv4', () => {
      SimpleRegExp.matchAllIPv4(text);
    });

    bench('StagedRegExp.matchAllIPv4', () => {
      StagedRegExp.matchAllIPv4(text);
    });
  });
});

await run();

