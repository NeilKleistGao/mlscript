import fs from "fs";
import { run, bench, boxplot, summary } from 'mitata';

import SimpleRegExp from "../../SimpleRegExp.mjs"
import StagedRegExp from "../out/StagedRegExp.mjs"

let text = fs.readFileSync("./input-text.txt", "utf8")

boxplot(() => {
  summary(() => {
    bench('SimpleRegExp.matchAllURI', () => {
      SimpleRegExp.matchAllURI(text);
    });

    bench('StagedRegExp.matchAllURI', () => {
      StagedRegExp.matchAllURI(text);
    });
  });
});

await run();

