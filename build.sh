#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
ASM_VERSION=9.8
ASM_JAR="lib/asm-${ASM_VERSION}.jar"
ASM_URL="https://repo.maven.apache.org/maven2/org/ow2/asm/asm/${ASM_VERSION}/asm-${ASM_VERSION}.jar"
if [[ ! -s "$ASM_JAR" ]]; then
  echo "Downloading ASM ${ASM_VERSION} (BSD-3-Clause) from Maven Central..."
  mkdir -p lib
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --retry 2 --connect-timeout 10 --max-time 45 "$ASM_URL" -o "${ASM_JAR}.tmp"
  elif command -v wget >/dev/null 2>&1; then
    wget --timeout=20 -O "${ASM_JAR}.tmp" "$ASM_URL"
  else
    echo "Missing curl/wget. Download $ASM_URL to $ASM_JAR" >&2
    exit 1
  fi
  mv "${ASM_JAR}.tmp" "$ASM_JAR"
fi
# Reject invalid HTML/error files before compiling.
unzip -tq "$ASM_JAR" >/dev/null || { echo "Invalid ASM jar: $ASM_JAR" >&2; exit 1; }
rm -rf build/classes build/asm-unpacked build/twstapecolor.jar
mkdir -p build/classes build/asm-unpacked
javac --release 21 -cp "$ASM_JAR" -d build/classes src/twstapecolor/*.java
cat > build/manifest.mf <<'MANIFEST'
Manifest-Version: 1.0
Main-Class: twstapecolor.Main
Agent-Class: twstapecolor.AgentV0310
Can-Retransform-Classes: true

MANIFEST
(cd build/asm-unpacked && jar xf "../../${ASM_JAR}")
# Strip dependency metadata and signatures; this is a single, self-contained agent jar.
rm -rf build/asm-unpacked/META-INF
jar cfm build/twstapecolor.jar build/manifest.mf -C build/classes . -C build/asm-unpacked .
jar tf build/twstapecolor.jar | grep -q 'org/objectweb/asm/ClassReader.class' || { echo "ASM not bundled" >&2; exit 1; }
echo "Built: build/twstapecolor.jar (bundled ASM ${ASM_VERSION})"
