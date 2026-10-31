#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_PARITY_DIR="$(mktemp -d /tmp/livemic-parity.XXXXXX)"
trap 'rm -rf "$TASK_PARITY_DIR"' EXIT
c++ -std=c++17 -O2 -pthread tests/guitar_voicing_export.cpp -o "$TASK_PARITY_DIR/export"
"$TASK_PARITY_DIR/export" > "$TASK_PARITY_DIR/voices.txt"
"${JAVA_HOME:-/workspace/toolchain/jdk}/bin/javac" -d "$TASK_PARITY_DIR" app/src/main/java/pro/livemic/studio/MusicTheory.java tests/GuitarVoicingParity.java
"${JAVA_HOME:-/workspace/toolchain/jdk}/bin/java" -cp "$TASK_PARITY_DIR" pro.livemic.studio.GuitarVoicingParity "$TASK_PARITY_DIR/voices.txt"
