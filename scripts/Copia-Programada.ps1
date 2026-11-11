$ErrorActionPreference='Stop'
$perfil=Join-Path $env:LOCALAPPDATA 'Biblioteca\biblioteca'
$estado=Join-Path $perfil 'ultima-copia.json'
try {
 $salida=& "$PSScriptRoot\Copia-Seguridad.ps1" -Base biblioteca
 @{exito=$true;fecha=(Get-Date).ToString('o');detalle=($salida -join ' ')} | ConvertTo-Json | Set-Content -LiteralPath $estado -Encoding utf8
 exit 0
} catch {
 @{exito=$false;fecha=(Get-Date).ToString('o');detalle=$_.Exception.Message} | ConvertTo-Json | Set-Content -LiteralPath $estado -Encoding utf8
 exit 1
}
