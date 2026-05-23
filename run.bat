@echo off
cls

echo ========================================
echo COMPILANDO PROYECTO BIBLIOTECA
echo ========================================
echo.

rem Ruta del JDK 26
set JAVA_HOME=C:\Program Files\Java\jdk-26.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

rem Ruta del SDK JavaFX 26
set JAVAFX_PATH=C:\javafx-sdk-26.0.1\lib

rem Carpeta de salida
if not exist "out\" mkdir out

echo [1/8] Compilando estructuras...
javac -d out estructuras\*.java
if errorlevel 1 goto error

echo [2/8] Compilando excepciones...
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
rem No salimos en error aqui por si no tienes clases de consola

echo Copiando recursos (iconos y CSS)...
if not exist "out\resources\" mkdir out\resources
xcopy /E /I /Y Resources out\resources > nul

echo [7/7] Compilando GUI...
javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics ^
      -cp out -d out gui\*.java gui\controllers\*.java
if errorlevel 1 goto error

echo.
echo [OK] Compilacion exitosa!
echo ========================================
echo EJECUTANDO APLICACION
echo ========================================
echo.

java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.base,javafx.graphics ^
     -cp out gui.BibliotecaApp
goto fin

:error
echo.
echo [ERROR] Fallo en la compilacion
pause
exit /b 1

:fin
