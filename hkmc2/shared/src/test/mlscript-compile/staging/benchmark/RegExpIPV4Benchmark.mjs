import fs from "fs";
import { run, bench, boxplot, summary } from 'mitata';

import SimpleRegExp from "../../SimpleRegExp.mjs"
import StagedRegExp from "../out/StagedRegExp.mjs"
import SpecialRegExpIPv4 from "../out/SpecialRegExpIPv4.mjs"
import TrickRegExpIPv4 from "../out/TrickRegExpIPv4.mjs"

let text = fs.readFileSync("./input-text.txt", "utf8")

boxplot(() => {
  summary(() => {
    bench('SimpleRegExp.matchAllIPv4', () => {
      SimpleRegExp.matchAllIPv4(text);
    });

    bench('StagedRegExp.matchAllIPv4', () => {
      StagedRegExp.matchAllIPv4(text);
    });

    bench('SpecialRegExp.matchAllIPv4', () => {
      SpecialRegExpIPv4.matchAllIPv4(text);
    });

    bench('TrickRegExpIPv4.matchAllIPv4', () => {
      TrickRegExpIPv4.matchAllIPv4(text);
    });
  });
});

await run();

