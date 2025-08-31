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
  [string]$AvdName = 'Medium_Phone_API_35',
  [switch]$NoWipe,
  [switch]$JustInstall
)

$ErrorActionPreference = 'Stop'

function Write-Section($t){ Write-Host "`n=== $t ===" -ForegroundColor Cyan }

# 1. Resolve project root (script directory's parent)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Root = Resolve-Path (Join-Path $ScriptDir '..')
Set-Location $Root

# 2. Infer SDK path from local.properties if present
$localProps = Join-Path $Root 'local.properties'
if(Test-Path $localProps){
  $sdkLine = (Get-Content $localProps | Where-Object { $_ -match '^sdk.dir=' })
  if($sdkLine){ $env:ANDROID_SDK_ROOT = ($sdkLine -replace '^sdk.dir=','') -replace '\\\\','\\' }
}
if(-not $env:ANDROID_SDK_ROOT){
  $defaultSdk = "$env:USERPROFILE\AppData\Local\Android\Sdk"
  if(Test-Path $defaultSdk){ $env:ANDROID_SDK_ROOT = $defaultSdk }
}
if(-not (Test-Path $env:ANDROID_SDK_ROOT)){ throw "ANDROID_SDK_ROOT not found. Set it manually or install the SDK." }

# 3. Update PATH (idempotent for session)
$pathsToAdd = @("$env:ANDROID_SDK_ROOT\platform-tools","$env:ANDROID_SDK_ROOT\emulator")
foreach($p in $pathsToAdd){ if($env:Path -notmatch [regex]::Escape($p)){ $env:Path = "$p;$env:Path" } }

Write-Section "Environment"
Write-Host "ANDROID_SDK_ROOT=$env:ANDROID_SDK_ROOT"

# Helper: wait for device boot
function Wait-ForDevice{
  param([int]$TimeoutSec=480)
  $sw = [Diagnostics.Stopwatch]::StartNew()
  Write-Host "Waiting for emulator to come online..."
  while($sw.Elapsed.TotalSeconds -lt $TimeoutSec){
    $state = & adb get-state 2>$null
    if($state -eq 'device'){
      $boot = (& adb shell getprop sys.boot_completed 2>$null).Trim()
      if($boot -eq '1'){ Write-Host 'Device ready.'; return }
    }
    Start-Sleep -Seconds 5
  }
  throw "Emulator failed to boot within $TimeoutSec seconds"
}

# 4. Start emulator unless already running
$devices = (& adb devices) -join "\n"
if($devices -notmatch 'emulator-'){ if(-not $JustInstall){
    Write-Section "Starting emulator $AvdName"
    $args = @('-avd', $AvdName)
    if(-not $NoWipe){ $args += '-no-snapshot-load' }
    Start-Process -FilePath 'emulator' -ArgumentList $args | Out-Null
    Wait-ForDevice
  } else { throw 'No emulator/device connected and JustInstall specified.' }
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
& adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 | Write-Host

Write-Section 'Done'
