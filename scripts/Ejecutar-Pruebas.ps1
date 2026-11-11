$ErrorActionPreference='Stop'
$raiz=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $raiz
. "$PSScriptRoot\Entorno.ps1" -Base biblioteca_pruebas
$perfil=Join-Path $env:LOCALAPPDATA 'Biblioteca\biblioteca_pruebas'
$credPruebas=Import-Clixml -LiteralPath (Join-Path $perfil 'pruebas.xml')
$env:BIBLIOTECA_TEST_USER=$credPruebas.UserName
$env:BIBLIOTECA_TEST_PASSWORD=$credPruebas.GetNetworkCredential().Password
$admin=Import-Clixml -LiteralPath (Join-Path $perfil 'acceso-inicial.xml')
$env:BIBLIOTECA_ADMIN_PASSWORD=$admin.GetNetworkCredential().Password
$evidencia=Join-Path $env:LOCALAPPDATA ('Biblioteca\pruebas\'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
$env:BIBLIOTECA_TEST_EVIDENCE=$evidencia
New-Item -ItemType Directory -Force -Path $evidencia | Out-Null
$mvn='C:\Program Files\Apache\Maven\apache-maven-3.10.0\bin\mvn.cmd'
& $mvn -q test-compile dependency:copy-dependencies '-DincludeScope=test' '-DoutputDirectory=target/test-lib' *> (Join-Path $evidencia 'compilacion.log')
if($LASTEXITCODE -ne 0){Get-Content -LiteralPath (Join-Path $evidencia 'compilacion.log') -Tail 50;exit 1}
$cp="target/classes;target/test-classes;target/test-lib/*"
$modulos=(Get-ChildItem -LiteralPath (Join-Path $raiz "target\test-lib") -Filter "javafx-*-win.jar" | Select-Object -ExpandProperty FullName) -join [IO.Path]::PathSeparator
& "$env:JAVA_HOME\bin\java.exe" --enable-native-access=ALL-UNNAMED --module-path $modulos --add-modules javafx.controls,javafx.swing '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp $cp PruebasNucleo (Join-Path $evidencia 'nucleo-resultados.tsv') *> (Join-Path $evidencia 'nucleo.log')
if($LASTEXITCODE -ne 0){Get-Content -LiteralPath (Join-Path $evidencia 'nucleo.log') -Tail 50;exit 1}
& "$env:JAVA_HOME\bin\java.exe" --enable-native-access=ALL-UNNAMED --module-path $modulos --add-modules javafx.controls,javafx.swing '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp $cp PruebasBD suite (Join-Path $evidencia 'bd-resultados.tsv') *> (Join-Path $evidencia 'bd.log')
if($LASTEXITCODE -ne 0){Get-Content -LiteralPath (Join-Path $evidencia 'bd.log') -Tail 65;exit 1}
& "$env:JAVA_HOME\bin\java.exe" --enable-native-access=ALL-UNNAMED --module-path $modulos --add-modules javafx.controls,javafx.swing '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp $cp LanzadorPruebasGui $evidencia *> (Join-Path $evidencia 'gui.log')
if($LASTEXITCODE -ne 0){Get-Content -LiteralPath (Join-Path $evidencia 'gui.log') -Tail 75;exit 1}
$env:BIBLIOTECA_QA_PASSWORD=$env:BIBLIOTECA_ADMIN_PASSWORD
'Premain-Class: AgenteArranque' | Set-Content -LiteralPath 'target/agente-manifest.txt' -Encoding ascii
& "$env:JAVA_HOME\bin\jar.exe" cfm target/agente-qa.jar target/agente-manifest.txt -C target/test-classes AgenteArranque.class -C target/test-classes SondaArranque.class
if($LASTEXITCODE -ne 0){throw 'No se pudo preparar el agente exclusivo de QA'}
& python src/test/python/PruebasArranqueMySQL.py
if($LASTEXITCODE -ne 0){exit 1}
Get-Content -LiteralPath (Join-Path $evidencia 'nucleo.log') -Tail 1
Get-Content -LiteralPath (Join-Path $evidencia 'gui.log') -Tail 1

Get-Content -LiteralPath (Join-Path $evidencia 'bd.log') -Tail 1

Write-Output "Resultados: $evidencia"
