#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
binary="$(mktemp)"
trap 'rm -f "$binary"' EXIT
c++ -std=c++17 -O2 -Wall -Wextra -pedantic "$root/tests/harmony_native_test.cpp" -o "$binary"
"$binary"
