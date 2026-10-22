# 屏幕脉搏 · ScreenPulse

Android 屏幕使用时长监控应用（Kotlin）。使用本机 Android UsageStatsManager，不读取屏幕内容、不上传应用使用记录。

## 📱 手机 APK 下载

**[最新版安装包：ScreenPulse-debug.apk](https://raw.githubusercontent.com/zi2111s/apk/main/ScreenPulse-debug.apk)**

在手机浏览器中打开链接即可下载，无需从 ChatGPT 附件保存。

## 最新统计修复（2026-10-22）

针对同一时间 vivo 系统显示 **5小时32分**，屏幕脉搏显示 **3小时41分**，以及“近7日平均显示00:00”的问题：

- 前台事件不完整时，不再把事件累计当作唯一正确值，同时读取 Android 的 **queryUsageStats(日汇总)**。
- 当系统日汇总比事件累计更完整且不超出物理使用时段时，今日总时长和应用排行榜统一以真实日汇总为准；反之继续使用事件数据。**不凭空补齐不存在的数据**。
- 修复同一应用内部 Activity 切换时，旧 Activity 的 PAUSE 把新 Activity 计时错误关掉的问题。
- 修复部分 ROM 缺失 KEYGUARD_HIDDEN 事件导致长时间暂停计时的问题。
- 7日平均剔除不可用的历史天数。少于5天可用记录时显示 **—**，不再错误地把未知日期当作0小时。
- 每次更新今日数据时会用最新的今日报告替换历史缓存中的旧“今天”；解决出现“今日有值、7日平均为00:00”的缓存问题。
- 小时图仅展示可读取的**原始前台事件**，和日汇总来源不同时可出现不一致；已经在页面明确说明，不会伪造缺失时段。
- 增加事件归因和来源选择的12项 Kotlin 逻辑测试，并通过。

### 数据口径声明

vivo 的“健康使用设备”可能采用厂商专有服务，其显示值并非 Android 公开 UsageStatsManager 保证能够完全复现的指标。本次是**减少可确认的漏计与错误显示**，并优先使用公开 API 返回的可信日汇总数据；**尚需在设备上复测**，不能保证与 vivo 系统完全相等。

## 覆盖安装和隐私

从 v1.2.2 开始使用你保存在 GitHub Actions Secrets 中的固定签名密钥。此后的签名版发布依然使用同一证书且递增 versionCode，按 Android 规则可以覆盖安装，无需卸载旧版、无需重新授权（前提是之前安装的也是相同签名版本）。

- 用户主动点击“检查更新”时才联网读取公开的 [version.json](./version.json)。
- 应用使用记录保留在本机，不上传到服务端。
- Android 13+ 系统通知、悬浮窗依然需要单独授权。

[查看 GitHub Actions 构建结果](https://github.com/zi2111s/apk/actions/workflows/build-apk.yml)
