package controlador;

import estructuras.ArbolBinario;
import modelo.Usuario;
import excepciones.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que gestiona la colección de usuarios registrados en el sistema.
 * Utiliza un árbol binario para almacenamiento eficiente, permitiendo búsqueda
 * rápida y ordenada.
 * Cada usuario se administra con validaciones para evitar duplicados y
 * operaciones inválidas.
 */
public class GestorUsuarios {

    // Árbol binario que almacena los usuarios ordenados por su ID
    private ArbolBinario<Usuario> arbolUsuarios;

    /**
     * Constructor que inicializa el árbol binario vacío para usuarios.
     */
    public GestorUsuarios() {
        arbolUsuarios = new ArbolBinario<>();
    }

    /**
     * Agrega un nuevo usuario al sistema.
     * Valida que el usuario no sea nulo y que no exista otro usuario con el mismo
     * ID.
     * 
     * @param usuario Objeto Usuario a agregar
     * @throws OperacionInvalidaException si el usuario es nulo o ya existe
     */
    public void agregarUsuario(Usuario usuario) throws OperacionInvalidaException {
        if (usuario == null) {
            throw new OperacionInvalidaException("El usuario no puede ser nulo");
        }
        if (arbolUsuarios.contiene(usuario)) {
            throw new OperacionInvalidaException("Ya existe un usuario con ese ID: " + usuario.getId());
        }
        arbolUsuarios.insertar(usuario);
    }

    /**
     * Busca un usuario por su ID.
     * Crea un usuario temporal con el ID para realizar la búsqueda en el árbol.
     * 
     * @param id Identificador único del usuario
     * @return Usuario encontrado
     * @throws ElementoNoEncontradoException si el usuario no existe
     */
    public Usuario buscarUsuario(String id) throws ElementoNoEncontradoException {
        try {
            Usuario usuarioBusqueda = new Usuario(id, "temp", "temp@example.com", 1);
            return arbolUsuarios.buscar(usuarioBusqueda);
        } catch (OperacionInvalidaException e) {
            throw new ElementoNoEncontradoException("Error al buscar usuario: " + e.getMessage());
        }
    }

    /**
     * Verifica si un usuario existe en el sistema.
     * 
     * @param id ID del usuario a verificar
     * @return true si el usuario existe, false si no
     */
    public boolean existeUsuario(String id) {
        try {
            buscarUsuario(id);
            return true;
        } catch (ElementoNoEncontradoException e) {
            return false;
        }
    }

    /**
     * Elimina un usuario por su ID.
     * Solo permite la eliminación si el usuario no tiene préstamos activos.
     * Esto asegura que no se pierda el control de libros prestados.
     * 
     * @param id ID del usuario a eliminar
     * @throws ElementoNoEncontradoException si el usuario no existe
     * @throws OperacionInvalidaException    si el usuario tiene préstamos activos
     */
    public void eliminarUsuario(String id) throws ElementoNoEncontradoException, OperacionInvalidaException {
        Usuario usuario = buscarUsuario(id);
        if (usuario.getPrestamosActuales() > 0) {
            throw new OperacionInvalidaException("No se puede eliminar un usuario con préstamos activos");
        }
        arbolUsuarios.eliminar(usuario);
    }

    /**
     * Muestra todos los usuarios en orden, haciendo un recorrido in-orden del
     * árbol.
     * Útil para reportes ordenados y listados en la interfaz.
     * 
     * @throws EstructuraVaciaException si no hay usuarios registrados
     */
    public void mostrarUsuariosOrdenados() throws EstructuraVaciaException {
        arbolUsuarios.recorrerEnOrden(usuario -> System.out.println(usuario));
    }

    /**
     * Muestra los usuarios que tienen multas acumuladas.
     * Permite visualizar usuarios en mora para tomar acciones correctivas.
     * 
     * @throws EstructuraVaciaException si no hay usuarios registrados
     */
    public void mostrarUsuariosConMultas() throws EstructuraVaciaException {
        arbolUsuarios.recorrerEnOrden(usuario -> {
            if (usuario.getMultaAcumulada() > 0) {
                System.out.println(usuario);
            }
        });
    }

    /**
     * Obtiene una lista con todos los usuarios registrados.
     * Útil para enviarlos a interfaces gráficas o reportes.
     * 
     * @return Lista con todos los usuarios (posiblemente vacía)
     */
    public List<Usuario> obtenerTodosUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        try {
            arbolUsuarios.recorrerEnOrden(usuario -> usuarios.add(usuario));
        } catch (EstructuraVaciaException e) {
            // Si el árbol está vacío, retorna lista vacía
        }
        return usuarios;
    }

    /**
     * Retorna el número total de usuarios registrados en el sistema.
     * 
     * @return Cantidad total de usuarios
     */
    public int getTotalUsuarios() {
        return arbolUsuarios.size();
    }

    /**
     * Verifica si no hay usuarios registrados en el sistema.
     * 
     * @return true si no hay usuarios, false si hay al menos uno
     */
    public boolean estaVacio() {
        return arbolUsuarios.estaVacio();
    }
}
