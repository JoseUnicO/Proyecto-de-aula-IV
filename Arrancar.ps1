param([switch]$Consola,[switch]$SoloCompilar,[ValidatePattern('^biblioteca(?:_[a-z0-9_]+)?$')][string]$Perfil='biblioteca')
$ErrorActionPreference='Stop'
try {
 Set-Location -LiteralPath $PSScriptRoot
 function EsJdk26([string]$Ruta){
  if(!$Ruta -or !(Test-Path -LiteralPath (Join-Path $Ruta 'bin\javac.exe'))){return $false}
  $release=Join-Path $Ruta 'release'
  return (Test-Path -LiteralPath $release) -and ((Get-Content -LiteralPath $release -Raw) -match 'JAVA_VERSION="26[."]')
 }
 $candidatos=@($env:JAVA_HOME)
 $javac=Get-Command javac.exe -ErrorAction SilentlyContinue
 if($javac){$candidatos+=Split-Path -Parent (Split-Path -Parent $javac.Source)}
 foreach($base in @('C:\Program Files\Java',(Join-Path $env:USERPROFILE '.biblioteca_env'))){if(Test-Path -LiteralPath $base){$candidatos+=Get-ChildItem -LiteralPath $base -Directory | Select-Object -ExpandProperty FullName}}
 $jdk=$candidatos | Where-Object {EsJdk26 $_} | Select-Object -First 1
 if(!$jdk){throw 'Instale un JDK 26 compatible o configure JAVA_HOME.'}
 $env:JAVA_HOME=$jdk;$env:Path="$jdk\bin;$env:Path"
 $maven=Get-Command mvn.cmd -ErrorAction SilentlyContinue
 if(!$maven){throw 'No se encontró Maven. Agréguelo al PATH.'}
 foreach($recurso in @('styles.css','icon1.png','agregar.png','editar.png','eliminar.png','cancelar.png','actualizar.png')){
  if(!(Test-Path -LiteralPath (Join-Path $PSScriptRoot "src\main\resources\resources\$recurso"))){throw "Recurso requerido ausente: $recurso"}
 }
 Write-Host "Compilando con $jdk"
 & $maven.Source -q '-Dmaven.test.skip=true' package dependency:copy-dependencies '-DincludeScope=runtime' '-DoutputDirectory=target/lib'
 if($LASTEXITCODE -ne 0){throw "Maven no pudo compilar o copiar recursos (código $LASTEXITCODE)."}
 $jar=Join-Path $PSScriptRoot 'target\biblioteca-3.0.0.jar'
 $contenido=& "$jdk\bin\jar.exe" tf $jar
 if($LASTEXITCODE -ne 0 -or $contenido -notcontains 'resources/styles.css' -or $contenido -notcontains 'resources/icon1.png'){throw 'Recursos incompletos en el JAR'}
 if($SoloCompilar){exit 0}
 $perfilRuta=Join-Path $env:LOCALAPPDATA "Biblioteca\$Perfil"
 $cred=Import-Clixml -LiteralPath (Join-Path $perfilRuta 'conexion.xml')
 $env:BIBLIOTECA_DB_NAME=$Perfil;$env:BIBLIOTECA_DB_USER=$cred.UserName;$env:BIBLIOTECA_DB_PASSWORD=$cred.GetNetworkCredential().Password
 $modulos=(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'target\lib') -Filter 'javafx-*-win.jar' | Select-Object -ExpandProperty FullName) -join [IO.Path]::PathSeparator
 if(!$modulos){throw 'No se encontraron los módulos JavaFX de Windows'}
 $argumentos=@();if($Consola){$argumentos+='--consola'}
 & "$jdk\bin\java.exe" --enable-native-access=javafx.graphics --module-path $modulos --add-modules javafx.controls '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp "$jar;$PSScriptRoot\target\lib\*" aplicacion.Lanzador @argumentos
 $resultado=$LASTEXITCODE
 $env:BIBLIOTECA_DB_PASSWORD=$null
 exit $resultado
}catch{Write-Host "No se pudo iniciar Biblioteca: $($_.Exception.Message)" -ForegroundColor Red;exit 1}
