#!/usr/bin/env bash
# Run on the build host to refresh the committed, cross-platform distribution.
set -euo pipefail
cd "$(dirname "$0")/.."
bash build.sh
package_version="$(java -jar build/twstapecolor.jar --version | awk '{print $2}')"
[[ "$package_version" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo 'Invalid package version' >&2; exit 1; }
mkdir -p dist
cp build/twstapecolor.jar "dist/twstapecolor-${package_version}.jar"
(cd dist && sha256sum "twstapecolor-${package_version}.jar" > SHA256SUMS)
echo "Packaged: dist/twstapecolor-${package_version}.jar"
