#!/usr/bin/env bash
# Installs the Android SDK + Gradle wrapper. Safe to re-run.
set -euo pipefail
cd "$(dirname "$0")"

SDK="$HOME/android-sdk"
mkdir -p "$SDK/cmdline-tools"
if [ ! -d "$SDK/cmdline-tools/latest" ]; then
  curl -fsSL -o /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q /tmp/cmdtools.zip -d "$SDK/cmdline-tools"
  mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
fi
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
echo "sdk.dir=$SDK" > local.properties

if [ ! -f gradlew ]; then
  curl -fsSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.9-bin.zip
  unzip -q -o /tmp/gradle.zip -d "$HOME/gradle"
  "$HOME/gradle/gradle-8.9/bin/gradle" wrapper --gradle-version 8.9
fi
chmod +x gradlew
grep -q ANDROID_HOME ~/.bashrc || echo "export ANDROID_HOME=$SDK" >> ~/.bashrc
echo "Setup done. Build with: ./gradlew assembleDebug"
