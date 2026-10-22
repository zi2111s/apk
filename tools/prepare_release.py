#!/usr/bin/env python3
"""Prepare a signed ScreenPulse release from the repository's source archive."""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path.cwd()
APP = ROOT / "ScreenPulse" / "app"
ACTIVITY = APP / "src" / "main" / "java" / "com" / "screenpulse" / "app" / "MainActivity.kt"
MANIFEST = APP / "src" / "main" / "AndroidManifest.xml"
GRADLE = APP / "build.gradle.kts"

for p in (ACTIVITY, MANIFEST, GRADLE):
    if not p.exists():
        raise SystemExit(f"Required source file missing: {p}")

# Give the app a distinct ScreenPulse palette. This is a custom restyle, not a copy of the reference image.
s = ACTIVITY.read_text(encoding="utf-8")
palette = {
    r'private val blue = Color\.rgb\(\d+,\s*\d+,\s*\d+\)': 'private val blue = Color.rgb(24, 157, 163)',
    r'private val deepBlue = Color\.rgb\(\d+,\s*\d+,\s*\d+\)': 'private val deepBlue = Color.rgb(17, 105, 126)',
    r'private val ink = Color\.rgb\(\d+,\s*\d+,\s*\d+\)': 'private val ink = Color.rgb(25, 39, 56)',
    r'private val muted = Color\.rgb\(\d+,\s*\d+,\s*\d+\)': 'private val muted = Color.rgb(112, 130, 143)',
    r'private val pale = Color\.rgb\(\d+,\s*\d+,\s*\d+\)': 'private val pale = Color.rgb(247, 249, 247)',
    r'private val line = Color\.rgb\(\d+,\s*\d+,\s*\d+\)': 'private val line = Color.rgb(225, 234, 232)',
    r'private val green = Color\.rgb\(\d+,\s*\d+,\s*\d+\)': 'private val green = Color.rgb(22, 151, 126)',
}
for pattern, replacement in palette.items():
    s, n = re.subn(pattern, lambda _m, r=replacement: r, s, count=1)
    if n != 1:
        raise SystemExit(f"Could not apply custom UI palette rule: {pattern}")

s = s.replace('Color.rgb(235, 242, 255)', 'Color.rgb(229, 245, 242)')
s = s.replace('"真实系统使用事件 · 自动统计"', '"看见时间的流动 · 让每次使用更有意义"')
s = s.replace('"了解你每天使用设备的变化"', '"每一天的变化，都值得看见"')
s = s.replace('"所有统计数据仅保存在本机"', '"使用记录保存在本机，版本更新可联网检查"')
s = s.replace('"不要求联网，也不上传任何使用记录"', '"仅联网检查版本；不会上传使用记录"')

# Add the update checker imports without duplicating them on repeated builds.
for import_line in ('import org.json.JSONObject', 'import java.net.HttpURLConnection', 'import java.net.URL'):
    if import_line not in s:
        s = s.replace('import android.widget.Toast\n', 'import android.widget.Toast\n' + import_line + '\n', 1)

settings_anchor = '        column.addView(data, pageWidth().apply { topMargin = dp(14) })'
if 'addSettingsRow(data, "检查软件更新"' not in s:
    if settings_anchor not in s:
        raise SystemExit("Could not find settings data card insertion point for update checker")
    update_row = '''        addSettingsRow(data, "检查软件更新", "检查是否有新版；使用记录不会上传", "检查") {
            checkForUpdates()
        }
'''
    s = s.replace(settings_anchor, update_row + settings_anchor, 1)

method_anchor = '    private fun showInfo() {'
if 'private fun checkForUpdates()' not in s:
    if method_anchor not in s:
        raise SystemExit("Could not find method insertion point for update checker")
    method = r'''    private fun checkForUpdates() {
        toast("正在检查更新…")
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                val conn = (URL("https://raw.githubusercontent.com/zi2111s/apk/main/version.json").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    requestMethod = "GET"
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("User-Agent", "ScreenPulse-Android")
                }
                connection = conn
                val jsonText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val remote = JSONObject(jsonText)
                val latestCode = remote.optLong("versionCode", 0L)
                val latestName = remote.optString("versionName", "未知")
                val downloadUrl = remote.optString("downloadUrl", "https://github.com/zi2111s/apk")
                val info = packageManager.getPackageInfo(packageName, 0)
                @Suppress("DEPRECATION")
                val legacyCode = info.versionCode.toLong()
                val currentCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else legacyCode
                val currentName = info.versionName ?: "未知"
                ui.post {
                    if (isFinishing || isDestroyed) return@post
                    if (latestCode <= 0L) {
                        AlertDialog.Builder(this)
                            .setTitle("暂时无法获取版本信息")
                            .setMessage("更新服务器返回的数据无效，请稍后重试。")
                            .setPositiveButton("知道了", null)
                            .show()
                    } else if (latestCode > currentCode) {
                        AlertDialog.Builder(this)
                            .setTitle("发现新版本 v$latestName")
                            .setMessage("当前版本：v$currentName\n最新版本：v$latestName\n\n点击下载后，请在系统安装界面确认更新。")
                            .setNegativeButton("稍后", null)
                            .setPositiveButton("下载更新") { _, _ ->
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
                            }
                            .show()
                    } else {
                        AlertDialog.Builder(this)
                            .setTitle("已经是最新版本")
                            .setMessage("当前版本 v$currentName，没有发现更新。")
                            .setPositiveButton("知道了", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                ui.post {
                    if (isFinishing || isDestroyed) return@post
                    AlertDialog.Builder(this)
                        .setTitle("检查更新失败")
                        .setMessage("无法连接更新服务器。请检查网络后重试。\n\n${e.localizedMessage ?: "网络连接不可用"}")
                        .setPositiveButton("知道了", null)
                        .show()
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

'''
    s = s.replace(method_anchor, method + method_anchor, 1)

ACTIVITY.write_text(s, encoding="utf-8")

manifest = MANIFEST.read_text(encoding="utf-8")
if 'android.permission.INTERNET' not in manifest:
    manifest = re.sub(r'(<manifest[^>]*>\s*)', r'\1    <uses-permission android:name="android.permission.INTERNET" />\n', manifest, count=1)
MANIFEST.write_text(manifest, encoding="utf-8")

gradle = GRADLE.read_text(encoding="utf-8")
m = re.search(r'versionCode\s*=\s*(\d+)', gradle)
if not m:
    raise SystemExit("Could not locate versionCode in app/build.gradle.kts")
version_code = max(int(m.group(1)) + 1, 4)
gradle = re.sub(r'versionCode\s*=\s*\d+', f'versionCode = {version_code}', gradle, count=1)
gradle = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.2.1"', gradle, count=1)
signing = '''
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("SCREENPULSE_KEYSTORE_PATH") ?: error("SCREENPULSE_KEYSTORE_PATH is missing"))
            storePassword = System.getenv("SCREENPULSE_STORE_PASSWORD") ?: error("SCREENPULSE_STORE_PASSWORD is missing")
            keyAlias = "screenpulse"
            keyPassword = System.getenv("SCREENPULSE_STORE_PASSWORD") ?: error("SCREENPULSE_STORE_PASSWORD is missing")
        }
    }
'''
if 'create("release")' not in gradle:
    if '    buildTypes {' not in gradle:
        raise SystemExit("Could not find buildTypes block in app/build.gradle.kts")
    gradle = gradle.replace('    buildTypes {', signing + '\n    buildTypes {', 1)
if 'signingConfig = signingConfigs.getByName("release")' not in gradle:
    gradle, n = re.subn(r'(release\s*\{\s*\n)', r'\1            signingConfig = signingConfigs.getByName("release")\n', gradle, count=1)
    if n != 1:
        raise SystemExit("Could not attach permanent signing config to release build")
GRADLE.write_text(gradle, encoding="utf-8")

(ROOT / "version.json").write_text(json.dumps({
    "versionName": "1.2.1",
    "versionCode": version_code,
    "downloadUrl": "https://raw.githubusercontent.com/zi2111s/apk/main/ScreenPulse-debug.apk"
}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Prepared ScreenPulse v1.2.1 (versionCode {version_code}) with teal UI and in-app update checker.")
