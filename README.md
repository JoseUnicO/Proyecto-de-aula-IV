# Sistema de Biblioteca

Aplicación JavaFX con interfaz alternativa de consola y almacenamiento en MySQL local de Wamp.

## Ejecutar

Requiere MySQL 8.4, JDK 26 y Maven. Iniciar `wampmysqld64` y ejecutar `run.bat`; para consola, `run.bat -Consola`. VS Code usa `.vscode/settings.json` y las dependencias de `pom.xml`.

Se conservan el administrador y su contraseña. Las credenciales de conexión están cifradas con DPAPI en `%LOCALAPPDATA%\Biblioteca\biblioteca`, fuera del proyecto. La instalación en otro equipo requiere preparar su base MySQL y su propio perfil de conexión.

## Estructura

- `src/main/java`: programa y estructuras académicas.
- `src/main/resources`: imágenes, estilos y revisiones SQL del esquema.
- `src/test`: pruebas del programa, GUI, consola y BAT.
- `target`: salida temporal de Maven; se regenera al compilar.

El importador antiguo fue retirado. Quedan ocho tablas: administradores, usuarios, libros, préstamos, reservas, secuencias, operaciones y control de versión. V001 conserva la definición original del esquema y V002 elimina la tabla obsoleta; ambas revisiones permiten verificar versiones e instalar desde cero.

## Verificación

Los fuentes de las pruebas permanecen en `src/test`; la versión actual fue validada con 99 escenarios. Las herramientas locales de ejecución y sus resultados se guardan fuera del repositorio, en `%LOCALAPPDATA%\Biblioteca`.

## Copias de seguridad

La tarea de Windows `Biblioteca - Copia MySQL` realiza una copia diaria a las 18:00 y al iniciar la sesión del propietario. Conserva 7 representantes diarios y 4 semanales en `%LOCALAPPDATA%\Biblioteca\biblioteca\copias`; el estado reciente está en `ultima-copia.json`.

El mantenimiento y los respaldos utilizan herramientas locales en `%LOCALAPPDATA%\Biblioteca\mantenimiento`, independientes de los archivos del repositorio. Al cambiar de usuario de Windows o de equipo se deben preparar nuevas credenciales de conexión.
