# Sistema de Biblioteca

Aplicación JavaFX con interfaz alternativa de consola y almacenamiento en MySQL local de Wamp.

## Ejecutar

Requiere MySQL 8.4, JDK 26 y Maven. Iniciar `wampmysqld64` y ejecutar `run.bat`; para consola, `run.bat -Consola`. VS Code usa `.vscode/settings.json` y las dependencias de `pom.xml`.

## Libros y ejemplares

Al agregar un libro, indique su cantidad inicial de copias. Seleccione un libro y pulse **Ejemplares** para consultar sus copias, agregar más o retirar una que no tenga préstamos relacionados. Cada préstamo asigna automáticamente un ejemplar disponible y conserva su identificador en el historial.

Las reservas mantienen su orden y tienen prioridad sobre los ejemplares disponibles. Las copias sobrantes pueden prestarse a otros usuarios. La tabla de libros muestra total, prestados y disponibles; la consola ofrece las mismas operaciones en el menú de libros.

El esquema requiere las versiones SQL 1, 2 y 3, con sus checksums originales. La actualización a la versión 3 requiere libros, préstamos y reservas vacíos; debe ejecutarse con la aplicación cerrada, una copia de seguridad verificada y permisos de instalación. La cuenta habitual de la aplicación necesita SELECT, INSERT, UPDATE y DELETE en `ejemplares`, además de sus permisos anteriores. Las credenciales y herramientas locales de mantenimiento permanecen fuera del repositorio.
