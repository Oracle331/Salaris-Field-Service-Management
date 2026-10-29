param([string]$JavaHome = 'C:\Program Files\Java\jdk-24')
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot 'backend')
if (-not (Test-Path (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Pass -JavaHome with an installed JDK 17–25 directory.' }
$env:JAVA_HOME = $JavaHome
if (-not $env:DEMO_PASSWORD) {
  $bytes = New-Object byte[] 24
  $randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
  try {
    $randomGenerator.GetBytes($bytes)
  } finally {
    $randomGenerator.Dispose()
  }
  $env:DEMO_PASSWORD = [Convert]::ToBase64String($bytes)
}
$localPort = if ($env:PORT) { $env:PORT } else { '8081' }
Write-Host "Local demo: http://localhost:$localPort"
Write-Host 'Accounts: worker@salaris.demo, office@salaris.demo, client@salaris.demo, admin@salaris.demo'
Write-Host "Session demo password: $env:DEMO_PASSWORD"
& .\mvnw.cmd spring-boot:run
if ($LASTEXITCODE -ne 0) { throw "Application exited with code $LASTEXITCODE" }
