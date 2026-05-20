import { run, bench, boxplot, summary } from 'mitata';

import NaiveQuery from "../../NaiveQuery.mjs"
import StagedQuery from "../out/StagedQuery.mjs"

const studentsLength = 100000;
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
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 0));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 1));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 2));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 3));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 4));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 0));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 1));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 2));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 3));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 4));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 0));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 1));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 2));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 3));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 4));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 0));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 1));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 2));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 3));
      naiveStudents.filter((s, i) => NaiveQuery.isGoodStudent(s, i % 5 === 4));
    });

    bench('StagedWhere(2000000 student)', () => {
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 0));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 1));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 2));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 3));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 4));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 0));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 1));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 2));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 3));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 4));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 0));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 1));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 2));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 3));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 4));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 0));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 1));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 2));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 3));
      stagedStudents.filter((s, i) => StagedQuery.isGoodStudent(s, i % 5 === 4));
    });
  });
});

await run();
