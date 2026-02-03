# PlayTime Real-Time Debugging Script
# Run this script to monitor your app in real-time

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  PlayTime Music Player - Live Debug" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Check if ADB exists
if (-not (Test-Path $adb)) {
    Write-Host "ERROR: ADB not found at $adb" -ForegroundColor Red
    Write-Host "Please install Android SDK Platform Tools" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1] Checking connected devices..." -ForegroundColor Yellow
& $adb devices

# Get physical device ID (not emulator)
$deviceId = (& $adb devices | Select-String -Pattern "^\w+\s+device$" | Select-Object -First 1) -replace '\s+device', ''
Write-Host "Using device: $deviceId" -ForegroundColor Green

Write-Host ""
Write-Host "[2] Launching PlayTime app..." -ForegroundColor Yellow
& $adb -s $deviceId shell am start -n com.eplaytime.app/.MainActivity

Write-Host ""
Write-Host "[3] Starting real-time log monitoring..." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Gray
Write-Host ""

# Clear previous logs
& $adb -s $deviceId logcat -c

# Monitor logs in real-time
& $adb -s $deviceId logcat | Select-String -Pattern "PlayTime|com.eplaytime|MainActivity|MusicService|AlarmReceiver|AndroidRuntime|FATAL"
