#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
TASK_TEST_DIR=$(mktemp -d)
trap 'rm -r "$TASK_TEST_DIR"' EXIT
c++ -std=c++17 -O2 tests/record_alignment_test.cpp -o "$TASK_TEST_DIR/alignment"
"$TASK_TEST_DIR/alignment"
javac -d "$TASK_TEST_DIR/classes" app/src/main/java/pro/livemic/studio/PracticeScore.java tests/PracticeScoreTest.java
java -ea -cp "$TASK_TEST_DIR/classes" PracticeScoreTest
