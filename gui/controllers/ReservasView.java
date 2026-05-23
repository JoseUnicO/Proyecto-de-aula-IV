package gui.controllers;

import controlador.GestorReservas;
import controlador.GestorLibros;
import controlador.GestorUsuarios;
import gui.BibliotecaApp;
import modelo.Prestamo;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.ImageView;
import modelo.Reserva;
import javafx.scene.control.TableRow;
import modelo.Libro;
import modelo.Usuario;
import excepciones.ElementoNoEncontradoException;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;

public class ReservasView extends VBox {
    private GestorReservas gestorReservas;
    private GestorLibros gestorLibros;
    private GestorUsuarios gestorUsuarios;
    private TableView<Reserva> tablaReservas;
    private ObservableList<Reserva> reservasObservable;
    private DateTimeFormatter dateFormatter;

    public ReservasView(GestorReservas gestorReservas, GestorLibros gestorLibros, GestorUsuarios gestorUsuarios) {
        this.gestorReservas = gestorReservas;
        this.gestorLibros = gestorLibros;
        this.gestorUsuarios = gestorUsuarios;
        this.reservasObservable = FXCollections.observableArrayList();
        this.dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        inicializar();
        cargarReservas();
    }

    private void inicializar() {
        this.setPadding(new Insets(24));
        this.setSpacing(24);

        Label titulo = new Label("🔖 Gestión de Reservas");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        titulo.setTextFill(Color.web("#2c3e50"));
        HBox panelBotones = crearPanelBotones();
        tablaReservas = crearTablaReservas();
        VBox.setVgrow(tablaReservas, Priority.ALWAYS);

        this.getChildren().addAll(titulo, panelBotones, tablaReservas);
    }

    private HBox crearPanelBotones() {
        HBox panel = new HBox(16);
        panel.setPadding(new Insets(16));
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");

        Button btnNueva = new Button("Agregar Reserva");
        ImageView iconoNueva = new ImageView(new Image("/resources/agregar.png"));
        iconoNueva.setFitWidth(16);
        iconoNueva.setFitHeight(16);
        btnNueva.setGraphic(iconoNueva);

        Button btnCancelar = new Button("Cancelar Reserva");
        ImageView iconoCancelar = new ImageView(new Image("/resources/cancelar.png"));
        iconoCancelar.setFitWidth(16);
        iconoCancelar.setFitHeight(16);
        btnCancelar.setGraphic(iconoCancelar);

        Button btnActualizar = new Button("Actualizar Lista");
        ImageView iconoActualizar = new ImageView(new Image("/resources/actualizar.png"));
        iconoActualizar.setFitWidth(16);
        iconoActualizar.setFitHeight(16);
        btnActualizar.setGraphic(iconoActualizar);

        aplicarEfectoHover(btnNueva, "#52c98f", "#42b87f");
        aplicarEfectoHover(btnCancelar, "#ec7063", "#e55e53");
        aplicarEfectoHover(btnActualizar, "#5dade2", "#4a9dd5");

        btnNueva.setOnAction(_ -> mostrarDialogoNuevaReserva());
        btnCancelar.setOnAction(_ -> cancelarReservaSeleccionada());
        btnActualizar.setOnAction(_ -> cargarReservas());

        panel.getChildren().addAll(btnNueva, btnCancelar, btnActualizar);
        return panel;
    }

    private TableView<Reserva> crearTablaReservas() {
        TableView<Reserva> tabla = new TableView<>();
        tabla.setItems(reservasObservable);
        tabla.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
        TableColumn<Reserva, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getIdReserva()));
        colId.setPrefWidth(100);

        TableColumn<Reserva, String> colUsuario = new TableColumn<>("Usuario");
        colUsuario.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getUsuario().getNombre()));
        colUsuario.setPrefWidth(200);

        TableColumn<Reserva, String> colLibro = new TableColumn<>("Libro");
        colLibro.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLibro().getTitulo()));
        colLibro.setPrefWidth(300);

        TableColumn<Reserva, String> colFecha = new TableColumn<>("Fecha Reserva");
        colFecha.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getFechaReserva().format(dateFormatter)));
        colFecha.setPrefWidth(150);

        TableColumn<Reserva, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEstado().toString()));
        colEstado.setPrefWidth(120);

        TableColumn<Reserva, String> colDisponible = new TableColumn<>("Disponible el");
        colDisponible.setCellValueFactory(
                cellData -> new SimpleStringProperty(calcularFechaDisponibilidad(cellData.getValue())));
        colDisponible.setPrefWidth(170);

        @SuppressWarnings("unchecked")
        final TableColumn<Reserva, String>[] columnas = new TableColumn[] {
                colId, colUsuario, colLibro, colFecha, colEstado, colDisponible
        };
        tabla.getColumns().addAll(columnas);

        tabla.setRowFactory(_ -> {
        TableRow<Reserva> row = new TableRow<>();
        row.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            final int index = row.getIndex();
            
            // Si es una fila vacía, deseleccionar
            if (row.isEmpty()) {
                tabla.getSelectionModel().clearSelection();
                event.consume();
            } 
            // Si es una fila ya seleccionada, deseleccionar
            else if (index >= 0 && index < tabla.getItems().size() 
                    && tabla.getSelectionModel().isSelected(index)) {
                tabla.getSelectionModel().clearSelection();
                event.consume();
            }
        });
        return row;
    });

        Label placeholder = new Label("🔖 No hay reservas registradas");
        placeholder.setFont(Font.font("Segoe UI", 16));
        placeholder.setTextFill(Color.web("#95a5a6"));
        tabla.setPlaceholder(placeholder);

        return tabla;
    }

    private String calcularFechaDisponibilidad(Reserva reserva) {

        if (reserva.getEstado() == Reserva.EstadoReserva.CANCELADA) {
            return "Cancelada";
        }

        Libro libro = reserva.getLibro();

        if (libro.isDisponible()) {
            return "Disponible ahora";
        }

        try {
            var gestorPrestamos = BibliotecaApp.getGestorPrestamos();
            if (gestorPrestamos == null) {
                return "Desconocido";
            }

            List<Prestamo> activos = gestorPrestamos.obtenerTodosPrestamosActivos();
            for (Prestamo p : activos) {
                if (p.getLibro().getId().equals(libro.getId()) && p.getEstado() == Prestamo.EstadoPrestamo.ACTIVO) {
                    LocalDate fecha = p.getFechaDevolucionEsperada();
                    int posicion = obtenerPosicionEnCola(reserva);
                    if (posicion <= 1) {
                        return fecha.format(dateFormatter);
                    } else {
                        final int DIAS_ESTIMADO_POR_PRESTAMO = 14;
                        LocalDate estimada = fecha.plusDays((long) (posicion - 1) * DIAS_ESTIMADO_POR_PRESTAMO);
                        return estimada.format(dateFormatter) + " (estimado, pos " + posicion + ")";
                    }
                }
            }
        } catch (Exception e) {
            return "Desconocido";
        }

        return "Desconocido";
    }

    private int obtenerPosicionEnCola(Reserva reserva) {
        try {
            List<Reserva> todas = gestorReservas.obtenerTodasReservas();
            int posicion = 0;
            for (Reserva r : todas) {
                if (r.getLibro().getId().equals(reserva.getLibro().getId()) &&
                        r.getEstado() == Reserva.EstadoReserva.PENDIENTE) {
                    if (r.getFechaReserva().isBefore(reserva.getFechaReserva()) ||
                            r.getIdReserva().equals(reserva.getIdReserva())) {
                        posicion++;
                    }
                }
            }
            return posicion == 0 ? 1 : posicion;
        } catch (Exception e) {
            return 1;
        }
    }

    private void mostrarDialogoNuevaReserva() {
        Dialog<Reserva> dialog = new Dialog<>();
        dialog.setTitle("Nueva Reserva");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(550);
        dialog.getDialogPane().setPrefHeight(420);

        gui.BibliotecaApp.aplicarIconoADialog(dialog);

        ButtonType btnAceptar = new ButtonType("Reservar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 12;");
        Label lblTituloDialogo = new Label();
        ImageView iconoReserva = new ImageView(new Image(getClass().getResourceAsStream("/resources/libro.png")));
        iconoReserva.setFitWidth(24);
        iconoReserva.setFitHeight(24);
        lblTituloDialogo.setGraphic(iconoReserva);
        lblTituloDialogo.setText("Nueva Reserva");
        lblTituloDialogo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblTituloDialogo.setTextFill(Color.web("#2c3e50"));

        Label subtitulo = new Label("Reservar un libro para cuando esté disponible");
        subtitulo.setFont(Font.font("Segoe UI", 14));
        subtitulo.setTextFill(Color.web("#7f8c8d"));

        Region separador = new Region();
        separador.setPrefHeight(10);
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(20));
        grid.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);");
        Label lblUsuario = new Label("Usuario:");
        lblUsuario.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblUsuario.setTextFill(Color.web("#2c3e50"));

        Label lblLibro = new Label("Libro:");
        lblLibro.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblLibro.setTextFill(Color.web("#2c3e50"));

        ComboBox<Usuario> comboUsuario = new ComboBox<>();
        try {
            List<Usuario> usuarios = gestorUsuarios.obtenerTodosUsuarios();
            if (usuarios != null && !usuarios.isEmpty()) {
                comboUsuario.getItems().addAll(usuarios);
            } else {
                mostrarAlerta("Advertencia", "No hay usuarios registrados", Alert.AlertType.WARNING);
                return;
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "Error al cargar usuarios: " + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }
        comboUsuario.setPromptText("Seleccione usuario");
        comboUsuario.setPrefWidth(300);
        comboUsuario.setPrefHeight(42);
        aplicarEstiloComboBox(comboUsuario);

        ComboBox<Libro> comboLibro = new ComboBox<>();
        try {
            List<Libro> libros = gestorLibros.obtenerTodosLibros();
            if (libros != null && !libros.isEmpty()) {
                comboLibro.getItems().addAll(libros);
            } else {
                mostrarAlerta("Advertencia", "No hay libros registrados", Alert.AlertType.WARNING);
                return;
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "Error al cargar libros: " + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }
        comboLibro.setPromptText("Seleccione libro");
        comboLibro.setPrefWidth(300);
        comboLibro.setPrefHeight(42);
        aplicarEstiloComboBox(comboLibro);

        grid.add(lblUsuario, 0, 0);
        grid.add(comboUsuario, 1, 0);
        grid.add(lblLibro, 0, 1);
        grid.add(comboLibro, 1, 1);

        container.getChildren().addAll(lblTituloDialogo, subtitulo, separador, grid);
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().lookupButton(btnAceptar).setStyle(
                "-fx-background-color: #9b59b6; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 24; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;");

        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
                "-fx-background-color: #e0e0e0; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 24; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;");
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAceptar) {
                try {
                    Usuario usuario = comboUsuario.getValue();
                    Libro libro = comboLibro.getValue();

                    if (usuario == null) {
                        mostrarAlerta("Error", "Debe seleccionar un usuario", Alert.AlertType.ERROR);
                        return null;
                    }
                    if (libro == null) {
                        mostrarAlerta("Error", "Debe seleccionar un libro", Alert.AlertType.ERROR);
                        return null;
                    }

                    return gestorReservas.agregarReserva(usuario, libro);
                } catch (Exception e) {
                    mostrarAlerta("Error", e.getMessage(), Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(reserva -> {
            if (reserva != null) {
                cargarReservas();
                mostrarAlerta("Exito", "Reserva registrada correctamente\nID: " + reserva.getIdReserva(),
                        Alert.AlertType.INFORMATION);
            }
        });
    }

    private void aplicarEstiloComboBox(ComboBox<?> comboBox) {
        comboBox.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-background-color: white; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;");
    }

    private void cancelarReservaSeleccionada() {
        Reserva reservaSeleccionada = tablaReservas.getSelectionModel().getSelectedItem();

        if (reservaSeleccionada == null) {
            mostrarAlerta("Advertencia", "Seleccione una reserva para cancelar", Alert.AlertType.WARNING);
            return;
        }
        if (reservaSeleccionada.getEstado() == Reserva.EstadoReserva.CANCELADA) {
            mostrarAlerta("Operación No Permitida",
                    "Esta reserva ya fue cancelada previamente.\n\n" +
                            "📋 ID: " + reservaSeleccionada.getIdReserva() + "\n" +
                            "📚 Libro: " + reservaSeleccionada.getLibro().getTitulo() + "\n" +
                            "⚠️ Estado: CANCELADA\n\n" +
                            "No es posible cancelar una reserva que ya está cancelada.",
                    Alert.AlertType.WARNING);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Cancelación");
        confirmacion.setHeaderText(null);

        gui.BibliotecaApp.aplicarIconoAAlert(confirmacion);
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(30));
        contenido.setAlignment(Pos.CENTER_LEFT);
        contenido.setStyle(
                "-fx-background-color: #fff5f5; " +
                        "-fx-background-radius: 12;");

        Label lblTitulo = new Label("❌ ¿Cancelar reserva?");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#e74c3c"));

        Label lblMensaje = new Label("La reserva será marcada como cancelada.");
        lblMensaje.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblMensaje.setTextFill(Color.web("#c0392b"));
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(16));
        infoBox.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #e74c3c; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8;");

        Label lblInfo = new Label("Información de la reserva:");
        lblInfo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblInfo.setTextFill(Color.web("#2c3e50"));

        Label lblId = new Label("🆔 ID: " + reservaSeleccionada.getIdReserva());
        lblId.setFont(Font.font("Segoe UI", 13));
        lblId.setTextFill(Color.web("#7f8c8d"));

        Label lblLibro = new Label("📚 Libro: " + reservaSeleccionada.getLibro().getTitulo());
        lblLibro.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblLibro.setTextFill(Color.web("#2c3e50"));
        lblLibro.setWrapText(true);

        Label lblUsuario = new Label("👤 Usuario: " + reservaSeleccionada.getUsuario().getNombre());
        lblUsuario.setFont(Font.font("Segoe UI", 13));
        lblUsuario.setTextFill(Color.web("#7f8c8d"));

        Label lblFechaReserva = new Label("📅 Fecha reserva: " +
                reservaSeleccionada.getFechaReserva()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblFechaReserva.setFont(Font.font("Segoe UI", 13));
        lblFechaReserva.setTextFill(Color.web("#7f8c8d"));

        Label lblEstado = new Label("📊 Estado actual: " + reservaSeleccionada.getEstado());
        lblEstado.setFont(Font.font("Segoe UI", 13));
        lblEstado.setTextFill(Color.web("#7f8c8d"));

        infoBox.getChildren().addAll(lblInfo, lblId, lblLibro, lblUsuario, lblFechaReserva, lblEstado);

        contenido.getChildren().addAll(lblTitulo, lblMensaje, infoBox);
        confirmacion.getDialogPane().setContent(contenido);
        confirmacion.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: #e74c3c; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 24; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;");

        confirmacion.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
                "-fx-background-color: #95a5a6; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 24; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;");
        ((Button) confirmacion.getDialogPane().lookupButton(ButtonType.OK)).setText("Cancelar Reserva");
        ((Button) confirmacion.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Volver");
        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    gestorReservas.cancelarReserva(reservaSeleccionada.getIdReserva());
                    cargarReservas();
                    mostrarAlerta("Éxito", "Reserva cancelada correctamente", Alert.AlertType.INFORMATION);
                } catch (excepciones.OperacionInvalidaException e) {
                    mostrarAlerta("Operación No Permitida", e.getMessage(), Alert.AlertType.WARNING);
                } catch (ElementoNoEncontradoException e) {
                    mostrarAlerta("Error", "No se pudo cancelar la reserva: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void cargarReservas() {
        reservasObservable.clear();
        List<Reserva> reservas = gestorReservas.obtenerTodasReservas();
        if (reservas != null && !reservas.isEmpty()) {
            reservasObservable.addAll(reservas);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);

        gui.BibliotecaApp.aplicarIconoAAlert(alert);
        alert.showAndWait();
    }

    private void aplicarEfectoHover(Button boton, String colorNormal, String colorHover) {

        javafx.scene.effect.DropShadow sombra = new javafx.scene.effect.DropShadow();
        sombra.setRadius(5.0);
        sombra.setOffsetX(0.0);
        sombra.setOffsetY(3.0);
        sombra.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.2));

        String estiloBase = String.format(
                "-fx-background-color: %s; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 12 24; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;",
                colorNormal);

        String estiloHover = String.format(
                "-fx-background-color: %s; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 12 24; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-scale-x: 1.02; " +
                        "-fx-scale-y: 1.02;",
                colorHover);

        boton.setStyle(estiloBase);
        boton.setEffect(sombra);

        boton.setOnMouseEntered(_ -> {
            boton.setStyle(estiloHover);
            javafx.scene.effect.DropShadow sombraHover = new javafx.scene.effect.DropShadow();
            sombraHover.setRadius(8.0);
            sombraHover.setOffsetX(0.0);
            sombraHover.setOffsetY(4.0);
            sombraHover.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.3));
            boton.setEffect(sombraHover);
        });

        boton.setOnMouseExited(_ -> {
            boton.setStyle(estiloBase);
            boton.setEffect(sombra);
        });
    }

}
