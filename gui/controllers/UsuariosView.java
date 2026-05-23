package gui.controllers;

import controlador.GestorUsuarios;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.control.TableRow;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import modelo.Usuario;
import java.io.IOException;

public class UsuariosView extends VBox {

    private GestorUsuarios gestorUsuarios;
    private TableView<Usuario> tablaUsuarios;
    private ObservableList<Usuario> usuariosObservable;
    private FilteredList<Usuario> usuariosFiltrados;
    private TextField txtBuscar;

    public UsuariosView(GestorUsuarios gestorUsuarios) {
        this.gestorUsuarios = gestorUsuarios;
        this.usuariosObservable = FXCollections.observableArrayList();
        inicializar();
        cargarUsuarios();
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

        Label titulo = new Label("👥 Gestión de Usuarios");
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
        txtBuscar.setPromptText("🔍 Buscar por ID, nombre o email...");
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
        txtBuscar.textProperty().addListener((_, _, newValue) -> filtrarUsuarios(newValue));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAgregar = new Button("Agregar Usuario");
        ImageView iconoAgregar = new ImageView(new Image("/resources/agregar.png"));
        iconoAgregar.setFitWidth(16);
        iconoAgregar.setFitHeight(16);
        btnAgregar.setGraphic(iconoAgregar);

        Button btnEditar = new Button("Editar Usuario");
        ImageView iconoEditar = new ImageView(new Image("/resources/editar.png"));
        iconoEditar.setFitWidth(16);
        iconoEditar.setFitHeight(16);
        btnEditar.setGraphic(iconoEditar);

        Button btnEliminar = new Button("Eliminar Usuario");
        ImageView iconoEliminar = new ImageView(new Image("/resources/eliminar.png"));
        iconoEliminar.setFitWidth(16);
        iconoEliminar.setFitHeight(16);
        btnEliminar.setGraphic(iconoEliminar);

        aplicarEfectoHover(btnAgregar, "#52c98f", "#42b87f");
        aplicarEfectoHover(btnEditar, "#5dade2", "#4a9dd5");
        aplicarEfectoHover(btnEliminar, "#ec7063", "#e55e53");

        btnAgregar.setOnAction(_ -> mostrarDialogoAgregar());
        btnEditar.setOnAction(_ -> editarUsuarioSeleccionado());
        btnEliminar.setOnAction(_ -> eliminarUsuarioSeleccionado());

        toolBar.getChildren().addAll(txtBuscar, spacer, btnAgregar, btnEditar, btnEliminar);
        return toolBar;
    }

    private VBox crearTabla() {
        VBox container = new VBox();
        container.setPadding(new Insets(20));
        container.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");
        VBox.setVgrow(container, Priority.ALWAYS);

        tablaUsuarios = new TableView<>();
        tablaUsuarios.setStyle("-fx-font-size: 14px; -fx-padding: 5;");

        TableColumn<Usuario, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(100);

        TableColumn<Usuario, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(200);

        TableColumn<Usuario, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(220);

        TableColumn<Usuario, Integer> colPrestamos = new TableColumn<>("Préstamos Actuales");
        colPrestamos.setCellValueFactory(new PropertyValueFactory<>("prestamosActuales"));
        colPrestamos.setPrefWidth(150);

        TableColumn<Usuario, Integer> colLimite = new TableColumn<>("Límite");
        colLimite.setCellValueFactory(new PropertyValueFactory<>("limitePrestamos"));
        colLimite.setPrefWidth(80);

        TableColumn<Usuario, Double> colMulta = new TableColumn<>("Multa");
        colMulta.setCellValueFactory(new PropertyValueFactory<>("multaAcumulada"));
        colMulta.setPrefWidth(100);

        tablaUsuarios.getColumns().add(colId);
        tablaUsuarios.getColumns().add(colNombre);
        tablaUsuarios.getColumns().add(colEmail);
        tablaUsuarios.getColumns().add(colPrestamos);
        tablaUsuarios.getColumns().add(colLimite);
        tablaUsuarios.getColumns().add(colMulta);

        tablaUsuarios.setRowFactory(_ -> {
        TableRow<Usuario> row = new TableRow<>();
        row.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            final int index = row.getIndex();
            
            // Si es una fila vacía, deseleccionar
            if (row.isEmpty()) {
                tablaUsuarios.getSelectionModel().clearSelection();
                event.consume();
            } 
            // Si es una fila ya seleccionada, deseleccionar
            else if (index >= 0 && index < tablaUsuarios.getItems().size() 
                    && tablaUsuarios.getSelectionModel().isSelected(index)) {
                tablaUsuarios.getSelectionModel().clearSelection();
                event.consume();
            }
        });
        return row;
    });

        usuariosFiltrados = new FilteredList<>(usuariosObservable, _ -> true);
        tablaUsuarios.setItems(usuariosFiltrados);

        VBox.setVgrow(tablaUsuarios, Priority.ALWAYS);
        container.getChildren().add(tablaUsuarios);

        return container;
    }

    private void cargarUsuarios() {
        usuariosObservable.clear();
        usuariosObservable.addAll(gestorUsuarios.obtenerTodosUsuarios());

        try {
            utilidades.PersistenciaArchivos.guardarUsuarios(gestorUsuarios.obtenerTodosUsuarios());
        } catch (IOException e) {
            System.out.println("Error al guardar usuarios: " + e.getMessage());
        }
    }

    private void filtrarUsuarios(String filtro) {
        usuariosFiltrados.setPredicate(usuario -> {
            if (filtro == null || filtro.isEmpty()) {
                return true;
            }

            String lowerCaseFilter = filtro.toLowerCase();

            if (usuario.getId().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            } else if (usuario.getNombre().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            } else if (usuario.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }

            return false;
        });
    }

    private void mostrarDialogoAgregar() {
        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle("Agregar Nuevo Usuario");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(550);
        dialog.getDialogPane().setPrefHeight(480);

        gui.BibliotecaApp.aplicarIconoADialog(dialog);

        ButtonType btnAceptar = new ButtonType("Agregar Usuario", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 12;");
        Label lblTituloDialogo = new Label();
ImageView iconoNuevoUsuario = new ImageView(new Image(getClass().getResourceAsStream("/resources/usuario.png")));
iconoNuevoUsuario.setFitWidth(24);
iconoNuevoUsuario.setFitHeight(24);
lblTituloDialogo.setGraphic(iconoNuevoUsuario);
lblTituloDialogo.setText("Nuevo Usuario");
lblTituloDialogo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
lblTituloDialogo.setTextFill(Color.web("#2c3e50"));

        Label subtitulo = new Label("Complete la información del usuario a registrar");
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

        Label lblId = new Label("ID del Usuario:");
        lblId.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblId.setTextFill(Color.web("#2c3e50"));

        Label lblNombre = new Label("Nombre:");
        lblNombre.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblNombre.setTextFill(Color.web("#2c3e50"));

        Label lblEmail = new Label("Email:");
        lblEmail.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblEmail.setTextFill(Color.web("#2c3e50"));

        Label lblLimite = new Label("Límite de Préstamos:");
        lblLimite.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblLimite.setTextFill(Color.web("#2c3e50"));

        TextField txtId = new TextField();
        txtId.setPromptText("Ej: USR001");
        aplicarEstiloTextField(txtId);

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ej: Juan Pérez");
        aplicarEstiloTextField(txtNombre);

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Ej: juan.perez@ejemplo.com");
        aplicarEstiloTextField(txtEmail);

        TextField txtLimite = new TextField();
        txtLimite.setPromptText("Ej: 3");
        txtLimite.setText("3");
        aplicarEstiloTextField(txtLimite);
        grid.add(lblId, 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(lblNombre, 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(lblEmail, 0, 2);
        grid.add(txtEmail, 1, 2);
        grid.add(lblLimite, 0, 3);
        grid.add(txtLimite, 1, 3);

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
                    int limite = Integer.parseInt(txtLimite.getText());
                    try {
                        return new Usuario(
                                txtId.getText().trim(),
                                txtNombre.getText().trim(),
                                txtEmail.getText().trim(),
                                limite);
                    } catch (Exception e) {
                        mostrarAlerta("Error", "Error al crear usuario: " + e.getMessage(), Alert.AlertType.ERROR);
                        return null;
                    }
                } catch (NumberFormatException e) {
                    mostrarAlerta("Error", "El límite debe ser un número válido", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(usuario -> {
            if (usuario != null) {
                try {
                    gestorUsuarios.agregarUsuario(usuario);
                    cargarUsuarios();
                    mostrarAlerta("Éxito", "Usuario agregado correctamente", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    mostrarAlerta("Error", "No se pudo agregar el usuario: " + e.getMessage(), Alert.AlertType.ERROR);
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

    private void editarUsuarioSeleccionado() {
        Usuario usuarioSeleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();

        if (usuarioSeleccionado == null) {
            mostrarAlerta("Advertencia", "Seleccione un usuario para editar", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle("Editar Usuario");
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
        lblTituloDialogo.setText("Editar Usuario");
        lblTituloDialogo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblTituloDialogo.setTextFill(Color.web("#2c3e50"));

        Label subtitulo = new Label("ID: " + usuarioSeleccionado.getId() + " - " + usuarioSeleccionado.getNombre());
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

        Label lblId = new Label("ID:");
        lblId.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblId.setTextFill(Color.web("#2c3e50"));

        Label lblNombre = new Label("Nombre:");
        lblNombre.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblNombre.setTextFill(Color.web("#2c3e50"));

        Label lblEmail = new Label("Email:");
        lblEmail.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblEmail.setTextFill(Color.web("#2c3e50"));

        Label lblLimite = new Label("Límite de Préstamos:");
        lblLimite.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblLimite.setTextFill(Color.web("#2c3e50"));

        TextField txtId = new TextField(usuarioSeleccionado.getId());
        txtId.setDisable(true);
        txtId.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-padding: 10 14; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-background-color: #f0f0f0; " +
                        "-fx-opacity: 0.7;");
        txtId.setPrefHeight(42);

        TextField txtNombre = new TextField(usuarioSeleccionado.getNombre());
        aplicarEstiloTextField(txtNombre);

        TextField txtEmail = new TextField(usuarioSeleccionado.getEmail());
        aplicarEstiloTextField(txtEmail);

        TextField txtLimite = new TextField(String.valueOf(usuarioSeleccionado.getLimitePrestamos()));
        aplicarEstiloTextField(txtLimite);

        grid.add(lblId, 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(lblNombre, 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(lblEmail, 0, 2);
        grid.add(txtEmail, 1, 2);
        grid.add(lblLimite, 0, 3);
        grid.add(txtLimite, 1, 3);

        container.getChildren().addAll(lblTituloDialogo, subtitulo, separador, grid);
        dialog.getDialogPane().setContent(container);

        dialog.getDialogPane().lookupButton(btnGuardar).setStyle(
                "-fx-background-color: #5dade2; " +
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
        int prestamosOriginales = usuarioSeleccionado.getPrestamosActuales();
        double multaOriginal = usuarioSeleccionado.getMultaAcumulada();

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                try {
                    int limite = Integer.parseInt(txtLimite.getText());
                    try {
                        Usuario usuarioEditado = new Usuario(
                                usuarioSeleccionado.getId(),
                                txtNombre.getText().trim(),
                                txtEmail.getText().trim(),
                                limite);

                        return usuarioEditado;
                    } catch (Exception e) {
                        mostrarAlerta("Error", "Error al editar usuario: " + e.getMessage(), Alert.AlertType.ERROR);
                        return null;
                    }
                } catch (NumberFormatException e) {
                    mostrarAlerta("Error", "El límite debe ser un número válido", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(usuarioEditado -> {
            if (usuarioEditado != null) {
                try {
                    gestorUsuarios.eliminarUsuario(usuarioSeleccionado.getId());
                    gestorUsuarios.agregarUsuario(usuarioEditado);

                    for (int i = 0; i < prestamosOriginales; i++) {
                        try {
                            usuarioEditado.agregarPrestamo();
                        } catch (Exception ex) {

                        }
                    }

                    if (multaOriginal > 0) {
                        usuarioEditado.agregarMulta(multaOriginal);
                    }

                    cargarUsuarios();
                    mostrarAlerta("Éxito", "Usuario editado correctamente", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    mostrarAlerta("Error", "No se pudo editar el usuario: " + e.getMessage(), Alert.AlertType.ERROR);
                    try {
                        gestorUsuarios.agregarUsuario(usuarioSeleccionado);
                    } catch (Exception ex) {

                    }
                }
            }
        });
    }

    private void eliminarUsuarioSeleccionado() {
        Usuario usuarioSeleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();

        if (usuarioSeleccionado == null) {
            mostrarAlerta("Advertencia", "Seleccione un usuario para eliminar", Alert.AlertType.WARNING);
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

        Label lblInfo = new Label("Información del usuario:");
        lblInfo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblInfo.setTextFill(Color.web("#2c3e50"));

        Label lblNombreUsuario = new Label("👤 " + usuarioSeleccionado.getNombre());
        lblNombreUsuario.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblNombreUsuario.setTextFill(Color.web("#2c3e50"));

        Label lblEmail = new Label("📧 Email: " + usuarioSeleccionado.getEmail());
        lblEmail.setFont(Font.font("Segoe UI", 13));
        lblEmail.setTextFill(Color.web("#7f8c8d"));

        Label lblId = new Label("🆔 ID: " + usuarioSeleccionado.getId());
        lblId.setFont(Font.font("Segoe UI", 13));
        lblId.setTextFill(Color.web("#7f8c8d"));
        if (usuarioSeleccionado.getPrestamosActuales() > 0 || usuarioSeleccionado.getMultaAcumulada() > 0) {
            Label lblAdvertencia = new Label();
            ImageView iconoWarning2 = new ImageView(
                    new Image(getClass().getResourceAsStream("/resources/warning2.png")));
            iconoWarning2.setFitWidth(16);
            iconoWarning2.setFitHeight(16);
            lblAdvertencia.setGraphic(iconoWarning2);
            lblAdvertencia.setText("Este usuario tiene préstamos activos o multas pendientes");
            lblAdvertencia.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblAdvertencia.setTextFill(Color.web("#e67e22"));
            lblAdvertencia.setWrapText(true);
            infoBox.getChildren().add(lblAdvertencia);
        }

        infoBox.getChildren().addAll(lblInfo, lblNombreUsuario, lblEmail, lblId);

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
                    gestorUsuarios.eliminarUsuario(usuarioSeleccionado.getId());
                    cargarUsuarios();
                    mostrarAlerta("Éxito", "Usuario eliminado correctamente", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    mostrarAlerta("Error", "No se pudo eliminar el usuario: " + e.getMessage(), Alert.AlertType.ERROR);
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
