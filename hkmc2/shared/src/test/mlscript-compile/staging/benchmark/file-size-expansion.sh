#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

# Measure the complete .mjs implementation files imported by each benchmark,
# without minification or transitive dependencies. Naive is the 1x baseline:
# expansion factor = version size in bytes / naive size in bytes.
benchmark_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
compile_dir="$benchmark_dir/../.."
staged_dir="$benchmark_dir/../out"

file_bytes() {
  local file=$1
  if [[ ! -f "$file" || ! -r "$file" ]]; then
    printf 'Missing or unreadable module: %s\nCompile the benchmark implementations before running this script.\n' "$file" >&2
    exit 1
  fi
  wc -c < "$file"
}

printf '%-16s %14s %14s %14s %14s %14s\n' \
  'Benchmark' 'Naive (bytes)' 'Staged (bytes)' 'Staged/naive' 'Special (bytes)' 'Special/naive'

# Names follow the benchmark imports; the naive and staged modules do not all
# share a naming convention (notably SimpleFib and InterpreterFib).
while read -r benchmark naive_module staged_module special_module; do
  naive_file="$compile_dir/$naive_module.mjs"
  naive_bytes=$(file_bytes "$naive_file")
  staged_bytes=$(file_bytes "$staged_dir/$staged_module.mjs")
  special_bytes=''
  if [[ -n "$special_module" ]]; then
    special_bytes=$(file_bytes "$staged_dir/$special_module.mjs")
  fi

  if (( naive_bytes == 0 )); then
    printf 'Naive module is empty; expansion factor is undefined: %s\n' "$naive_file" >&2
    exit 1
  fi

  awk -v benchmark="$benchmark" -v naive="$naive_bytes" \
      -v staged="$staged_bytes" -v special="$special_bytes" '
    BEGIN {
      printf "%-16s %14d %14d %13.3fx", benchmark, naive, staged, staged / naive
      if (special == "")
        printf " %14s %14s\n", "-", "-"
      else
        printf " %14d %13.3fx\n", special, special / naive
    }
  '
done <<'BENCHMARKS'
FFT NaiveFFT StagedFFT
Interpreter SimpleFib InterpreterFib SpecialFib
Pipeline NaivePipeline Pipeline
RegExpURI SimpleRegExp StagedRegExp SpecialRegExpURI
SparseTensor NaiveSparseTensor StagedSparseTensor
StateMachine NaiveStateMachine StateMachine
Transform3D NaiveTransform3D Transform3D
BENCHMARKS
