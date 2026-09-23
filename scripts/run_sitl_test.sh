#!/usr/bin/env bash
# Run ArduRover SITL + Android emulator + install APK for end-to-end testing.
# Usage: ./scripts/run_sitl_test.sh
#
# SITL connects via UDP. The emulator reaches the host at 10.0.2.2.
# We tell SITL to --out to port 14550 on the host (where mavsdk_server listens
# after the app calls MavsdkConnectionManager.connect()).
#
# The app currently uses the default address "udpin://0.0.0.0:14540".
# For the emulator, SITL must push heartbeats OUT to the app.
# This script patches the default address to udpin://0.0.0.0:14550 so SITL
# can reach it with --out udp:127.0.0.1:14550.

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"
APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
SITL="$HOME/sitl/ardurover"
SITL_DIR="$HOME/sitl/run"

echo "=== ArduTarget SITL Test ==="
echo ""

# 1. Build APK if needed
if [[ ! -f "$APK" ]]; then
  echo "[1/5] Building APK..."
  cd "$PROJECT_DIR" && ./gradlew :app:assembleDebug
else
  echo "[1/5] APK already built: $(ls -sh "$APK" | awk '{print $1}')"
fi

# 2. Start emulator headless if none running
DEVICE=$("$ADB" devices | grep -v "List" | grep "emulator" | head -1 | awk '{print $1}')
if [[ -z "$DEVICE" ]]; then
  echo "[2/5] Starting emulator (headless, may take ~60s)..."
  export ANDROID_HOME
  export ANDROID_SDK_ROOT=$ANDROID_HOME
  "$EMULATOR" -avd Rover_Test -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect &
  EMULATOR_PID=$!
  echo "      emulator PID: $EMULATOR_PID"

  echo "      Waiting for device boot..."
  "$ADB" wait-for-device
  "$ADB" shell while [[ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null)" != "1" ]]; do sleep 3; done
  DEVICE=$("$ADB" devices | grep "emulator" | head -1 | awk '{print $1}')
  echo "      Device ready: $DEVICE"
else
  echo "[2/5] Emulator already running: $DEVICE"
fi

# 3. Start SITL
mkdir -p "$SITL_DIR"
echo "[3/5] Starting ArduRover SITL..."
echo "      Output UDP to 127.0.0.1:14550 (emulator reverse-proxied to app)"
cd "$SITL_DIR" && "$SITL" \
  --model rover \
  --speedup 1 \
  --out udp:127.0.0.1:14550 \
  --defaults "" \
  > /tmp/ardurover_sitl.log 2>&1 &
SITL_PID=$!
echo "      SITL PID: $SITL_PID  (log: /tmp/ardurover_sitl.log)"
sleep 4

# 4. Set up adb reverse tunnel: emulator port 14550 → host port 14550
echo "[4/5] Setting adb reverse: device:14550 → host:14550"
"$ADB" -s "$DEVICE" reverse tcp:14550 tcp:14550

# 5. Install + launch app
echo "[5/5] Installing APK..."
"$ADB" -s "$DEVICE" install -r "$APK"
echo "      Launching app..."
"$ADB" -s "$DEVICE" shell am start -n com.example.mavlinkapplication/.MainActivity

echo ""
echo "=== Running ==="
echo "  SITL log:     tail -f /tmp/ardurover_sitl.log"
echo "  App logcat:   $ADB -s $DEVICE logcat -s MavsdkTelemetryRepository:* HitTargetApp:* *:E"
echo ""
echo "Press Ctrl+C to stop SITL (emulator keeps running)."
wait $SITL_PID
