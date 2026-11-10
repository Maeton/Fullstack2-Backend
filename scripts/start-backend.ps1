# Levanta el backend Huerto Hogar en segundo plano (puerto 8080).
# Uso: powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$logs = Join-Path $root "logs"
New-Item -ItemType Directory -Force -Path $logs | Out-Null

$jar = Join-Path $root "target\huerto-hogar-backend-0.0.1-SNAPSHOT.jar"

if (-not (Test-Path $jar)) {
    throw "No existe $jar. Ejecuta primero: .\mvnw.cmd -DskipTests package"
}

$salida = Join-Path $logs "backend.out.log"
$errorLog = Join-Path $logs "backend.err.log"

$proceso = Start-Process -FilePath "java" `
    -ArgumentList @("-jar", "`"$jar`"") `
    -WorkingDirectory $root `
    -RedirectStandardOutput $salida `
    -RedirectStandardError $errorLog `
    -PassThru -WindowStyle Hidden

"$($proceso.Id)" | Set-Content (Join-Path $logs "pid.txt")
Write-Host "Backend iniciado (PID $($proceso.Id)) -> http://localhost:8080"
Write-Host "Swagger: http://localhost:8080/swagger-ui.html"
