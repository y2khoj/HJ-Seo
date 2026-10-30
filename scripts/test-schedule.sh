#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_TEST_DIR="$(mktemp -d /tmp/livemic-schedule.XXXXXX)"
trap 'rm -rf "$TASK_TEST_DIR"' EXIT
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -d "$TASK_TEST_DIR" app/src/main/java/pro/livemic/studio/RecordingCountdown.java tests/RecordingCountdownTest.java
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "$TASK_TEST_DIR" RecordingCountdownTest
