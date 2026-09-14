# Build Instructions

## 사전 요구사항
- JDK 17 이상
- Android SDK (API Level 34, Build-Tools 34.0.0)
- Gradle 8.8 (wrapper 포함)

## 빌드 방법

### 1. 프로젝트 컴파일
```bash
./gradlew compileDebugKotlin
```

### 2. Release APK 생성
```bash
./gradlew assembleRelease
```
생성 위치: `app/build/outputs/apk/release/app-release-unsigned.apk`

### 3. 서명 적용 (선택)
```bash
keytool -genkeypair -v -keystore test.keystore -alias testkey -keyalg RSA -keysize 2048 -validity 10000 -storepass password -keypass password -dname "CN=Test, OU=Test, O=Test, L=Test, S=Test, C=US"

$ANDROID_HOME/build-tools/34.0.0/apksigner sign --ks test.keystore --ks-pass pass:password --out app/build/outputs/apk/release/app-release.apk app/build/outputs/apk/release/app-release-unsigned.apk
```
서명된 APK 위치: `app/build/outputs/apk/release/app-release.apk`
