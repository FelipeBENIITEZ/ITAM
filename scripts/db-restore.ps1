param(
    [string]$InputFile = "database/backups/itam_dump.sql"
)

if (!(Test-Path $InputFile)) {
    Write-Error "Archivo de dump no encontrado: $InputFile"
    exit 1
}

Write-Output "Restaurando base de datos desde: $InputFile"

# Parar app para evitar conflictos (opcional)
docker compose stop itam-app

docker compose exec -T postgres psql -U postgres -d ITASSET < $InputFile

if ($LASTEXITCODE -eq 0) {
    Write-Output "Restore completado"
} else {
    Write-Error "Error durante psql restore"
}

docker compose start itam-app
