#!/usr/bin/env bash
set -euo pipefail

# Usage: benchmark-test-times.sh [FFT Interpreter Pipeline RegExpURI SparseTensor StateMachine Transform3D]
# With no arguments, measure all listed benchmarks. SBT can name an executable.
# Before a first run, generate shared JS dependencies with:
#   sbt -J-Xss1G -J-Xmx8G ctest
# Each implementation sample separately sums the reported milliseconds:
#   ctest staging/<module>; ctest staging/out/<module>; dtest block-staging/<test>
# Run all ten samples in one SBT process to avoid repeated startup costs.
benchmark_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_dir=$(cd -- "$benchmark_dir/../../../../../../.." && pwd)
runs=5
sbt_bin=${SBT:-sbt}
if ! command -v "$sbt_bin" >/dev/null 2>&1; then
  if [[ -z ${SBT+x} && -x "$HOME/.local/share/coursier/bin/sbt" ]]; then
    sbt_bin="$HOME/.local/share/coursier/bin/sbt"
  else
    printf 'SBT executable not found: %s (set SBT to its path).\n' "$sbt_bin" >&2
    exit 1
  fi
fi

benchmarks=(FFT Interpreter Pipeline RegExpURI SparseTensor StateMachine Transform3D)
# benchmarks=(FFT FFT2)
if (( $# > 0 )); then
  benchmarks=("$@")
fi
selected=' '
for benchmark in "${benchmarks[@]}"; do
  case "$benchmark" in
    FFT|Interpreter|Pipeline|RegExpURI|SparseTensor|StateMachine|Transform3D) ;;
    *) printf 'Unknown benchmark: %s\n' "$benchmark" >&2; exit 1 ;;
  esac
  if [[ "$selected" == *" $benchmark "* ]]; then
    printf 'Duplicate benchmark: %s\n' "$benchmark" >&2
    exit 1
  fi
  selected+="$benchmark "
done

log_dir=$(mktemp -d "${TMPDIR:-/tmp}/mlscript-benchmark-times.XXXXXX")
plan="$log_dir/plan.tsv"
log="$log_dir/sbt.log"
commands=()

add_test() {
  local benchmark=$1 version=$2 iteration=$3 phase=$4 test_name=$5
  local runner='hkmc2JVM/testOnly hkmc2.CompileTestRunner'
  if [[ "$phase" == dtest ]]; then
    runner='hkmc2DiffTests/testOnly hkmc2.DiffTestRunner'
  fi
  # Select full ScalaTest names with -t so every command runs exactly one test.
  commands+=("$runner -- -t \"$test_name\"")
  printf '%s\t%s\t%s\t%s\t%s.mls\n' \
    "$benchmark" "$version" "$iteration" "$phase" "$test_name" >> "$plan"
}

add_implementation() {
  local benchmark=$1 version=$2 module=$3 diff_test=$4 iteration
  for ((iteration = 1; iteration <= runs; iteration++)); do
    # ScalaTest names include these prefixes, even with exact matching.
    add_test "$benchmark" "$version" "$iteration" ctest "mlscript-compile/staging/$module"
    add_test "$benchmark" "$version" "$iteration" out "mlscript-compile/staging/out/$module"
    add_test "$benchmark" "$version" "$iteration" dtest "mlscript/block-staging/$diff_test"
  done
}

for benchmark in "${benchmarks[@]}"; do
  # These names follow the benchmark imports and the tests that generate out/.
  special=''
  special_test=''
  case "$benchmark" in
    FFT) staged=StagedFFT; staged_test=StagedFFTTest ;;
    # FFT2) staged=StagedFFT; staged_test=StagedFFTTest ;;
    Interpreter) staged=InterpreterFib; staged_test=InterpreterFibTest; special=SpecialFib; special_test=SpecialFibBenchmarkTest ;;
    Pipeline) staged=Pipeline; staged_test=PipelineTest ;;
    RegExpURI) staged=StagedRegExp; staged_test=StagedRegExpTest; special=SpecialRegExpURI; special_test=SpecialRegExpURITest ;;
    SparseTensor) staged=StagedSparseTensor; staged_test=SparseTensorTest ;;
    StateMachine) staged=StateMachine; staged_test=StateMachineTest ;;
    Transform3D) staged=Transform3D; staged_test=Transform3DTest ;;
  esac

  if [[ "$staged" == "$special" || "$staged_test" == "$special_test" ]]; then
    printf 'Overlapping staged and special tests for %s.\n' "$benchmark" >&2
    exit 1
  fi
  add_implementation "$benchmark" staged "$staged" "$staged_test"
  if [[ -n "$special" ]]; then
    add_implementation "$benchmark" special "$special" "$special_test"
  fi
done

printf 'Running %d tests (%d samples per implementation). Log: %s\n' "${#commands[@]}" "$runs" "$log" >&2
cd -- "$repo_dir"
# Generated programs overflowed the default stack in earlier runs; use the
# larger stack and heap requested for these benchmarks.
if ! "$sbt_bin" -batch -J-Xss1G -J-Xmx8G \
    -Dsbt.server.autostart=false -Dsbt.supershell=false -Dsbt.log.noformat=true \
    "${commands[@]}" 2>&1 | tee "$log" | awk '
      {
        gsub(/\033\[[0-9;]*m/, "")
        if ($0 ~ /^(Compiled|Processed) in +[0-9]+ ms /) {
          print > "/dev/stderr"
          fflush("/dev/stderr")
        }
      }
    '; then
  printf 'A test failed; no averages computed. See %s\n' "$log" >&2
  tail -n 30 "$log" >&2
  exit 1
fi

# Keep SBT in the user locale so the JVM can read non-ASCII filesystem paths;
# only the numeric report needs the C locale.
LC_ALL=C awk -v runs="$runs" '
  function reject(message) {
    print "Invalid timing output: " message > "/dev/stderr"
    invalid = 1
    exit 1
  }
  NR == FNR {
    expected[++planned] = $5
    phase[planned] = $4
    group[planned] = $1 SUBSEP $2
    if (!(group[planned] in seen)) {
      seen[group[planned]] = 1
      order[++groups] = group[planned]
      benchmark[group[planned]] = $1
      version[group[planned]] = $2
    }
    next
  }
  {
    gsub(/\033\[[0-9;]*m/, "")
    sub(/^\[info\] +/, "")
    if ($0 ~ /^Total number of tests run:/ && $NF != 1)
      reject("a filter selected " $NF " tests; expected exactly one")
    if ($0 == "All tests passed.") passed++
    if ($0 ~ /^(Compiled|Processed) in +[0-9]+ ms /) {
      measured++
      kind = phase[measured] == "dtest" ? "Processed" : "Compiled"
      if (measured > planned || $1 != kind || $5 != expected[measured])
        reject("expected " expected[measured] ", got " $0)
      total[group[measured], phase[measured]] += $3
    }
  }
  END {
    if (invalid) exit 1
    if (measured != planned || passed != planned)
      reject("expected " planned " timings and successful tests, got " measured " and " passed)
    printf "%-16s %-8s %5s %12s %12s %12s %14s\n", \
      "Benchmark", "Version", "Runs", "ctest (ms)", "out (ms)", "dtest (ms)", "Total avg (ms)"
    for (i = 1; i <= groups; i++) {
      key = order[i]
      source = total[key, "ctest"] / runs
      generated = total[key, "out"] / runs
      diff = total[key, "dtest"] / runs
      printf "%-16s %-8s %5d %12.1f %12.1f %12.1f %14.1f\n", \
        benchmark[key], version[key], runs, source, generated, diff, source + generated + diff
    }
  }
' "$plan" "$log"
