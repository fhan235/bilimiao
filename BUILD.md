# bilimiao2 本地编译指南

本文档说明如何在本地把 `bilimiao2` 编译成 APK。

---

## 一、项目编译矩阵

`app/build.gradle.kts` 里定义了 `flavor × buildType`，常用组合如下：

| Flavor | BuildType | 任务名 | 是否需要签名 | 是否依赖闭源库 | 用途 |
|---|---|---|---|---|---|
| `foss` | `debug` | `assembleFossDebug` | 否 | 否 | **首选**，本地快速验证 |
| `foss` | `release` | `assembleFossRelease` | 是 | 否 | F-Droid 风格发布版 |
| `full` | `debug` | `assembleFullDebug` | 否 | 是（百度统计 / 极验 / av1） | 完整功能调试版 |
| `full` | `release` | `assembleFullRelease` | 是 | 是 | 官方发布版 |

> `full` flavor 依赖 `app/libs/lib-decoder-av1-release.aar` 与百度、极验闭源 SDK；
> 仓库里已有 av1 aar，但百度/极验是远端依赖，正常会从 maven 拉取。
> 如果只是想"先编一个能装的包"，**用 `fossDebug` 最省心**。

---

## 二、环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17+（推荐 21） | Gradle 8.13 要求 |
| Android SDK | platform-35、build-tools 36 | 见 `app/build.gradle.kts` |
| Gradle | 8.13 | 由 `./gradlew` 自动下载，无需手装 |

### 2.1 JDK

macOS 上推荐**直接用 Android Studio 自带的 JBR**（已自带 JDK 21），无需额外安装：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version   # 应输出 openjdk 21.x
```

> 也可以用 Homebrew：`brew install openjdk@17`，然后按 brew 提示设置 `JAVA_HOME`。

### 2.2 Android SDK

如果安装过 Android Studio，SDK 默认在：

- macOS: `~/Library/Android/sdk`
- Windows: `%LOCALAPPDATA%\Android\Sdk`
- Linux: `~/Android/Sdk`

确认根目录下的 `local.properties` 指向该路径（已有就跳过）：

```properties
sdk.dir=/Users/你的用户名/Library/Android/sdk
```

确认已安装：
- `platforms/android-35`
- `build-tools/36.x.x`（或 35）
- `cmdline-tools` / `platform-tools`

缺什么用 SDK Manager 装一下即可。

---

## 三、一键编译（fossDebug，推荐）

在项目根目录 `bilimiao2/` 下执行：

```bash
# macOS / Linux
chmod +x ./gradlew
./gradlew :app:assembleFossDebug
```

```bat
:: Windows
gradlew.bat :app:assembleFossDebug
```

> 首次编译会下载 Gradle 8.13 + 所有依赖，**耗时通常 5~15 分钟**，请耐心等待。
> 之后增量编译一般 30 秒以内。

成功后 APK 输出位置：

```
app/build/outputs/apk/foss/debug/app-foss-debug.apk
```

直接把该 apk 推到手机即可：

```bash
adb install -r app/build/outputs/apk/foss/debug/app-foss-debug.apk
```

---

## 四、编译 Release 包（需要签名）

### 4.1 准备签名文件

在 `app/` 目录下新建 `signing.properties`（**不要提交到 git**）：

```properties
KEY_ALIAS=你的alias
KEY_PASSWORD=你的key密码
KEYSTORE_FILE=/绝对路径/到/你的.jks
KEYSTORE_PASSWORD=你的keystore密码
```

> 没有 keystore 时，可以用：
> ```bash
> keytool -genkeypair -v -keystore bilimiao.jks -alias bilimiao \
>   -keyalg RSA -keysize 2048 -validity 36500
> ```

### 4.2 编译

```bash
# 推荐：F-Droid 风格、不含闭源 SDK
./gradlew :app:assembleFossRelease

# 完整功能版（包含百度统计、极验、av1 解码）
./gradlew :app:assembleFullRelease
```

输出位置：

```
app/build/outputs/apk/foss/release/app-foss-release.apk
app/build/outputs/apk/full/release/app-full-release.apk
```

如果没有提供 `signing.properties`，`release` 包会因为没有签名而无法安装；
但你仍然能编出 `*-release-unsigned.apk`，自己手动用 `apksigner` 签名也行。

---

## 五、常见问题

### 1. `Unable to locate a Java Runtime`
说明当前 shell 找不到 Java。按 [2.1 JDK](#21-jdk) 设置 `JAVA_HOME` 即可。

### 2. `SDK location not found`
检查根目录下 `local.properties` 中的 `sdk.dir` 是否指向正确的 Android SDK。

### 3. `Failed to install the following Android SDK packages...`
缺 SDK 组件。打开 Android Studio → SDK Manager 装齐 platform-35 / build-tools 36。

### 4. 编译卡在下载依赖
项目根 `build.gradle.kts` 已配置阿里云镜像 + jitpack；如果你在内网，可在
`~/.gradle/init.gradle.kts` 里加全局镜像。

### 5. 想清理重编
```bash
./gradlew clean
./gradlew :app:assembleFossDebug
```

### 6. 编 `full` flavor 报缺少 av1 aar
确认 `app/libs/lib-decoder-av1-release.aar` 存在；不存在就只能编 `foss` flavor。

---

## 六、Android Studio 图形化编译（最简单）

1. 用 Android Studio 打开项目根目录 `bilimiao2/`；
2. 等 Gradle Sync 完成；
3. 顶部菜单 **Build → Select Build Variant**，选择 `fossDebug`；
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**；
5. 完成后右下角通知里点 **locate** 即可看到 apk。

---

## TL;DR

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
chmod +x ./gradlew
./gradlew :app:assembleFossDebug
# 产物: app/build/outputs/apk/foss/debug/app-foss-debug.apk
```
