@echo off
setlocal enabledelayedexpansion
cls

echo ========================================
echo CONFIGURANDO ENTORNO DE BIBLIOTECA
echo ========================================
echo.

set ENV_DIR=%USERPROFILE%\.biblioteca_env
if not exist "%ENV_DIR%" mkdir "%ENV_DIR%"

rem 1. Verificar/Descargar JDK 26
set JAVA_HOME=C:\Program Files\Java\jdk-26.0.1
if not exist "!JAVA_HOME!\bin\javac.exe" (
    set JAVA_HOME=%ENV_DIR%\jdk-26.0.1
    if not exist "!JAVA_HOME!\bin\javac.exe" (
        echo [INFO] No se detecto JDK 26 instalado. Descargando JDK portable...
        powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Write-Host 'Descargando JDK 26...'; Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/26/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile '%ENV_DIR%\jdk26.zip'; Write-Host 'Extrayendo JDK...'; Expand-Archive -Path '%ENV_DIR%\jdk26.zip' -DestinationPath '%ENV_DIR%' -Force; Remove-Item '%ENV_DIR%\jdk26.zip'; $f = Get-ChildItem '%ENV_DIR%' -Directory | Where-Object { $_.Name -like '*jdk-26*' } | Select-Object -First 1; if ($f.Name -ne 'jdk-26.0.1') { Rename-Item -Path $f.FullName -NewName 'jdk-26.0.1' }"
        if errorlevel 1 goto error_download
    )
)

rem 2. Verificar/Descargar JavaFX 26
set JAVAFX_PATH=C:\javafx-sdk-26.0.1\lib
if not exist "!JAVAFX_PATH!\javafx.controls.jar" (
    set JAVAFX_PATH=%ENV_DIR%\javafx-sdk-26.0.1\lib
    if not exist "!JAVAFX_PATH!\javafx.controls.jar" (
        echo [INFO] No se detecto JavaFX 26. Descargando JavaFX portable...
        powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Write-Host 'Descargando JavaFX 26...'; Invoke-WebRequest -Uri 'https://download2.gluonhq.com/openjfx/26.0.1/openjfx-26.0.1_windows-x64_bin-sdk.zip' -OutFile '%ENV_DIR%\javafx26.zip'; Write-Host 'Extrayendo JavaFX...'; Expand-Archive -Path '%ENV_DIR%\javafx26.zip' -DestinationPath '%ENV_DIR%' -Force; Remove-Item '%ENV_DIR%\javafx26.zip'; $f = Get-ChildItem '%ENV_DIR%' -Directory | Where-Object { $_.Name -like '*javafx-sdk-26*' } | Select-Object -First 1; if ($f.Name -ne 'javafx-sdk-26.0.1') { Rename-Item -Path $f.FullName -NewName 'javafx-sdk-26.0.1' }"
        if errorlevel 1 goto error_download
    )
)

set PATH=!JAVA_HOME!\bin;%PATH%

echo.
echo [OK] Entorno listo. Compilando proyecto...
echo ========================================
echo.

rem Carpeta de salida
if not exist "out\" mkdir out

echo [1/7] Compilando estructuras...
javac -d out estructuras\*.java
if errorlevel 1 goto error

echo [2/7] Compilando excepciones...
javac -cp out -d out excepciones\*.java
if errorlevel 1 goto error

echo [3/7] Compilando modelos...
javac -cp out -d out modelo\*.java
if errorlevel 1 goto error

echo [4/7] Compilando utilidades + controladores (juntos por dependencia circular)...
javac -cp out -d out utilidades\*.java controlador\*.java
if errorlevel 1 goto error

echo [6/7] Compilando aplicacion consola (si existe)...
javac -cp out -d out aplicacion\*.java

echo Copiando recursos (iconos y CSS)...
if not exist "out\resources\" mkdir out\resources
xcopy /E /I /Y Resources out\resources > nul

echo [7/7] Compilando GUI...
javac --module-path "!JAVAFX_PATH!" --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics ^
      -cp out -d out gui\*.java gui\controllers\*.java
if errorlevel 1 goto error

echo.
echo [OK] Compilacion exitosa!
echo ========================================
echo EJECUTANDO APLICACION
echo ========================================
echo.

java --module-path "!JAVAFX_PATH!" --add-modules javafx.controls,javafx.base,javafx.graphics ^
     -cp out gui.BibliotecaApp
goto fin

:error_download
echo.
echo [ERROR] No se pudieron descargar las herramientas de entorno.
echo Verifica tu conexion a Internet.
pause
exit /b 1

:error
echo.
echo [ERROR] Fallo en la compilacion
pause
exit /b 1

:fin
