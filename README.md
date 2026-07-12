# YoutubeShortBlocker

Androidのユーザー補助サービスを利用し、YouTube Shortsの長時間閲覧を防ぐアプリです。

## 現在の状態

Phase 2（検知・遮断機能）を実装中です。

- Android 8.0（API 26）以上
- Kotlin
- ユーザー補助サービスの登録
- サービス有効状態の表示
- ユーザー補助設定画面への導線
- Shorts再生画面の初回検知ロジック
- BACK／HOMEによる初回退避シーケンス

Phase 2ではテスト用の仮ON設定で画面解析と退避処理を実行します。永続設定と同意管理はPhase 3で追加します。

## 開発環境

- JDK 17
- Android SDK 35
- Android Gradle Plugin 8.13.2
- Gradle 8.13

Android Studioでプロジェクトを開き、必要なSDKをインストールして同期してください。

## ドキュメント

- [基本設計書](docs/basic-design.md)
- [開発ロードマップ](docs/roadmap.md)
