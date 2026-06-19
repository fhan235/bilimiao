#!/usr/bin/env bash
# 激活当前 shell 的 Android 编译环境
# 用法：source /Users/fenghan/Desktop/PycharmProj/bilimiao2/env-android.sh

export JAVA_HOME="$HOME/.jdks/jdk-17.0.19+10/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "[env-android] JAVA_HOME=$JAVA_HOME"
echo "[env-android] ANDROID_HOME=$ANDROID_HOME"
echo "[env-android] java=$(java -version 2>&1 | head -1)"
