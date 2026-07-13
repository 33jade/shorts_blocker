# Phase 4 Acceptance Test Plan

Last updated: 2026-07-12

## 1. Purpose

Phase 4 verifies that ShortBlocker works safely in realistic YouTube usage:

- Shorts playback is blocked when blocking is active.
- Normal YouTube screens are not blocked.
- Settings, consent, allowance, temporary unblock, and explanation screens behave as expected.
- The app does not loop, freeze, or affect apps other than YouTube.

## 2. Test Environment Record

Fill this table for each run.

| Item | Value |
| --- | --- |
| Test date | 2026-07-12 |
| Tester | User manual confirmation |
| Device / emulator | `emulator-5554` |
| Android version / API | Android 15 / API 35 |
| YouTube version | 21.26.364 |
| Display language | Japanese |
| App version | 0.1.0 debug |
| APK | `app\build\outputs\apk\debug\app-debug.apk` |

## 3. PowerShell Setup

Run from this directory:

```powershell
Set-Location -LiteralPath "<repo-root>\ShortBlocker"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:Path"
```

Preflight:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" --version
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" version
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

Useful log commands:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -c
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -s ShortsBlockerService
```

Launch app:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell monkey -p com.shortblocker 1
```

## 4. Acceptance Tests

| ID | Scenario | Steps | Expected result | Result | Evidence |
| --- | --- | --- | --- | --- | --- |
| AC-01 | AccessibilityService can be enabled | Open app, tap user accessibility settings button, accept disclosure, enable Shorts Blocker in Android settings | Service can be enabled and app shows enabled after returning | OK | User manual confirmation, 2026-07-12 |
| AC-02 | Shorts playback is blocked | Set blocking ON, use up or exceed the daily allowance, no temporary unblock, open a Shorts playback screen | Block explanation screen is shown; user can choose YouTube Home or temporary unblock | OK | User manual confirmation, 2026-07-12 |
| AC-03 | Normal video is not blocked | Clear logcat, open a normal YouTube video and watch for at least 10 seconds | No `Shorts screen detected` log and no BACK/HOME action | OK | User confirmed no false detection, 2026-07-12 |
| AC-03B | Search results are not blocked | Clear logcat, search in YouTube and scroll results | No `Shorts screen detected` log and no BACK/HOME action | OK | User confirmed no false detection, 2026-07-12 |
| AC-03C | Channel screen is not blocked | Clear logcat, open a channel page and browse tabs | No `Shorts screen detected` log and no BACK/HOME action | OK | User confirmed no false detection, 2026-07-12 |
| AC-03D | Shorts tab shell does not cause loops | Clear logcat, open Shorts tab/list and observe behavior | No repeated BACK/HOME loop; only actual Shorts playback should be handled | OK | ADB manual confirmation: Shorts tab transitions to block explanation without uncontrolled loop, 2026-07-12 |
| AC-04 | Blocking OFF disables blocking | Turn app blocking OFF, open Shorts playback | Shorts remains viewable and no evacuation sequence runs | OK | ADB manual confirmation: OFF state retained, Shorts viewable, no `ShortsBlockerService` log, 2026-07-12 |
| AC-05 | Continuous events do not loop | With blocking ON, open Shorts, then rapidly navigate back into Shorts several times | No repeated uncontrolled loop; at most one evacuation sequence per detection window | OK | ADB manual confirmation: repeated Shorts entries converged to block explanation; no uncontrolled loop, 2026-07-12 |
| AC-06 | Other apps are unaffected | Open another app and navigate normally | No ShortsBlockerService action and no UI interruption | OK | ADB manual confirmation: Android Settings scroll produced no service action/log, 2026-07-12 |
| AC-07 | HOME fallback is bounded | If BACK cannot leave Shorts, observe fallback behavior | No more than 2 BACK attempts, then 1 HOME attempt, then explanation screen or Toast | OK | Unit test `fallsBackHomeWhenShortsStillContinuesAfterTwoBacks`; `testDebugUnitTest` 29/29 passed, 2026-07-12 |
| AC-08 | OFF during sequence cancels next actions | Start blocking sequence, quickly return to app and turn blocking OFF if possible | No further BACK/HOME after OFF is reflected | OK | Unit test `stopsBeforeSecondBackWhenRuntimeBecomesDisabled`; saved OFF manual confirmation, 2026-07-12 |
| AC-09 | Root node failure is safe | Observe normal navigation/transition gaps during YouTube use | No crash and no extra HOME caused only by missing root | OK | Unit tests `retriesRootOnceAndStopsWhenRootIsStillUnavailable` and `retriesRootOnceAndContinuesWhenRetryGetsShortsScreen`; no crash during manual smoke, 2026-07-12 |
| AC-10 | Consent gate works | From fresh app data or reset state, tap user accessibility settings button and decline disclosure | Android accessibility settings is not opened; service remains operationally inactive until consent | OK | User manual confirmation, 2026-07-12 |
| AC-11 | Initial settings load and saved OFF are safe | Set blocking OFF, restart app/service, open Shorts | No analysis/evacuation until settings load; saved OFF prevents blocking | OK | ADB manual confirmation: saved OFF persisted and Shorts stayed viewable with no service log, 2026-07-12 |
| AC-12 | HOME failure does not show success | Simulate or observe a failed HOME action if reproducible | Success message/explanation is not shown when HOME action fails | OK | Unit test `doesNotShowHomeSuccessWhenHomeActionFails`; `testDebugUnitTest` 29/29 passed, 2026-07-12 |

## 5. Phase 3 Settings Tests

| ID | Scenario | Steps | Expected result | Result | Evidence |
| --- | --- | --- | --- | --- | --- |
| P3-01 | Daily allowance dropdown works | Select 許可しない, 5, 10, 15, 20, 30, and 60 minutes from the dropdown | Selected allowance is retained and reflected in remaining time display | OK | ADB manual confirmation: all options selectable; `60分` retained after restart; reset to `許可しない`, 2026-07-12 |
| P3-02 | Daily allowance reaches block state | Use Shorts until allowance is exhausted, or use a debug-shortened scenario if added later | Shorts is blocked after allowance is exhausted | OK | ADB manual confirmation with `許可しない` / remaining 0 minutes; unit test `blocksWhenDailyAllowanceIsFullyUsed`, 2026-07-12 |
| P3-03 | Temporary unblock permits Shorts | Trigger block explanation screen, tap 5 minute temporary unblock, open Shorts | Shorts is not blocked during temporary unblock | OK | User confirmed block explanation screen actions, 2026-07-12 |
| P3-04 | Temporary unblock expires | Wait until temporary unblock expires, open Shorts | Blocking resumes automatically | OK | ADB manual confirmation: after 5 minute temporary unblock expired, Shorts opened block explanation again, 2026-07-12 |
| P3-05 | Temporary unblock can be canceled | Start 5 minute temporary unblock, tap cancel temporary unblock in app settings | Temporary unblock status returns to inactive and blocking resumes | OK | User confirmed temporary unblock stop works, 2026-07-12 |
| P3-06 | Block explanation actions work | Trigger block explanation screen | YouTube button opens YouTube Home, 5 minute temporary unblock button sets unblock and opens YouTube; cancel button stops active temporary unblock | OK | User confirmed all block explanation screen actions, 2026-07-12 |
| P3-07 | Privacy policy is accessible | Tap privacy policy button in app | Privacy policy screen opens and closes normally | OK | ADB manual confirmation: privacy policy opened and BACK returned to MainActivity, 2026-07-12 |

## 6. Performance Smoke Tests

| ID | Scenario | Steps | Expected result | Result | Evidence |
| --- | --- | --- | --- | --- | --- |
| PERF-01 | Logcat crash smoke | Use YouTube for 3 minutes across home/search/video/Shorts | No app crash, no ANR, no repeated exception spam | OK | ADB/logcat smoke: no `FATAL`, `ANR`, `Exception`, or `am_crash` lines for ShortBlocker, 2026-07-12 |
| PERF-02 | Responsiveness smoke | Scroll YouTube home/search for 30 seconds | YouTube remains responsive; no visible stutter caused by blocker | OK | ADB manual confirmation: YouTube Home scroll remained on YouTube Home; no visible blocker interruption, 2026-07-12 |
| PERF-03 | Parser unit safety | Run `testDebugUnitTest` | 29 tests pass, failures/errors 0 | OK | `testDebugUnitTest`: 29 tests, 0 failures, 0 errors; `assembleDebug` successful, 2026-07-12 |

## 7. Initial Manual Confirmation Order

Run the first pass in this order:

1. Preflight build and install.
2. AC-10 consent decline.
3. AC-01 consent accept and service enable.
4. P3-06 privacy policy screen.
5. AC-03 normal video.
6. AC-03B search.
7. AC-03C channel.
8. AC-02 Shorts block.
9. P3-06 block explanation buttons.
10. AC-04 blocking OFF.
11. P3-03 temporary unblock.
12. P3-05 temporary unblock cancel.
13. PERF-01 3 minute crash smoke.

## 8. Evidence Naming

Suggested local evidence folder:

```text
<local-evidence-root>\20260712_ShortBlocker_Phase4Evidence
```

Suggested evidence names:

```text
AC-01_accessibility_enabled.png
AC-02_shorts_block.webm
AC-03_normal_video_no_detection.txt
AC-03B_search_no_detection.txt
AC-03C_channel_no_detection.txt
P3-05_block_explanation.webm
PERF-01_logcat_smoke.txt
```

## 9. Known Carryovers

- Right-side Shorts action bar View ID candidates were not captured in Phase 2 and remain a fallback-hardening item.
- Store privacy policy public URL is not published yet.
- Full performance measurement with profiler or Perfetto is not completed yet.
- Multi-device/API/language coverage is not completed yet.
