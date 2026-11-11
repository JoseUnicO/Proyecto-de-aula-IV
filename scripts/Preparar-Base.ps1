param([ValidatePattern('^biblioteca(?:_[a-z0-9_]+)?$')][string]$Base='biblioteca')
$ErrorActionPreference='Stop'
$raiz=Split-Path $PSScriptRoot -Parent
$mysql='C:\wamp64\bin\mysql\mysql8.4.7\bin\mysql.exe'
if(!(Test-Path -LiteralPath $mysql)){throw 'No se encontró MySQL 8.4.7 de Wamp'}
$perfil=Join-Path $env:LOCALAPPDATA "Biblioteca\$Base"
New-Item -ItemType Directory -Force -Path $perfil | Out-Null
$identidad=[Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $perfil /inheritance:r /grant:r "${identidad}:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
if($LASTEXITCODE -ne 0){throw 'No se pudo proteger el directorio'}
if(Test-Path -LiteralPath (Join-Path $perfil 'conexion.xml')){throw "El perfil $Base ya existe; no se sobrescriben credenciales"}
function Nueva-Clave {return [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(30))}
$clave=Nueva-Clave; $adminClave=Nueva-Clave
$usuario="${Base}_app"
$adminUsuario=if($Base -eq 'biblioteca_pruebas'){'QA'}else{'Administrador'}
$sal=[Security.Cryptography.RandomNumberGenerator]::GetBytes(16)
$hash=[Security.Cryptography.Rfc2898DeriveBytes]::Pbkdf2($adminClave,$sal,600000,[Security.Cryptography.HashAlgorithmName]::SHA256,32)
$salHex=[Convert]::ToHexString($sal);$hashHex=[Convert]::ToHexString($hash)
$ddl=[IO.File]::ReadAllText((Join-Path $raiz 'src\main\resources\db\V001__biblioteca.sql')).TrimStart([char]0xFEFF)
$checksum=(Get-FileHash -LiteralPath (Join-Path $raiz 'src\main\resources\db\V001__biblioteca.sql') -Algorithm SHA256).Hash.ToLowerInvariant()
$ddl2=[IO.File]::ReadAllText((Join-Path $raiz 'src\main\resources\db\V002__retirar_importaciones.sql')).TrimStart([char]0xFEFF)
$checksum2=(Get-FileHash -LiteralPath (Join-Path $raiz 'src\main\resources\db\V002__retirar_importaciones.sql') -Algorithm SHA256).Hash.ToLowerInvariant()
$permisosPruebas=''
if($Base -eq 'biblioteca_pruebas'){
 $clavePruebas=Nueva-Clave;$usuarioPruebas="${Base}_test"
 $permisosPruebas="CREATE USER '$usuarioPruebas'@'localhost' IDENTIFIED BY '$clavePruebas'; GRANT SELECT,INSERT,UPDATE,DELETE,TRIGGER ON ``$Base``.* TO '$usuarioPruebas'@'localhost';"
}
$sql=@"
CREATE DATABASE ``$Base`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE ``$Base``;
$ddl
INSERT INTO schema_version(version,checksum) VALUES(1,'$checksum');
$ddl2
INSERT INTO schema_version(version,checksum) VALUES(2,'$checksum2');
INSERT INTO administradores(usuario,nombre,hash,salt,iteraciones) VALUES('$adminUsuario','Administrador del Sistema',UNHEX('$hashHex'),UNHEX('$salHex'),600000);
CREATE USER '$usuario'@'localhost' IDENTIFIED BY '$clave';
GRANT SELECT ON ``$Base``.* TO '$usuario'@'localhost';
GRANT INSERT,UPDATE,DELETE ON ``$Base``.libros TO '$usuario'@'localhost';
GRANT INSERT,UPDATE,DELETE ON ``$Base``.usuarios TO '$usuario'@'localhost';
GRANT INSERT,UPDATE ON ``$Base``.prestamos TO '$usuario'@'localhost';
GRANT INSERT,UPDATE ON ``$Base``.reservas TO '$usuario'@'localhost';
GRANT UPDATE ON ``$Base``.secuencias TO '$usuario'@'localhost';
GRANT INSERT ON ``$Base``.operaciones TO '$usuario'@'localhost';
GRANT UPDATE(fallos,bloqueado_hasta) ON ``$Base``.administradores TO '$usuario'@'localhost';
$permisosPruebas
"@
$salida=$sql | & $mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --default-character-set=utf8mb4 --batch 2>&1
if($LASTEXITCODE -ne 0){throw "Falló la preparación de MySQL. No se activó el perfil. Revise el estado del esquema $Base con un administrador. "}
[pscredential]::new($usuario,(ConvertTo-SecureString $clave -AsPlainText -Force)) | Export-Clixml -LiteralPath (Join-Path $perfil 'conexion.xml')
if($Base -eq 'biblioteca_pruebas'){[pscredential]::new($usuarioPruebas,(ConvertTo-SecureString $clavePruebas -AsPlainText -Force)) | Export-Clixml -LiteralPath (Join-Path $perfil 'pruebas.xml')}
# La contraseña inicial permanece cifrada con DPAPI para entrega al propietario.
[pscredential]::new($adminUsuario,(ConvertTo-SecureString $adminClave -AsPlainText -Force)) | Export-Clixml -LiteralPath (Join-Path $perfil 'acceso-inicial.xml')
@{base=$Base;host='127.0.0.1';puerto=3306;mysql=$mysql} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $perfil 'configuracion.json') -Encoding utf8
Write-Output "Esquema $Base creado. Credenciales protegidas con DPAPI y permisos locales."
