# ScreenPulse：只需一次设置永久 APK 签名

## 你需要做的两件事

1. 在自己的 Windows 电脑上生成永久签名密钥。
2. 把两个机密值保存到 GitHub **Repository secrets**；不要提交到代码仓库，不要发在聊天里。

### 第 1 步：生成私钥
安装 Java 17 JDK（或使用已安装的 Android Studio 自带 keytool）。

在 GitHub 上打开并下载 [create-signing-key.ps1](./tools/create-signing-key.ps1)，放到下载目录，打开 PowerShell 执行：
```powershell
cd "$HOME\Downloads"
powershell -NoProfile -ExecutionPolicy Bypass -File .\create-signing-key.ps1
```

它会提示你为签名密钥设置一个**强密码**。完成后在你的电脑上创建：
`%USERPROFILE%\ScreenPulse-PrivateSigning\ScreenPulse-release.jks`

**务必备份此 .jks 文件和密码到安全位置**，例如离线加密备份。**绝不要提交 .jks 到公开仓库。**

脚本会把密钥文件的 Base64 文本复制到剪贴板（仅暂存，不会提交到 GitHub）。

### 第 2 步：添加 GitHub Actions Secrets
用 GitHub 登录并打开： https://github.com/zi2111s/apk/settings/secrets/actions

在 **Repository secrets** 区域，点 **New repository secret** 添加两项：

| Name | Secret |
| --- | --- |
| `SCREENPULSE_KEYSTORE_BASE64` | 运行脚本后粘贴剪贴板内容（完整一行）。 |
| `SCREENPULSE_STORE_PASSWORD` | 生成密钥时自己设置的密码。 |

密钥别名已固定为 `screenpulse`，不用额外填写。

完成后回到 ChatGPT **只说“两个 Secrets 已添加”**，不用分享任何密钥、文本或密码。我会接着更新仓库编译工作流，验证 APK 签名并发布下一版。

### 关于旧版覆盖安装
现有 Android 安装包如果使用了不同签名，即使建立永久密钥，也无法以新密钥覆盖。**可能还需要卸载一次旧版**，之后（保管好这份新签名密钥、保持包名不变且递增 versionCode）就可覆盖安装。卸载可能删除 ScreenPulse 内部设置，Android 系统本身存储的使用情况统计不会因为卸载 ScreenPulse 而被删除。

### 保密
- 不要把 `.jks`、签名密码、Base64 私钥提交到公有代码库。
- GitHub Secrets 名称可以给我确认；值不要发给我。
- 私钥与密码丢失，可能无法再发布与已安装版本兼容的 APK。
