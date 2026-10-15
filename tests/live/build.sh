#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
./build.sh
mkdir -p work/audit-classes work/audits
javac --release 21 -cp lib/asm-9.8.jar -d work/audit-classes tests/live/*.java
for agent in AccuracyAudit StateAudit LegacyCleanup EventGapAudit ResourceAudit Level2IsolationAudit; do
  manifest="work/audits/${agent}.mf"
  printf 'Manifest-Version: 1.0\nAgent-Class: %s\nCan-Retransform-Classes: true\nCan-Redefine-Classes: true\n\n' "$agent" > "$manifest"
  jar cfm "work/audits/${agent}.jar" "$manifest" -C work/audit-classes . -C build/asm-unpacked org
 done
