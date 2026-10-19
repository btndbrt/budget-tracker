#!/usr/bin/env bash
# Builds an installable Linux package (.deb) of Budget Tracker with jpackage (part of the JDK).
# The package contains its own Java runtime, so it runs on machines without Java installed.
#
# Usage:   ./packaging/build-linux.sh
# Result:  target/dist/budget-tracker_<version>_amd64.deb
set -euo pipefail
cd "$(dirname "$0")/.."

# Version from pom.xml, e.g. 1.0
VERSION=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)

echo "Building the jar..."
./mvnw -q -B clean package

# jpackage takes everything in the input folder, so give it only our jar
rm -rf target/jpackage-input target/dist
mkdir -p target/jpackage-input
cp "target/budget-tracker-$VERSION.jar" target/jpackage-input/

echo "Packaging (this takes a minute)..."
jpackage \
  --type deb \
  --name "Budget Tracker" \
  --linux-package-name budget-tracker \
  --app-version "$VERSION" \
  --description "Simple budget tracker with categories, periods and charts" \
  --vendor "btndbrt" \
  --input target/jpackage-input \
  --main-jar "budget-tracker-$VERSION.jar" \
  --main-class btndbrt.Main \
  --icon src/main/resources/icon.png \
  --linux-shortcut \
  --linux-menu-group "Office" \
  --dest target/dist

echo
echo "Done: $(ls target/dist/*.deb)"
echo "Install it with:  sudo apt install ./$(ls target/dist/*.deb)"
