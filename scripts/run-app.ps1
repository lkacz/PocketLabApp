<#!
.SYNOPSIS
  Build, install, and launch the PocketLab debug app on an Android emulator.
.DESCRIPTION
  Ensures ANDROID_SDK_ROOT and PATH are set for this session, starts (or reuses) the specified AVD,
  waits for full boot, assembles :app:debug, installs via adb (replace), and launches the main activity.
.PARAMETER AvdName
  Name of the AVD to start (default: Pixel_API_34)
.PARAMETER Port
  Emulator port to bind; helps avoid clashes when multiple emulators are present (default: 5580)
.PARAMETER NoWipe
  If set, does not wipe data when starting emulator (default wipes only if emulator fails first attempt)
.PARAMETER JustInstall
  Skip build & emulator start; only install & launch latest assembled debug APK.
.EXAMPLE
  ./scripts/run-app.ps1
.EXAMPLE
  ./scripts/run-app.ps1 -AvdName Pixel_API_34 -Port 5580 -NoWipe
#>
[CmdletBinding()]
param(
  [string]$AvdName = 'Pixel_API_34',
  [int]$Port = 5580,
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

$deviceIdTarget = "emulator-$Port"

# Ensure adb server is running early so subsequent calls are fast
& adb start-server | Out-Null

function Wait-ForDevice{
  param(
    [string]$DeviceId,
    [int]$TimeoutSec = 420
  )

  $sw = [Diagnostics.Stopwatch]::StartNew()
  Write-Host "Waiting for $DeviceId to come online (timeout=${TimeoutSec}s)..."

  while($sw.Elapsed.TotalSeconds -lt $TimeoutSec){
    $state = ''
    try {
      $state = (& adb -s $DeviceId get-state 2>$null).Trim()
    } catch {
      $state = ''
    }

    if($state -eq 'device'){
      $boot = (& adb -s $DeviceId shell getprop sys.boot_completed 2>$null).Trim()
      $unlock = (& adb -s $DeviceId shell getprop dev.bootcomplete 2>$null).Trim()
      if($boot -eq '1' -and $unlock -eq '1'){
        Write-Host "Device $DeviceId ready in $([int]$sw.Elapsed.TotalSeconds)s." -ForegroundColor Green
        return
      }
      Write-Host 'ADB device detected; waiting for Android framework...' -ForegroundColor DarkGray
    } elseif($state){
      Write-Host "State: $state" -ForegroundColor DarkGray
    }

    Start-Sleep -Seconds 2
  }

  throw "Emulator $DeviceId failed to boot within $TimeoutSec seconds."
}

function Start-Emulator{
  param(
    [string]$Name,
    [int]$EmulatorPort,
    [switch]$ColdStart
  )

  Write-Section "Starting emulator $Name (port $EmulatorPort)"

  $emuArgs = @('-avd',$Name,'-port',$EmulatorPort,'-no-snapshot-load','-no-snapshot-save','-gpu','angle_indirect','-no-boot-anim','-netdelay','none','-netspeed','full')
  if($ColdStart){ $emuArgs += '-wipe-data' }

  $logDir = Join-Path $Root 'logs'
  if(-not (Test-Path $logDir)){ New-Item -ItemType Directory -Path $logDir | Out-Null }
  $timestamp = (Get-Date -Format 'yyyyMMdd_HHmmss')
  $stdoutLog = Join-Path $logDir "emulator-${Name}-${timestamp}.out.log"
  $stderrLog = Join-Path $logDir "emulator-${Name}-${timestamp}.err.log"

  $emuExecutable = Join-Path $env:ANDROID_SDK_ROOT 'emulator\emulator.exe'
  if(-not (Test-Path $emuExecutable)){ $emuExecutable = 'emulator' }

  Write-Host "StdOut: $stdoutLog"
  Write-Host "StdErr: $stderrLog"

  Start-Process -FilePath $emuExecutable -ArgumentList $emuArgs -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog | Out-Null
  Start-Sleep -Seconds 4
}

# Helper: wait for device boot
function Get-ActiveEmulatorId{
  param(
    [string]$PreferredId
  )

  $devicesOutput = (& adb devices) | Out-String
  $deviceRegex = [regex]'(emulator-\d+)\s+device'

  if($devicesOutput -match ("{0}\s+device" -f [regex]::Escape($PreferredId))){
    return $PreferredId
  }

  $match = $deviceRegex.Match($devicesOutput)
  if($match.Success){
    return $match.Groups[1].Value
  }

  return $null
}

# 4. Start emulator unless already running; include one automatic retry with cold boot on failure
$activeDeviceId = Get-ActiveEmulatorId -PreferredId $deviceIdTarget

if($JustInstall){
  if(-not $activeDeviceId){ throw 'No emulator/device connected and JustInstall specified.' }
  Write-Host "Using already running device $activeDeviceId (JustInstall)."
  Wait-ForDevice -DeviceId $activeDeviceId
} elseif($activeDeviceId){
  if($activeDeviceId -ne $deviceIdTarget){
    Write-Host "Using emulator $activeDeviceId (preferred $deviceIdTarget)." -ForegroundColor Yellow
  } else {
    Write-Host "Emulator $activeDeviceId already running."
  }
  Wait-ForDevice -DeviceId $activeDeviceId
} else {
  $attempt = 1
  $maxAttempts = 2
  if($NoWipe.IsPresent){
    $maxAttempts = 1
  }
  while(-not $activeDeviceId -and $attempt -le $maxAttempts){
    $useCold = $ColdBoot.IsPresent -or ($attempt -gt 1 -and -not $NoWipe.IsPresent)
    if($attempt -gt 1){
      Write-Host "Retrying emulator launch (attempt $attempt)..." -ForegroundColor Yellow
    }

    try {
      Start-Emulator -Name $AvdName -EmulatorPort $Port -ColdStart:$useCold
      Wait-ForDevice -DeviceId $deviceIdTarget
      $activeDeviceId = $deviceIdTarget
    } catch {
      if($attempt -lt $maxAttempts){
        Write-Host "Attempt $attempt failed: $($_.Exception.Message)" -ForegroundColor Yellow
        Get-Process | Where-Object { $_.Name -match 'emulator|qemu' } | ForEach-Object {
          try { $_.Kill() } catch {}
        }
        Start-Sleep -Seconds 5
        $attempt++
      } else {
        throw
      }
    }
  }
}

if(-not $activeDeviceId){
  throw 'Unable to acquire an emulator device.'
}

Write-Host "Target device: $activeDeviceId"

if($activeDeviceId -ne $deviceIdTarget){
  # If we ended up with a different emulator id (e.g., physical device), align target for downstream commands
  $deviceIdTarget = $activeDeviceId
}

if(-not $JustInstall){
  Write-Section 'Assembling Debug APK'
  & .\gradlew.bat :app:assembleDebug
  if($LASTEXITCODE -ne 0){ throw 'Gradle build failed.' }
}

# 5. Locate APK
$apk = Get-ChildItem -Path (Join-Path $Root 'app\build\outputs\apk\debug') -Filter '*-debug.apk' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if(-not $apk){ throw 'Debug APK not found. Build may have failed.' }
Write-Host "Using APK: $($apk.FullName)"

Write-Section 'Installing APK'
$installOutput = & adb -s $deviceIdTarget install -r -t $apk.FullName
Write-Host $installOutput
if($LASTEXITCODE -ne 0){ throw 'adb install failed.' }

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
  $res = & adb -s $deviceIdTarget shell am start -W -n $component 2>&1
  Write-Host $res
  if($LASTEXITCODE -eq 0 -and $res -match 'Status: ok'){ Write-Section 'Done'; return }
  Write-Host 'Explicit start failed; falling back to monkey.' -ForegroundColor Yellow
}

$monkeyOutput = & adb -s $deviceIdTarget shell monkey -p $pkg -c android.intent.category.LAUNCHER 1
Write-Host $monkeyOutput
if($LASTEXITCODE -ne 0){ throw 'adb monkey failed.' }

Write-Section 'Done'
