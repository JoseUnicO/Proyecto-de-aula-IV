# Sistema de Biblioteca

Aplicación JavaFX con interfaz alternativa de consola y almacenamiento en MySQL local de Wamp.

## Ejecutar

Requiere MySQL 8.4, JDK 26 y Maven. Iniciar `wampmysqld64` y ejecutar `run.bat`; para consola, `run.bat -Consola`. VS Code usa `.vscode/settings.json` y las dependencias de `pom.xml`.

Se conservan el administrador y su contraseña. Las credenciales de conexión están cifradas con DPAPI en `%LOCALAPPDATA%\Biblioteca\biblioteca`, fuera del proyecto. Para instalar en una base nueva, ejecutar `scripts/Preparar-Base.ps1` con PowerShell 7; crea una biblioteca vacía y guarda el acceso inicial aleatorio en el perfil protegido. No sobrescribe instalaciones existentes.

## Estructura

- `src/main/java`: programa y estructuras académicas.
- `src/main/resources`: imágenes, estilos y revisiones SQL del esquema.
- `src/test`: pruebas del programa, GUI, consola y BAT.
- `scripts`: preparación, pruebas y copias de seguridad.
- `target`: salida temporal de Maven; se regenera al compilar.

El importador antiguo fue retirado. Quedan ocho tablas: administradores, usuarios, libros, préstamos, reservas, secuencias, operaciones y control de versión. V001 conserva la definición original del esquema y V002 elimina la tabla obsoleta; ambas revisiones permiten verificar versiones e instalar desde cero.

## Pruebas

Ejecutar `scripts/Ejecutar-Pruebas.ps1` con PowerShell 7 y Python 3. Usa exclusivamente `biblioteca_pruebas`; si no existe, preparar con `scripts/Preparar-Base.ps1 -Base biblioteca_pruebas`.

La batería mantiene los 99 escenarios aplicables al sistema actual. Se retiraron los 13 casos exclusivos del importador eliminado. Logs, resultados y capturas van a `%LOCALAPPDATA%\Biblioteca\pruebas`, fuera del proyecto. El agente de QA se genera temporalmente y no está incluido en la aplicación.

## Copias de seguridad

La tarea de Windows `Biblioteca - Copia MySQL` realiza una copia diaria a las 18:00 y al iniciar la sesión del propietario. Conserva 7 representantes diarios y 4 semanales en `%LOCALAPPDATA%\Biblioteca\biblioteca\copias`; el estado reciente está en `ultima-copia.json`. Copia manual: `scripts/Copia-Seguridad.ps1`.

Para comprobar recuperación: `scripts/Verificar-Restauracion.ps1 -Copia <archivo.sql> -Origen biblioteca -Destino biblioteca_recuperacion`. Exige un destino nuevo, verifica SHA256 y compara las ocho tablas. Al cambiar de usuario de Windows o de equipo se deben preparar nuevas credenciales de conexión.
