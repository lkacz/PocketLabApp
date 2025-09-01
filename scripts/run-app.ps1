<#!
.SYNOPSIS
  Build, install, and launch the PocketLab debug app on an Android emulator.
.DESCRIPTION
  Ensures ANDROID_SDK_ROOT and PATH are set for this session, starts (or reuses) the specified AVD,
  waits for full boot, assembles :app:debug, installs via adb (replace), and launches the main activity.
.PARAMETER AvdName
  Name of the AVD to start (default: Medium_Phone_API_35)
.PARAMETER NoWipe
  If set, does not wipe data when starting emulator (default wipes only if emulator fails first attempt)
.PARAMETER JustInstall
  Skip build & emulator start; only install & launch latest assembled debug APK.
.EXAMPLE
  ./scripts/run-app.ps1
.EXAMPLE
  ./scripts/run-app.ps1 -AvdName Medium_Phone_API_35 -NoWipe
#>
[CmdletBinding()]
param(
  [string]$AvdName = 'Pixel_8_Pro',
  [switch]$NoWipe,
  [switch]$JustInstall,
  [switch]$ColdBoot,          # Force a cold boot (-wipe-data) on first start
  [string]$LaunchActivity     # Optional fully qualified activity name (e.g. com.lkacz.pola/.MainActivity or .MainActivity)
)

$ErrorActionPreference = 'Stop'

function Write-Section($t){ Write-Host "`n=== $t ===" -ForegroundColor Cyan }

# 1. Resolve project root (script directory's parent)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Root = Resolve-Path (Join-Path $ScriptDir '..')
Set-Location $Root

# 2. Infer SDK path from local.properties if present (only if not already valid)
if(-not ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT))){
  $localProps = Join-Path $Root 'local.properties'
  if(Test-Path $localProps){
    $sdkLine = (Get-Content $localProps | Where-Object { $_ -match '^sdk.dir=' })
    if($sdkLine){
      $raw = ($sdkLine -replace '^sdk.dir=','').Trim()
      # Collapse doubled backslashes from properties escaping
      while($raw -match '\\\\'){ $raw = $raw -replace '\\\\','\\' }
      $env:ANDROID_SDK_ROOT = $raw
    }
  }
}
if(-not $env:ANDROID_SDK_ROOT){
  $defaultSdk = "$env:USERPROFILE\AppData\Local\Android\Sdk"
  if(Test-Path $defaultSdk){ $env:ANDROID_SDK_ROOT = $defaultSdk }
}
if(-not (Test-Path $env:ANDROID_SDK_ROOT)){
  Write-Host "DEBUG ANDROID_SDK_ROOT value='$env:ANDROID_SDK_ROOT'" -ForegroundColor Yellow
  throw "ANDROID_SDK_ROOT not found. Set it manually or install the SDK."
}

# 3. Update PATH (idempotent for session)
$pathsToAdd = @("$env:ANDROID_SDK_ROOT\platform-tools","$env:ANDROID_SDK_ROOT\emulator")
foreach($p in $pathsToAdd){ if($env:Path -notmatch [regex]::Escape($p)){ $env:Path = "$p;$env:Path" } }

Write-Section "Environment"
Write-Host "ANDROID_SDK_ROOT=$env:ANDROID_SDK_ROOT"

# Helper: wait for device boot
function Wait-ForDevice{
  param([int]$TimeoutSec=420)
  $sw = [Diagnostics.Stopwatch]::StartNew()
  Write-Host "Waiting for emulator to come online (timeout=${TimeoutSec}s)..."
  $lastPhase = ''
  while($sw.Elapsed.TotalSeconds -lt $TimeoutSec){
  $state = ''
  try { $state = (& adb get-state 2>$null) } catch { $state = '' }
    if($state -eq 'device'){
      $boot = (& adb shell getprop sys.boot_completed 2>$null).Trim()
      $unlock = (& adb shell getprop dev.bootcomplete 2>$null).Trim()
      if($boot -eq '1' -and $unlock -eq '1'){
        Write-Host "Device ready in $([int]$sw.Elapsed.TotalSeconds)s." -ForegroundColor Green
        return
      } elseif($lastPhase -ne 'framework'){ Write-Host 'ADB device detected; waiting for framework...' ; $lastPhase='framework' }
    } elseif($lastPhase -ne 'adb'){ Write-Host 'ADB offline; polling...' ; $lastPhase='adb' }
    Start-Sleep -Seconds 3
  }
  throw "Emulator failed to boot within $TimeoutSec seconds"
}

function Start-Emulator([string]$Name, [switch]$Cold, [switch]$SkipSnapshots){
  Write-Section "Starting emulator $Name";
  $emuArgs = @('-avd', $Name, '-netdelay','none','-netspeed','full')
  if($Cold){ $emuArgs += '-wipe-data' }
  if($SkipSnapshots){ $emuArgs += @('-no-snapshot-load','-no-snapshot-save') }
  # Ensure logs directory
  $logDir = Join-Path $Root 'logs'
  if(-not (Test-Path $logDir)){ New-Item -ItemType Directory -Path $logDir | Out-Null }
  $timestamp = (Get-Date -Format 'yyyyMMdd_HHmmss')
  $stdoutLog = Join-Path $logDir "emulator-${Name}-${timestamp}.out.log"
  $stderrLog = Join-Path $logDir "emulator-${Name}-${timestamp}.err.log"
  Write-Host "StdOut: $stdoutLog"; Write-Host "StdErr: $stderrLog"
  Start-Process -FilePath 'emulator' -ArgumentList $emuArgs -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog | Out-Null
  Start-Sleep -Seconds 4
}

# 4. Start emulator unless already running; include one automatic retry with cold boot on failure
$devices = (& adb devices) -join "\n"
if($devices -notmatch 'emulator-'){
  if($JustInstall){ throw 'No emulator/device connected and JustInstall specified.' }
  $attempt = 1
  $maxAttempts = 2
  $coldFirst = $ColdBoot.IsPresent
  while($attempt -le $maxAttempts){
    $useCold = $coldFirst -or ($attempt -gt 1)
    Start-Emulator -Name $AvdName -Cold:($useCold) -SkipSnapshots:(!$NoWipe)
    try {
      Wait-ForDevice
      break
    } catch {
      if($attempt -lt $maxAttempts){
        Write-Host "Emulator start attempt $attempt failed: $($_.Exception.Message). Retrying with cold boot..." -ForegroundColor Yellow
        # Kill any stray emulator/qemu processes before retry
        Get-Process | Where-Object { $_.Name -match 'emulator|qemu' } | ForEach-Object { try { $_.Kill() } catch {} }
        Start-Sleep -Seconds 5
        $attempt++
        continue
      } else { throw }
    }
  }
} else {
  Write-Host 'Emulator already running.'
  Wait-ForDevice
}

if(-not $JustInstall){
  Write-Section 'Assembling Debug APK'
  ./gradlew.bat :app:assembleDebug | Write-Host
}

# 5. Locate APK
$apk = Get-ChildItem -Path (Join-Path $Root 'app\build\outputs\apk\debug') -Filter '*-debug.apk' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if(-not $apk){ throw 'Debug APK not found. Build may have failed.' }
Write-Host "Using APK: $($apk.FullName)"

Write-Section 'Installing APK'
& adb install -r $apk.FullName | Write-Host

Write-Section 'Launching App'
$pkg = 'com.lkacz.pola'

# Resolve component if LaunchActivity provided
$component = ''
if($LaunchActivity){
  if($LaunchActivity -match '/'){ # already a component
    $component = $LaunchActivity
  } elseif($LaunchActivity.StartsWith('.')){ # relative class
    $component = "$pkg/$pkg$LaunchActivity"
  } elseif($LaunchActivity -like 'com.*'){ # fully qualified class, missing slash
    $component = "$pkg/$LaunchActivity"
  } else { # simple class name
    $component = "$pkg/.$LaunchActivity"
  }
}

if($component){
  Write-Host "Attempting explicit start: $component"
  $res = & adb shell am start -W -n $component 2>&1
  Write-Host $res
  if($LASTEXITCODE -eq 0 -and $res -match 'Status: ok'){ Write-Section 'Done'; return }
  Write-Host 'Explicit start failed; falling back to monkey.' -ForegroundColor Yellow
}

& adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 | Write-Host

Write-Section 'Done'
