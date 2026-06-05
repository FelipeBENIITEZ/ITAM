param(
    [string]$Output = "database/backups/itam_dump_$(Get-Date -Format yyyyMMdd_HHmmss).sql"
)

# Asegura que la carpeta exista
$dir = Split-Path $Output
if (!(Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }

Write-Output "Creando backup de la base de datos en: $Output"

docker compose exec -T postgres pg_dump -U postgres ITASSET > $Output

if ($LASTEXITCODE -eq 0) {
    Write-Output "Backup completado: $Output"
} else {
    Write-Error "Error durante pg_dump"
}
