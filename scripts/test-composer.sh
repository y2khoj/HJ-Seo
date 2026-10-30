#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_COMPOSER_BINARY="$(mktemp /tmp/lv-composer.XXXXXX)"
trap 'rm -f "$TASK_COMPOSER_BINARY"' EXIT
c++ -std=c++17 -O2 -pthread tests/composer_native_test.cpp -o "$TASK_COMPOSER_BINARY"
"$TASK_COMPOSER_BINARY" app/src/main/assets/soundfonts/generaluser-gs-1.44.sf2
