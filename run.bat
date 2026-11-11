@echo off
setlocal
pushd "%~dp0" || exit /b 1
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0Arrancar.ps1" %*
set "RESULTADO=%ERRORLEVEL%"
popd
exit /b %RESULTADO%
