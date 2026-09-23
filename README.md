# ArduTarget Android App

Native Android prototype for an ArduPilot Rover–based hit-target system.
Communicates with the ArduCube flight controller via MAVLink/MAVSDK only.

## What's in scope

- Live connection status driven by heartbeat timing (NEVER_CONNECTED / FRESH / DEGRADED / LOST)
- Armed/mode state preserved through degraded states
- View and edit ArduCube parameters via the MAVSDK Param plugin
- Switch ArduPilot Rover drive modes via `MAV_CMD_DO_SET_MODE` (id 176)

## Architecture

```
:domain   — pure Kotlin/JVM; zero Android/MAVSDK imports
:app      — Android, Jetpack Compose, Hilt DI, MAVSDK-Java 3.17.4
```

`domain` holds all business logic: `LinkHealthEvaluator`, `LinkQuality`, `RoverMode`, and
the three repository interfaces (`TelemetryRepository`, `ParamRepository`, `ModeRepository`).
`app` provides MAVSDK implementations wired through Hilt.

## Prerequisites

- Android Studio Meerkat or later
- Android SDK 37 (target) / minSdk 24
- AGP 9.4.1, Kotlin 2.2.0, KSP 2.2.0-2.0.2
- JDK 11+

## Running domain tests

```bash
./gradlew :domain:test
```

24 tests, all pure JVM — no emulator needed.

## Building

```bash
./gradlew :app:assembleDebug
```

APK ends up at `app/build/outputs/apk/debug/app-debug.apk` (~149 MB due to MAVSDK
native binaries for all ABIs).

## Connecting to ArduPilot Rover SITL

The app's `MavsdkConnectionManager` defaults to `udpin://0.0.0.0:14540`
(mavsdk_server listens; SITL connects in). Update the address before shipping.

### Android Emulator

SITL on the host machine, emulator on the same host:

```bash
# Start SITL (from ArduPilot repo)
sim_vehicle.py -v Rover --out udp:127.0.0.1:14540

# Or with explicit output to emulator's host alias
sim_vehicle.py -v Rover --out udp:10.0.2.2:14550
```

For `udpout` addresses, change the call in `MavsdkConnectionManager.connect()`:

```kotlin
connectionManager.connect("udpout://10.0.2.2:14550")
```

### Physical Device

Find the device's IP (`adb shell ip addr`) and tell SITL to push to it:

```bash
sim_vehicle.py -v Rover --out udp:<device-ip>:14550
```

Then in the app:

```kotlin
connectionManager.connect("udpout://<device-ip>:14550")
```

## Known build workarounds

Two `gradle.properties` flags were needed to make Hilt 2.56.2 work with AGP 9.4.1:

```properties
# Hilt 2.56.2 looks for BaseExtension which AGP 9 only registers on the legacy DSL path.
android.newDsl=false
# KSP registers generated sources via kotlin.sourceSets; suppress the check.
android.disallowKotlinSourceSets=false
```

Both are deprecated by AGP and scheduled for removal in AGP 10. Remove them when
upgrading to a Hilt version that targets `AndroidComponentsExtension`.

## Verified vs. assumed

| Claim | Status |
|---|---|
| Domain tests: 24 green | **VERIFIED** |
| `./gradlew :app:assembleDebug` succeeds | **VERIFIED** |
| `MavsdkServer` constructor/methods | **VERIFIED** (jar inspection) |
| `MavlinkDirect.getMessage()` / `sendMessage()` signatures | **VERIFIED** (jar inspection) |
| `Param.getAllParams()` / `getParamFloat()` / `setParamFloat()` | **VERIFIED** (jar inspection) |
| `MavlinkMessage.getFieldsJson()` field names | **VERIFIED** (jar inspection) |
| HEARTBEAT JSON field names (`custom_mode`, `base_mode`) | PLAUSIBLE — confirm with real SITL traffic |
| `MAV_CMD_DO_SET_MODE` param layout for ArduPilot Rover | CONFIRMED from ArduPilot docs |
| MavsdkServer connects to SITL without crashing | NOT YET TESTED (needs SITL running) |
