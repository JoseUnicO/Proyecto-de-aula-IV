# Sistema de Biblioteca

Aplicación JavaFX con interfaz alternativa de consola y almacenamiento en MySQL local de Wamp.

## Ejecutar

Requiere MySQL 8.4, JDK 26 y Maven. Iniciar `wampmysqld64` y ejecutar `run.bat`; para consola, `run.bat -Consola`. VS Code usa `.vscode/settings.json` y las dependencias de `pom.xml`.

Se conservan el administrador y su contraseña. Las credenciales de conexión están cifradas con DPAPI en `%LOCALAPPDATA%\Biblioteca\biblioteca`, fuera del proyecto. La instalación en otro equipo requiere preparar su base MySQL y su propio perfil de conexión.
