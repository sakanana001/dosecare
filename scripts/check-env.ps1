# check-env.ps1
# 检查 DoseCare 工程编译所需的全部环境
#
# 用法：在工程根目录打开 PowerShell
#   .\scripts\check-env.ps1
#
# 期望：所有项都显示 ✓ OK
# 如果有 ❌，按提示安装

$ErrorActionPreference = 'Stop'

function Test-Command {
    param([string]$Name, [string]$MinVersion, [scriptblock]$VersionCheck)
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $cmd) {
        Write-Host "  ❌ $Name 未找到" -ForegroundColor Red
        return $false
    }
    try {
        $ver = & $VersionCheck
        Write-Host "  ✓ $Name  $ver" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "  ✓ $Name  (found, version unknown)" -ForegroundColor Green
        return $true
    }
}

function Test-Env {
    param([string]$Name, [string]$Value, [string]$Hint)
    if ($Value) {
        Write-Host "  ✓ $Name  = $Value" -ForegroundColor Green
        return $true
    } else {
        Write-Host "  ❌ $Name 未设置" -ForegroundColor Red
        if ($Hint) { Write-Host "      → $Hint" -ForegroundColor Yellow }
        return $false
    }
}

Write-Host ""
Write-Host "=== DoseCare 环境检查 ===" -ForegroundColor Cyan
Write-Host ""

$allOk = $true

# 1. JDK 17
Write-Host "[1/4] JDK 17" -ForegroundColor Yellow
$ok = Test-Command "java" "17" {
    $jv = & java -version 2>&1 | Select-Object -First 1
    return ($jv -replace '.*"(\d+\.\d+).*','$1')
}
if (-not $ok) {
    Write-Host "      → 安装：winget install Microsoft.OpenJDK.17  或  choco install temurin17" -ForegroundColor Yellow
    Write-Host "      → 或下载：https://adoptium.net/temurin/releases/?version=17" -ForegroundColor Yellow
    $allOk = $false
}

# 2. JAVA_HOME
Write-Host "[2/4] JAVA_HOME" -ForegroundColor Yellow
if (Test-Env "JAVA_HOME" $env:JAVA_HOME "设置 JAVA_HOME 指向 JDK 17 安装根目录") { } else { $allOk = $false }

# 3. Android SDK
Write-Host "[3/4] Android SDK (compileSdk 35, minSdk 26)" -ForegroundColor Yellow
$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if ($sdk -and (Test-Path $sdk)) {
    Write-Host "  ✓ Android SDK  = $sdk" -ForegroundColor Green
    $platformsPath = Join-Path $sdk "platforms"
    $buildToolsPath = Join-Path $sdk "build-tools"
    if (Test-Path (Join-Path $platformsPath "android-35")) {
        Write-Host "  ✓ android-35 platform 已安装" -ForegroundColor Green
    } else {
        Write-Host "  ❌ android-35 未安装（SDK Manager 里勾选 Android 35）" -ForegroundColor Red
        $allOk = $false
    }
    if ((Get-ChildItem $buildToolsPath -ErrorAction SilentlyContinue | Where-Object { $_.PSIsContainer -eq $false }).Count -gt 0) {
        Write-Host "  ✓ build-tools 已安装" -ForegroundColor Green
    } else {
        Write-Host "  ❌ build-tools 未安装（SDK Manager 里勾选）" -ForegroundColor Red
        $allOk = $false
    }
} else {
    Write-Host "  ❌ Android SDK 未配置（ANDROID_HOME 或 ANDROID_SDK_ROOT）" -ForegroundColor Red
    Write-Host "      → 最简单：装 Android Studio（自带 SDK）" -ForegroundColor Yellow
    Write-Host "      → https://developer.android.com/studio" -ForegroundColor Yellow
    $allOk = $false
}

# 4. adb（可选）
Write-Host "[4/4] adb（可选，需要时再装）" -ForegroundColor Yellow
$adbOk = Test-Command "adb" "" {
    $av = & adb --version 2>&1 | Select-Object -First 1
    return $av
}
if (-not $adbOk) {
    Write-Host "      → 通常 Android SDK platform-tools 自带，ANDROID_HOME/platform-tools/ 应在 PATH" -ForegroundColor Yellow
}

Write-Host ""
if ($allOk) {
    Write-Host "✅ 环境就绪，可以跑 build" -ForegroundColor Green
    Write-Host "   下一步：.\scripts\build.ps1" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host "❌ 环境未完整，请按上面提示安装" -ForegroundColor Red
    Write-Host "   最快路径：装 Android Studio（一次解决全部）" -ForegroundColor Cyan
    exit 1
}
