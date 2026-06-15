import fs from "fs";
import { run, bench, boxplot, summary } from 'mitata';

import SimpleRegExp from "../../SimpleRegExp.mjs"
import StagedRegExp from "../out/StagedRegExp.mjs"
import SpecialRegExp from "../out/SpecialRegExp.mjs"

let text = fs.readFileSync("./input-text.txt", "utf8")

boxplot(() => {
  summary(() => {
    bench('SimpleRegExp.matchAllEmail', () => {
      SimpleRegExp.matchAllEmail(text);
    });

    bench('StagedRegExp.matchAllEmail', () => {
      StagedRegExp.matchAllEmail(text);
    });

    bench('SpecialRegExp.matchAllEmail', () => {
      SpecialRegExp.matchAllEmail(text);
    });
  });
});

await run();

