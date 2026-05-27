# Documentación Breve del Proyecto (Clases)

Proyecto: Sistema de Biblioteca (consola + JavaFX)

## Paquete `aplicacion`

### `Main`
Punto de entrada en **consola**: inicializa gestores, carga datos desde archivos, valida login de admin y expone menús para libros/usuarios/préstamos/reservas/reportes.

## Paquete `gui`

### `BibliotecaApp`
Punto de entrada **JavaFX**: inicializa el sistema (gestores + carga de datos), muestra el login y al cerrar guarda todo el estado (libros, usuarios, préstamos, reservas).

## Paquete `gui.controllers`

### `LoginController`
Pantalla de inicio de sesión del admin: carga credenciales desde persistencia, controla intentos y abre el dashboard si las credenciales son válidas.

### `DashboardController`
Contenedor principal de la UI: crea la barra superior y el menú lateral, y cambia la vista central entre módulos (inicio, libros, usuarios, préstamos, reservas, reportes).

### `LibrosView`
Vista de gestión de libros: tabla, búsqueda/filtro y diálogos para agregar/editar/eliminar libros usando `GestorLibros`.

### `UsuariosView`
Vista de gestión de usuarios: tabla, búsqueda/filtro y diálogos para agregar/editar/eliminar usuarios usando `GestorUsuarios`.

### `PrestamosView`
Vista de gestión de préstamos: lista/filtrado por estado, creación de préstamo, devolución, edición y cancelación usando `GestorPrestamos` (y consulta a libros/usuarios).

### `ReservasView`
Vista de gestión de reservas: lista de reservas, alta/cancelación y cálculo de disponibilidad estimada usando `GestorReservas` y la información de préstamos activos.

### `ReportesView`
Vista que muestra reportes en un `TextArea`: captura la salida de consola de `GeneradorReportes` (general, libros, usuarios, préstamos, top 5, estado financiero, completo).

## Paquete `controlador`

### `GestorLibros`
Lógica de negocio para libros: alta/baja/búsqueda/listado. Almacena libros en un `ArbolBinario<Libro>` (ordenado por ID) y aplica validaciones (duplicados, no eliminar si está prestado).

### `GestorUsuarios`
Lógica de negocio para usuarios: alta/baja/búsqueda/listado. Usa `ArbolBinario<Usuario>` (ordenado por ID) y valida reglas (no eliminar si tiene préstamos activos, evitar duplicados).

### `GestorPrestamos`
Lógica de préstamos: registrar préstamo, devolución, renovación, edición de fechas y cancelación. Mantiene préstamos activos en `ArbolBinario<Prestamo>` y un historial en `Cola<Prestamo>`. Integra reservas (`GestorReservas`) para respetar prioridad del libro.

### `GestorReservas`
Lógica de reservas: agrega/cancela/consulta reservas por libro. Mantiene un `Map<String, ListaEnlazada<Reserva>>` (cola por ID de libro) y gestiona estados (pendiente, notificada, cancelada) con reglas de negocio.

## Paquete `modelo`

### `Libro`
Entidad libro: datos (ID, título, autor, ISBN, año, categoría), validaciones y estado de disponibilidad. Implementa `Comparable` para ordenarse por ID.

### `Usuario`
Entidad usuario: datos (ID, nombre, email), límite de préstamos, préstamos actuales y multa acumulada. Incluye reglas simples (`puedePedirPrestamo`, pagar multa, etc.) e implementa `Comparable` por ID.

### `Prestamo`
Entidad préstamo: asocia `Usuario` + `Libro` con fechas (préstamo, devolución esperada/real) y estado. Calcula retraso/multa y soporta renovación. Implementa `Comparable` por ID de préstamo.

### `Reserva`
Entidad reserva: asocia `Usuario` + `Libro` con fecha y estado. Genera ID automático y permite transiciones básicas (notificar, cancelar).

### `Admin`
Entidad de administrador del sistema: guarda usuario/contraseña/nombre y valida credenciales.

## Paquete `utilidades`

### `PersistenciaArchivos`
Capa de persistencia en archivos de texto (`datos/*.txt`): guarda y carga libros, usuarios, préstamos, reservas y admin. Incluye DTOs internos (`DatosPrestamo`, `DatosReserva`) para reconstruir objetos.

### `GeneradorReportes`
Genera reportes por consola (estadísticas y agregaciones) usando los gestores: distribución por categoría/autor, estado de usuarios, préstamos vencidos, top 5 usuarios y estado financiero (multas).

## Paquete `estructuras`

### `ArbolBinario<T>`
Árbol binario de búsqueda genérico (ABB): inserción, búsqueda, eliminación y recorridos (in/pre/post orden). Se usa como base para almacenar libros, usuarios y préstamos ordenados.

### `ListaEnlazada<T>`
Lista doblemente enlazada: agregar al inicio/final, eliminar (primero/último/por dato) y recorrer. Se usa como “cola” de reservas por libro.

### `Cola<T>`
Cola enlazada: `encolar`, `desencolar`, ver frente, recorrer y limpiar. Se usa para el historial de préstamos.

### `Pila<T>`
Pila enlazada: `apilar`, `desapilar`, ver cima, recorrer y limpiar. (Estructura utilitaria del proyecto).

### `Nodo<T>`
Nodo genérico para estructuras enlazadas: contiene `dato` y referencias `siguiente/anterior`.

## Paquete `excepciones`

### `BibliotecaException`
Excepción base del dominio del sistema (extiende `Exception`).

### `ElementoNoEncontradoException`
Excepción para “no se encontró el elemento buscado” (ej: libro/usuario/préstamo inexistente).

### `EstructuraVaciaException`
Excepción para operaciones inválidas sobre estructuras vacías (árbol/lista/cola/pila sin elementos).

### `OperacionInvalidaException`
Excepción para reglas de negocio u operaciones no permitidas (duplicados, libro no disponible, etc.).

---

# Conceptos Básicos de Java (rápido)

## Tipos de datos comunes

| Tipo | Qué representa | Ejemplo |
|---|---|---|
| `int` | Entero (sin decimales), 32 bits | `int edad = 20;` |
| `double` | Número con decimales, 64 bits | `double multa = 2.5;` |
| `boolean` | Verdadero/Falso | `boolean disponible = true;` |
| `char` | Un carácter | `char letra = 'A';` |
| `String` | Texto (clase, no primitivo) | `String nombre = "Ana";` |

## Modificadores de acceso (visibilidad)

| Modificador | Accesible desde | Uso típico |
|---|---|---|
| `public` | Cualquier clase | APIs/métodos que se exponen hacia afuera |
| `private` | Solo dentro de la misma clase | Encapsular atributos y detalles internos |
| `protected` | Misma clase, mismo paquete y subclases | Herencia y extensiones |
| *(sin palabra)* “package-private” | Solo dentro del mismo paquete | Detalles compartidos en un módulo |

## Palabras clave muy usadas

| Palabra | Para qué sirve | Ejemplo |
|---|---|---|
| `class` | Declara una clase | `class Libro { ... }` |
| `new` | Crea un objeto | `new Usuario(...)` |
| `static` | Pertenece a la clase, no al objeto | `static void main(...)` |
| `final` | No se puede reasignar (o no se puede heredar/sobrescribir) | `final int DIAS = 14;` |
| `void` | Método no retorna valor | `void guardar()` |
| `return` | Retorna un valor (o corta el método) | `return true;` |
| `null` | “Sin referencia” (solo para objetos) | `String s = null;` |
| `this` | Referencia al objeto actual | `this.id = id;` |
| `extends` | Herencia | `class X extends Y` |
| `implements` | Implementa interfaz | `class Libro implements Comparable<Libro>` |
| `enum` | Define valores fijos | `enum Estado { ACTIVO, ... }` |
