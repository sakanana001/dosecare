# build.ps1
# 一键编译 DoseCare Debug APK
#
# 前置：环境检查通过（.\scripts\check-env.ps1）
#
# 用法：在工程根目录
#   .\scripts\build.ps1
#
# 成功：app/build/outputs/apk/debug/app-debug.apk 出现

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host "=== DoseCare 一键构建 ===" -ForegroundColor Cyan
Write-Host "Project root: $projectRoot"
Write-Host ""

# 1. 环境检查
Write-Host "[1/3] 环境检查..." -ForegroundColor Yellow
& "$PSScriptRoot\check-env.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 环境检查失败，先解决环境" -ForegroundColor Red
    exit 1
}

# 2. 触发 wrapper 生成
Write-Host ""
Write-Host "[2/3] 准备 gradle wrapper..." -ForegroundColor Yellow
if (-not (Test-Path "gradlew")) {
    Write-Host "  gradlew 不存在（Android Studio 第一次 import 会自动生成）" -ForegroundColor Yellow
    Write-Host "  临时方案：手动从 Android Studio 同步或运行 gradle wrapper 命令" -ForegroundColor Yellow
    if (Get-Command gradle -ErrorAction SilentlyContinue) {
        Write-Host "  系统 gradle 存在，跑 gradle wrapper..." -ForegroundColor Cyan
        & gradle wrapper --gradle-version 8.10.2
    } else {
        Write-Host "  ❌ 没装 gradle 也没装 Android Studio" -ForegroundColor Red
        Write-Host "  请先装 Android Studio 并 import 一次（IDE 会自动生成 wrapper）" -ForegroundColor Red
        exit 1
    }
}

# 3. assembleDebug
Write-Host ""
Write-Host "[3/3] 跑 ./gradlew assembleDebug..." -ForegroundColor Yellow
Write-Host "  (首次会下载 200MB+ 依赖，可能 5-10 分钟)" -ForegroundColor Gray
& .\gradlew assembleDebug --stacktrace

if ($LASTEXITCODE -eq 0) {
    $apk = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apk) {
        $size = [math]::Round((Get-Item $apk).Length / 1MB, 2)
        Write-Host ""
        Write-Host "✅ 构建成功！" -ForegroundColor Green
        Write-Host "   APK: $apk ($size MB)" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "   下一步：adb install $apk" -ForegroundColor Cyan
        Write-Host "   或：在 Android Studio 里点 Run 'app'" -ForegroundColor Cyan
    } else {
        Write-Host "⚠️  gradlew 返回 0 但 APK 文件未找到，请检查 build/outputs/" -ForegroundColor Yellow
    }
} else {
    Write-Host ""
    Write-Host "❌ 构建失败，错误日志见上" -ForegroundColor Red
    Write-Host "   把错误贴回来给我看" -ForegroundColor Cyan
    exit 1
}
