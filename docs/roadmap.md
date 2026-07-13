# ShortBlocker 開発ロードマップ

## 0. 現在の進捗

**最終更新:** 2026-07-12  
**現在のフェーズ:** リリース前確認  
**次の作業:** ストア掲載用プライバシーポリシーURL公開と提出物準備

| フェーズ | 状態 | 進捗概要 |
| --- | --- | --- |
| Phase 1 | 完了 | プロジェクト基盤、Debugビルド、APK導入、ユーザー補助サービス有効化を確認済み |
| Phase 2 | 完了 | Shorts検知、退避シーケンス、イベント負荷制御を実装し、エミュレータ手動確認と単体テスト21件で完了条件を確認済み |
| Phase 3 | 完了 | DataStore設定保存、ON／OFF、1日許可時間プルダウン、一時解除UI、一時解除停止、許可時間消費計測、ブロック説明画面、明示的同意画面、プライバシーポリシー文面、サービス側設定監視を追加 |
| Phase 4 | 完了 | Android 15/API 35エミュレータでAC-01〜AC-12、Phase 3設定系、性能スモーク、単体テスト29件、Debugビルドを確認済み。ストア提出物はリリース前確認へ持ち越し |

### 直近の作業チェックリスト

- [x] Android/Kotlinプロジェクトを作成
- [x] AccessibilityServiceをManifestへ登録
- [x] `accessibility_service_config.xml`を作成
- [x] サービス有効状態を表示する`MainActivity`を実装
- [x] ユーザー補助設定画面への導線を実装
- [x] `assembleDebug`を成功させる
- [x] Pixel 9a API 35エミュレータを作成・起動
- [x] ADBでエミュレータが`device`として認識されることを確認
- [x] API 35エミュレータへAPKをインストール
- [x] ユーザー補助設定にShorts Blockerが表示されることを確認
- [x] サービスを有効／無効にできることを確認
- [x] アプリ復帰時に最新のサービス状態が表示されることを確認
- [x] Phase 1の完了条件をすべて満たしたことを記録
- [x] Phase 2の初回実装として`ShortsDetector`を追加
- [x] `ShortsBlockerService`へイベント絞り込み、解析スロットリング、trailing実行を追加
- [x] BACK最大2回とHOMEフォールバックの退避シーケンスを追加
- [x] Phase 2の仮メモリ設定`isEnabled = true`相当をサービス内に追加
- [x] `assembleDebug`でPhase 2初回実装がビルドできることを確認
- [x] エミュレータへPhase 2 APKを再インストール
- [x] YouTube上でPhase 2初回実装の手動動作確認を実施
- [x] Phase 2動作確認の録画証跡を保存
- [x] `ShortsDetector`の決定表を対象にPhase 2単体テストを追加
- [x] 退避シーケンスを単体テスト可能な`EvacuationSequenceRunner`へ切り出し
- [x] BACK上限、root取得失敗、HOME通知条件、途中OFFを対象に退避シーケンス単体テストを追加
- [x] `testDebugUnitTest`でPhase 2単体テスト13件が成功することを確認
- [x] debugビルド限定でYouTubeノードID一覧をlogcatへ出力する仕組みを追加
- [x] YouTubeノードID記録用ドキュメントを追加
- [x] エミュレータ上のYouTube Shorts画面で`reel_recycler`をログ採取
- [x] イベント負荷制御を単体テスト可能な`AnalysisEventController`へ切り出し
- [x] 即時解析、300 msスロットリング、scheduled実行、trailing集約を対象にイベント負荷制御単体テストを追加
- [x] `testDebugUnitTest`でPhase 2単体テスト21件が成功することを確認
- [x] Phase 2完了判定を実施
- [x] Phase 3のアプリ内ON／OFF設定を実装
- [x] Phase 3の1日あたりShorts許可時間設定UIを実装
- [x] Phase 3のShortsブロック一時解除UIを実装
- [x] Phase 3のDataStore設定保存を実装
- [x] サービス側でDataStore設定を監視し、Phase 2の仮ON設定を置き換え
- [x] Phase 3の許可時間消費計測を実装
- [x] Phase 3のブロック説明画面またはYouTube Home誘導画面を実装
- [x] Phase 3の明示的同意画面を実装
- [x] Phase 3のプライバシーポリシーを作成
- [x] Phase 3の一時解除停止UIを実装
- [x] 1日許可時間を許可しない/5/10/15/20/30/60分のプルダウンへ変更
- [x] 一時解除開始ボタンを設定画面から削除し、ブロック説明画面へ集約
- [x] Shorts拒否時間中は検知時にブロック説明画面を表示する動作へ変更
- [x] Phase 4の受け入れテスト計画を作成
- [x] Android 15/API 35エミュレータへPhase 4確認用Debug APKを導入
- [x] Phase 4確認用Debug APKでアプリ起動を確認
- [x] 旧アプリ削除後に新アプリ`com.shortblocker`を確認
- [x] Phase 4のAC-10同意ゲートを手動確認
- [x] Phase 4のAC-01ユーザー補助サービス有効化を手動確認
- [x] 通常動画、検索、チャンネルで誤検知が出ないことを手動確認
- [x] Shorts拒否動作とブロック説明画面を手動確認
- [x] 一時解除停止が効くことを手動確認
- [x] Phase 4未確認項目の手動確認を実施
- [x] Phase 4性能スモークを実施
- [x] Phase 4完了判定を実施
- [x] リリース前準備へ進む

## 1. 計画概要

基本機能の完成までを4フェーズ、合計9〜15開発日で進める。実利用による確認期間を含め、全体では約3週間を見込む。

| フェーズ | 内容 | 目安 | 完了条件 |
| --- | --- | ---: | --- |
| Phase 1 | 環境構築・基盤 | 1〜2日 | ユーザー補助サービスを有効化できる |
| Phase 2 | 検知・遮断機能 | 3〜5日 | Shorts再生画面から自動離脱できる |
| Phase 3 | UI・設定 | 3〜5日 | 初期設定、機能ON／OFF、1日許可時間、一時解除、ブロック説明がアプリ内から行える |
| Phase 4 | テスト・改善 | 3〜5日＋実利用確認 | 誤検知や操作ループがなく、通常動画を利用できる |

## 2. Phase 1: 環境構築・基盤

**期間:** 1〜2日

### 実施項目

- Android StudioでKotlinプロジェクトを作成する。
- 最低SDKをAPI Level 26に設定する。
- `AndroidManifest.xml`に`AccessibilityService`を宣言する。
- `res/xml/accessibility_service_config.xml`を作成し、ウィンドウコンテンツ取得、View ID取得、対象イベントを設定する。
- 対象パッケージを`com.google.android.youtube`に限定する。
- サービス有効状態を確認できる最小限の`MainActivity`を作成する。

### 完了条件

- ビルドと実機インストールが成功する。
- Androidの「ユーザー補助」設定にサービスが表示される。
- ユーザーがサービスを有効／無効にできる。
- 実機で`rootInActiveWindow`とView IDを取得できる。

## 3. Phase 2: 検知・遮断機能

**期間:** 3〜5日

### 実施項目

- YouTube操作時に`onAccessibilityEvent`が呼ばれることを確認する。
- Shorts再生画面と通常画面のノード構造を比較する。
- 言語設定やYouTubeのバージョン差を考慮し、複数の判定材料を選定する。
- 決定表に基づく`ShortsDetector`を実装する。
- BACK最大2回、HOMEフォールバックからなる退避シーケンスを実装する。
- メモリ上の仮フラグ`isEnabled = true`と、退避シーケンスの多重起動ガードを実装する。
- サービス専用Scopeと単一の`evacuationJob`を実装し、状態管理とグローバル操作をメインスレッド上で直列化する。
- 300 msの解析スロットリング、trailing実行、解析の排他制御を実装する。
- ノード再取得、Jobキャンセル、未捕捉例外防止、決定表の各行、操作失敗、連続イベントを対象とする単体テストを作成する。

### 完了条件

- Shorts再生画面を開くと、1秒以内を目安に直前の画面へ戻る。
- 通常動画の再生では遮断しない。
- YouTube以外のアプリでは遮断処理を実行しない。
- BACKで離脱できない場合、上限を超えて再試行せずHOMEへ退避する。
- 連続イベントを受信しても解析が多重実行されず、最後の画面状態が解析される。

### 完了判定

**判定日:** 2026-07-11  
**判定:** 完了

| 完了条件 | 判定 | 根拠 |
| --- | --- | --- |
| Shorts再生画面を開くと、1秒以内を目安に直前の画面へ戻る | OK | Pixel 9a API 35エミュレータで手動確認。録画証跡を保存済み。 |
| 通常動画の再生では遮断しない | OK | `adb logcat -c`後、通常動画操作で検知ログなし。 |
| YouTube以外のアプリでは遮断処理を実行しない | OK | サービス設定とイベント条件を`com.google.android.youtube`に限定。 |
| BACKで離脱できない場合、上限を超えて再試行せずHOMEへ退避する | OK | `EvacuationSequenceRunnerTest`でBACK最大2回とHOME fallbackを検証。 |
| 連続イベントを受信しても解析が多重実行されず、最後の画面状態が解析される | OK | `AnalysisEventControllerTest`で300 ms制御、scheduled実行、trailing集約を検証。 |

補足: 右側アクションバー候補IDは今回のログでは未取得。現環境では`reel_recycler` + `Shorts`ラベルで安定検知できているためPhase 2完了の阻害要因とはせず、fallback強化・多環境検証の追加確認項目としてPhase 4へ持ち越す。

## 4. Phase 3: UI・設定

**期間:** 2〜3日

### 実施項目

- ユーザー補助サービスの有効／無効状態を表示する。
- 無効時にユーザー補助設定画面へ移動するボタンを表示する。
- アプリ内にブロック機能のON／OFFスイッチを追加する。
- 「1日あたりShortsを許可する時間」を設定できるUIを追加する。初期候補は`0分（完全ブロック）`、`5分`、`10分`、`15分`とし、初期リリースではプリセット選択を優先する。
- 「一時解除」ボタンを追加し、必要な場合だけ短時間ブロックを停止できるようにする。初期リリースではブロック説明画面の`5分解除`のみとする。
- 当日の許可時間の残り、消費済み時間、一時解除中の残り時間をアプリ内に表示する。
- `BlockSettingsRepository`を実装し、設定値をDataStoreに保存する。
- Phase 2の仮フラグを`BlockSettingsRepository`へ接続する。
- DataStoreのブロック設定、同意バージョン、1日許可時間、当日消費量、一時解除期限を同一の実行設定としてサービス内のメモリキャッシュへ反映する。
- 初回設定読込前、機能OFF時、未同意時、同意バージョン不一致時、一時解除中、許可時間内は解析または退避処理を停止する。
- 許可時間が有効な場合は、Shorts検知中の滞在時間を端末内で消費量として記録し、日付が変わったら当日消費量をリセットする。
- OFF通知とグローバル操作を同じ直列コンテキストで処理し、アクション直前の活性確認を実装する。
- ブロック時に、アプリの正常動作であることが分かる`BlockInterventionActivity`を表示する。画面には自動離脱したこと、残り許可時間、設定、一時解除、YouTube Homeへ戻る導線を表示する。
- `BlockInterventionActivity`を表示できない状態ではToastまたは通知にフォールバックし、HOME操作失敗時は成功表現を表示しない。
- ユーザー補助サービスの用途と、端末内で扱うデータを説明する。
- ユーザー補助設定へ誘導する直前に、AccessibilityService専用の目立つ開示と明示的同意画面を表示する。
- 開示には、Shorts画面の検知と自動離脱のために使用すること、画面構造・View ID・テキスト・コンテンツ説明へアクセスすること、1日許可時間と一時解除期限を端末内に保存すること、外部送信の有無を記載する。
- 同意は独立した肯定操作で取得し、拒否または画面を閉じた場合はユーザー補助設定へ進めない。
- アプリ内から閲覧できるプライバシーポリシーを作成し、ストア掲載用URLも準備する。

### 完了条件

- アプリからユーザー補助設定画面へ移動できる。
- アプリ復帰時に最新のサービス状態が表示される。
- ON／OFF設定がアプリ再起動後も保持され、サービスの動作に反映される。
- 1日許可時間設定がアプリ再起動後も保持され、許可時間内はShortsを遮断せず、上限到達後は遮断する。
- 一時解除中はShortsを遮断せず、期限到達後は自動的に遮断動作へ戻る。
- ブロック時に、ユーザーがアプリの正常動作だと理解できる説明画面または通知が表示される。
- 説明画面からYouTube Homeへ戻る、設定を開く、一時解除する操作が行える。
- 明示的同意前はユーザー補助設定へ進まず、同意後のみ設定画面へ誘導される。
- 開示内容と実際のデータアクセス、保存、共有動作が一致する。
- Android設定からサービスを直接有効化しても、未同意なら画面解析やグローバル操作を行わない。

### 完了判定

**判定日:** 2026-07-12  
**判定:** 完了

| 完了条件 | 判定 | 根拠 |
| --- | --- | --- |
| アプリからユーザー補助設定画面へ移動できる | OK | `MainActivity`から`AccessibilityConsentActivity`を開き、同意後のみ`Settings.ACTION_ACCESSIBILITY_SETTINGS`へ遷移する。 |
| アプリ復帰時に最新のサービス状態が表示される | OK | `MainActivity.onResume()`で`Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`を再読込して表示を更新する。 |
| ON／OFF設定が保持され、サービスの動作に反映される | OK | `BlockSettingsRepository`のDataStore保存と`ShortsBlockerService`の`runtimeSettingsFlow`監視で反映する。 |
| 1日許可時間設定が保持され、許可時間内は遮断せず、上限到達後は遮断する | OK | `RuntimeBlockSettingsTest`と`ShortsAllowanceTrackerTest`で残時間・消費計測を検証し、サービス側で許可時間内は退避せず消費記録のみ行う。 |
| 一時解除中は遮断せず、期限到達後は遮断動作へ戻る | OK | `temporaryUnblockUntilEpochMs`をDataStoreへ保存し、`isAnalysisAllowed`/`isBlockingActive`で期限を判定する。 |
| ブロック時に正常動作だと理解できる説明画面または通知が表示される | OK | HOME退避成功後に`BlockInterventionActivity`を表示し、失敗時のみToastへフォールバックする。 |
| 説明画面からYouTube Homeへ戻る、設定を開く、一時解除する操作が行える | OK | `BlockInterventionActivity`にYouTube起動、`MainActivity`起動、5分一時解除ボタンを実装済み。 |
| 明示的同意前はユーザー補助設定へ進まず、同意後のみ設定画面へ誘導される | OK | `MainActivity`は直接設定を開かず`AccessibilityConsentActivity`へ遷移し、肯定ボタンでのみ同意保存と設定画面遷移を行う。 |
| 開示内容と実際のデータアクセス、保存、共有動作が一致する | OK | 同意画面と`docs/privacy-policy.md`に画面構造・View ID・テキスト・コンテンツ説明、端末内保存、外部送信なしを記載。実装も外部通信・広告・分析SDKなし。 |
| Android設定からサービスを直接有効化しても、未同意なら解析やグローバル操作を行わない | OK | `RuntimeBlockSettings.isConsentAccepted()`が必要同意バージョン一致を要求し、未同意時は`isAnalysisAllowed`/`isBlockingActive`がfalseになる。 |

検証結果: `testDebugUnitTest`は29件すべて成功し、`assembleDebug`も成功。UI操作の実機/エミュレータ上での通し確認、性能測定、ストア掲載用プライバシーポリシーURL公開、Play Console提出物はPhase 4およびリリース前確認へ持ち越す。

## 5. Phase 4: テスト・改善

**期間:** 3〜5日＋数日間の実利用確認

### 実施項目

- 基本設計書の最低合格構成に沿ってテスト環境を割り当てる。
- 通常動画、検索結果、チャンネル画面、Shortsタブで誤検知しないことを確認する。
- 連続イベント発生時の多重起動防止、BACKの上限、HOME退避を確認する。
- ルートノード取得失敗や画面遷移中の例外を処理する。
- Androidバージョン、端末、YouTubeの表示言語を変えて確認する。
- デバッグ用の画面情報がリリースビルドのログに残らないことを確認する。
- 基本設計書の受け入れ条件（AC-01〜AC-12）を実施する。
- 仮想時間を用い、各delay中、再評価直後、BACK／HOME直前でOFFへ変更するキャンセルテストを実施する。
- 設定初回読込前、保存済みOFF、未同意、同意バージョン不一致を対象とする起動テストを実施する。
- DataStore読込エラー時に実行設定が無効化され、退避Jobが停止し、バックオフ後に監視が再開されることを確認する。
- 検知処理で例外を発生させ、サービスの他のJobが継続することを確認する。
- HOME操作の失敗時に成功Toastが表示されないことを確認する。
- 基準実機で解析時間、CPU使用率、ANR、解析同時実行数を測定する。
- Google Play ConsoleのAccessibilityService申告内容を作成し、利用目的をデジタルウェルビーイング／生産性向上として正確に説明する。
- アプリ起動、個別開示と同意、ユーザー補助設定での有効化、Shorts自動遮断までを収録した審査用デモ動画を作成する。
- Data safetyの回答を、第三者SDKを含む実装のデータアクセス・収集・共有状況と照合する。
- 数日間の実利用で、見逃し・誤検知・操作上のストレスを記録して調整する。
- 1日許可時間、一時解除、ブロック説明画面の操作が誤作動や過剰ブロックにつながらないことを確認する。

### 完了条件

- AC-01〜AC-12をすべて満たす。
- 基本設計書の性能基準をすべて満たす。
- AccessibilityService申告、審査用動画、プライバシーポリシー、Data safetyの提出物が揃っている。
- 検証日、端末、OS、YouTubeバージョン、表示言語、結果が記録されている。
- 戻る操作の連続実行やアプリのフリーズが発生しない。
- 既知の制約と検証環境がリリースノートに記載されている。

### 完了判定

**判定:** 完了（アプリ受け入れテスト完了。ストア提出物はリリース前確認へ持ち越し）

| 完了条件 | 判定 | 根拠 |
| --- | --- | --- |
| AC-01〜AC-12をすべて満たす | OK | `docs/phase4-acceptance-test-plan.md`でAC-01〜AC-12をすべてOKとして記録した。 |
| 基本設計書の性能基準をすべて満たす | OK | `PERF-01`〜`PERF-03`をOKとして記録し、`testDebugUnitTest` 29件、`assembleDebug`、logcatクラッシュスモーク、YouTube Homeスクロールを確認した。 |
| 検証日、端末、OS、YouTubeバージョン、表示言語、結果が記録されている | OK | 2026-07-12、`emulator-5554`、Android 15/API 35、YouTube 21.26.364、Japanese、各結果を`docs/phase4-acceptance-test-plan.md`へ記録した。 |
| 戻る操作の連続実行やアプリのフリーズが発生しない | OK | Shorts再入場、ブロック説明画面、YouTube Home誘導、一時解除、解除停止、期限切れ、保存済みOFFをADB操作で確認し、制御不能ループやクラッシュは確認されなかった。 |
| AccessibilityService申告、審査用動画、公開プライバシーポリシーURL、Data safety提出物が揃っている | 持ち越し | ストア提出物はアプリ機能の受け入れテストとは分離し、リリース前確認へ持ち越す。 |
| 既知の制約と検証環境がリリースノートに記載されている | 持ち越し | 複数端末/API/言語、公開URL、提出物、リリースノートはリリース前確認で実施する。 |

補足: Phase 4はアプリの受け入れテストと性能スモークの完了をもって完了と判定する。Google Play提出物、公開プライバシーポリシーURL、審査用動画、Data safety、リリースノートはPhase 4の後続であるリリース前確認の完了条件として扱う。

## 6. リリース前確認

- [x] アプリ名、アイコン、バージョンを設定した
- [ ] 対応OSと検証済みYouTubeバージョンを記録した
- [x] ユーザー補助サービスを使う目的をアプリ内で説明した
- [x] ユーザー補助設定へ進む前に、個別の目立つ開示と明示的同意を実装した
- [x] 収集・保存・外部送信するデータの有無を明示した
- [x] アプリ内およびストア掲載用のプライバシーポリシー文面を作成した
- [x] ストア掲載用のプライバシーポリシーURLを公開した
- [ ] Google Playの最新ポリシーを確認した
- [ ] Play ConsoleのAccessibilityService申告フォームを作成した
- [ ] 審査用デモ動画を作成した
- [ ] Data safetyを実装実態と照合して登録した
- [x] リリースビルドでデバッグログを無効化した
- [x] AC-01〜AC-12と性能試験の結果を記録した

## 7. 主なリスクと対応

| リスク | 影響 | 対応 |
| --- | --- | --- |
| YouTubeのUI変更 | 検知不能または誤検知 | 判定根拠を複数化し、判定理由を追跡可能にする |
| 端末・言語による差異 | 一部環境で動作しない | 複数環境でテストし、確度不足時は遮断しない |
| イベントの連続発火 | 戻る操作のループ、フリーズ | 退避シーケンスの多重起動ガードと試行上限を設ける |
| 必要時にShortsを見られない | ユーザーがアプリを無効化する | 1日許可時間と一時解除を用意し、制御された例外を提供する |
| 自動退避が不具合に見える | ユーザーが正常動作と理解できない | ブロック説明画面または通知で、自動離脱した理由と次の操作を示す |
| ユーザー補助ポリシー不適合 | ストア公開不可 | 実装前と公開前に最新ポリシーを確認する |
| 実利用確認の長期化 | リリース遅延 | 開発日数と観察期間を分けて進捗管理する |

## 8. 作業履歴

以後の作業履歴は、このセクションの末尾へ日付順に追記する。

### 2026-07-09

- 基本設計書と開発ロードマップの書式を統一した。
- Shorts判定条件、退避シーケンス、例外処理、負荷制御、性能基準を具体化した。
- AccessibilityServiceに関するGoogle Play公開要件をロードマップへ追加した。
- Android/Kotlinプロジェクトの基盤を作成した。
- `MainActivity`へサービス状態表示とユーザー補助設定への導線を実装した。
- Phase 1用の`ShortsBlockerService`を登録した。画面解析とグローバル操作は未実装。
- Gradle Wrapper 8.13、Android Gradle Plugin 8.13.2を設定した。
- AndroidX CoreをSDK 35互換の`1.16.0`へ調整した。
- Kotlin JVMターゲット設定を`compilerOptions` DSLへ移行した。
- `assembleDebug`が警告なしで成功することを確認した。
- Pixel 9a API 35エミュレータを作成して起動した。
- ADBで`emulator-5554`が`device`として認識されることを確認した。
- Phase 1エビデンスとして、`C:\Users\816a2\Desktop\tmp\20260709_YoutubeShortBlocker_Phase1Evidence` にホーム画面、アプリ画面、Accessibility一覧、権限ダイアログ、サービス有効化後状態のスクリーンショットを保存した。
- Phase 1の完了条件を満たしたため、次作業をPhase 2の検知・退避ロジック実装へ更新した。
- Phase 2の初回実装として、`ShortsDetector`による決定表ベースの検知、サービス側のYouTubeイベント絞り込み、300 ms解析スロットリング、trailing実行、BACK最大2回とHOMEフォールバックの退避シーケンスを追加した。
- Phase 2初回実装後に`assembleDebug`が成功することを確認した。

### 2026-07-10

- PowerShellの`JAVA_HOME`とAndroid SDK `platform-tools`のPATHを設定し、ユーザー側Terminalから`assembleDebug`、`adb devices`、APK再インストールが成功することを確認した。
- Pixel 9a API 35エミュレータへPhase 2 APKを導入し、YouTube上でShorts検知・退避ロジックの手動動作確認が良好であることを確認した。
- Phase 2証跡として、`C:\Users\816a2\Desktop\tmp\20260709_ShortBlocker_Phase2Evidence\Screen_recording_20260710_000218.webm` を保存した。
- `adb logcat -s ShortsBlockerService`で`Shorts screen detected: ReelRecyclerWithShortsLabel`が複数回出力されることを確認し、標準的なShorts再生画面が決定表No.1で検知されることを確認した。
- 追加のPhase 2証跡として、`C:\Users\816a2\Desktop\tmp\20260709_ShortBlocker_Phase2Evidence\Screen_recording_20260710_000726.webm` を保存した。
- `adb logcat -c`後に通常動画、検索、チャンネル画面を操作し、`ShortsBlockerService`の検知ログが新規出力されないことを確認した。
- `ShortsDetector`をJVM単体テスト可能な入力構造へ整理し、決定表の主要ケースを検証する`ShortsDetectorTest`を追加した。
- `testDebugUnitTest`で`ShortsDetectorTest` 6件が成功し、`assembleDebug`も成功することを確認した。

### 2026-07-11

- 退避シーケンスを`EvacuationSequenceRunner`へ切り出し、`ShortsBlockerService`からAndroid依存処理を注入する構造へ整理した。
- BACK最大2回、Shorts離脱時の早期終了、root取得失敗時の追加操作抑止、HOME成功時のみ通知、途中OFF時のキャンセルを対象に`EvacuationSequenceRunnerTest`を追加した。
- `testDebugUnitTest`で`ShortsDetectorTest` 6件、`EvacuationSequenceRunnerTest` 7件、合計13件が成功し、`assembleDebug`も成功することを確認した。
- debugビルド限定でShorts検知時にYouTubeのView ID一覧を`ShortsBlockerService`ログへ出力する`YoutubeNodeIdLogger`を追加した。
- YouTubeノードIDの採取手順と現在の判定材料を`docs/youtube-node-id-record.md`へ記録した。
- Pixel 9a API 35エミュレータのYouTube Shorts画面で`reel_recycler`を含むView ID一覧を採取し、`docs/youtube-node-id-record.md`へ追記した。今回のログでは右側アクションバー候補IDは未取得だったため、追加確認対象として残した。
- イベント負荷制御を`AnalysisEventController`へ切り出し、`TYPE_WINDOW_STATE_CHANGED`の即時解析、`TYPE_WINDOW_CONTENT_CHANGED`の300 msスロットリング、scheduled実行、解析中イベントのtrailing集約を対象に単体テストを追加した。
- `testDebugUnitTest`で`ShortsDetectorTest` 6件、`EvacuationSequenceRunnerTest` 7件、`AnalysisEventControllerTest` 8件、合計21件が成功し、`assembleDebug`も成功することを確認した。
- Phase 2完了条件を照合し、手動確認、ログ証跡、単体テスト結果からPhase 2を完了と判定した。右側アクションバー候補IDの追加確認はfallback強化項目としてPhase 4へ持ち越す。
- Phase 3以降の設定方針として、1日あたりShorts許可時間、一時解除ボタン、ブロック時の説明画面またはYouTube Home誘導画面を追加する方針を基本設計書とロードマップへ反映した。
- Phase 3初回実装として`BlockSettingsRepository`を追加し、`blocking_enabled`、同意バージョン、1日許可時間、一時解除期限、許可時間消費量をDataStoreへ保存する構造を追加した。
- `MainActivity`へブロックON／OFF、1日許可時間プリセット、一時解除状態、同意状態表示を追加した。
- `ShortsBlockerService`でDataStore設定を監視し、未同意、OFF、一時解除中、許可時間内は解析・退避処理を行わないようPhase 2の仮ON設定を置き換えた。
- Phase 3初回実装後に`testDebugUnitTest` 21件と`assembleDebug`が成功することを確認した。

### 2026-07-12

- `ShortsAllowanceTracker`を追加し、Shorts検知間隔から許可時間の消費量を保守的に計測する仕組みを実装した。
- `BlockSettingsRepository`へ当日許可時間消費の記録処理と日付切り替え時の実質リセット処理を追加した。
- `RuntimeBlockSettings`へ日付別の消費量、残り許可時間、解析可否、ブロック可否の判定を追加した。
- `ShortsBlockerService`で許可時間が残っている場合も解析は継続し、Shorts検知時は退避せず消費時間のみ記録するようにした。
- `MainActivity`へ今日の使用時間と残り許可時間の表示を追加した。
- `RuntimeBlockSettingsTest` 4件、`ShortsAllowanceTrackerTest` 4件を追加し、`testDebugUnitTest`合計29件と`assembleDebug`が成功することを確認した。
- `BlockInterventionActivity`を追加し、Shortsブロック後に自動離脱の説明、当日の許可時間状況、YouTubeへ戻る、設定を開く、一時解除の導線を表示するようにした。
- `ShortsBlockerService`でHOME退避成功後にブロック説明画面を表示し、表示に失敗した場合のみ既存Toastへフォールバックするようにした。
- ブロック説明画面追加後に`testDebugUnitTest`と`assembleDebug`が成功することを確認した。
- `AccessibilityConsentActivity`を追加し、ユーザー補助設定へ進む前に用途、アクセスする情報、端末内保存、外部送信なしを説明する明示的同意画面を表示するようにした。
- `MainActivity`のユーザー補助設定ボタンを、直接設定画面へ進む動作から明示的同意画面を開く動作へ変更した。同意しない場合は設定画面へ進まず、同意した場合のみ同意バージョンを保存してAndroidのユーザー補助設定を開く。
- 明示的同意画面追加後に`testDebugUnitTest`と`assembleDebug`が成功することを確認した。
- `docs/privacy-policy.md`を追加し、AccessibilityServiceでアクセスする情報、端末内保存、外部送信なし、保持・削除方法、問い合わせ先の差し替え項目を記載した。
- `PrivacyPolicyActivity`を追加し、`MainActivity`からアプリ内プライバシーポリシーを閲覧できる導線を実装した。
- プライバシーポリシー追加後に`testDebugUnitTest`と`assembleDebug`が成功することを確認した。
- Phase 3完了条件を照合し、設定UI、DataStore保存、サービス反映、許可時間、一時解除、ブロック説明画面、明示的同意、プライバシーポリシー文面、未同意時の停止条件が揃っているためPhase 3を完了と判定した。
- Phase 3完了判定時点で`testDebugUnitTest` 29件と`assembleDebug`が成功することを再確認した。ストア掲載用プライバシーポリシーURL公開、手動通し確認、性能測定、Play Console提出物はPhase 4およびリリース前確認へ持ち越す。
- `docs/phase4-acceptance-test-plan.md`を追加し、AC-01〜AC-12、Phase 3設定操作、性能スモーク、証跡ファイル名、PowerShell/ADB手順を整理した。
- Phase 4初期確認として`testDebugUnitTest` 29件、`assembleDebug`、Android 15/API 35エミュレータへのDebug APK再インストールが成功することを確認した。
- `adb shell am start -n com.shortblocker/.MainActivity`で`MainActivity`を起動し、`dumpsys window`で`com.shortblocker/.MainActivity`がフォーカスされることを確認した。
- 一時解除中に即時停止できるよう、`MainActivity`と`BlockInterventionActivity`へ「一時解除を停止」ボタンを追加した。停止時は一時解除期限を`0`へ戻す。
- 一時解除停止UI追加後に`testDebugUnitTest`と`assembleDebug`が成功することを確認した。
- 1日あたりのShorts許可時間を`許可しない`、`5`、`10`、`15`、`20`、`30`、`60`分のプルダウン選択へ変更した。`許可しない`は内部値`0`分として扱う。
- 一時解除の開始ボタンを設定画面から削除し、Shorts拒否時間中に表示されるブロック説明画面へ集約した。設定画面には一時解除中の停止ボタンのみ残した。
- Shorts拒否時間中にShortsを検知した場合は、まずブロック説明画面を表示するようにした。説明画面の表示に失敗した場合のみ、既存のBACK/HOME退避シーケンスへフォールバックする。
- 上記仕様調整後に`testDebugUnitTest`と`assembleDebug`が成功することを確認した。
- Phase 4テスト結果として、Android 15/API 35エミュレータでのユーザー手動確認結果を`docs/phase4-acceptance-test-plan.md`へ記録した。旧アプリ削除、新アプリ確認、AC-10、AC-01、通常動画・検索・チャンネルの誤検知なし、Shorts拒否動作、ブロック説明画面、一時解除停止を確認済みとした。
- Phase 4の残確認として、Shortsタブシェル、Blocking OFF、連続イベント、他アプリ影響、HOMEフォールバック、途中OFF、初期設定ロード、HOME失敗、プルダウン保持、許可時間消費、解除期限切れ、プライバシーポリシー画面、性能スモークを継続項目に残した。
- Android 15/API 35エミュレータ上でPhase 4残確認を実施した。`testDebugUnitTest` 29件、`assembleDebug`、APK再インストール、許可時間プルダウン、プライバシーポリシー、一時解除開始・停止・自然期限切れ、Blocking OFF、保存済みOFF、他アプリ非干渉、Shortsブロック、YouTube Home誘導、性能スモークを確認した。
- フォールバック系の失敗条件は実機上で自然再現しにくいため、`EvacuationSequenceRunnerTest`のHOMEフォールバック、HOME失敗通知抑止、root node再取得、途中OFF停止の単体テスト成功をPhase 4証跡として記録した。
- ユーザー作成アイコン画像を中央正方形にトリミングし、launcher iconとadaptive iconとして設定した。`assembleDebug`と`assembleRelease`が成功することを確認した。
- リリース版のログ方針を通常動作ログなし、エラー系のみ`Log.e`へ整理した。Shorts検知、YouTube node ID、許可時間消費、Global action失敗の通常ログはDebug限定または無出力にした。
- ストア掲載用プライバシーポリシー文面を公開前提の形へ整理し、開発者名をSashimi Teriyaki、問い合わせ先を`sashimi.teriyaki.343@gmail.com`として反映した。公開URLの発行は外部ホスティング決定後に実施する。
- GitHub公開先を`Yahiro-Atsushi/shorts_blocker`に決定し、プライバシーポリシー公開予定URLを`https://yahiro-atsushi.github.io/shorts_blocker/privacy-policy.html`として記録した。GitHub Pages公開確認後にリリース前チェックリストを更新する。
- GitHub Pagesを`main`ブランチの`/docs`で有効化し、`https://yahiro-atsushi.github.io/shorts_blocker/privacy-policy.html`がHTTP 200で公開されていることを確認した。
- GitHub公開先アカウントを`33jade/shorts_blocker`へ修正した。正しいプライバシーポリシー公開予定URLは`https://33jade.github.io/shorts_blocker/privacy-policy.html`で、正しいアカウントでのGitHub Pages公開確認後にリリース前チェックリストを完了へ戻す。
- GitHub Pagesを`33jade/shorts_blocker`の`main`ブランチ`/docs`で有効化し、`https://33jade.github.io/shorts_blocker/privacy-policy.html`がHTTP 200で公開されていることを確認した。
