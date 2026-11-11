$ErrorActionPreference='Stop'
$script=Join-Path $PSScriptRoot 'Copia-Programada.ps1'
if(!(Test-Path -LiteralPath (Join-Path $env:LOCALAPPDATA 'Biblioteca\biblioteca\conexion.xml'))){throw 'Primero debe activarse la base definitiva'}
$usuario=[Security.Principal.WindowsIdentity]::GetCurrent().Name
$accion=New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -NonInteractive -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$script`""
$diario=New-ScheduledTaskTrigger -Daily -At '18:00'
$inicio=New-ScheduledTaskTrigger -AtLogOn -User $usuario
$principal=New-ScheduledTaskPrincipal -UserId $usuario -LogonType Interactive -RunLevel Limited
$opciones=New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Minutes 10)
Register-ScheduledTask -TaskName 'Biblioteca - Copia MySQL' -Description 'Copia SQL local protegida; 7 diarias y 4 semanales. Requiere la sesión de Windows del propietario para DPAPI.' -Action $accion -Trigger @($diario,$inicio) -Principal $principal -Settings $opciones -Force | Out-Null
Write-Output 'Copia programada diariamente a las 18:00 y al iniciar la sesión de Windows.'
