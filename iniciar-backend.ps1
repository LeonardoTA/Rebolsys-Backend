param([ValidateRange(1024,65535)][int]$Puerto = 8085)
$ErrorActionPreference = 'Stop'
$rebolsysWork = Join-Path $PSScriptRoot '.work'
$rebolsysParent = Split-Path -Parent $PSScriptRoot
if ((Test-Path -LiteralPath (Join-Path $rebolsysParent 'entregables')) -and
    (Test-Path -LiteralPath (Join-Path $rebolsysParent 'iniciar-backend.ps1'))) {
    $rebolsysWork = Join-Path $rebolsysParent '.work'
}
$rebolsysTmp = Join-Path $rebolsysWork 'tmp'
New-Item -ItemType Directory -Path $rebolsysTmp -Force | Out-Null
$env:TEMP = $rebolsysTmp
$env:TMP = $rebolsysTmp
$env:MAVEN_USER_HOME = Join-Path $rebolsysWork 'maven-home'
if (-not $env:JAVA_HOME) {
    $rebolsysJdks = Get-ChildItem -LiteralPath (Join-Path $env:ProgramFiles 'Java') -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^jdk-(\d+)' -and [int]$Matches[1] -ge 21 } |
        Sort-Object { [int]($_.Name -replace '^jdk-(\d+).*','$1') } -Descending
    if ($rebolsysJdks) { $env:JAVA_HOME = ($rebolsysJdks | Select-Object -First 1).FullName }
}
Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd -B -ntp "-Dmaven.repo.local=$rebolsysWork\maven-repository" spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Puerto"
    $rebolsysExit = $LASTEXITCODE
} finally { Pop-Location }
exit $rebolsysExit
