#!/usr/bin/env bash
# Runs the light screenshot tour once per language on a running emulator (Android 13+), using
# the per-app language setting, and copies the PNGs to shots/<locale>/. The app's data is
# cleared before each language so the sample story is seeded in that language.
#
# Locales are the tags phones report (zh-CN, zh-TW, id-ID...), not the resource folder names,
# so this also checks that each phone language finds its translation.
set -uo pipefail

APP=io.github.hdhvhit123.idolchat
RUNNER=$APP.test/androidx.test.runner.AndroidJUnitRunner
LOCALES=${LOCALES:-"en-US vi-VN zh-CN zh-TW ko-KR ja-JP hi-IN th-TH id-ID es-ES pt-BR fr-FR"}
OUT=${OUT:-shots}

adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

status=0
for locale in $LOCALES; do
  echo "::group::$locale"
  adb shell am force-stop "$APP"
  adb shell pm clear "$APP"
  adb shell cmd locale set-app-locales "$APP" --locales "$locale"
  adb shell cmd locale get-app-locales "$APP"
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null
  result=$(adb shell am instrument -w -r -e class com.fandom.auchatstudio.ScreenshotTourTest#lightTour "$RUNNER")
  echo "$result" | tail -n 5
  if ! echo "$result" | grep -q "^OK ("; then
    echo "::error::Screenshot tour failed in $locale"
    echo "$result" | grep -A 20 "INSTRUMENTATION_STATUS: stack" | head -n 40
    status=1
  fi
  mkdir -p "$OUT/$locale"
  adb pull "/sdcard/Android/media/$APP/additional_test_output/." "$OUT/$locale/" > /dev/null || true
  ls "$OUT/$locale"
  echo "::endgroup::"
done
exit $status
