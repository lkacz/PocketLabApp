<#!
.SYNOPSIS
  Watch Kotlin and resource files for changes; on change rebuild, install, and relaunch the app.
.DESCRIPTION
  Uses a polling loop (portable) to detect modified timestamps. When changes are detected,
  debounces rapid edits, runs :app:assembleDebug, then calls run-app.ps1 -JustInstall -LaunchActivity .MainActivity.
.PARAMETER IntervalSec
  Polling interval (default 2 seconds).
.PARAMETER Quiet
  Suppress per-file change logs.
#>
[CmdletBinding()]
param(
  [int]$IntervalSec = 2,
  [switch]$Quiet
)
$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) '..')
Set-Location $root

$paths = @('app/src/main/java','app/src/main/res','app/src/main/AndroidManifest.xml')
$tracked = @{}
function Snapshot {
  foreach($p in $paths){
    if(Test-Path $p){
      Get-ChildItem -Recurse -File $p | Where-Object { $_.Extension -in '.kt','.xml','.png','.webp','.txt' } | ForEach-Object {
        $tracked[$_.FullName] = $_.LastWriteTimeUtc.Ticks
      }
    }
  }
}
Snapshot
Write-Host "Watching for changes... (Ctrl+C to stop)" -ForegroundColor Cyan

$pending = $null
while($true){
  Start-Sleep -Seconds $IntervalSec
  $changed = @()
  foreach($k in $tracked.Keys){
    if(Test-Path $k){
      $t = (Get-Item $k).LastWriteTimeUtc.Ticks
      if($t -ne $tracked[$k]){ $changed += $k; $tracked[$k] = $t }
    }
  }
  # detect new files
  foreach($p in $paths){
    if(Test-Path $p){
      Get-ChildItem -Recurse -File $p | Where-Object { $_.Extension -in '.kt','.xml','.png','.webp','.txt' } | ForEach-Object {
        if(-not $tracked.ContainsKey($_.FullName)){ $tracked[$_.FullName] = $_.LastWriteTimeUtc.Ticks; $changed += $_.FullName }
      }
    }
  }
  if($changed.Count -gt 0){
    $pending = [DateTime]::UtcNow
    if(-not $Quiet){ $changed | ForEach-Object { Write-Host "Changed: $_" -ForegroundColor Yellow } }
  }
  if($pending -and ([DateTime]::UtcNow - $pending).TotalSeconds -ge 1){
    Write-Host "Rebuilding..." -ForegroundColor Green
    $pending = $null
    $build = & ./gradlew.bat :app:assembleDebug 2>&1
    if($LASTEXITCODE -ne 0){ Write-Host "Build failed" -ForegroundColor Red; Write-Host $build; continue }
    Write-Host "Build OK - reinstalling & relaunching" -ForegroundColor Green
    & powershell -ExecutionPolicy Bypass -File ./scripts/run-app.ps1 -JustInstall -LaunchActivity .MainActivity
  }
}
