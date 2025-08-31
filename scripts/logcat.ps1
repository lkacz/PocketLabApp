<#!
.SYNOPSIS
  Tail filtered logcat output for the app package.
#>
[CmdletBinding()]
param(
  [string]$Package = 'com.lkacz.pola',
  [string]$Filter = ''
)
$ErrorActionPreference='Stop'
if(-not $env:ANDROID_SDK_ROOT){
  $defaultSdk = "$env:USERPROFILE\AppData\Local\Android\Sdk"
  if(Test-Path $defaultSdk){ $env:ANDROID_SDK_ROOT = $defaultSdk }
}
$paths = @("$env:ANDROID_SDK_ROOT\platform-tools","$env:ANDROID_SDK_ROOT\emulator")
foreach($p in $paths){ if($env:Path -notmatch [regex]::Escape($p)){ $env:Path = "$p;$env:Path" } }

Write-Host "Filtering for package=$Package pattern=$Filter" -ForegroundColor Cyan
adb logcat --clear 2>$null | Out-Null
adb logcat -v color | ForEach-Object {
  if($_ -match $Package){
    if([string]::IsNullOrEmpty($Filter) -or $_ -match $Filter){ $_ }
  }
}
