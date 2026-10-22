# ScreenPulse permanent Android signing key setup (Windows PowerShell).
# Run this on your own PC. Never upload the .jks file or password to a public GitHub repository.
$ErrorActionPreference = "Stop"
$folder = Join-Path $HOME "ScreenPulse-PrivateSigning"
$keystore = Join-Path $folder "ScreenPulse-release.jks"
$keytoolCmd = Get-Command keytool.exe -ErrorAction SilentlyContinue
$keytool = if ($null -ne $keytoolCmd) { $keytoolCmd.Source } else { $null }
if (-not $keytool) {
    $candidates = @(
        (Join-Path $env:ProgramFiles "Android\Android Studio\jbr\bin\keytool.exe"),
        (Join-Path $env:ProgramFiles "Eclipse Adoptium\jdk-17\bin\keytool.exe")
    )
    foreach ($c in $candidates) { if (Test-Path $c) { $keytool = $c; break } }
}
if (-not $keytool) {
    Write-Host "Java keytool was not found. Install JDK 17 (e.g. Temurin) or Android Studio, then run this again." -ForegroundColor Yellow
    Write-Host "Official download: https://adoptium.net/"
    exit 1
}
New-Item -ItemType Directory -Path $folder -Force | Out-Null
if (Test-Path $keystore) {
    Write-Host "Existing private key found at: $keystore" -ForegroundColor Yellow
    Write-Host "Do NOT create a new key. This script will reuse your existing key."
} else {
    Write-Host "Generate ScreenPulse signing key. Choose a STRONG password, and record it privately." -ForegroundColor Cyan
    Write-Host "The keytool prompts for your password; no password is added to a script or repository."
    & $keytool -genkeypair -v -keystore $keystore -storetype PKCS12 -alias screenpulse -keyalg RSA -keysize 3072 -validity 12000 -dname "CN=ScreenPulse, OU=Personal App, O=ScreenPulse, L=Private, ST=NA, C=US"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed. Signing key was not created." }
}
$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore))
Set-Clipboard -Value $base64
Write-Host ""
Write-Host "SUCCESS: Signing key was created / found." -ForegroundColor Green
Write-Host "Backup this file safely: $keystore"
Write-Host "Secret 1: SCREENPULSE_KEYSTORE_BASE64 -> Already copied to CLIPBOARD. Paste to GitHub Actions repository secret NOW."
Write-Host "Secret 2: SCREENPULSE_STORE_PASSWORD -> The password YOU typed when generating the key."
Write-Host "Alias is fixed to: screenpulse. PKCS12 key password matches the store password."
Write-Host ""
Write-Host "GitHub Settings: https://github.com/zi2111s/apk/settings/secrets/actions"
Write-Host "Do NOT share the key, base64 clipboard, or password in any chat."
