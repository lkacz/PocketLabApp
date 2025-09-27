param(
    [int]
    $Port = 8080,

    [switch]
    $OpenBrowser
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$editorPath = Join-Path $repoRoot 'OnlineProtocolEditor'

if (-not (Test-Path -Path $editorPath -PathType Container)) {
    Write-Error "OnlineProtocolEditor directory not found at '$editorPath'. Run this script from the repo's scripts folder."
}

Push-Location $editorPath
try {
    $pythonCommand = Get-Command python -ErrorAction SilentlyContinue
    if (-not $pythonCommand) {
        $pythonCommand = Get-Command py -ErrorAction SilentlyContinue
    }

    if (-not $pythonCommand) {
        Write-Error "Python executable not found. Install Python 3 or serve the directory with another HTTP tool."
    }

    $url = "http://localhost:$Port/"
    Write-Host "Serving Online Protocol Editor from '$editorPath' at $url" -ForegroundColor Green
    Write-Host "Press Ctrl+C to stop." -ForegroundColor Yellow

    if ($OpenBrowser.IsPresent) {
        Start-Process $url | Out-Null
    }

    & $pythonCommand.Path '-m' 'http.server' $Port
}
finally {
    Pop-Location
}
