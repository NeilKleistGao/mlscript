import { run, bench, boxplot, summary } from 'mitata';

import NaiveQuery from "../../NaiveQuery.mjs"
import StagedQuery from "../out/StagedQuery.mjs"

const studentsLength = 20000;
const scores = Array.from({ length: studentsLength }, () => [
  Math.random() * 100,
  Math.random() * 100,
  Math.random() * 100,
  Math.random() * 100,
]);

const naiveStudents = scores.map((t, i) =>
  new NaiveQuery.Student("Student " + (i + 1).toString(), t[0], t[1], t[2], t[3]));
const stagedStudents = scores.map((t, i) =>
  new StagedQuery.Student("Student " + (i + 1).toString(), t[0], t[1], t[2], t[3]));

boxplot(() => {
  summary(() => {
    bench('NaiveWhere(2000000 students)', () => {
      NaiveQuery.filterGoodStudents(naiveStudents, true)
      NaiveQuery.filterGoodStudents(naiveStudents, false)
    });

    bench('StagedWhere(2000000 student)', () => {
      StagedQuery.filterGoodStudents(stagedStudents, true)
      StagedQuery.filterGoodStudents(stagedStudents, false)
    });
  });
});

await run();
