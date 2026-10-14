#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./build.sh
mkdir -p build/test-classes
javac --release 21 -cp build/classes -d build/test-classes tests/com/ib/chart/timesales/columns/ColoredColumn.java tests/twstapecolor/ColorTagDetectorTest.java
java -Djava.awt.headless=true -cp build/classes:build/test-classes twstapecolor.ColorTagDetectorTest
