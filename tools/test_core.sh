#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out_dir=$(mktemp -d)
trap 'rm -rf "$out_dir"' EXIT
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d "$out_dir" app/src/main/java/ir/hefze/quran/core/*.java tests/CoreTests.java tests/AudioTransferTests.java
java -cp "$out_dir" CoreTests

java -cp "$out_dir" AudioTransferTests
