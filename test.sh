#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./build.sh
rm -rf build/test-classes
mkdir -p build/test-classes
find tests -name '*.java' -print > build/test-sources.txt
javac --release 21 -cp build/classes:lib/asm-9.8.jar -d build/test-classes @build/test-sources.txt
java -Xverify:all -Djava.awt.headless=true -cp build/classes:build/test-classes:lib/asm-9.8.jar twstapecolor.ColorTagDetectorTest
java -Xverify:all -Djava.awt.headless=true -cp build/classes:build/test-classes:lib/asm-9.8.jar twstapecolor.TapeTest
