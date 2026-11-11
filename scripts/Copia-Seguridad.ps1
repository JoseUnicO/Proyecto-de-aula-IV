param([ValidatePattern('^biblioteca(?:_[a-z0-9_]+)?$')][string]$Base='biblioteca')
$ErrorActionPreference='Stop'
$perfil=Join-Path $env:LOCALAPPDATA "Biblioteca\$Base"
$cred=Import-Clixml -LiteralPath (Join-Path $perfil 'conexion.xml')
$copias=Join-Path $perfil 'copias'
New-Item -ItemType Directory -Force -Path $copias | Out-Null
$identidad=[Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $copias /inheritance:r /grant:r "${identidad}:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
if($LASTEXITCODE -ne 0){throw 'No se pudo proteger la copia'}
$nombre='biblioteca-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff')+'.sql'
$destino=Join-Path $copias $nombre
$parcial=$destino+'.parcial'
$pi=New-Object Diagnostics.ProcessStartInfo
$pi.FileName='C:\wamp64\bin\mysql\mysql8.4.7\bin\mysqldump.exe'
$pi.Arguments="--protocol=TCP --host=127.0.0.1 --port=3306 --user=$($cred.UserName) --hex-blob --single-transaction --skip-lock-tables --no-tablespaces --set-gtid-purged=OFF --skip-triggers --routines=false --default-character-set=utf8mb4 $Base"
$pi.UseShellExecute=$false;$pi.CreateNoWindow=$true;$pi.RedirectStandardOutput=$true;$pi.RedirectStandardError=$true
$pi.EnvironmentVariables['MYSQL_PWD']=$cred.GetNetworkCredential().Password
$proceso=[Diagnostics.Process]::Start($pi)
$flujo=[IO.File]::Create($parcial)
try{$tarea=$proceso.StandardError.ReadToEndAsync();$proceso.StandardOutput.BaseStream.CopyTo($flujo);$flujo.Flush($true);$proceso.WaitForExit();$errorDump=$tarea.Result}finally{$flujo.Dispose()}
if($proceso.ExitCode -ne 0){throw "Falló mysqldump; se conservaron todas las copias previas: $errorDump"}
if((Get-Item -LiteralPath $parcial).Length -lt 1000){throw 'Copia inesperadamente vacía; no se aplicó retención'}
Move-Item -LiteralPath $parcial -Destination $destino
(Get-FileHash -LiteralPath $destino -Algorithm SHA256).Hash | Set-Content -LiteralPath ($destino+'.sha256') -Encoding ascii
# Conservar 7 representantes diarios y 4 semanales. La copia nueva se confirma primero.
$archivos=Get-ChildItem -LiteralPath $copias -Filter '*.sql' -File | Sort-Object Name -Descending
$diarias=$archivos | Group-Object {$_.LastWriteTime.ToString('yyyy-MM-dd')} | Select-Object -First 7 | ForEach-Object {$_.Group[0]}
$semanales=$archivos | Group-Object {$d=$_.LastWriteTime;$d.AddDays(-(([int]$d.DayOfWeek+6)%7)).ToString('yyyy-MM-dd')} | Select-Object -First 4 | ForEach-Object {$_.Group[0]}
$conservar=@($diarias.FullName)+@($semanales.FullName)
foreach($f in $archivos){if($conservar -notcontains $f.FullName){
 $ruta=[IO.Path]::GetFullPath($f.FullName);$permitida=[IO.Path]::GetFullPath($copias)+[IO.Path]::DirectorySeparatorChar
 if(!$ruta.StartsWith($permitida,[StringComparison]::OrdinalIgnoreCase)){throw 'Destino de retención fuera del directorio de copias'}
 Remove-Item -LiteralPath $ruta -Force
 if(Test-Path -LiteralPath ($ruta+'.sha256')){Remove-Item -LiteralPath ($ruta+'.sha256') -Force}
}}
Write-Output "Copia completa verificada: $destino"
