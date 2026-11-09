#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./build.sh
rm -rf out/test-classes
mkdir -p out/test-classes
find tests -path tests/live -prune -o -name '*.java' -print > out/test-sources.txt
javac --release 21 -cp out/classes:lib/asm-9.8.jar -d out/test-classes @out/test-sources.txt
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.ColorTagDetectorTest
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.TapeTest
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.LiveDataTest
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.RestorationTest
java -Xmx64m -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.AccuracyPropertiesTest
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.CallbackStopTest
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.FailedStartTest
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.FailedStopTest
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.RepaintStabilityTest
java -Xmx64m -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.IsolationTest
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.SizeAndPaintTest
java -Xmx64m -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.SymbolSwitchTest
java -Xmx64m -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.SymbolQuoteTest
java -Xmx64m -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.NativeResetStressTest
java -Xmx64m -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.DebugLogTest
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.DiscoveryTest
java -Xverify:all -Djava.awt.headless=true -cp out/classes:out/test-classes:lib/asm-9.8.jar twstapecolor.Tws1052Test
