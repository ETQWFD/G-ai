#!/bin/bash
# G-ai APK 构建脚本（aapt2 + javac + d8 + zipalign + apksigner，无 Gradle 依赖）
set -e
export JAVA_HOME=/home/user/tools/jdk-17.0.20.1+1
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=/home/user/tools/android-sdk
BT=$ANDROID_HOME/build-tools/34.0.0
AJ=$ANDROID_HOME/platforms/android-34/android.jar
APP=/home/user/Doubao/chats/38441427699972610/G-ai
cd "$APP"

echo "== 1/6 编译资源 =="
rm -rf build && mkdir -p build/gen build/classes build/apk
$BT/aapt2 compile --dir res -o build/res.zip

echo "== 2/6 链接资源与 Manifest =="
$BT/aapt2 link -o build/apk/base.apk -I "$AJ" \
  --manifest AndroidManifest.xml \
  --java build/gen \
  --auto-add-overlay \
  --min-sdk-version 24 --target-sdk-version 34 \
  build/res.zip

echo "== 3/6 编译 Java =="
find src build/gen -name "*.java" > build/sources.txt
javac -source 1.8 -target 1.8 -bootclasspath "$AJ" -classpath "$AJ" \
  -d build/classes @build/sources.txt 2> build/javac.log || { cat build/javac.log; echo "JAVAC FAILED"; exit 1; }
grep -v "^Note:" build/javac.log || true

echo "== 4/6 D8 生成 classes.dex =="
$BT/d8 --release --lib "$AJ" --output build/apk $(find build/classes -name "*.class")
cd build/apk && zip -q -X base.apk classes.dex && cd ../..

echo "== 5/6 对齐 =="
$BT/zipalign -f 4 build/apk/base.apk build/apk/aligned.apk

echo "== 6/6 签名 =="
KS="$APP/keystore.jks"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias gai -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass gai2026 -keypass gai2026 \
    -dname "CN=G-ai,O=G-ai,OU=G-ai,L=Meizhou,ST=Guangdong,C=CN" 2>/dev/null
fi
$BT/apksigner sign --ks "$KS" --ks-key-alias gai --ks-pass pass:gai2026 --key-pass pass:gai2026 \
  --out "$APP/G-ai-v1.1.0.apk" build/apk/aligned.apk
$BT/apksigner verify --verbose "$APP/G-ai-v1.1.0.apk" | head -6
echo "== 完成 =="
ls -la "$APP/G-ai-v1.1.0.apk"
