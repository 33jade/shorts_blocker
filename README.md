# ShortBlocker

ShortBlockerは、YouTube Shortsの長時間閲覧を防ぐためのAndroidアプリです。

## Current Status

Phase 4の受け入れテストと改善対応は完了済みです。

- Android 8.0 (API 26) 以上
- Kotlin
- AccessibilityServiceによるYouTube Shorts画面検知
- Shorts検知時のYouTube Home誘導
- 1日あたりのShorts許可時間設定
- 一時解除機能
- ブロック説明画面
- 明示的同意画面
- アプリ内プライバシーポリシー
- リリースビルドの通常ログ無効化

## Development Environment

- JDK 17以上
- Android SDK 35
- Android Gradle Plugin 8.13.2
- Gradle 8.13

Android Studioでプロジェクトを開き、必要なSDKをインストールして同期してください。

## Useful Commands

```powershell
cd <repo-root>\ShortsBlocker
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:Path"

.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

## Documents

- [Basic design](docs/basic-design.md)
- [Roadmap](docs/roadmap.md)
- [Phase 4 acceptance test plan](docs/phase4-acceptance-test-plan.md)
- [Privacy policy](docs/privacy-policy.md)
- [Privacy policy publication plan](docs/privacy-policy-publication.md)
