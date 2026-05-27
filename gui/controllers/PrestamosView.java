package gui.controllers;

import controlador.GestorPrestamos;
import controlador.GestorLibros;
import controlador.GestorUsuarios;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import modelo.Libro;
import javafx.scene.control.TableRow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import modelo.Prestamo;
import modelo.Usuario;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PrestamosView extends VBox {
    private GestorPrestamos gestorPrestamos;
    @SuppressWarnings("unused")
    private GestorLibros gestorLibros;
    @SuppressWarnings("unused")
    private GestorUsuarios gestorUsuarios;
    private TableView<Prestamo> tablaPrestamos;
    private ObservableList<Prestamo> prestamosObservable;
    private FilteredList<Prestamo> prestamosFiltrados;
    private TextField txtBuscar;
    private ComboBox<String> comboFiltro;
    private DateTimeFormatter dateFormatter;
    private static final double TARIFA_MULTA_DIA = 2.0;

    public PrestamosView(GestorPrestamos gestorPrestamos, GestorLibros gestorLibros, GestorUsuarios gestorUsuarios) {
        this.gestorPrestamos = gestorPrestamos;
        this.gestorLibros = gestorLibros;
        this.gestorUsuarios = gestorUsuarios;
        this.prestamosObservable = FXCollections.observableArrayList();
        this.dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        System.out.println("[DEBUG PrestamosView] Inicializando vista...");
        System.out.println("[DEBUG PrestamosView] GestorPrestamos hashCode: " + gestorPrestamos.hashCode());

        inicializar();
        cargarPrestamos();
    }

    private void inicializar() {
        this.setPadding(new Insets(24));
        this.setSpacing(24);
        this.setStyle("-fx-background-color: #f5f5f5;");
        HBox header = crearHeader();
        HBox toolBar = crearToolBar();
        VBox tablaContainer = crearTabla();
        this.getChildren().addAll(header, toolBar, tablaContainer);
    }

    private HBox crearHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label("📖 Gestión de Préstamos");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        titulo.setTextFill(Color.web("#2c3e50"));
        header.getChildren().add(titulo);
        return header;
    }

    private HBox crearToolBar() {
        HBox toolBar = new HBox(16);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setPadding(new Insets(16));
        toolBar.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");

        txtBuscar = new TextField();
        txtBuscar.setPromptText("🔍 Buscar por usuario o libro...");
        txtBuscar.setPrefWidth(300);
        txtBuscar.setPrefHeight(44);
        txtBuscar.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-padding: 10; " +
                        "-fx-border-color: #ddd; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;");
        txtBuscar.textProperty().addListener((_, _, _) -> filtrarPrestamos());

        comboFiltro = new ComboBox<>();
        comboFiltro.getItems().addAll("Todos", "Activos", "Devueltos", "Vencidos", "Cancelados");
        comboFiltro.setValue("Activos");
        comboFiltro.setPrefWidth(150);
        comboFiltro.setPrefHeight(44);
        comboFiltro.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-padding: 12 16; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;");
        comboFiltro.setOnAction(_ -> filtrarPrestamos());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNuevo = new Button("Nuevo Préstamo");
        ImageView iconoNuevo = new ImageView(new Image("/resources/agregar.png"));
        iconoNuevo.setFitWidth(16);
        iconoNuevo.setFitHeight(16);
        btnNuevo.setGraphic(iconoNuevo);

        Button btnDevolver = new Button("Devolver");
        ImageView iconoDevolver = new ImageView(new Image("/resources/devolver.png"));
        iconoDevolver.setFitWidth(16);
        iconoDevolver.setFitHeight(16);
        btnDevolver.setGraphic(iconoDevolver);

        Button btnEditar = new Button("Editar Préstamo");
        ImageView iconoEditar = new ImageView(new Image("/resources/editar.png"));
        iconoEditar.setFitWidth(16);
        iconoEditar.setFitHeight(16);
        btnEditar.setGraphic(iconoEditar);

        Button btnCancelar = new Button("Cancelar Préstamo");
        ImageView iconoCancelar = new ImageView(new Image("/resources/cancelar.png"));
        iconoCancelar.setFitWidth(16);
        iconoCancelar.setFitHeight(16);
        btnCancelar.setGraphic(iconoCancelar);

        aplicarEfectoHover(btnNuevo, "#52c98f", "#42b87f");
        aplicarEfectoHover(btnDevolver, "#5dade2", "#4a9dd5");
        aplicarEfectoHover(btnEditar, "#f8b739", "#e8a729");
        aplicarEfectoHover(btnCancelar, "#ec7063", "#e55e53");

        btnNuevo.setOnAction(_ -> mostrarDialogoNuevoPrestamo());
        btnDevolver.setOnAction(_ -> devolverPrestamoSeleccionado());
        btnEditar.setOnAction(_ -> editarPrestamoSeleccionado());
        btnCancelar.setOnAction(_ -> cancelarPrestamoSeleccionado());

        toolBar.getChildren().addAll(txtBuscar, comboFiltro, spacer, btnNuevo, btnDevolver, btnEditar, btnCancelar);
        return toolBar;
    }

    private VBox crearTabla() {
        VBox container = new VBox();
        container.setPadding(new Insets(20));
        container.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");
        VBox.setVgrow(container, Priority.ALWAYS);

        tablaPrestamos = new TableView<>();
        tablaPrestamos.setStyle("-fx-font-size: 13px;");

        TableColumn<Prestamo, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("idPrestamo"));
        colId.setPrefWidth(100);

        TableColumn<Prestamo, String> colUsuario = new TableColumn<>("Usuario");
        colUsuario.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getUsuario().getNombre()));
        colUsuario.setPrefWidth(180);

        TableColumn<Prestamo, String> colLibro = new TableColumn<>("Libro");
        colLibro.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLibro().getTitulo()));
        colLibro.setPrefWidth(220);

        TableColumn<Prestamo, String> colFechaPrestamo = new TableColumn<>("Fecha Préstamo");
        colFechaPrestamo.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getFechaPrestamo().format(dateFormatter)));
        colFechaPrestamo.setPrefWidth(120);

        TableColumn<Prestamo, String> colFechaDevolucion = new TableColumn<>("Fecha Devolución");
        colFechaDevolucion.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getFechaDevolucionEsperada().format(dateFormatter)));
        colFechaDevolucion.setPrefWidth(130);
        TableColumn<Prestamo, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cellData -> {
            Prestamo p = cellData.getValue();
            if (p.getEstado() == Prestamo.EstadoPrestamo.DEVUELTO) {
                return new SimpleStringProperty("✅ Devuelto");
            } else if (p.getEstado() == Prestamo.EstadoPrestamo.CANCELADO) {
                return new SimpleStringProperty("❌ Cancelado");
            } else if (p.estaVencido()) {
                return new SimpleStringProperty("⚠️ Vencido");
            } else {
                return new SimpleStringProperty("📖 Activo");
            }
        });
        colEstado.setPrefWidth(100);

        TableColumn<Prestamo, String> colMulta = new TableColumn<>("Multa");
        colMulta.setCellValueFactory(cellData -> {
            double multa = cellData.getValue().calcularMulta(TARIFA_MULTA_DIA);
            return new SimpleStringProperty(String.format("$%.2f", multa));
        });
        colMulta.setPrefWidth(80);

        tablaPrestamos.getColumns().add(colId);
        tablaPrestamos.getColumns().add(colUsuario);
        tablaPrestamos.getColumns().add(colLibro);
        tablaPrestamos.getColumns().add(colFechaPrestamo);
        tablaPrestamos.getColumns().add(colFechaDevolucion);
        tablaPrestamos.getColumns().add(colEstado);
        tablaPrestamos.getColumns().add(colMulta);

        tablaPrestamos.setRowFactory(_ -> {
            TableRow<Prestamo> row = new TableRow<>();
            row.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                final int index = row.getIndex();

                // Si es una fila vacía, deseleccionar
                if (row.isEmpty()) {
                    tablaPrestamos.getSelectionModel().clearSelection();
                    event.consume();
                }
                // Si es una fila ya seleccionada, deseleccionar
                else if (index >= 0 && index < tablaPrestamos.getItems().size()
                        && tablaPrestamos.getSelectionModel().isSelected(index)) {
                    tablaPrestamos.getSelectionModel().clearSelection();
                    event.consume();
                }
            });
            return row;
        });

        prestamosFiltrados = new FilteredList<>(prestamosObservable, _ -> true);
        tablaPrestamos.setItems(prestamosFiltrados);
        VBox.setVgrow(tablaPrestamos, Priority.ALWAYS);
        container.getChildren().add(tablaPrestamos);

        return container;
    }

    private void cargarPrestamos() {
        System.out.println("\n[DEBUG] === INICIANDO cargarPrestamos() ===");
        System.out.println("[DEBUG] GestorPrestamos hashCode: " + gestorPrestamos.hashCode());

        prestamosObservable.clear();
        List<Prestamo> todosPrestamos = gestorPrestamos.obtenerTodosPrestamos();

        System.out.println("[DEBUG] Total préstamos obtenidos: " + todosPrestamos.size());

        for (Prestamo p : todosPrestamos) {
            System.out.println("[DEBUG] - " + p.getIdPrestamo() +
                    " | " + p.getLibro().getTitulo() +
                    " | Estado: " + p.getEstado() +
                    " | Usuario: " + p.getUsuario().getNombre());
        }

        prestamosObservable.addAll(todosPrestamos);

        System.out.println("[DEBUG] Préstamos agregados a ObservableList: " + prestamosObservable.size());
        System.out.println("[DEBUG] Filtro actual: " + comboFiltro.getValue());
        filtrarPrestamos();

        System.out.println("[DEBUG] Préstamos visibles después del filtro: " + prestamosFiltrados.size());
        System.out.println("[DEBUG] === FIN cargarPrestamos() ===\n");
    }

    private void filtrarPrestamos() {
        String textoBusqueda = txtBuscar.getText();
        String filtroEstado = comboFiltro.getValue();

        System.out.println("\n[DEBUG FILTRO] Aplicando filtro...");
        System.out.println("[DEBUG FILTRO] - Texto búsqueda: '" + textoBusqueda + "'");
        System.out.println("[DEBUG FILTRO] - Filtro estado: '" + filtroEstado + "'");
        System.out.println("[DEBUG FILTRO] - Total en observable: " + prestamosObservable.size());

        prestamosFiltrados.setPredicate(prestamo -> {

            boolean coincideTexto = true;
            if (textoBusqueda != null && !textoBusqueda.isEmpty()) {
                String lowerCaseFilter = textoBusqueda.toLowerCase();
                coincideTexto = prestamo.getUsuario().getNombre().toLowerCase().contains(lowerCaseFilter) ||
                        prestamo.getLibro().getTitulo().toLowerCase().contains(lowerCaseFilter);
            }

            boolean coincideEstado = true;
            if (filtroEstado != null && !filtroEstado.equals("Todos")) {
                switch (filtroEstado) {
                    case "Activos":
                        coincideEstado = (prestamo.getEstado() != null &&
                                prestamo.getEstado() == Prestamo.EstadoPrestamo.ACTIVO);
                        break;
                    case "Devueltos":
                        coincideEstado = (prestamo.getEstado() == Prestamo.EstadoPrestamo.DEVUELTO);
                        break;
                    case "Vencidos":
                        coincideEstado = prestamo.getEstado() != Prestamo.EstadoPrestamo.DEVUELTO
                                && prestamo.estaVencido();
                        break;
                    case "Cancelados":
                        coincideEstado = (prestamo.getEstado() == Prestamo.EstadoPrestamo.CANCELADO);
                        break;
                }
            }

            boolean resultado = coincideTexto && coincideEstado;
            return resultado;
        });

        System.out.println("[DEBUG FILTRO] Resultado: " + prestamosFiltrados.size() + " préstamos visibles\n");
    }

    private void mostrarDialogoNuevoPrestamo() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Registrar Nuevo Préstamo");
        dialog.setHeaderText(null);

        dialog.getDialogPane().setPrefWidth(550);
        dialog.getDialogPane().setPrefHeight(480);

        gui.BibliotecaApp.aplicarIconoADialog(dialog);

        ButtonType btnRegistrar = new ButtonType("Registrar Préstamo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnRegistrar, ButtonType.CANCEL);

        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 12;");

        Label lblTituloDialogo = new Label();
        ImageView iconoNuevo = new ImageView(new Image(getClass().getResourceAsStream("/resources/libro.png")));
        iconoNuevo.setFitWidth(24);
        iconoNuevo.setFitHeight(24);
        lblTituloDialogo.setGraphic(iconoNuevo);
        lblTituloDialogo.setText("Nuevo Préstamo");
        lblTituloDialogo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblTituloDialogo.setTextFill(Color.web("#2c3e50"));

        Label subtitulo = new Label("Complete la información del préstamo a registrar");
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

        Label lblDias = new Label("Días de Préstamo:");
        lblDias.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblDias.setTextFill(Color.web("#2c3e50"));
        ComboBox<Usuario> cmbUsuarios = new ComboBox<>();
        cmbUsuarios.setItems(FXCollections.observableArrayList(gestorUsuarios.obtenerTodosUsuarios()));
        cmbUsuarios.setPromptText("Seleccione un usuario");
        cmbUsuarios.setPrefHeight(42);
        cmbUsuarios.setMaxWidth(Double.MAX_VALUE);
        aplicarEstiloComboBox(cmbUsuarios);

        cmbUsuarios.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Usuario usuario, boolean empty) {
                super.updateItem(usuario, empty);
                if (empty || usuario == null) {
                    setText(null);
                } else {
                    setText(usuario.getNombre() + " (" + usuario.getId() + ")");
                }
            }
        });
        cmbUsuarios.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Usuario usuario, boolean empty) {
                super.updateItem(usuario, empty);
                if (empty || usuario == null) {
                    setText("Seleccione un usuario");
                } else {
                    setText(usuario.getNombre() + " (" + usuario.getId() + ")");
                }
            }
        });
        ComboBox<Libro> cmbLibros = new ComboBox<>();
        List<Libro> librosDisponibles = gestorLibros.obtenerTodosLibros().stream()
                .filter(Libro::isDisponible)
                .collect(java.util.stream.Collectors.toList());
        cmbLibros.setItems(FXCollections.observableArrayList(librosDisponibles));
        cmbLibros.setPromptText("Seleccione un libro");
        cmbLibros.setPrefHeight(42);
        cmbLibros.setMaxWidth(Double.MAX_VALUE);
        aplicarEstiloComboBox(cmbLibros);

        cmbLibros.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Libro libro, boolean empty) {
                super.updateItem(libro, empty);
                if (empty || libro == null) {
                    setText(null);
                } else {
                    setText(libro.getTitulo() + " - " + libro.getAutor());
                }
            }
        });
        cmbLibros.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Libro libro, boolean empty) {
                super.updateItem(libro, empty);
                if (empty || libro == null) {
                    setText("Seleccione un libro");
                } else {
                    setText(libro.getTitulo() + " - " + libro.getAutor());
                }
            }
        });

        TextField txtDias = new TextField();
        txtDias.setPromptText("Ej: 14");
        txtDias.setText("14");
        aplicarEstiloTextField(txtDias);

        grid.add(lblUsuario, 0, 0);
        grid.add(cmbUsuarios, 1, 0);
        grid.add(lblLibro, 0, 1);
        grid.add(cmbLibros, 1, 1);
        grid.add(lblDias, 0, 2);
        grid.add(txtDias, 1, 2);

        container.getChildren().addAll(lblTituloDialogo, subtitulo, separador, grid);
        dialog.getDialogPane().setContent(container);

        dialog.getDialogPane().lookupButton(btnRegistrar).setStyle(
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

        dialog.showAndWait().ifPresent(response -> {
            if (response == btnRegistrar) {
                Usuario usuarioSeleccionado = cmbUsuarios.getValue();
                Libro libroSeleccionado = cmbLibros.getValue();
                String dias = txtDias.getText().trim();

                if (usuarioSeleccionado == null || libroSeleccionado == null || dias.isEmpty()) {
                    mostrarAlerta("Error", "Todos los campos son obligatorios", Alert.AlertType.ERROR);
                    return;
                }

                try {
                    int diasPrestamo = Integer.parseInt(dias);
                    if (diasPrestamo <= 0) {
                        mostrarAlerta("Error", "Los días deben ser mayores a 0", Alert.AlertType.ERROR);
                        return;
                    }
                    gestorPrestamos.registrarPrestamo(usuarioSeleccionado.getId(), libroSeleccionado.getId(),
                            diasPrestamo);
                    cargarPrestamos();
                    mostrarAlerta("Éxito", "Préstamo registrado correctamente", Alert.AlertType.INFORMATION);
                } catch (NumberFormatException e) {
                    mostrarAlerta("Error", "Los días deben ser un número válido", Alert.AlertType.ERROR);
                } catch (Exception e) {
                    mostrarAlerta("Error", "No se pudo registrar el préstamo: " + e.getMessage(),
                            Alert.AlertType.ERROR);
                }
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

    private void aplicarEstiloTextField(TextField field) {
        field.setPrefHeight(42);
        field.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-padding: 10 14; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-background-color: white;");

        field.focusedProperty().addListener((_, _, isNowFocused) -> {
            if (isNowFocused) {
                field.setStyle(
                        "-fx-font-size: 14px; " +
                                "-fx-font-family: 'Segoe UI'; " +
                                "-fx-padding: 10 14; " +
                                "-fx-border-color: #5dade2; " +
                                "-fx-border-width: 2; " +
                                "-fx-border-radius: 8; " +
                                "-fx-background-radius: 8; " +
                                "-fx-background-color: white;");
            } else {
                field.setStyle(
                        "-fx-font-size: 14px; " +
                                "-fx-font-family: 'Segoe UI'; " +
                                "-fx-padding: 10 14; " +
                                "-fx-border-color: #dee2e6; " +
                                "-fx-border-width: 1.5; " +
                                "-fx-border-radius: 8; " +
                                "-fx-background-radius: 8; " +
                                "-fx-background-color: white;");
            }
        });
    }

    private void devolverPrestamoSeleccionado() {
        Prestamo prestamoSeleccionado = tablaPrestamos.getSelectionModel().getSelectedItem();

        if (prestamoSeleccionado == null) {
            mostrarAlerta("Advertencia", "Seleccione un préstamo para devolver", Alert.AlertType.WARNING);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Devolución");
        confirmacion.setHeaderText(null);

        gui.BibliotecaApp.aplicarIconoAAlert(confirmacion);
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(30));
        contenido.setAlignment(Pos.CENTER_LEFT);
        contenido.setStyle(
                "-fx-background-color: #f0f9ff; " +
                        "-fx-background-radius: 12;");

        Label lblTitulo = new Label();
        ImageView iconoDevolver = new ImageView(new Image(getClass().getResourceAsStream("/resources/devolucion.png")));
        iconoDevolver.setFitWidth(24);
        iconoDevolver.setFitHeight(24);
        lblTitulo.setGraphic(iconoDevolver);
        lblTitulo.setText("¿Confirmar devolución?");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#2980b9"));

        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(16));
        infoBox.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #3498db; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8;");

        Label lblInfo = new Label("Información del préstamo:");
        lblInfo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblInfo.setTextFill(Color.web("#2c3e50"));

        Label lblLibro = new Label("📚 Libro: " + prestamoSeleccionado.getLibro().getTitulo());
        lblLibro.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblLibro.setTextFill(Color.web("#2c3e50"));
        lblLibro.setWrapText(true);

        Label lblUsuario = new Label("👤 Usuario: " + prestamoSeleccionado.getUsuario().getNombre());
        lblUsuario.setFont(Font.font("Segoe UI", 13));
        lblUsuario.setTextFill(Color.web("#7f8c8d"));

        Label lblFechaPrestamo = new Label("📅 Fecha préstamo: " +
                prestamoSeleccionado.getFechaPrestamo()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblFechaPrestamo.setFont(Font.font("Segoe UI", 13));
        lblFechaPrestamo.setTextFill(Color.web("#7f8c8d"));
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate fechaEsperada = prestamoSeleccionado.getFechaDevolucionEsperada();
        long diasRetraso = java.time.temporal.ChronoUnit.DAYS.between(fechaEsperada, hoy);

        if (diasRetraso > 0) {
            double multa = diasRetraso * 2.0;
            Label lblMulta = new Label(
                    String.format("⚠️ Multa a aplicar: $%.2f (%d días de retraso)", multa, diasRetraso));
            lblMulta.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            lblMulta.setTextFill(Color.web("#e74c3c"));
            infoBox.getChildren().add(lblMulta);
        } else {
            Label lblSinMulta = new Label("✅ Sin multa - Devolución a tiempo");
            lblSinMulta.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            lblSinMulta.setTextFill(Color.web("#27ae60"));
            infoBox.getChildren().add(lblSinMulta);
        }

        infoBox.getChildren().addAll(lblInfo, lblLibro, lblUsuario, lblFechaPrestamo);

        contenido.getChildren().addAll(lblTitulo, infoBox);
        confirmacion.getDialogPane().setContent(contenido);
        confirmacion.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: #3498db; " +
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

        ((Button) confirmacion.getDialogPane().lookupButton(ButtonType.OK)).setText("Devolver");
        ((Button) confirmacion.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    gestorPrestamos.registrarDevolucion(prestamoSeleccionado.getIdPrestamo());
                    cargarPrestamos();
                    mostrarAlerta("Éxito", "Préstamo devuelto correctamente", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    mostrarAlerta("Error", "No se pudo devolver el préstamo: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void editarPrestamoSeleccionado() {
        Prestamo prestamoSeleccionado = tablaPrestamos.getSelectionModel().getSelectedItem();

        if (prestamoSeleccionado == null) {
            mostrarAlerta("Advertencia", "Seleccione un préstamo para editar", Alert.AlertType.WARNING);
            return;
        }

        if (prestamoSeleccionado.getEstado() != Prestamo.EstadoPrestamo.ACTIVO) {
            mostrarAlerta("Advertencia",
                    "Solo se pueden editar préstamos ACTIVOS.\nEste préstamo tiene estado: " +
                            prestamoSeleccionado.getEstado(),
                    Alert.AlertType.WARNING);
            return;
        }

        mostrarDialogoEditarPrestamo(prestamoSeleccionado);
    }

    private void mostrarDialogoEditarPrestamo(Prestamo prestamo) {
        Dialog<Prestamo> dialog = new Dialog<>();
        dialog.setTitle("Editar Préstamo");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(550);
        dialog.getDialogPane().setPrefHeight(520);

        gui.BibliotecaApp.aplicarIconoADialog(dialog);

        ButtonType btnGuardar = new ButtonType("Guardar Cambios", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 12;");
        Label lblTituloDialogo = new Label();
        ImageView iconoEditar = new ImageView(new Image(getClass().getResourceAsStream("/resources/editar.png")));
        iconoEditar.setFitWidth(24);
        iconoEditar.setFitHeight(24);
        lblTituloDialogo.setGraphic(iconoEditar);
        lblTituloDialogo.setText("Editar Préstamo");
        lblTituloDialogo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblTituloDialogo.setTextFill(Color.web("#2c3e50"));

        Label subtitulo = new Label("ID: " + prestamo.getIdPrestamo());
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
        Label lblUsuarioLabel = new Label("Usuario:");
        lblUsuarioLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblUsuarioLabel.setTextFill(Color.web("#2c3e50"));

        Label lblLibroLabel = new Label("Libro:");
        lblLibroLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblLibroLabel.setTextFill(Color.web("#2c3e50"));

        Label lblFechaPrestamoLabel = new Label("Fecha Préstamo:");
        lblFechaPrestamoLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblFechaPrestamoLabel.setTextFill(Color.web("#2c3e50"));

        Label lblFechaDevolucionLabel = new Label("Fecha Devolución:");
        lblFechaDevolucionLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblFechaDevolucionLabel.setTextFill(Color.web("#2c3e50"));
        Label lblUsuario = new Label("👤 " + prestamo.getUsuario().getNombre());
        lblUsuario.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblUsuario.setTextFill(Color.web("#2c3e50"));
        lblUsuario.setStyle(
                "-fx-background-color: #f0f0f0; " +
                        "-fx-padding: 10 14; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 8;");
        lblUsuario.setPrefHeight(42);
        lblUsuario.setMaxWidth(Double.MAX_VALUE);

        Label lblLibro = new Label("📚 " + prestamo.getLibro().getTitulo());
        lblLibro.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblLibro.setTextFill(Color.web("#2c3e50"));
        lblLibro.setWrapText(true);
        lblLibro.setStyle(
                "-fx-background-color: #f0f0f0; " +
                        "-fx-padding: 10 14; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 8;");
        lblLibro.setPrefHeight(42);
        lblLibro.setMaxWidth(Double.MAX_VALUE);

        Label lblFechaPrestamo = new Label("📅 " + prestamo.getFechaPrestamo().format(dateFormatter));
        lblFechaPrestamo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblFechaPrestamo.setTextFill(Color.web("#2c3e50"));
        lblFechaPrestamo.setStyle(
                "-fx-background-color: #f0f0f0; " +
                        "-fx-padding: 10 14; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 8;");
        lblFechaPrestamo.setPrefHeight(42);
        lblFechaPrestamo.setMaxWidth(Double.MAX_VALUE);
        DatePicker dpFechaDevolucion = new DatePicker(prestamo.getFechaDevolucionEsperada());
        dpFechaDevolucion.setPrefWidth(200);
        dpFechaDevolucion.setPrefHeight(42);
        dpFechaDevolucion.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-family: 'Segoe UI';");
        dpFechaDevolucion.setDayCellFactory(_ -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(prestamo.getFechaPrestamo()));
            }
        });
        grid.add(lblUsuarioLabel, 0, 0);
        grid.add(lblUsuario, 1, 0);

        grid.add(lblLibroLabel, 0, 1);
        grid.add(lblLibro, 1, 1);

        grid.add(lblFechaPrestamoLabel, 0, 2);
        grid.add(lblFechaPrestamo, 1, 2);

        grid.add(lblFechaDevolucionLabel, 0, 3);
        grid.add(dpFechaDevolucion, 1, 3);

        container.getChildren().addAll(lblTituloDialogo, subtitulo, separador, grid);
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().lookupButton(btnGuardar).setStyle(
                "-fx-background-color: #f8b739; " +
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
            if (dialogButton == btnGuardar) {
                try {
                    java.time.LocalDate nuevaFecha = dpFechaDevolucion.getValue();

                    if (nuevaFecha == null) {
                        mostrarAlerta("Error", "Debe seleccionar una fecha de devolución",
                                Alert.AlertType.ERROR);
                        return null;
                    }

                    if (nuevaFecha.isBefore(prestamo.getFechaPrestamo())) {
                        mostrarAlerta("Error",
                                "La fecha de devolución no puede ser anterior a la fecha de préstamo",
                                Alert.AlertType.ERROR);
                        return null;
                    }

                    gestorPrestamos.editarFechaDevolucion(prestamo.getIdPrestamo(), nuevaFecha);
                    return prestamo;

                } catch (Exception e) {
                    mostrarAlerta("Error", "No se pudo editar el préstamo: " + e.getMessage(),
                            Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            if (p != null) {
                cargarPrestamos();
                mostrarAlerta("Éxito",
                        "Préstamo actualizado correctamente.\nNueva fecha de devolución: " +
                                dpFechaDevolucion.getValue().format(dateFormatter),
                        Alert.AlertType.INFORMATION);
            }
        });
    }

    private void cancelarPrestamoSeleccionado() {
        Prestamo prestamoSeleccionado = tablaPrestamos.getSelectionModel().getSelectedItem();

        if (prestamoSeleccionado == null) {
            mostrarAlerta("Advertencia", "Seleccione un préstamo para cancelar", Alert.AlertType.WARNING);
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

        Label lblTitulo = new Label("❌ ¿Cancelar préstamo?");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#e74c3c"));

        Label lblMensaje = new Label("El préstamo será marcado como cancelado.");
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

        Label lblInfo = new Label("Información del préstamo:");
        lblInfo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblInfo.setTextFill(Color.web("#2c3e50"));

        Label lblId = new Label("🆔 ID: " + prestamoSeleccionado.getIdPrestamo());
        lblId.setFont(Font.font("Segoe UI", 13));
        lblId.setTextFill(Color.web("#7f8c8d"));

        Label lblLibro = new Label("📚 Libro: " + prestamoSeleccionado.getLibro().getTitulo());
        lblLibro.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblLibro.setTextFill(Color.web("#2c3e50"));
        lblLibro.setWrapText(true);

        Label lblUsuario = new Label("👤 Usuario: " + prestamoSeleccionado.getUsuario().getNombre());
        lblUsuario.setFont(Font.font("Segoe UI", 13));
        lblUsuario.setTextFill(Color.web("#7f8c8d"));

        Label lblFechaPrestamo = new Label("📅 Fecha préstamo: " +
                prestamoSeleccionado.getFechaPrestamo()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblFechaPrestamo.setFont(Font.font("Segoe UI", 13));
        lblFechaPrestamo.setTextFill(Color.web("#7f8c8d"));

        Label lblEstado = new Label("📊 Estado actual: " + prestamoSeleccionado.getEstado());
        lblEstado.setFont(Font.font("Segoe UI", 13));
        lblEstado.setTextFill(Color.web("#7f8c8d"));

        infoBox.getChildren().addAll(lblInfo, lblId, lblLibro, lblUsuario, lblFechaPrestamo, lblEstado);

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
        ((Button) confirmacion.getDialogPane().lookupButton(ButtonType.OK)).setText("Cancelar Préstamo");
        ((Button) confirmacion.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Volver");
        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    gestorPrestamos.cancelarPrestamo(prestamoSeleccionado.getIdPrestamo());
                    cargarPrestamos();
                    mostrarAlerta("Éxito", "Préstamo cancelado correctamente", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    mostrarAlerta("Error", "No se pudo cancelar el préstamo: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
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
