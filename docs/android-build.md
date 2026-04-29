# 安卓编译与 GitHub Actions 出包说明

本仓库是一个原生 Android 单机小游戏合集，入口在 `app/src/main/java/com/kk14222/gamex/MainActivity.java`。

## 本地编译

需要本机安装：

- JDK 17
- Android SDK（包含对应 `compileSdk` 的平台包）

在仓库根目录运行：

```bash
./gradlew assembleDebug
```

生成的调试安装包位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 不配置本地环境，直接用 GitHub Actions 生成 APK

可以。仓库已经包含 `.github/workflows/android-build.yml`，它会在 GitHub 托管的 Ubuntu runner 上自动安装 JDK、Android SDK，并执行 Gradle 构建。

使用方式：

1. 把代码推送到 GitHub。
2. 进入仓库页面的 **Actions**。
3. 选择 **Android APK Build**。
4. 点击 **Run workflow** 手动触发，或者在推送/PR 后等待自动执行。
5. 构建完成后，在 workflow run 页面底部的 **Artifacts** 下载 `game-x-debug-apk`。
6. 解压 artifact 后得到 `app-debug.apk`，可以安装到安卓设备测试。

> 说明：当前 workflow 生成的是 debug APK，适合测试安装。如果要发布到应用商店，应再配置签名证书并构建 release APK/AAB，不要把签名密钥直接提交到仓库。
