#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_RENDER_DIR="${1:-/tmp/livemic-arrangement}"
mkdir -p "$TASK_RENDER_DIR"
TASK_RENDER_DIR="$(cd "$TASK_RENDER_DIR" && pwd)"
TASK_ANDROID_JAR="${ANDROID_HOME:-/workspace/toolchain/android}/platforms/android-35/android.jar"
TASK_JAVA="${JAVA_HOME:-/workspace/toolchain/jdk}/bin"
TASK_SOURCE=app/src/main/java/pro/livemic/studio
c++ -std=c++17 -O2 -pthread tests/arrangement_render.cpp -o "$TASK_RENDER_DIR/render"
for revision in before after; do
 mkdir -p "$TASK_RENDER_DIR/$revision/src" "$TASK_RENDER_DIR/$revision/classes"
 for source in ComposerProject TrackConfig VoicePhrase VoiceInstrumentSettings ComposerTransport ComposerRhythm MusicTheory; do
  if [[ "$revision" == before && ( "$source" == ComposerTransport || "$source" == ComposerRhythm || "$source" == MusicTheory ) ]]; then
   git show "ad22bd5:$TASK_SOURCE/$source.java" > "$TASK_RENDER_DIR/$revision/src/$source.java"
  else cp "$TASK_SOURCE/$source.java" "$TASK_RENDER_DIR/$revision/src/$source.java"; fi
 done
 "$TASK_JAVA/javac" -cp "$TASK_ANDROID_JAR" -d "$TASK_RENDER_DIR/$revision/classes" "$TASK_RENDER_DIR/$revision/src/"*.java tests/ArrangementExport.java
 for style in 0 6 5; do
  "$TASK_JAVA/java" -cp "$TASK_RENDER_DIR/$revision/classes:$TASK_ANDROID_JAR" pro.livemic.studio.ArrangementExport "$style" > "$TASK_RENDER_DIR/$revision-$style.txt"
  "$TASK_RENDER_DIR/render" app/src/main/assets/soundfonts/generaluser-gs-1.44.sf2 "$TASK_RENDER_DIR/$revision-$style.txt" "$TASK_RENDER_DIR/$revision-$style.wav"
 done
done
