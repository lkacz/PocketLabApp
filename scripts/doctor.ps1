<#!
.SYNOPSIS
  Diagnose Java toolchain and Android Studio Gradle JDK configuration for Pocket Lab App contributors.
.DESCRIPTION
  Performs lightweight checks that help newcomers confirm their environment can build the project:
  - Ensures JAVA_HOME points to an existing JDK and reports the detected version.
  - Looks for Android Studio gradle.xml files and validates that the IDE is set to an embedded/default JDK 17 (or later).
  The script prints colorized status messages by default and can emit JSON for automation.
.PARAMETER Json
  Emit machine-readable JSON instead of human-friendly console output.
.PARAMETER Strict
  Return a non-zero exit code if warnings or errors are detected (useful for CI or pre-commit hooks).
.EXAMPLE
  ./scripts/doctor.ps1
.EXAMPLE
  ./scripts/doctor.ps1 -Json | jq
.EXAMPLE
  ./scripts/doctor.ps1 -Strict
#>
[CmdletBinding()]
param(
  [switch]$Json,
  [switch]$Strict
)

Set-StrictMode -Version 3
$ErrorActionPreference = 'Stop'

$isWindows = [System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::Windows)

$results = [System.Collections.Generic.List[psobject]]::new()

function Add-Result {
  param(
    [Parameter(Mandatory)] [string]$Check,
    [Parameter(Mandatory)] [ValidateSet('ok','warn','error')] [string]$Status,
    [Parameter(Mandatory)] [string]$Message,
    [hashtable]$Data
  )

  $entry = [pscustomobject]@{
    Check   = $Check
    Status  = $Status
    Message = $Message
    Data    = $Data
  }
  $results.Add($entry)

  if(-not $Json){
    $color = switch ($Status) {
      'ok'    { 'Green' }
      'warn'  { 'Yellow' }
      default { 'Red' }
    }
  Write-Host "[$($Status.ToUpper())] ${Check}: $Message" -ForegroundColor $color
    if($Data){
      foreach($key in ($Data.Keys | Sort-Object)){
  Write-Host "  - ${key}: $($Data[$key])" -ForegroundColor DarkGray
      }
    }
  }
}

function Get-JavaVersion {
  param(
    [Parameter(Mandatory)] [string]$JavaExecutable
  )

  try {
    $processOutput = & $JavaExecutable -version 2>&1
  } catch {
    return $null
  }

  if(-not $processOutput){ return $null }

  $firstLine = $processOutput | Select-Object -First 1
  if($firstLine -match '"([0-9]+(?:\.[0-9._]+)?)"'){
    return [pscustomobject]@{ Raw = $firstLine; Version = $Matches[1] }
  }

  return [pscustomobject]@{ Raw = $firstLine; Version = $null }
}

function Check-JavaHome {
  $javaHome = $env:JAVA_HOME
  if([string]::IsNullOrWhiteSpace($javaHome)){
    Add-Result -Check 'JAVA_HOME' -Status 'warn' -Message 'JAVA_HOME is not set; Gradle wrapper will supply a JDK but IDE tooling may miss code insight.' -Data @{}
    return
  }

  $resolvedHome = $null
  try {
    $resolvedHome = (Resolve-Path -Path $javaHome -ErrorAction Stop).ProviderPath
  } catch {
    Add-Result -Check 'JAVA_HOME' -Status 'error' -Message "JAVA_HOME path '$javaHome' does not exist." -Data @{}
    return
  }

  $javaExeName = if($isWindows){ 'java.exe' } else { 'java' }
  $javaExecutable = Join-Path $resolvedHome (Join-Path 'bin' $javaExeName)
  if(-not (Test-Path -Path $javaExecutable -PathType Leaf)){
    Add-Result -Check 'JAVA_HOME' -Status 'error' -Message "JAVA_HOME does not contain a $javaExeName executable." -Data @{ Path = $resolvedHome }
    return
  }

  $versionInfo = Get-JavaVersion -JavaExecutable $javaExecutable
  $data = @{ Path = $resolvedHome; Java = $versionInfo?.Raw }

  if(-not $versionInfo -or -not $versionInfo.Version){
    Add-Result -Check 'JAVA_HOME' -Status 'warn' -Message 'Unable to determine JAVA_HOME version (java -version output unexpected).' -Data $data
    return
  }

  $major = $versionInfo.Version.Split('.')[0]
  if($major -eq '1'){
    # Legacy formatting "1.8.0_202" => major derived from second number
    $parts = $versionInfo.Version.Split('.')
    if($parts.Length -ge 2){ $major = $parts[1] }
  }

  if([int]::TryParse($major, [ref]$null) -and [int]$major -lt 17){
    Add-Result -Check 'JAVA_HOME' -Status 'warn' -Message "Detected Java $($versionInfo.Version); Gradle 8+/AGP 8 require JDK 17." -Data $data
  } else {
    Add-Result -Check 'JAVA_HOME' -Status 'ok' -Message "JAVA_HOME points to Java $($versionInfo.Version)." -Data $data
  }
}

function Get-AndroidStudioGradleConfigs {
  $candidates = [System.Collections.Generic.List[string]]::new()

  if($env:APPDATA){
    $root = Join-Path $env:APPDATA 'Google'
    if(Test-Path $root){
      Get-ChildItem -Path $root -Directory -Filter 'AndroidStudio*' -ErrorAction SilentlyContinue | ForEach-Object {
        $candidates.Add((Join-Path $_.FullName 'options\gradle.xml'))
      }
    }
  }

  if($env:LOCALAPPDATA){
    $root = Join-Path $env:LOCALAPPDATA 'Google'
    if(Test-Path $root){
      Get-ChildItem -Path $root -Directory -Filter 'AndroidStudio*' -ErrorAction SilentlyContinue | ForEach-Object {
        $candidates.Add((Join-Path $_.FullName 'options\gradle.xml'))
      }
    }
  }

  $userProfile = [Environment]::GetFolderPath('UserProfile')
  if(-not [string]::IsNullOrWhiteSpace($userProfile)){
    foreach($child in @('Library/Application Support/Google','Library/Application Support/Google/AndroidStudio*','Library/Application Support/Google/AndroidStudioPreview*')){
      $path = Join-Path $userProfile $child
      if(Test-Path $path){
        Get-ChildItem -Path $path -Directory -Filter 'AndroidStudio*' -ErrorAction SilentlyContinue | ForEach-Object {
          $candidates.Add((Join-Path $_.FullName 'options/gradle.xml'))
        }
      }
    }

    foreach($child in @('.config/Google')){
      $path = Join-Path $userProfile $child
      if(Test-Path $path){
        Get-ChildItem -Path $path -Directory -Filter 'AndroidStudio*' -ErrorAction SilentlyContinue | ForEach-Object {
          $candidates.Add((Join-Path $_.FullName 'options/gradle.xml'))
        }
      }
    }
  }

  return $candidates | Where-Object { Test-Path $_ }
}

function Interpret-GradleJvmValue {
  param(
    [Parameter()] [string]$Value
  )

  if([string]::IsNullOrWhiteSpace($Value)){
    return [pscustomobject]@{ Status = 'warn'; Message = 'Gradle JVM value missing (IDE may fall back to default).'; Notes = '' }
  }

  $normalized = $Value.Trim()
  $lower = $normalized.ToLowerInvariant()

  $goodKeywords = @('android studio default jdk','embedded jdk','jbr-17','17','jbr17','temurin 17','jdk17')
  foreach($keyword in $goodKeywords){
    if($lower -like "*$keyword*"){
      return [pscustomobject]@{ Status = 'ok'; Message = "Android Studio uses '$normalized'."; Notes = '' }
    }
  }

  if($normalized -match '^[A-Za-z]:\\' -or $normalized -match '^/' ){
    $jdkPath = $normalized
    if(Test-Path $jdkPath){
      $releaseFile = Join-Path $jdkPath 'release'
      $version = $null
      if(Test-Path $releaseFile){
        $line = (Get-Content $releaseFile | Where-Object { $_ -match '^JAVA_VERSION=' } | Select-Object -First 1)
        if($line -and ($line -match '"([0-9]+(?:\.[0-9._]+)?)"')){ $version = $Matches[1] }
      }

      if($version -and $version.StartsWith('17')){
        return [pscustomobject]@{ Status = 'ok'; Message = "Android Studio uses custom JDK $version."; Notes = $jdkPath }
      } elseif($version){
        return [pscustomobject]@{ Status = 'warn'; Message = "Gradle JVM set to JDK $version; switch to JDK 17."; Notes = $jdkPath }
      }

      return [pscustomobject]@{ Status = 'warn'; Message = 'Gradle JVM path found but version could not be determined.'; Notes = $jdkPath }
    } else {
      return [pscustomobject]@{ Status = 'warn'; Message = 'Gradle JVM path does not exist on this machine.'; Notes = $jdkPath }
    }
  }

  return [pscustomobject]@{ Status = 'warn'; Message = "Gradle JVM set to '$normalized' (unable to verify version)."; Notes = '' }
}

function Check-AndroidStudioGradleJdk {
  $configs = Get-AndroidStudioGradleConfigs
  if(-not $configs -or $configs.Count -eq 0){
    Add-Result -Check 'Android Studio Gradle JDK' -Status 'warn' -Message 'No Android Studio gradle.xml found; IDE may not be configured yet (defaults to embedded JDK).' -Data @{}
    return
  }

  $latest = $configs | Sort-Object { (Get-Item $_).LastWriteTime } -Descending | Select-Object -First 1
  $data = @{ ConfigPath = $latest }

  try {
    $node = Select-Xml -Path $latest -XPath '//option[@name="gradleJvm"]'
  } catch {
    Add-Result -Check 'Android Studio Gradle JDK' -Status 'warn' -Message 'Unable to parse gradle.xml; open Android Studio once and resave Gradle settings.' -Data $data
    return
  }

  if(-not $node){
    Add-Result -Check 'Android Studio Gradle JDK' -Status 'warn' -Message 'gradleJvm option not present; IDE likely uses embedded default JDK (check Settings > Gradle).' -Data $data
    return
  }

  $value = $node.Node.GetAttribute('value')
  $data['Value'] = $value

  $interpretation = Interpret-GradleJvmValue -Value $value
  Add-Result -Check 'Android Studio Gradle JDK' -Status $interpretation.Status -Message $interpretation.Message -Data $data
}

Check-JavaHome
Check-AndroidStudioGradleJdk

if($Json){
  $results | ConvertTo-Json -Depth 4
}

$hasWarn = $results | Where-Object { $_.Status -eq 'warn' }
$hasError = $results | Where-Object { $_.Status -eq 'error' }

if(-not $Json){
  Write-Host ''
  if($hasError){
    Write-Host 'One or more checks failed. Please address the errors above.' -ForegroundColor Red
  } elseif($hasWarn){
    Write-Host 'Checks completed with warnings. Review the guidance above to finish setup.' -ForegroundColor Yellow
  } else {
    Write-Host 'All checks passed. You are ready to build the project!' -ForegroundColor Green
  }
}

if($Strict){
  if($hasError -or $hasWarn){ exit 1 }
}

exit 0