# Detiene el backend Huerto Hogar.
# Uso: powershell -ExecutionPolicy Bypass -File .\scripts\stop-backend.ps1

$root = Split-Path -Parent $PSScriptRoot
$archivoPid = Join-Path $root "logs\pid.txt"

if (Test-Path $archivoPid) {
    $procesoId = Get-Content $archivoPid | Select-Object -First 1
    if ($procesoId) {
        Stop-Process -Id ([int]$procesoId) -Force -ErrorAction SilentlyContinue
        Write-Host "Detenido backend (PID $procesoId)"
    }
    Remove-Item $archivoPid -Force -ErrorAction SilentlyContinue
}

# Respaldo: detener cualquier proceso que siga escuchando en el puerto del backend.
$conexiones = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
foreach ($conexion in $conexiones) {
    Stop-Process -Id $conexion.OwningProcess -Force -ErrorAction SilentlyContinue
    Write-Host "Detenido proceso en puerto 8080 (PID $($conexion.OwningProcess))"
}

Write-Host "Backend detenido." -ForegroundColor Green
