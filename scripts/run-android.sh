#!/usr/bin/env bash
# Android Studio 없이 터미널에서: SDK 설치 → 에뮬레이터 생성/부팅 → 빌드 → 설치 → 실행.
# macOS(Apple Silicon/Intel) 기준, Linux에서도 동작한다. 처음 한 번은 SDK·시스템 이미지(약 3GB)를 받는다.
#
#   scripts/run-android.sh            에뮬레이터에서 디버그 빌드 실행
#   scripts/run-android.sh --logs     실행 후 앱 로그(logcat)를 계속 보여줌
#   scripts/run-android.sh --eea      EEA(유럽) 사용자처럼 광고 동의 양식 테스트
#   scripts/run-android.sh --device   에뮬레이터 대신 USB로 연결된 실기기에 설치
#   scripts/run-android.sh --wipe     에뮬레이터 데이터 초기화 후 부팅 (앱 첫 실행 상태 확인용)
#   scripts/run-android.sh --stop     실행 중인 에뮬레이터 종료
set -euo pipefail

APP_ID="com.hyunnyApps.nonogram.debug"
ACTIVITY="com.hyunnyApps.nonogram.MainActivity"
AVD_NAME="nonogram_api36"
API_IMAGE="android-36"
PLATFORM="platforms;android-37.0"
BUILD_TOOLS="build-tools;36.0.0"

# 고정 버전 command-line tools (SHA-1은 dl.google.com/android/repository/repository2-3.xml 기준)
CLT_MAC_ZIP="commandlinetools-mac-15641748_latest.zip"
CLT_MAC_SHA1="b62a5d8cf63ded173b47be867be4ee058ceda6df"
CLT_LINUX_ZIP="commandlinetools-linux-16111833_latest.zip"
CLT_LINUX_SHA1="e025545c62a8e64c7559119566a569fb1dec5f60"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
USE_DEVICE=0; WIPE=0; LOGS=0; GRADLE_EXTRA=()

for arg in "$@"; do
  case "$arg" in
    --device) USE_DEVICE=1 ;;
    --wipe) WIPE=1 ;;
    --logs) LOGS=1 ;;
    --eea) GRADLE_EXTRA+=("-Pump.debugEea=true") ;;
    --stop) STOP=1 ;;
    -h|--help) sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "알 수 없는 옵션: $arg (도움말: --help)" >&2; exit 2 ;;
  esac
done

info() { printf '\033[1;34m▶ %s\033[0m\n' "$*"; }
fail() { printf '\033[1;31m✖ %s\033[0m\n' "$*" >&2; exit 1; }

OS="$(uname -s)"
# 에뮬레이터 이미지는 "하드웨어" 아키텍처와 같아야 한다.
# 터미널이 Rosetta(x86_64)로 실행 중이면 uname -m 이 x86_64 로 나오므로, 맥에서는 sysctl 로 실제 CPU를 확인한다.
if [ "$OS" = "Darwin" ] && [ "$(sysctl -n hw.optional.arm64 2>/dev/null || true)" = "1" ]; then
  ABI="arm64-v8a"
  if [ "$(sysctl -n sysctl.proc_translated 2>/dev/null || true)" = "1" ]; then
    printf '\033[1;33m! 터미널이 Rosetta(x86_64)로 실행 중입니다. 동작은 하지만 빌드가 느릴 수 있습니다.\033[0m\n'
  fi
else
  case "$(uname -m)" in
    arm64|aarch64) ABI="arm64-v8a" ;;
    *) ABI="x86_64" ;;
  esac
fi
SYSTEM_IMAGE="system-images;${API_IMAGE};google_apis_playstore;${ABI}"

# ---- SDK 위치 ----
sdk_from_local_properties() {
  [ -f "$ROOT/local.properties" ] && sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" | head -1 || true
}
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$(sdk_from_local_properties)}}"
if [ -z "$SDK" ]; then
  if [ "$OS" = "Darwin" ]; then SDK="$HOME/Library/Android/sdk"; else SDK="$HOME/Android/Sdk"; fi
fi
mkdir -p "$SDK"
SDK="$(cd "$SDK" && pwd -P)"  # 링크를 풀어 실제 경로로 고정
export ANDROID_HOME="$SDK" ANDROID_SDK_ROOT="$SDK"
AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.android/avd}"

# cmdline-tools 는 반드시 이 SDK 안에 실제로 있는 것을 쓴다.
# Homebrew(android-commandlinetools) 등 다른 SDK를 가리키는 링크면 구버전 sdkmanager/avdmanager 가
# 자기 위치를 SDK로 착각해 시스템 이미지를 엉뚱한 곳에 설치한다 → 에뮬레이터가 이미지를 못 찾음.
TOOLS="$SDK/cmdline-tools/latest"
if [ -d "$TOOLS" ]; then
  real_tools="$(cd "$TOOLS" && pwd -P)"
  case "$real_tools" in
    "$SDK"/*) ;;
    *)
      printf '\033[1;33m! %s 이(가) 다른 SDK(%s)를 가리킵니다. 이 SDK 전용 도구를 따로 설치해 사용합니다.\033[0m\n' "$TOOLS" "$real_tools"
      TOOLS="$SDK/cmdline-tools/run-android"
      ;;
  esac
fi
SDKMANAGER="$TOOLS/bin/sdkmanager"
AVDMANAGER="$TOOLS/bin/avdmanager"
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"

if [ "${STOP:-0}" = 1 ]; then
  [ -x "$ADB" ] || fail "adb가 없습니다 ($ADB)"
  for s in $("$ADB" devices | awk '/^emulator-/{print $1}'); do "$ADB" -s "$s" emu kill || true; done
  exit 0
fi

# ---- Java 17+ ----
java_major() {
  local v
  v="$("$1" -version 2>&1 | awk -F'"' '/version/{split($2,v,"."); print (v[1]=="1"?v[2]:v[1]); exit}')"
  case "$v" in ''|*[!0-9]*) echo 0 ;; *) echo "$v" ;; esac
}
find_java_home() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ] && [ "$(java_major "$JAVA_HOME/bin/java")" -ge 17 ]; then
    echo "$JAVA_HOME"; return
  fi
  if [ "$OS" = "Darwin" ]; then
    local h
    for v in 21 17 25; do
      h="$(/usr/libexec/java_home -v "$v" 2>/dev/null || true)"
      [ -n "$h" ] && { echo "$h"; return; }
    done
    h="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    [ -x "$h/bin/java" ] && { echo "$h"; return; }
  fi
  if command -v java >/dev/null 2>&1 && [ "$(java_major java)" -ge 17 ]; then
    dirname "$(dirname "$(readlink -f "$(command -v java)" 2>/dev/null || command -v java)")"; return
  fi
}
JAVA_HOME="$(find_java_home || true)"
if [ -z "$JAVA_HOME" ]; then
  fail "JDK 17 이상이 필요합니다. 설치: brew install --cask temurin@21  (설치 후 다시 실행)"
fi
export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
# JDK 버전은 위에서 확인했으므로 sdkmanager/avdmanager 자체 버전 검사(로케일·환경에 따라 오작동)는 끈다
export SKIP_JDK_VERSION_CHECK=1

# ---- command-line tools ----
if [ ! -x "$SDKMANAGER" ]; then
  info "Android command-line tools 설치 → $SDK"
  if [ "$OS" = "Darwin" ]; then zip="$CLT_MAC_ZIP"; sha="$CLT_MAC_SHA1"; else zip="$CLT_LINUX_ZIP"; sha="$CLT_LINUX_SHA1"; fi
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  curl -fL --progress-bar -o "$tmp/clt.zip" "https://dl.google.com/android/repository/$zip"
  actual="$( (shasum -a 1 "$tmp/clt.zip" 2>/dev/null || sha1sum "$tmp/clt.zip") | awk '{print $1}')"
  [ "$actual" = "$sha" ] || fail "체크섬 불일치 ($actual != $sha). 다운로드가 손상되었을 수 있습니다."
  mkdir -p "$SDK/cmdline-tools"
  unzip -q "$tmp/clt.zip" -d "$tmp/x"
  rm -rf "$TOOLS"
  mv "$tmp/x/cmdline-tools" "$TOOLS"
fi

# ---- SDK 구성요소 (이미 있으면 건너뜀) ----
need=()
[ -x "$ADB" ] || need+=("platform-tools")
[ -d "$SDK/platforms/android-37.0" ] || need+=("$PLATFORM")
[ -d "$SDK/build-tools/36.0.0" ] || need+=("$BUILD_TOOLS")
if [ "$USE_DEVICE" = 0 ]; then
  [ -x "$EMULATOR" ] || need+=("emulator")
  [ -d "$SDK/system-images/${API_IMAGE}/google_apis_playstore/${ABI}" ] || need+=("$SYSTEM_IMAGE")
fi
if [ "${#need[@]}" -gt 0 ]; then
  info "SDK 라이선스 동의 및 설치: ${need[*]}"
  yes | "$SDKMANAGER" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
  "$SDKMANAGER" --sdk_root="$SDK" --install "${need[@]}"
  if [ "$USE_DEVICE" = 0 ] && [ ! -d "$SDK/system-images/${API_IMAGE}/google_apis_playstore/${ABI}" ]; then
    fail "시스템 이미지가 $SDK 에 설치되지 않았습니다. 'ls $SDK/system-images' 결과와 함께 문의하세요."
  fi
fi

# Gradle 도 같은 SDK를 쓰도록 local.properties 의 sdk.dir 를 맞춘다 (local.properties 는 git 에 올라가지 않음)
if [ "$(sdk_from_local_properties)" != "$SDK" ]; then
  { [ -f "$ROOT/local.properties" ] && grep -v '^sdk\.dir=' "$ROOT/local.properties" || true; echo "sdk.dir=$SDK"; } > "$ROOT/local.properties.tmp"
  mv "$ROOT/local.properties.tmp" "$ROOT/local.properties"
fi

# ---- 대상 기기 ----
"$ADB" start-server >/dev/null
# $1 = emulator | device
online_serial() {
  "$ADB" devices | awk -v kind="$1" 'NR>1 && $2=="device" {
    emu = ($1 ~ /^emulator-/); if ((kind=="emulator") == emu) { print $1; exit } }'
}

if [ "$USE_DEVICE" = 1 ]; then
  SERIAL="$(online_serial device)"
  [ -n "$SERIAL" ] || fail "연결된 실기기가 없습니다. USB 디버깅을 켜고 'adb devices'로 확인하세요."
else
  # AVD 가 이 SDK의 시스템 이미지를 가리키는지 확인 (다른 SDK로 만들어진 AVD는 다시 만든다)
  avd_ok() {
    local cfg="$AVD_HOME/$AVD_NAME.avd/config.ini" sysdir
    [ -f "$cfg" ] || return 1
    sysdir="$(sed -n 's/^image\.sysdir\.1=//p' "$cfg" | head -1)"
    [ -n "$sysdir" ] && [ -d "$SDK/$sysdir" ] && grep -qx "abi.type=$ABI" "$cfg"
  }
  if ! avd_ok; then
    if [ -e "$AVD_HOME/$AVD_NAME.ini" ] || [ -d "$AVD_HOME/$AVD_NAME.avd" ]; then
      info "기존 에뮬레이터($AVD_NAME)가 이 SDK의 이미지를 찾지 못해 다시 만듭니다"
      "$AVDMANAGER" delete avd -n "$AVD_NAME" >/dev/null 2>&1 || true
      rm -rf "$AVD_HOME/$AVD_NAME.ini" "$AVD_HOME/$AVD_NAME.avd"
    fi
    info "에뮬레이터($AVD_NAME, $SYSTEM_IMAGE) 생성"
    device=""
    for d in pixel_8 pixel_7 pixel_6 pixel_5; do
      if "$AVDMANAGER" list device -c 2>/dev/null | grep -qx "$d"; then device="$d"; break; fi
    done
    echo no | "$AVDMANAGER" create avd -n "$AVD_NAME" -k "$SYSTEM_IMAGE" ${device:+-d "$device"} --force >/dev/null
    cfg="$AVD_HOME/$AVD_NAME.avd/config.ini"
    if [ -f "$cfg" ]; then
      grep -v '^hw\.keyboard=' "$cfg" > "$cfg.tmp" || true
      echo 'hw.keyboard=yes' >> "$cfg.tmp"
      mv "$cfg.tmp" "$cfg"
    fi
    avd_ok || fail "에뮬레이터를 만들었지만 시스템 이미지 경로가 맞지 않습니다 ($cfg 의 image.sysdir.1 확인)"
  fi

  SERIAL="$(online_serial emulator)"
  if [ -z "$SERIAL" ] || [ "$WIPE" = 1 ]; then
    if [ -n "$SERIAL" ]; then "$ADB" -s "$SERIAL" emu kill >/dev/null 2>&1 || true; sleep 3; fi
    log="${TMPDIR:-/tmp}/nonogram-emulator.log"
    info "에뮬레이터 부팅 (로그: $log)"
    args=(-avd "$AVD_NAME" -netdelay none -netspeed full -no-snapshot-save)
    [ "$WIPE" = 1 ] && args+=(-wipe-data)
    nohup "$EMULATOR" "${args[@]}" >"$log" 2>&1 &
    emu_pid=$!
    SERIAL=""
    for _ in $(seq 1 120); do
      kill -0 "$emu_pid" 2>/dev/null || { tail -20 "$log" >&2; fail "에뮬레이터가 종료되었습니다 (위 로그 참고)"; }
      SERIAL="$("$ADB" devices | awk 'NR>1 && $1 ~ /^emulator-/ {print $1; exit}')"
      [ -n "$SERIAL" ] && break
      sleep 2
    done
    [ -n "$SERIAL" ] || fail "에뮬레이터가 adb에 나타나지 않습니다 ($log)"
    printf '  부팅 대기'
    for _ in $(seq 1 180); do
      kill -0 "$emu_pid" 2>/dev/null || { echo; tail -20 "$log" >&2; fail "에뮬레이터가 종료되었습니다"; }
      if [ "$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then break; fi
      printf '.'; sleep 2
    done
    echo
    [ "$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] || fail "부팅 시간 초과 ($log)"
    "$ADB" -s "$SERIAL" shell input keyevent 82 >/dev/null 2>&1 || true # 잠금화면 해제
  fi
fi
export ANDROID_SERIAL="$SERIAL"

# ---- 빌드 · 설치 · 실행 ----
info "빌드 및 설치 → $SERIAL"
(cd "$ROOT" && ./gradlew :app:installDebug ${GRADLE_EXTRA[@]+"${GRADLE_EXTRA[@]}"} --console=plain -q)
"$ADB" -s "$SERIAL" shell am force-stop "$APP_ID" >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell am start -n "$APP_ID/$ACTIVITY" >/dev/null
info "실행 완료: $APP_ID ($SERIAL)"

if [ "$LOGS" = 1 ]; then
  sleep 2
  pid="$("$ADB" -s "$SERIAL" shell pidof -s "$APP_ID" | tr -d '\r')"
  info "앱 로그 (Ctrl+C로 종료)"
  "$ADB" -s "$SERIAL" logcat -v color --pid="$pid"
fi
