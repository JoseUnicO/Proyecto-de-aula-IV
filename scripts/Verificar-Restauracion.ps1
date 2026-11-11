param([Parameter(Mandatory=$true)][string]$Copia,[ValidatePattern('^biblioteca_[a-z0-9_]+$')][string]$Destino='biblioteca_restauracion_ensayo',[ValidatePattern('^biblioteca(?:_[a-z0-9_]+)?$')][string]$Origen='biblioteca_ensayo')
$ErrorActionPreference='Stop'
$mysql='C:\wamp64\bin\mysql\mysql8.4.7\bin\mysql.exe'
if(!(Test-Path -LiteralPath ($Copia+'.sha256'))){throw 'La copia no tiene comprobante SHA256'}
$esperado=(Get-Content -LiteralPath ($Copia+'.sha256') -Raw).Trim()
if((Get-FileHash -LiteralPath $Copia -Algorithm SHA256).Hash -ne $esperado){throw 'Checksum de copia incorrecto. No se restauró nada.'}
function Consulta([string]$Texto){
 $salida=& $mysql --protocol=TCP --host=127.0.0.1 --user=root --default-character-set=utf8mb4 --batch --raw --skip-column-names -e $Texto 2>&1
 if($LASTEXITCODE -ne 0){throw "Falló la comprobación de restauración: $salida"}
 return $salida
}
# CREATE falla si el destino ya existe: nunca se sobrescribe una base existente.
Consulta "CREATE DATABASE ``$Destino`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" | Out-Null
$pi=New-Object Diagnostics.ProcessStartInfo
$pi.FileName=$mysql
$pi.Arguments="--protocol=TCP --host=127.0.0.1 --user=root --default-character-set=utf8mb4 $Destino"
$pi.UseShellExecute=$false;$pi.CreateNoWindow=$true;$pi.RedirectStandardInput=$true;$pi.RedirectStandardError=$true
$pi.StandardInputEncoding=New-Object Text.UTF8Encoding($false)
$proceso=[Diagnostics.Process]::Start($pi)
$errorLectura=$proceso.StandardError.ReadToEndAsync()
$lector=New-Object IO.StreamReader($Copia,[Text.Encoding]::UTF8)
try{while(!$lector.EndOfStream){$proceso.StandardInput.WriteLine($lector.ReadLine())}}finally{$lector.Dispose();$proceso.StandardInput.Close()}
$proceso.WaitForExit()
if($proceso.ExitCode -ne 0){throw "La restauración falló en el destino aislado: $($errorLectura.Result)"}
$tablas=@('schema_version','administradores','usuarios','libros','prestamos','reservas','secuencias','operaciones')
$resultados=@()
foreach($tabla in $tablas){
 $columnas=Consulta "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$Origen' AND TABLE_NAME='$tabla' ORDER BY ORDINAL_POSITION;"
 $campos=($columnas | ForEach-Object {"IFNULL(HEX(``$_``),'NULL')"}) -join ','
 $sql="SELECT CONCAT_WS('|',$campos) FROM ``{0}``.``$tabla`` ORDER BY 1;"
 $antes=@(Consulta ($sql -f $Origen));$despues=@(Consulta ($sql -f $Destino))
 if(($antes -join "`n") -cne ($despues -join "`n")){throw "Las filas restauradas difieren: $tabla"}
 $resultados+=@{tabla=$tabla;filas=$antes.Count;coincidenciaExacta=$true}
}
$reporte=@{origen=$Origen;destino=$Destino;copia=$Copia;sha256=$esperado;fecha=(Get-Date).ToString('o');tablas=$resultados}
$evidencia=Join-Path (Split-Path $Copia -Parent) ('restauracion-'+$Destino+'.json')
$reporte | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $evidencia -Encoding utf8
Write-Output "Restauración verificada: las ocho tablas coinciden exactamente en $Destino."
