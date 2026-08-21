import { run, bench, boxplot, summary } from 'mitata';

import JsonSchemaParser from "../../JsonSchemaParser.mjs"
import StagedJsonSchemaParser from "../out/StagedJsonSchemaParser.mjs"

const inputLength = 10000;
const letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
const departments = ["cse", "mat", "phy", "bio", "eng"];

const randomInt = (min, max) =>
  Math.floor(Math.random() * (max - min + 1)) + min;

const randomString = (length) =>
  Array.from({ length }, () => letters[randomInt(0, letters.length - 1)]).join("");

const randomId = () =>
  Array.from({ length: 8 }, () => randomInt(0, 9)).join("");

const detailedFlags = Array.from({ length: inputLength }, (_, i) => i % 2 === 0);
const studentData = detailedFlags.map((detailed) => {
  const name = randomString(randomInt(3, 12));
  const id = randomId();

  if (detailed) {
    const list = Array.from({ length: randomInt(1, 4) }, () =>
      `{'cs': '${randomString(randomInt(1, 4))}', 'sc': ${randomInt(0, 100)}}`
    ).join(", ");
    return `{'name': '${name}', 'id': '${id}', 'gpa': ${randomInt(0, 4)}, 'list': [${list}]}`;
  }

  const dept = departments[randomInt(0, departments.length - 1)];
  return `{'name': '${name}', 'id': '${id}', 'year': ${randomInt(1, 6)}, 'dept': '${dept}'}`;
});

boxplot(() => {
  summary(() => {
    bench('JsonSchemaParser', () => {
      for (let i = 0; i < inputLength * 50; ++i) {
        JsonSchemaParser.parseStudentData(studentData[i % inputLength], true);
        JsonSchemaParser.parseStudentData(studentData[i % inputLength], false);
      }
    });

    bench('StagedJsonSchemaParser', () => {
      for (let i = 0; i < inputLength * 50; ++i) {
        StagedJsonSchemaParser.parseStudentData(studentData[i % inputLength], true);
        StagedJsonSchemaParser.parseStudentData(studentData[i % inputLength], false);
      }
    });
  });
});

await run();
