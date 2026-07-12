# ShortBlocker

Androidのユーザー補助サービスを利用し、YouTube Shortsの長時間閲覧を防ぐアプリです。

## 現在の状態

Phase 4（テスト・改善）を実施中です。

- Android 8.0（API 26）以上
- Kotlin
- ユーザー補助サービスの登録
- サービス有効状態の表示
- ユーザー補助設定画面への導線
- Shorts再生画面の検知ロジック
- 1日許可時間、一時解除、ブロック説明画面
- ユーザー補助サービス向けの明示的同意画面
- アプリ内プライバシーポリシー
- Phase 4受け入れテスト計画

現在はDebug APKで受け入れテストと改善を進めています。

## 開発環境

- JDK 17
- Android SDK 35
- Android Gradle Plugin 8.13.2
- Gradle 8.13

Android Studioでプロジェクトを開き、必要なSDKをインストールして同期してください。

## ドキュメント

- [基本設計書](docs/basic-design.md)
- [開発ロードマップ](docs/roadmap.md)
