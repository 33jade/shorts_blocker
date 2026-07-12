# YouTubeノードID記録

## 目的

Phase 2のShorts検知条件を、実際のYouTube画面で取得できるView IDに基づいて追跡する。

## 記録手順

1. debugビルドをインストールする。
2. ユーザー補助サービスを有効化する。
3. ログをクリアする。

```powershell
adb logcat -c
```

4. ログ監視を開始する。

```powershell
adb logcat -s ShortsBlockerService
```

5. YouTubeでShorts再生画面を開く。
6. `YouTube node IDs for ...` の行を確認し、このファイルへ記録する。

## 現在の判定材料

| 種別 | View ID / ID部分一致 | 用途 | 状態 |
| --- | --- | --- | --- |
| 必須 | `com.google.android.youtube:id/reel_recycler` | Shorts再生画面の主要コンテナ | 実装済み・2026-07-11のdebugログで確認 |
| 追加 | `reel_player_overlay` | Shorts再生画面のフォールバック材料 | 実装済み・実画面ID記録待ち |
| 追加 | `reel_right_discovery_action_bar` | 右側アクションバー候補 | 実装済み・実画面ID記録待ち |
| 追加 | `right_action_bar` | 右側アクションバー候補 | 実装済み・実画面ID記録待ち |
| 追加 | `reel_like_button` | 右側アクションバー候補 | 実装済み・実画面ID記録待ち |
| 追加 | `reel_dislike_button` | 右側アクションバー候補 | 実装済み・実画面ID記録待ち |
| 追加 | `reel_comments_button` | 右側アクションバー候補 | 実装済み・実画面ID記録待ち |
| 追加 | `reel_share_button` | 右側アクションバー候補 | 実装済み・実画面ID記録待ち |

## 実機・エミュレータ記録

### 2026-07-10 / Pixel 9a API 35 エミュレータ

| 画面 | 検知理由 | 記録 |
| --- | --- | --- |
| Shorts再生画面 | `ReelRecyclerWithShortsLabel` | `Shorts screen detected: ReelRecyclerWithShortsLabel` が複数回出力された。View ID一覧は次回debugログで記録する。 |
| 通常動画 | なし | `adb logcat -c`後、検知ログなし。 |
| 検索 | なし | `adb logcat -c`後、検知ログなし。 |
| チャンネル | なし | `adb logcat -c`後、検知ログなし。 |

### 2026-07-11 / Pixel 9a API 35 エミュレータ

| 項目 | 内容 |
| --- | --- |
| APK導入 | `adb install -r app\build\outputs\apk\debug\app-debug.apk` が `Success` |
| 検知理由 | `ReelRecyclerWithShortsLabel` |
| 検知ログ | `Shorts screen detected: ReelRecyclerWithShortsLabel` |

取得できたYouTube View ID:

```text
com.google.android.youtube:id/more_drawer_container
com.google.android.youtube:id/reel_time_bar
com.google.android.youtube:id/slim_status_bar_player_container
com.google.android.youtube:id/accessibility_layer_container
com.google.android.youtube:id/watch_while_layout_coordinator_layout
com.google.android.youtube:id/pivot_bar
com.google.android.youtube:id/browse_fragment_layout_coordinator_layout
com.google.android.youtube:id/nerd_stats_container
com.google.android.youtube:id/reel_recycler
com.google.android.youtube:id/reel_playback_loading_spinner
com.google.android.youtube:id/toolbar_container
com.google.android.youtube:id/reel_player_page_container
com.google.android.youtube:id/collapsing_header_container
com.google.android.youtube:id/toolbar
com.google.android.youtube:id/text
com.google.android.youtube:id/reel_progress_bar
```

確認結果:

- `reel_recycler`はShorts再生画面で取得できた。
- 今回のログでは`reel_player_overlay`および右側アクションバー候補IDは取得されなかった。
- 現環境では`reel_recycler` + `Shorts`ラベルによる決定表No.1で検知できている。
