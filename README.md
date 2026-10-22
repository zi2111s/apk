# 屏幕脉搏 · ScreenPulse

Android 屏幕使用时长监控应用（Kotlin + 原生 Android 视图）。应用通过 Android `UsageStatsManager` 读取系统前台使用记录。

## 📱 手机直接下载

**[下载最新版 ScreenPulse APK](https://raw.githubusercontent.com/zi2111s/apk/main/ScreenPulse-debug.apk)**

仓库固定文件名，手机可直接打开上方链接下载，不需要从 ChatGPT 附件保存文件。

## 最新版本：v1.2.1
- 青绿色与暖灰色的 ScreenPulse 专属主题、更新后的文案与卡片配色。
- 设置页新增“检查软件更新”：只读取公开的 `version.json`，发现新版本后打开 APK 下载链接。
- 正式使用 GitHub Secrets 中的固定签名密钥签署 APK，并在构建后验证签名。
- 每次构建自动递增 `versionCode`，成功后将 APK 和版本信息更新到仓库。
- 保留本机使用统计、24小时分布、应用排行、提醒、悬浮计时、CSV 导出等功能。

## 第一次升级说明
从之前使用临时 debug 签名的 v1.1.0 升级到固定 release 签名的 v1.2.1，Android 可能提示签名冲突。**需要卸载旧版一次后再安装 v1.2.1**。卸载会清除 ScreenPulse 自己保存的提醒/限额设置；Android 系统保留的应用使用统计不依赖 ScreenPulse，通常仍可读取。此后版本请保管好同一把签名密钥，直接覆盖安装即可。

## 签名密钥
- 永久签名私钥由本人本地生成。
- `SCREENPULSE_KEYSTORE_BASE64` 和 `SCREENPULSE_STORE_PASSWORD` 只保存在 GitHub Actions Repository Secrets。
- 私钥文件与密码不得提交到仓库或发送到聊天中。
- 请把 `ScreenPulse-release.jks` 与密码安全备份；丢失私钥将影响未来覆盖升级。

## 隐私与网络
使用记录在本机计算，不会上传。v1.2.1 增加了联网权限，但只用于用户主动点击“检查软件更新”时读取公开版本文件；不上传应用使用数据。

## 自动构建
修改源代码或构建工作流后，GitHub Actions 会编译、签名、验证 APK，并更新仓库根目录的 `ScreenPulse-debug.apk`。
[查看构建历史](https://github.com/zi2111s/apk/actions)。
