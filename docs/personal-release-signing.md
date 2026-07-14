# Personal Release APK Signing

This project supports a local-only release signing setup for personal APK distribution.

Do not commit any real keystore file, password, or `keystore.properties` file. Keep them only on your local machine and back them up somewhere private.

## 1. Create a Local Keystore

Run from the repository root:

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

Use a password you can keep long-term. If this key is lost, APKs signed with a new key cannot be installed as an update over the old signed app.

## 2. Create `keystore.properties`

Copy the template:

```powershell
Copy-Item keystore.properties.example keystore.properties
```

Edit `keystore.properties` locally:

```properties
storeFile=release/shortsblocker-release.p12
storePassword=<your-keystore-password>
keyAlias=shortsblocker
keyPassword=<your-key-password>
```

## 3. Build a Signed Release APK

Release builds intentionally fail until keystore.properties is created. This prevents accidentally using an unsigned release APK.

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:Path"
.\gradlew.bat assembleRelease
```

Expected output:

```text
app\build\outputs\apk\release\app-release.apk
```

## 4. Install on Your Device

Enable USB debugging, connect the device, then run:

```powershell
adb devices
adb install -r .\app\build\outputs\apk\release\app-release.apk
```

If Android blocks installing from an unknown source, allow installs for the app you use to open the APK or keep using ADB.

## 5. Updating Later

Keep using the same keystore and alias for future versions. Increase `versionCode` before installing a newer release over an older release.