# ScreenPulse · 屏幕脉搏

Android 原生屏幕使用时长监控应用（Kotlin + UsageStatsManager）。

**[直接下载最新版 APK（无需打开 ChatGPT 附件）](https://raw.githubusercontent.com/zi2111s/apk/main/ScreenPulse-debug.apk)**

首次安装后需在 Android 设置中授权 **使用情况访问权限**。不需要 Root、不读取屏幕画面、不使用网络权限。

## 更新记录

### v1.0.1 · 2026-10-22
- 优化每 15 秒的自动刷新：使用数据在后台读取，界面原位更新，不再先清空整页或显示全屏加载动画。
- 今日总时长、当前应用、小时图表、应用时长与限额进度条均可直接更新。
- 保持滚动位置，排行未变化时保留原有应用列表和图标。
- 修复启动时重复执行一次刷新，以及临时查询失败导致统计页面被覆盖。

### v1.0.0
- 今日前台使用统计、24 小时图表、应用使用排行、历史趋势。
- 手动启用状态栏监控、超时提醒、悬浮计时器和 CSV 导出。

## 自动构建与分发

`.github/workflows/build-apk.yml` 在 `main` 分支源代码更新后自动构建 Android APK。
构建成功将自动更新仓库根目录的 **`ScreenPulse-debug.apk`**；手机直接访问上面的固定链接即可取得最新版本，不依赖 ChatGPT 文件下载。
[查看构建记录](https://github.com/zi2111s/apk/actions)。

**注意**：当前仍采用 GitHub 临时构建机的 debug 签名。不同构建的签名可能不一致，导致 Android 不允许覆盖安装；如出现签名冲突，需卸载旧版后再安装，既有系统用量记录仍由 Android 系统保留，但 App 内自定义偏好会丢失。后续正式版应采用仅存放在私密仓库 Secrets 中的固定签名密钥。
