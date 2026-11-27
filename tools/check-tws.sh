#!/usr/bin/env bash
# Read-only installed-archive check. Does not attach to or execute TWS.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ $# -eq 0 ]]; then
  echo 'Usage: bash tools/check-tws.sh /absolute/path/to/jts4launch-*.dat [...]' >&2
  exit 2
fi
bash build.sh
mkdir -p work/compat-check
javac --release 21 -cp out/classes:lib/asm-9.8.jar -d work/compat-check tools/CheckTwsCompatibility.java tools/CheckTwsReflection.java
java -Xverify:all -cp work/compat-check:out/classes:lib/asm-9.8.jar twstapecolor.CheckTwsCompatibility "$@"
java -Xverify:all -cp work/compat-check:out/classes:lib/asm-9.8.jar twstapecolor.CheckTwsReflection "$@"
