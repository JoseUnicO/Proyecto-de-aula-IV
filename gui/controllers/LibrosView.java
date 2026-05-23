package gui.controllers;

import controlador.GestorLibros;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.control.TableRow;
import modelo.Libro;
import java.io.IOException;

public class LibrosView extends VBox {

    private GestorLibros gestorLibros;
    private TableView<Libro> tablaLibros;
    private ObservableList<Libro> librosObservable;
    private FilteredList<Libro> librosFiltrados;
    private TextField txtBuscar;

    public LibrosView(GestorLibros gestorLibros) {
        this.gestorLibros = gestorLibros;
        this.librosObservable = FXCollections.observableArrayList();
        inicializar();
        cargarLibros();
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

        Label titulo = new Label("📚 Gestión de Libros");
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
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");

        txtBuscar = new TextField();
        txtBuscar.setPromptText("🔍 Buscar por título, autor o ISBN...");
        txtBuscar.setPrefWidth(400);
        txtBuscar.setPrefHeight(44);
        txtBuscar.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-padding: 12 16; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;");
        txtBuscar.textProperty().addListener((_, _, newValue) -> filtrarLibros(newValue));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAgregar = new Button("Agregar Libro");
        ImageView iconoAgregar = new ImageView(new Image("/resources/agregar.png"));
        iconoAgregar.setFitWidth(16);
        iconoAgregar.setFitHeight(16);
        btnAgregar.setGraphic(iconoAgregar);

        Button btnEditar = new Button("Editar Libro");
        ImageView iconoEditar = new ImageView(new Image("/resources/editar.png"));
        iconoEditar.setFitWidth(16);
        iconoEditar.setFitHeight(16);
        btnEditar.setGraphic(iconoEditar);

        Button btnEliminar = new Button("Eliminar Libro");
        ImageView iconoEliminar = new ImageView(new Image("/resources/eliminar.png"));
        iconoEliminar.setFitWidth(16);
        iconoEliminar.setFitHeight(16);
        btnEliminar.setGraphic(iconoEliminar);

        aplicarEfectoHover(btnAgregar, "#52c98f", "#42b87f");
        aplicarEfectoHover(btnEditar, "#5dade2", "#4a9dd5");
        aplicarEfectoHover(btnEliminar, "#ec7063", "#e55e53");

        btnAgregar.setOnAction(_ -> mostrarDialogoAgregar());
        btnEditar.setOnAction(_ -> editarLibroSeleccionado());
        btnEliminar.setOnAction(_ -> eliminarLibroSeleccionado());

        toolBar.getChildren().addAll(txtBuscar, spacer, btnAgregar, btnEditar, btnEliminar);
        return toolBar;
    }

    private VBox crearTabla() {
        VBox container = new VBox();
        container.setPadding(new Insets(20));
        container.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");
        VBox.setVgrow(container, Priority.ALWAYS);

        tablaLibros = new TableView<>();
        tablaLibros.setStyle("-fx-font-size: 13px;");

        TableColumn<Libro, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(80);

        TableColumn<Libro, String> colTitulo = new TableColumn<>("Título");
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colTitulo.setPrefWidth(250);

        TableColumn<Libro, String> colAutor = new TableColumn<>("Autor");
        colAutor.setCellValueFactory(new PropertyValueFactory<>("autor"));
        colAutor.setPrefWidth(180);

        TableColumn<Libro, String> colIsbn = new TableColumn<>("ISBN");
        colIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        colIsbn.setPrefWidth(130);

        TableColumn<Libro, Integer> colAnio = new TableColumn<>("Año");
        colAnio.setCellValueFactory(new PropertyValueFactory<>("anioPublicacion"));
        colAnio.setPrefWidth(80);

        TableColumn<Libro, String> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCategoria.setPrefWidth(150);

        TableColumn<Libro, String> colDisponible = new TableColumn<>("Estado");
        colDisponible.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().isDisponible() ? "✅ Disponible" : "❌ Prestado"));
        colDisponible.setPrefWidth(120);

        tablaLibros.getColumns().add(colId);
        tablaLibros.getColumns().add(colTitulo);
        tablaLibros.getColumns().add(colAutor);
        tablaLibros.getColumns().add(colIsbn);
        tablaLibros.getColumns().add(colAnio);
        tablaLibros.getColumns().add(colCategoria);
        tablaLibros.getColumns().add(colDisponible);

        tablaLibros.setRowFactory(_ -> {
            TableRow<Libro> row = new TableRow<>();
            row.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                final int index = row.getIndex();

                // Si es una fila vacía, deseleccionar
                if (row.isEmpty()) {
                    tablaLibros.getSelectionModel().clearSelection();
                    event.consume();
                }
                // Si es una fila ya seleccionada, deseleccionar
                else if (index >= 0 && index < tablaLibros.getItems().size()
                        && tablaLibros.getSelectionModel().isSelected(index)) {
                    tablaLibros.getSelectionModel().clearSelection();
                    event.consume();
                }
            });
            return row;
        });

        librosFiltrados = new FilteredList<>(librosObservable, libro -> {
            java.util.Objects.requireNonNull(libro);
            return true;
        });
        tablaLibros.setItems(librosFiltrados);

        VBox.setVgrow(tablaLibros, Priority.ALWAYS);
        container.getChildren().add(tablaLibros);

        return container;
    }

    private void cargarLibros() {
        librosObservable.clear();
        librosObservable.addAll(gestorLibros.obtenerTodosLibros());

        try {
            utilidades.PersistenciaArchivos.guardarLibros(gestorLibros.obtenerTodosLibros());
        } catch (IOException e) {
            System.out.println("Error al guardar libros: " + e.getMessage());
        }
    }

    private void filtrarLibros(String filtro) {
        librosFiltrados.setPredicate(libro -> {
            if (filtro == null || filtro.isEmpty()) {
                return true;
            }

            String lowerCaseFilter = filtro.toLowerCase();

            if (libro.getTitulo().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            } else if (libro.getAutor().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            } else if (libro.getIsbn().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            } else if (libro.getCategoria().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }

            return false;
        });
    }

    private void mostrarDialogoAgregar() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Agregar Nuevo Libro");
        dialog.setHeaderText(null);

        dialog.getDialogPane().setPrefWidth(550);
        dialog.getDialogPane().setPrefHeight(520);

        gui.BibliotecaApp.aplicarIconoADialog(dialog);

        ButtonType btnAceptar = new ButtonType("Agregar Libro", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, btnCancelar);

        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 12;");
        Label lblTituloDialogo = new Label();
        ImageView iconoNuevoLibro = new ImageView(new Image(getClass().getResourceAsStream("/resources/libro2.png")));
        iconoNuevoLibro.setFitWidth(24);
        iconoNuevoLibro.setFitHeight(24);
        lblTituloDialogo.setGraphic(iconoNuevoLibro);
        lblTituloDialogo.setText("Nuevo Libro");
        lblTituloDialogo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblTituloDialogo.setTextFill(Color.web("#2c3e50"));

        Label subtitulo = new Label("Complete la información del libro a registrar");
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

        Label lblId = new Label("ID del Libro:");
        lblId.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblId.setTextFill(Color.web("#2c3e50"));

        Label lblTitulo = new Label("Título:");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblTitulo.setTextFill(Color.web("#2c3e50"));

        Label lblAutor = new Label("Autor:");
        lblAutor.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblAutor.setTextFill(Color.web("#2c3e50"));

        Label lblIsbn = new Label("ISBN:");
        lblIsbn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblIsbn.setTextFill(Color.web("#2c3e50"));

        Label lblAnio = new Label("Año:");
        lblAnio.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblAnio.setTextFill(Color.web("#2c3e50"));

        Label lblCategoria = new Label("Categoría:");
        lblCategoria.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblCategoria.setTextFill(Color.web("#2c3e50"));

        TextField txtId = new TextField();
        txtId.setPromptText("Ej: LIB001");
        aplicarEstiloTextField(txtId);

        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Ej: Cien Años de Soledad");
        aplicarEstiloTextField(txtTitulo);

        TextField txtAutor = new TextField();
        txtAutor.setPromptText("Ej: Gabriel García Márquez");
        aplicarEstiloTextField(txtAutor);

        TextField txtIsbn = new TextField();
        txtIsbn.setPromptText("Ej: 9780307474728");
        aplicarEstiloTextField(txtIsbn);

        TextField txtAnio = new TextField();
        txtAnio.setPromptText("Ej: 1967");
        aplicarEstiloTextField(txtAnio);

        TextField txtCategoria = new TextField();
        txtCategoria.setPromptText("Ej: Ficción");
        aplicarEstiloTextField(txtCategoria);

        grid.add(lblId, 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(lblTitulo, 0, 1);
        grid.add(txtTitulo, 1, 1);
        grid.add(lblAutor, 0, 2);
        grid.add(txtAutor, 1, 2);
        grid.add(lblIsbn, 0, 3);
        grid.add(txtIsbn, 1, 3);
        grid.add(lblAnio, 0, 4);
        grid.add(txtAnio, 1, 4);
        grid.add(lblCategoria, 0, 5);
        grid.add(txtCategoria, 1, 5);
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

        dialog.getDialogPane().lookupButton(btnCancelar).setStyle(
                "-fx-background-color: #e0e0e0; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 24; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;");

        dialog.showAndWait().ifPresent(response -> {
            if (response == btnAceptar) {
                String id = txtId.getText().trim();
                String titulo = txtTitulo.getText().trim();
                String autor = txtAutor.getText().trim();
                String isbn = txtIsbn.getText().trim();
                String anio = txtAnio.getText().trim();
                String categoria = txtCategoria.getText().trim();

                if (id.isEmpty() || titulo.isEmpty() || autor.isEmpty() ||
                        isbn.isEmpty() || anio.isEmpty() || categoria.isEmpty()) {
                    mostrarAlerta("Error", "Todos los campos son obligatorios", Alert.AlertType.ERROR);
                    return;
                }

                try {
                    int anioNum = Integer.parseInt(anio);
                    Libro nuevoLibro = new Libro(id, titulo, autor, isbn, anioNum, categoria);
                    gestorLibros.agregarLibro(nuevoLibro);

                    try {
                        utilidades.PersistenciaArchivos.guardarLibros(gestorLibros.obtenerTodosLibros());
                    } catch (IOException e) {
                        mostrarAlerta("Error", "No se pudieron guardar los datos", Alert.AlertType.ERROR);
                    }

                    cargarLibros();
                    mostrarAlerta("Éxito", "Libro agregado correctamente", Alert.AlertType.INFORMATION);

                } catch (NumberFormatException e) {
                    mostrarAlerta("Error", "El año debe ser un número válido", Alert.AlertType.ERROR);
                } catch (Exception e) {
                    mostrarAlerta("Error", e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
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

    private void editarLibroSeleccionado() {
        Libro libroSeleccionado = tablaLibros.getSelectionModel().getSelectedItem();

        if (libroSeleccionado == null) {
            mostrarAlerta("Advertencia", "Seleccione un libro para editar", Alert.AlertType.WARNING);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Libro");
        dialog.setHeaderText(null);

        dialog.getDialogPane().setPrefWidth(550);
        dialog.getDialogPane().setPrefHeight(520);

        gui.BibliotecaApp.aplicarIconoADialog(dialog);

        ButtonType btnAceptar = new ButtonType("Guardar Cambios", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, btnCancelar);

        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 12;");

        Label lblTituloDialogo = new Label();
        ImageView iconoEditarLibro = new ImageView(
                new Image(getClass().getResourceAsStream("/resources/editar.png")));
        iconoEditarLibro.setFitWidth(24);
        iconoEditarLibro.setFitHeight(24);
        lblTituloDialogo.setGraphic(iconoEditarLibro);
        lblTituloDialogo.setText("Editar Libro");
        lblTituloDialogo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblTituloDialogo.setTextFill(Color.web("#2c3e50"));

        Label subtitulo = new Label("ID: " + libroSeleccionado.getId() + " - " + libroSeleccionado.getTitulo());
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

        Label lblTitulo = new Label("Título:");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblTitulo.setTextFill(Color.web("#2c3e50"));

        Label lblAutor = new Label("Autor:");
        lblAutor.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblAutor.setTextFill(Color.web("#2c3e50"));

        Label lblIsbn = new Label("ISBN:");
        lblIsbn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblIsbn.setTextFill(Color.web("#2c3e50"));

        Label lblAnio = new Label("Año:");
        lblAnio.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblAnio.setTextFill(Color.web("#2c3e50"));

        Label lblCategoria = new Label("Categoría:");
        lblCategoria.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblCategoria.setTextFill(Color.web("#2c3e50"));

        TextField txtTitulo = new TextField(libroSeleccionado.getTitulo());
        aplicarEstiloTextField(txtTitulo);

        TextField txtAutor = new TextField(libroSeleccionado.getAutor());
        aplicarEstiloTextField(txtAutor);

        TextField txtIsbn = new TextField(libroSeleccionado.getIsbn());
        aplicarEstiloTextField(txtIsbn);

        TextField txtAnio = new TextField(String.valueOf(libroSeleccionado.getAnioPublicacion()));
        aplicarEstiloTextField(txtAnio);

        TextField txtCategoria = new TextField(libroSeleccionado.getCategoria());
        aplicarEstiloTextField(txtCategoria);

        grid.add(lblTitulo, 0, 0);
        grid.add(txtTitulo, 1, 0);
        grid.add(lblAutor, 0, 1);
        grid.add(txtAutor, 1, 1);
        grid.add(lblIsbn, 0, 2);
        grid.add(txtIsbn, 1, 2);
        grid.add(lblAnio, 0, 3);
        grid.add(txtAnio, 1, 3);
        grid.add(lblCategoria, 0, 4);
        grid.add(txtCategoria, 1, 4);

        container.getChildren().addAll(lblTituloDialogo, subtitulo, separador, grid);
        dialog.getDialogPane().setContent(container);

        dialog.getDialogPane().lookupButton(btnAceptar).setStyle(
                "-fx-background-color: #5dade2; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 24; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;");

        dialog.getDialogPane().lookupButton(btnCancelar).setStyle(
                "-fx-background-color: #e0e0e0; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 24; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;");
        dialog.showAndWait().ifPresent(response -> {
            if (response == btnAceptar) {
                String titulo = txtTitulo.getText().trim();
                String autor = txtAutor.getText().trim();
                String isbn = txtIsbn.getText().trim();
                String anio = txtAnio.getText().trim();
                String categoria = txtCategoria.getText().trim();

                if (titulo.isEmpty() || autor.isEmpty() || isbn.isEmpty() ||
                        anio.isEmpty() || categoria.isEmpty()) {
                    mostrarAlerta("Error", "Todos los campos son obligatorios", Alert.AlertType.ERROR);
                    return;
                }

                try {
                    int anioNum = Integer.parseInt(anio);

                    boolean disponibilidadOriginal = libroSeleccionado.isDisponible();

                    gestorLibros.eliminarLibro(libroSeleccionado.getId());
                    Libro libroActualizado = new Libro(
                            libroSeleccionado.getId(),
                            titulo,
                            autor,
                            isbn,
                            anioNum,
                            categoria);
                    if (!disponibilidadOriginal) {
                        libroActualizado.prestar();
                    }
                    gestorLibros.agregarLibro(libroActualizado);

                    try {
                        utilidades.PersistenciaArchivos.guardarLibros(gestorLibros.obtenerTodosLibros());
                    } catch (IOException e) {
                        mostrarAlerta("Error", "No se pudieron guardar los datos: " + e.getMessage(),
                                Alert.AlertType.ERROR);
                    }

                    cargarLibros();
                    mostrarAlerta("Éxito", "Libro actualizado correctamente", Alert.AlertType.INFORMATION);

                } catch (NumberFormatException e) {
                    mostrarAlerta("Error", "El año debe ser un número válido", Alert.AlertType.ERROR);
                } catch (Exception e) {
                    mostrarAlerta("Error", e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void eliminarLibroSeleccionado() {
        Libro libroSeleccionado = tablaLibros.getSelectionModel().getSelectedItem();

        if (libroSeleccionado == null) {
            mostrarAlerta("Advertencia", "Seleccione un libro para eliminar", Alert.AlertType.WARNING);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Eliminación");
        confirmacion.setHeaderText(null);

        gui.BibliotecaApp.aplicarIconoAAlert(confirmacion);
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(30));
        contenido.setAlignment(Pos.CENTER_LEFT);
        contenido.setStyle(
                "-fx-background-color: #fff5f5; " +
                        "-fx-background-radius: 12;");

        Label lblTitulo = new Label();
        ImageView iconoWarning = new ImageView(new Image(getClass().getResourceAsStream("/resources/warning.png")));
        iconoWarning.setFitWidth(24);
        iconoWarning.setFitHeight(24);
        lblTitulo.setGraphic(iconoWarning);
        lblTitulo.setText("¿Eliminar libro?");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#e74c3c"));

        Label lblMensaje = new Label("Esta acción no se puede deshacer.");
        lblMensaje.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblMensaje.setTextFill(Color.web("#c0392b"));
        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(16));
        infoBox.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #e74c3c; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8;");

        Label lblInfo = new Label("Información del libro:");
        lblInfo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblInfo.setTextFill(Color.web("#2c3e50"));

        Label lblTituloLibro = new Label("📚 " + libroSeleccionado.getTitulo());
        lblTituloLibro.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblTituloLibro.setTextFill(Color.web("#2c3e50"));

        Label lblAutor = new Label("✍️ Autor: " + libroSeleccionado.getAutor());
        lblAutor.setFont(Font.font("Segoe UI", 13));
        lblAutor.setTextFill(Color.web("#7f8c8d"));

        Label lblIsbn = new Label("🔢 ISBN: " + libroSeleccionado.getIsbn());
        lblIsbn.setFont(Font.font("Segoe UI", 13));
        lblIsbn.setTextFill(Color.web("#7f8c8d"));

        infoBox.getChildren().addAll(lblInfo, lblTituloLibro, lblAutor, lblIsbn);

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
        ((Button) confirmacion.getDialogPane().lookupButton(ButtonType.OK)).setText("Eliminar");
        ((Button) confirmacion.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");
        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    gestorLibros.eliminarLibro(libroSeleccionado.getId());
                    cargarLibros();
                    mostrarAlerta("Éxito", "Libro eliminado correctamente", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    mostrarAlerta("Error", "No se pudo eliminar el libro: " + e.getMessage(), Alert.AlertType.ERROR);
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
