# YoutubeShortBlocker 基本設計書

## 1. 文書情報

| 項目 | 内容 |
| --- | --- |
| アプリ名 | YoutubeShortBlocker |
| 対象OS | Android 8.0（API Level 26）以上 |
| 対象アプリ | Android版 YouTube（パッケージ名: `com.google.android.youtube`） |
| 主な実装言語 | Kotlin |

## 2. システム概要

### 2.1 目的

Androidのユーザー補助サービス（`AccessibilityService`）を利用し、YouTubeアプリ内のShorts再生画面を検知して自動的に離脱する。これにより、Shortsの長時間閲覧を防ぐ。

### 2.2 対象範囲

#### 対象

- YouTubeアプリ内のShorts再生画面の検知
- 検知後の戻る操作による画面離脱
- ユーザー補助サービスの有効／無効状態の表示と設定画面への誘導
- ブロック機能のON／OFF、1日あたりの許可時間設定、一時解除停止
- ブロック時にユーザーへ正常動作であることを示す説明画面または通知

#### 対象外

- ブラウザ版YouTubeおよびYouTube以外のアプリ
- Shortsのサムネイル、タブなど、再生前の導線そのものの非表示
- YouTubeアプリの通信遮断や改変
- 完全な利用時間管理、ペアレンタルコントロール

> 初期リリースでは誤検知を避けるため、Shortsの「導線」ではなく「再生画面」のみを遮断対象とする。

## 3. アーキテクチャ

```text
[ユーザー操作] --> [YouTubeアプリ]
                         | 画面変更イベント
                         v
               [ShortsBlockerService]
                         |
       +-----------------+-----------------+
       |                                   |
       v                                   v
[イベント受信条件]                  [DataStore監視]
  1. パッケージ／種別                 | OFFを検知
  2. 排他・負荷制御                   v
  3. Job多重起動防止          [evacuationJob.cancel()]
       |                              |
       v                              v
[ShortsDetector]              [finallyで状態初期化]
       |
       +-- 条件不一致 --> 終了
       |
       +-- Shorts検知
               |
               v
       [evacuationJob開始]
               |
       [BACK 1回目]
               |
          500 ms待機
               |
       [ルートノード取得]
               |
               +-- null --> 100 ms後に1回再取得
               |                 |
               |                 +-- 失敗 --> 終了
               |
       [Shorts再評価]
               |
               +-- 離脱成功 --> 終了
               |
               +-- Shorts継続
                       |
               [BACK 2回目]
                       |
                  500 ms待機
                       |
              [取得・再評価]
                       |
                       +-- 離脱成功／取得失敗 --> 終了
                       |
                       +-- Shorts継続
                               |
                       [HOME + Toast] --> 終了
```

イベント処理、設定キャッシュの更新、退避シーケンスの状態変更、グローバル操作は`Dispatchers.Main.immediate`上で直列化する。退避シーケンスは単一の`evacuationJob`へ閉じ込め、キャンセル・正常終了・例外終了のいずれでも`finally`で状態を初期化する。ノード探索自体を別スレッドへ移す場合は、`AccessibilityNodeInfo`の受け渡し方法とスレッド安全性を別途検証する。

## 4. 主要モジュール

| モジュール／クラス | 役割 |
| --- | --- |
| `MainActivity` | サービスの有効状態を表示し、Androidのユーザー補助設定画面へ誘導する。ブロック機能のON／OFF設定を提供する。 |
| `ShortsBlockerService` | `AccessibilityService`を継承し、YouTubeの画面変更イベントを受信する。検知、再評価、遮断処理を制御する。 |
| `ShortsDetector` | `AccessibilityNodeInfo`の画面構造から、Shorts再生画面かどうかを判定する。Android APIへの依存を局所化し、単体テスト可能な判定結果を返す。 |
| `BlockSettingsRepository` | ブロック機能のON／OFF状態、同意バージョン、許可時間、一時解除期限、再評価間隔などをDataStoreに保存する。Phase 3で実装する。 |
| `ShortsAllowanceManager` | 1日あたりのShorts許可時間、日付切り替え、消費時間、一時解除状態を判定する。Phase 3以降で実装する。 |
| `BlockInterventionActivity` | Shorts拒否時間中にShortsを検知した場合、ブロック説明、残り許可時間、一時解除、YouTube Homeへ戻る導線を表示する。Phase 3以降で実装する。 |
| `BlockHistoryManager`（将来拡張） | ブロック回数や日時、許可時間消費を保存し、抑止効果を可視化する。初期リリースには最小限のみ含める。 |

## 5. 検知・遮断処理

### 5.1 イベント受信条件

以下をすべて満たす場合のみ、画面解析を開始する。

1. イベントのパッケージ名が`com.google.android.youtube`である。
2. イベント種別が`TYPE_WINDOW_CONTENT_CHANGED`または`TYPE_WINDOW_STATE_CHANGED`である。
3. 設定の初回読込が完了している。
4. 現在の開示バージョンへ同意済みである。
5. ブロック機能がONである。
6. 退避シーケンスの実行中ではない。

条件を満たしたイベントには、次の優先順位で負荷制御を適用する。

1. **排他制御（最優先）**
   - ノード解析は同時に1つだけ実行し、`AtomicBoolean`または同等の排他制御で多重実行を防ぐ。
   - 解析中に受信したイベントは、イベント種別を問わず新しい解析を開始せず、解析完了後のtrailing実行へ集約する。
2. **イベント種別による制御**
   - `TYPE_WINDOW_STATE_CHANGED`: 300 msの時間制限をバイパスする。解析中でなければ即時解析し、解析中ならtrailing実行へ集約する。
   - `TYPE_WINDOW_CONTENT_CHANGED`: 前回のノード解析開始から最低300 ms空ける。制限期間内のイベントは個別に解析せず、最新イベントが存在することだけを記録し、期間終了時に現在の画面を1回だけ解析する。
3. **事前除外**
   - 実機調査で安全性を確認できた`className`などの除外条件がある場合のみ、`TYPE_WINDOW_CONTENT_CHANGED`をノード探索前に除外する。未知の値は除外しない。

trailing実行では保存した`AccessibilityEvent`やノードを再利用せず、その時点の`rootInActiveWindow`から現在の画面を解析する。スロットリング用の保留処理は、サービス破棄時および機能OFF時にキャンセルする。

### 5.2 Shorts判定

判定には、単一のテキストやView IDではなく、以下の決定表を使用する。必須条件と、いずれかの追加条件を満たした場合のみShorts再生画面と判定する。

| No. | 必須条件 | 追加条件 | 判定 | 意図 |
| ---: | --- | --- | :---: | --- |
| 1 | `com.google.android.youtube:id/reel_recycler`が存在する | 表示テキストまたはコンテンツ説明に「Shorts」が含まれる | TRUE | 標準的なShorts再生画面 |
| 2 | `com.google.android.youtube:id/reel_recycler`が存在する | `reel_player_overlay`を含むIDと、右側アクションバーを示すIDの両方が存在する | TRUE | テキストが省略された画面のフォールバック |
| 3 | `reel_recycler`が存在しない | 「Shorts」テキストとコンテンツ説明が存在する | FALSE | ホーム画面のShortsシェルフや検索結果を除外 |
| 4 | `reel_recycler`が存在する | No. 1、No. 2のどちらも満たさない | FALSE | 遷移途中やノード生成途中の誤検知を防止 |

右側アクションバーの具体的なID候補は実機調査で確定し、検証済みYouTubeバージョンとともにテスト仕様へ記録する。`ShortsDetector`は判定結果に加えて一致した条件を返し、UI変更時に判定理由を追跡できるようにする。

```kotlin
fun isShortsScreen(rootNode: AccessibilityNodeInfo): Boolean {
    val hasReelRecycler =
        rootNode.hasViewId("com.google.android.youtube:id/reel_recycler")
    if (!hasReelRecycler) return false

    val hasShortsLabel = rootNode.hasTextOrDescription("Shorts")
    val hasReelOverlay = rootNode.hasViewIdContaining("reel_player_overlay")
    val hasActionBar = rootNode.hasShortsActionBar()

    return hasShortsLabel || (hasReelOverlay && hasActionBar)
}
```

`hasViewId`などは探索用の独自ヘルパーを表す。実装ではノード数と探索深度に上限を設ける。確度が不足する場合は遮断せず、誤検知の回避を優先する。

### 5.3 遮断アクション

1. Shorts再生画面と判定した場合でも、まずメモリ上へ反映済みの実行設定を確認する。OFF、未同意、同意バージョン不一致の場合は退避シーケンスを開始せず終了する。
2. ブロック機能がONでも、一時解除中または当日の許可時間が残っている場合は退避シーケンスを開始せず、閲覧許可状態として扱う。許可時間制御を有効にする場合は、Shorts画面滞在中の消費時間を記録し、上限到達後の次回検知でブロック対象とする。
3. ブロック対象の場合は、まず`BlockInterventionActivity`を表示し、「Shortsを拒否時間中であること」「YouTube Homeへ戻る導線」「一時解除」を示す。
4. `BlockInterventionActivity`を表示できない場合のみ退避シーケンスを開始し、1回目の`GLOBAL_ACTION_BACK`を実行する。
5. 実行後500 ms待機し、`rootInActiveWindow`を新たに取得して明示的に再評価する。待機中に受信した画面変更イベントでは、新しい退避シーケンスを開始しない。
6. `rootInActiveWindow`が`null`の場合は100 ms待機して1回だけ再取得する。再取得時も`null`の場合は、シーケンス状態を初期化して終了する。ノード取得失敗を理由にHOMEへ退避しない。
7. Shortsが継続している場合は、2回目のBACKの直前に実行設定を再確認する。OFF、一時解除、許可時間内へ変化している場合は待機・再評価をキャンセルし、状態を初期化して終了する。ブロック対象の場合のみ2回目の`GLOBAL_ACTION_BACK`を実行する。BACKは合計2回を上限とする。
8. さらに500 ms後もShortsが継続している場合は、HOMEの直前に実行設定を再確認する。ブロック対象の場合のみ`GLOBAL_ACTION_HOME`を1回実行する。
9. Shortsでないことを確認した時点、またはHOME/説明画面表示後にシーケンスを終了する。
10. グローバル操作APIが`false`を返した場合も試行回数に含め、無制限に再実行しない。HOME操作が失敗した場合は成功通知や説明画面の成功表示を出さない。

機能ON／OFF、同意バージョン、許可時間、一時解除期限はDataStoreから監視し、サービスが参照するメモリ上のキャッシュへ反映する。初回読込完了までは解析を開始しない。各アクション直前の確認ではストレージを同期読み込みせず、このキャッシュを使用する。同意済みバージョンが現在必要なバージョンと一致しない場合は、機能OFFと同様に保留処理をキャンセルする。

### 5.5 許可時間・一時解除

ユーザーが必要な場面でShortsを閲覧できるよう、ブロック機能は単純なON／OFFに加えて、以下の例外設定を持つ。

| 設定 | 初期値 | 動作 |
| --- | --- | --- |
| 1日あたりのShorts許可時間 | `10`分または初回選択 | `許可しない`、`5`、`10`、`15`、`20`、`30`、`60`分から選択する。`許可しない`は内部値`0`分として扱う。当日消費時間が上限未満の場合はShorts退避を行わない。上限到達後はブロック対象とする。 |
| 一時解除 | 無効 | ユーザー操作で指定時間だけブロックを停止する。期限到達後は自動で通常ブロックへ戻る。 |
| 一時解除開始ボタン | ブロック説明画面のみ表示 | Shorts拒否時間中に表示されるブロック説明画面から、必要な場合だけ一時解除する。 |
| 一時解除停止ボタン | 設定画面とブロック説明画面に表示 | 一時解除中に期限を即時クリアし、通常ブロックへ戻す。 |

許可時間の消費は端末内でのみ管理する。日付切り替えは端末のローカル日付を基準とし、日付が変わった場合は当日消費時間をリセットする。アプリがShorts滞在時間を完全に測定できない場合は、検知イベント時刻と離脱/画面遷移時刻から保守的に推定し、精度不足時はユーザーに不利な過大消費を避ける。

一時解除は「どうしてもShortsを見る必要がある」場合の逃げ道として提供する。解除操作は意図しないタップで発動しないよう、設定画面またはブロック説明画面の明示的なボタンからのみ実行する。

### 5.6 ブロック時のユーザー通知・説明画面

自動的なBACK/HOMEだけではユーザーがアプリの正常動作と認識しづらいため、ブロック時は以下のいずれかを表示する。

1. **ブロック説明画面（推奨）**
   - Shorts拒否時間中のためブロック画面を表示したことを示す。
   - 今日の残り許可時間または上限到達を表示する。
   - 「YouTube Homeへ戻る」「一時解除」「設定を開く」を提供する。
   - 画面にはYouTubeの動画タイトルや閲覧内容など、画面由来の個人情報を保存・表示しない。
2. **軽量通知/Toast（フォールバック）**
   - Activity起動が不安定な状態、またはHOME操作失敗時など、説明画面を出せない場合に限定する。
   - HOME操作失敗時は成功表現を出さない。

画面効果を使う場合は、ユーザーの操作を妨げるオーバーレイではなく、アプリ自身の画面遷移または短時間の通知に限定する。AccessibilityServiceから他アプリ上へ常時重なる表示は初期リリースでは行わない。

### 5.4 並行処理・キャンセル制御

`AccessibilityService`専用のScopeを`SupervisorJob + Dispatchers.Main.immediate`で生成し、イベント処理、設定変更通知、退避シーケンスを同じ直列コンテキストで処理する。`AccessibilityService`は`LifecycleOwner`ではないため、`lifecycleScope`には依存しない。サービスの`onDestroy()`では親Jobをキャンセルする。

- 退避シーケンス全体を単一の`evacuationJob`として管理する。
- DataStoreからOFFまたは同意の未取得・失効を受信したら、キャッシュを更新した同じメインスレッド上で`evacuationJob.cancel()`を呼び出す。
- 設定監視Flowで読込例外が発生した場合は、設定読込済みフラグを解除して実行を禁止し、退避Jobをキャンセルする。その後、上限付きバックオフで監視を再試行する。
- `delay`およびルートノード再取得の待機はキャンセル可能なサスペンド処理とする。
- 各グローバル操作の直前に、`blocking_enabled`とJobの活性状態を確認する。
- `ensureActive()`から`performGlobalAction()`まではサスペンド関数を呼ばない。同じメインスレッド上でOFF処理が割り込めない一続きの処理とする。
- 解析Jobと退避Jobの参照更新および状態初期化も同じメインスレッド上で行う。
- `CancellationException`は再送出して通常のキャンセルとして扱う。それ以外の例外は安全に記録して当該シーケンスのみ終了し、`finally`で実行中フラグ、試行回数、Job参照を初期化する。
- 古いJobの`finally`が新しいJobの状態を消さないよう、状態初期化時には自身が現在の`evacuationJob`であることを確認する。

```kotlin
private val serviceJob = SupervisorJob()
private val serviceScope =
    CoroutineScope(serviceJob + Dispatchers.Main.immediate)

private var evacuationJob: Job? = null
private var settingsLoaded = false
private var isBlockingEnabledCache = false
private var acceptedConsentVersion = 0
private val requiredConsentVersion = 1

private fun observeSettings() {
    serviceScope.launch {
        settingsRepository.runtimeSettingsFlow
            .retryWhen { cause, attempt ->
                invalidateRuntimeSettings()
                logSettingsReadError(cause)
                delay(retryDelayMs(attempt))
                true
            }
            .collect { settings ->
                isBlockingEnabledCache = settings.blockingEnabled
                acceptedConsentVersion = settings.acceptedConsentVersion
                settingsLoaded = true

                if (!isRuntimeAllowed()) {
                    evacuationJob?.cancel()
                }
            }
    }
}

private fun invalidateRuntimeSettings() {
    settingsLoaded = false
    isBlockingEnabledCache = false
    acceptedConsentVersion = 0
    evacuationJob?.cancel()
}

private fun startEvacuationSequence() {
    if (!isRuntimeAllowed() || evacuationJob?.isActive == true) return

    val newJob = serviceScope.launch(start = CoroutineStart.LAZY) {
        try {
            ensureBlockingActive()
            performGlobalAction(GLOBAL_ACTION_BACK)

            delay(reevaluationIntervalMs)
            val firstRoot = getRootNodeWithRetry() ?: return@launch
            if (!shortsDetector.isShortsScreen(firstRoot)) return@launch

            ensureBlockingActive()
            performGlobalAction(GLOBAL_ACTION_BACK)

            delay(reevaluationIntervalMs)
            val secondRoot = getRootNodeWithRetry() ?: return@launch
            if (!shortsDetector.isShortsScreen(secondRoot)) return@launch

            ensureBlockingActive()
            val movedHome = performGlobalAction(GLOBAL_ACTION_HOME)
            if (movedHome) {
                showToast("Shortsから自動離脱しました")
            } else {
                logGlobalActionFailure(GLOBAL_ACTION_HOME)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logEvacuationError(e)
        } finally {
            resetSequenceStateIfCurrent(coroutineContext.job)
        }
    }
    evacuationJob = newJob
    newJob.start()
}

private suspend fun ensureBlockingActive() {
    currentCoroutineContext().ensureActive()
    if (!isRuntimeAllowed()) {
        throw CancellationException("Blocking is not currently allowed")
    }
}

private fun isRuntimeAllowed(): Boolean =
    settingsLoaded &&
        isBlockingEnabledCache &&
        acceptedConsentVersion == requiredConsentVersion

override fun onDestroy() {
    serviceJob.cancel()
    super.onDestroy()
}
```

上記は制御構造を示す擬似コードである。`runtimeSettingsFlow`はブロック設定と同意バージョンを同一スナップショットとして通知する。`retryDelayMs`は1秒から開始し、最大30秒まで増加するバックオフ値を返す。`CoroutineStart.LAZY`によりJob参照を設定してから処理を開始し、即時完了した古いJobが新しい状態を誤って初期化することを防ぐ。

```text
[Shorts検知]
      |
      v
[BACK 1回目] -- 500 ms後に明示的再評価 --+-- [離脱成功] --> 終了
                                          |
                                          +-- [Shorts継続]
                                                   |
                                                   v
                                             [BACK 2回目]
                                                   |
                                      500 ms後に明示的再評価
                                                   |
                                   +---------------+---------------+
                                   |                               |
                              [離脱成功]                    [Shorts継続]
                                   |                               |
                                  終了                  [HOME + Toast] --> 終了
```

## 6. 設定・データ

| キー | 型 | 初期値 | 用途 |
| --- | --- | --- | --- |
| `blocking_enabled` | Boolean | `true` | ブロック機能のON／OFF |
| `accepted_consent_version` | Int | `0` | ユーザーが同意した開示バージョン。`0`は未同意 |
| `daily_allowance_minutes` | Int | `10` | 1日あたりのShorts許可時間。選択肢は`0`（許可しない）、`5`、`10`、`15`、`20`、`30`、`60`分 |
| `allowance_used_ms` | Long | `0` | 当日のShorts許可時間の消費量 |
| `allowance_date` | String | 端末ローカル日付 | 許可時間消費を紐づける日付 |
| `temporary_unblock_until_epoch_ms` | Long | `0` | 一時解除の期限。現在時刻以下なら解除なし |
| `block_intervention_screen_enabled` | Boolean | `true` | ブロック時の説明画面を表示するか |
| `reevaluation_interval_ms` | Long | `500` | 退避操作後に画面を再評価するまでの待機時間 |
| `analysis_interval_ms` | Long | `300` | 通常イベントに対するノード探索の最短開始間隔 |

Phase 2ではテスト用ビルドに限り`isEnabled = true`のメモリ上の仮フラグを使用する。Phase 3でDataStoreを使用する`BlockSettingsRepository`へ置き換え、初回読込完了前は`blocking_enabled = false`相当として扱う。開示内容を変更した場合は必要同意バージョンを更新し、再同意まで解析・退避処理を停止する。初期リリースで保存する情報は端末内の設定値、許可時間消費量、一時解除期限のみとし、外部送信は行わない。画面テキストやノード情報はデバッグ用途に限定し、リリースビルドではログに出力しない。

## 7. AccessibilityService設定

`res/xml/accessibility_service_config.xml`では、少なくとも以下を指定する。

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:packageNames="com.google.android.youtube"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFlags="flagDefault|flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100" />
```

| 設定 | 意図 |
| --- | --- |
| `canRetrieveWindowContent` | ビュー階層を取得して解析できるようにする。 |
| `flagReportViewIds` | `viewIdResourceName`を判定材料として利用できるようにする。 |
| `flagRetrieveInteractiveWindows` | ダイアログやオーバーレイを含む対話可能ウィンドウを取得する。 |
| `accessibilityEventTypes` | 画面切り替えと画面内要素の動的更新を捕捉する。 |
| `packageNames` | イベントの対象をYouTubeアプリに限定する。 |

## 8. 例外・安全設計

- ルートノードを取得できない場合は処理を終了する。
- ノード探索には件数または深さの上限を設け、メインスレッドの長時間占有を防ぐ。
- 解析中に例外が発生してもサービスを停止させず、そのイベントの処理のみを終了する。
- YouTube以外のアプリではノードを解析せず、グローバル操作を実行しない。
- 通常動画、検索結果、チャンネル画面、Shortsタブでは遮断しない。
- サービス破棄時には保留中の再評価処理をキャンセルする。
- 退避シーケンスは同時に1つだけ実行し、終了時に試行回数と実行状態を必ず初期化する。
- 機能がOFFになった場合は、イベント解析、trailing実行、退避シーケンスをキャンセルして状態を初期化する。
- 非同期処理の終了処理は`finally`相当の経路に集約し、例外発生時にも実行中フラグを解除する。
- サービス破棄時にはサービス専用Scopeの親Jobをキャンセルし、設定監視を含むすべての子Jobを終了する。
- 設定未読込または現在の開示バージョンへ未同意の場合は、画面解析およびグローバル操作を行わない。
- 設定読込に失敗した場合は実行設定を無効化し、退避処理を停止してから監視を再試行する。
- HOME操作が失敗した場合は成功通知を表示せず、個人情報や画面内容を含まない失敗情報のみを記録する。
- 許可時間や一時解除中は退避シーケンスを開始しない。解除期限到達後または許可時間上限到達後は自動的にブロック対象へ戻す。
- ブロック説明画面を表示する場合でも、YouTubeの画面内容、動画タイトル、チャンネル名などは保存・表示しない。

## 9. 受け入れ条件

| ID | 条件 |
| --- | --- |
| AC-01 | Android 8.0以上の端末で、サービスをユーザー補助設定から有効化できる。 |
| AC-02 | 通常のShorts再生画面では、遷移から1秒以内に画面離脱が完了する。 |
| AC-03 | 通常動画、検索結果、ホーム、チャンネル画面、Shortsタブでは、Shorts判定がTRUEにならず、BACK、HOME、Toastを含む退避シーケンスが開始されない。 |
| AC-04 | 機能をOFFにすると、Shorts再生画面を開いても遮断されない。 |
| AC-05 | 画面変更イベントが連続しても、戻る操作がループしない。 |
| AC-06 | YouTube以外のアプリ操作に影響しない。 |
| AC-07 | BACKを2回実行してもShortsが継続する場合、遷移から1.5秒以内にHOMEへ退避し、通知を1回表示する。 |
| AC-08 | 退避シーケンス中に機能をOFFにすると、次のアクションを実行せず、保留処理と実行状態が解除される。 |
| AC-09 | 再評価時にルートノードを取得できない場合、100 ms後に1回だけ再取得し、失敗時はグローバル操作を追加実行せず終了する。 |
| AC-10 | ユーザー補助設定へ誘導する前に個別の目立つ開示を表示し、ユーザーが明示的に同意した場合のみ設定画面へ進む。同意しない場合は設定画面を開かない。Android設定から直接サービスを有効化しても、未同意または同意バージョン不一致なら解析・退避処理を行わない。 |
| AC-11 | サービス起動後、DataStoreの初回読込が完了するまでは解析・退避処理を行わない。保存済み設定がOFFの場合、読込後も処理を開始しない。 |
| AC-12 | HOME操作が失敗した場合、「Shortsから自動離脱しました」という成功通知を表示しない。 |

## 10. テスト環境

### 10.1 最低合格構成

| 項目 | 最低合格構成 | 推奨構成 |
| --- | --- | --- |
| Android OS | API 26、31、34 | API 26〜35の代表バージョン |
| 表示言語 | 日本語、英語 | 日本語、英語 |
| YouTube | 検証時点の最新安定版 | 最新安定版と、入手可能な場合は1世代前 |
| 実行環境 | Pixel実機1台以上、および不足するAPIのエミュレータ | Pixel、Galaxyなど複数メーカーの実機 |

最低合格構成の各OSで日本語または英語の少なくとも一方を検証し、全体として両言語を含める。全OS・全言語・全端末の直積は必須としない。検証日、端末、OS、YouTubeバージョン、表示言語、結果を記録する。

### 10.2 合格基準

- AC-01〜AC-12を満たす。
- 通常動画、検索画面、チャンネル画面、Shortsタブで誤検知しない。
- 通常ケースは1秒以内、HOMEへのフォールバックを含むケースは1.5秒以内に離脱する。

### 10.3 性能基準

| 評価項目 | 合格基準 | 測定方法 |
| --- | --- | --- |
| 単一解析時間 | `isShortsScreen`の処理時間が基準実機でp95 16 ms以内 | ウォームアップ後100回以上測定し、単調増加時計による開始・終了差分を集計 |
| CPU使用率 | 10秒間の連続スクロール時、本アプリプロセスの平均CPU使用率が5%未満 | 同一端末、同一ビルド、同一操作シナリオでProfilerまたはPerfettoを使用 |
| ANR | 3分間の連続画面切り替えで0件 | 実機操作とLogcat／ANR記録を確認 |
| 解析多重実行 | 同時実行数が常に1以下 | テスト用カウンターの最大値を記録 |

性能試験では端末名、OS、YouTubeバージョン、ビルド種別、測定ツールを記録する。CPU測定値はProfiler接続によるオーバーヘッドを含むため、同一条件で比較する。基準端末は検証時点で調達可能なPixel実機1台とし、具体的な機種を試験記録で固定する。

## 11. 制約・リスク

- YouTubeのUI、View ID、コンテンツ説明の変更により検知できなくなる可能性がある。
- 端末メーカー、Androidバージョン、YouTubeの言語設定によって画面構造が異なる可能性がある。
- ユーザー補助サービスの用途、説明文、データ処理はGoogle Playの最新ポリシーに適合させる必要がある。
- OS仕様上、ユーザーが明示的にユーザー補助サービスを有効化する必要がある。

## 12. 将来拡張

- ブロック履歴・回数の可視化
- 検知条件のリモート設定
- 曜日や時間帯によるブロック設定
- 多言語・複数バージョンの検知条件拡充
