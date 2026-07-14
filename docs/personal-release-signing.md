# 個人利用向け Release APK 署名

このプロジェクトは、個人利用向けにローカルだけで完結する release APK 署名設定に対応しています。

実際の keystore ファイル、パスワード、`keystore.properties` は絶対にコミットしないでください。ローカルマシン内だけに置き、必要なら自分だけが見られる安全な場所へバックアップしてください。

## 1. ローカル keystore を作成する

リポジトリルートから実行します。

```powershell
cd <repo-root>\ShortsBlocker
New-Item -ItemType Directory -Force -Path release
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair `
  -v `
  -keystore release\shortsblocker-release.p12 `
  -storetype PKCS12 `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -alias shortsblocker
```

長期間なくさず管理できるパスワードを使ってください。このキーを失うと、新しいキーで署名したAPKを、古い署名済みアプリの上書きアップデートとしてインストールできなくなります。

## 2. `keystore.properties` を作成する

テンプレートをコピーします。

```powershell
Copy-Item keystore.properties.example keystore.properties
```

ローカルの `keystore.properties` を編集します。

```properties
storeFile=release/shortsblocker-release.p12
storePassword=<your-keystore-password>
keyAlias=shortsblocker
keyPassword=<your-key-password>
```

## 3. 署名済み Release APK をビルドする

`keystore.properties` が作成されるまで、releaseビルドは意図的に失敗します。これは、未署名のrelease APKを誤って使わないようにするためです。

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:Path"
.\gradlew.bat assembleRelease
```

出力先は次の通りです。

```text
app\build\outputs\apk\release\app-release.apk
```

## 4. 自分の端末へインストールする

USBデバッグを有効にし、端末を接続してから実行します。

```powershell
adb devices
adb install -r .\app\build\outputs\apk\release\app-release.apk
```

Android側で「不明なアプリのインストール」がブロックされる場合は、APKを開くために使うアプリにインストール許可を与えるか、ADBでのインストールを使ってください。

## 5. あとで更新する場合

今後のバージョンでも、同じ keystore と同じ alias を使い続けてください。古いrelease版の上に新しいrelease版をインストールする前に、`versionCode` を増やしてください。
