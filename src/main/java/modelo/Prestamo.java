package modelo;

import excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Clase que representa un préstamo de un libro a un usuario.
 * Implementa Comparable para permitir ordenamiento por idPrestamo.
 * Controla fechas de préstamo, devolución, estado y cálculos de retrasos y multas.
 */
public class Prestamo implements Comparable<Prestamo> {
    private String idPrestamo;
    private Usuario usuario;
    private Libro libro;
    private LocalDate fechaPrestamo;
    private LocalDate fechaDevolucionEsperada;
    private LocalDate fechaDevolucionReal;
    private EstadoPrestamo estado;


    public enum EstadoPrestamo {
        ACTIVO, DEVUELTO, VENCIDO, CANCELADO
    }

    /**
     * Constructor para crear un nuevo préstamo activo desde un usuario, libro
     * y días a prestar. Se genera automáticamente un ID único y la fecha de préstamo
     * es la fecha actual.
     */
    public Prestamo(Usuario usuario, Libro libro, int diasPrestamo) {
        this.idPrestamo = generarId();
        this.usuario = usuario;
        this.libro = libro;
        this.fechaPrestamo = LocalDate.now();
        this.fechaDevolucionEsperada = fechaPrestamo.plusDays(diasPrestamo);
        this.estado = EstadoPrestamo.ACTIVO;
    }

    /**
     * Constructor completo para cargar préstamos existentes con todos sus atributos.
     * Sincroniza el contador estático para evitar IDs duplicados.
     */
    public Prestamo(String idPrestamo, Usuario usuario, Libro libro,
            LocalDate fechaPrestamo, LocalDate fechaDevolucionEsperada,
            LocalDate fechaDevolucionReal, EstadoPrestamo estado) {
        this.idPrestamo = idPrestamo;
        this.usuario = usuario;
        this.libro = libro;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaDevolucionEsperada = fechaDevolucionEsperada;
        this.fechaDevolucionReal = fechaDevolucionReal;
        this.estado = estado;

    }
    private String generarId(){return infraestructura.Identificadores.nuevo("prestamo","PREST-");}

    public void registrarDevolucion() {
        this.fechaDevolucionReal = LocalDate.now();
        this.estado = EstadoPrestamo.DEVUELTO;
    }

    public void cancelar() {
        this.fechaDevolucionReal = LocalDate.now();
        this.estado = EstadoPrestamo.CANCELADO;
    }

    public boolean estaVencido() {
        if (estado == EstadoPrestamo.DEVUELTO || estado == EstadoPrestamo.CANCELADO) {
            return false;
        }
        return LocalDate.now().isAfter(fechaDevolucionEsperada);
    }

    public long getDiasRetraso() {
        if (estado == EstadoPrestamo.DEVUELTO || estado == EstadoPrestamo.CANCELADO) {
            if (fechaDevolucionReal != null && fechaDevolucionReal.isAfter(fechaDevolucionEsperada)) {
                return ChronoUnit.DAYS.between(fechaDevolucionEsperada, fechaDevolucionReal);
            }
            return 0;
        }

        if (estaVencido()) {
            return ChronoUnit.DAYS.between(fechaDevolucionEsperada, LocalDate.now());
        }

        return 0;
    }

    /**
     * Calcula la multa a pagar en base a los días de retraso y la tarifa por día.
     */
    public double calcularMulta(double tarifaPorDia) {
        return getDiasRetraso() * tarifaPorDia;
    }

    /**
     * Renueva el préstamo extendiendo la fecha de devolución esperada por los días
     * adicionales indicados. Solo se pueden renovar préstamos en estado ACTIVO y no vencidos.
     */
    public void renovar(int diasAdicionales) throws OperacionInvalidaException {
        if (estado != EstadoPrestamo.ACTIVO) {
            throw new OperacionInvalidaException("Solo se pueden renovar préstamos activos");
        }

        if (estaVencido()) {
            throw new OperacionInvalidaException("No se puede renovar un préstamo vencido");
        }

        this.fechaDevolucionEsperada = this.fechaDevolucionEsperada.plusDays(diasAdicionales);
    }

    public boolean puedeRenovarse() {
        return estado == EstadoPrestamo.ACTIVO && !estaVencido();
    }

    public String getIdPrestamo() {
        return idPrestamo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Libro getLibro() {
        return libro;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public LocalDate getFechaDevolucionEsperada() {
        return fechaDevolucionEsperada;
    }

    public LocalDate getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamo estado) {
        this.estado = estado;
    }

    public void setFechaDevolucionEsperada(LocalDate fechaDevolucionEsperada) {
        this.fechaDevolucionEsperada = fechaDevolucionEsperada;
    }

    /**
     * Compara dos préstamos para ordenarlos por su ID, importante para colecciones ordenadas.
     */
    @Override
    public int compareTo(Prestamo otro) {
        return this.idPrestamo.compareTo(otro.idPrestamo);
    }

    @Override
    public String toString() {
        return String.format("Prestamo[%s] Usuario: %s, Libro: %s, Estado: %s, Dias retraso: %d",
                idPrestamo, usuario.getNombre(), libro.getTitulo(), estado, getDiasRetraso());
    }
public void restaurarFechaReal(java.time.LocalDate fecha){fechaDevolucionReal=fecha;}
public void restaurarDatos(Prestamo p){
 if(!idPrestamo.equals(p.idPrestamo)||usuario!=p.usuario||libro!=p.libro)throw new IllegalArgumentException("Identidad distinta del préstamo");
 fechaPrestamo=p.fechaPrestamo;fechaDevolucionEsperada=p.fechaDevolucionEsperada;fechaDevolucionReal=p.fechaDevolucionReal;estado=p.estado;
}
}
