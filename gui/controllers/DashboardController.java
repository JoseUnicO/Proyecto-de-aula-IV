package gui.controllers;

import controlador.GestorLibros;
import controlador.GestorUsuarios;
import controlador.GestorPrestamos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import modelo.Admin;
import gui.BibliotecaApp;

public class DashboardController {
    private Stage stage;
    private Admin admin;
    private GestorLibros gestorLibros;
    private GestorUsuarios gestorUsuarios;
    private GestorPrestamos gestorPrestamos;

    private BorderPane mainLayout;
    private StackPane contentArea;

    private Button btnInicio;
    private Button btnLibros;
    private Button btnUsuarios;
    private Button btnPrestamos;
    private Button btnReservas;
    private Button btnReportes;

    public DashboardController(Stage stage, Admin admin) {
        this.stage = stage;
        this.admin = admin;
        this.gestorLibros = BibliotecaApp.getGestorLibros();
        this.gestorUsuarios = BibliotecaApp.getGestorUsuarios();
        this.gestorPrestamos = BibliotecaApp.getGestorPrestamos();
    }

    public void show() {
        mainLayout = new BorderPane();

        HBox topBar = crearBarraSuperior();
        mainLayout.setTop(topBar);

        VBox sidebar = crearMenuLateral();
        mainLayout.setLeft(sidebar);

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #f5f5f5;");
        mainLayout.setCenter(contentArea);

        mostrarInicio();

        Scene scene = new Scene(mainLayout, 1200, 700);
        stage.setScene(scene);
        stage.show();
    }

    private HBox crearBarraSuperior() {
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(18, 28, 18, 28));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setSpacing(24);
        topBar.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-width: 0 0 1 0; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);");

        Label titulo = new Label("Sistema de Biblioteca");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titulo.setTextFill(Color.web("#333333"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblUsuario = new Label("👤 " + admin.getNombre());
        lblUsuario.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 15));
        lblUsuario.setTextFill(Color.web("#666666"));
        lblUsuario.setPadding(new Insets(8, 16, 8, 16));
        lblUsuario.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 8;");

        Button btnCerrarSesion = new Button("Cerrar Sesión");
        btnCerrarSesion.setStyle(
                "-fx-background-color: #ff4757; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 20; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;");

        btnCerrarSesion.setOnMouseEntered(_ -> {
            btnCerrarSesion.setStyle(
                    "-fx-background-color: #ee3f4d; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-family: 'Segoe UI'; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-padding: 10 20; " +
                            "-fx-background-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-scale-x: 1.02; " +
                            "-fx-scale-y: 1.02;");
        });

        btnCerrarSesion.setOnMouseExited(_ -> {
            btnCerrarSesion.setStyle(
                    "-fx-background-color: #ff4757; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-family: 'Segoe UI'; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-padding: 10 20; " +
                            "-fx-background-radius: 8; " +
                            "-fx-cursor: hand;");
        });

        btnCerrarSesion.setOnAction(_ -> cerrarSesion());

        topBar.getChildren().addAll(titulo, spacer, lblUsuario, btnCerrarSesion);
        return topBar;
    }

    private VBox crearMenuLateral() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(250);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: #34495e;");

        Label menuHeader = new Label("MENÚ PRINCIPAL");
        menuHeader.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        menuHeader.setTextFill(Color.WHITE);
        menuHeader.setPadding(new Insets(0, 0, 20, 0));

        btnInicio = crearBotonMenu("🏠 Inicio", true);
        btnLibros = crearBotonMenu("📚 Libros", false);
        btnUsuarios = crearBotonMenu("👥 Usuarios", false);
        btnPrestamos = crearBotonMenu("📖 Préstamos", false);
        btnReservas = crearBotonMenu("🔖 Reservas", false);
        btnReportes = crearBotonMenu("📊 Reportes", false);

        btnInicio.setOnAction(_ -> {
            seleccionarBoton(btnInicio);
            mostrarInicio();
        });

        btnLibros.setOnAction(_ -> {
            seleccionarBoton(btnLibros);
            mostrarLibros();
        });

        btnUsuarios.setOnAction(_ -> {
            seleccionarBoton(btnUsuarios);
            mostrarUsuarios();
        });

        btnPrestamos.setOnAction(_ -> {
            seleccionarBoton(btnPrestamos);
            mostrarPrestamos();
        });

        btnReservas.setOnAction(_ -> {
            seleccionarBoton(btnReservas);
            mostrarReservas();
        });

        btnReportes.setOnAction(_ -> {
            seleccionarBoton(btnReportes);
            mostrarReportes();
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label footer = new Label("v1.0.0");
        footer.setFont(Font.font("Arial", 10));
        footer.setTextFill(Color.web("#95a5a6"));

        sidebar.getChildren().addAll(
                menuHeader,
                btnInicio,
                btnLibros,
                btnUsuarios,
                btnPrestamos,
                btnReservas,
                btnReportes,
                spacer,
                footer);

        return sidebar;
    }

    private Button crearBotonMenu(String texto, boolean seleccionado) {
        Button btn = new Button(texto);
        btn.setPrefWidth(210);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 20, 12, 20));
        btn.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        String estiloSeleccionado = "-fx-background-color: #5dade2; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;";

        String estiloNormal = "-fx-background-color: transparent; " +
                "-fx-text-fill: #ecf0f1; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;";

        String estiloHover = "-fx-background-color: #34495e; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;";

        if (seleccionado) {
            btn.setStyle(estiloSeleccionado);
        } else {
            btn.setStyle(estiloNormal);
        }

        btn.setOnMouseEntered(_ -> {
            if (!btn.getStyle().contains("#5dade2")) {
                btn.setStyle(estiloHover);
            }
        });

        btn.setOnMouseExited(_ -> {
            if (!btn.getStyle().contains("#5dade2")) {
                btn.setStyle(estiloNormal);
            }
        });

        return btn;
    }

    private void seleccionarBoton(Button botonSeleccionado) {
        String estiloNormal = "-fx-background-color: transparent; " +
                "-fx-text-fill: #ecf0f1; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;";

        String estiloSeleccionado = "-fx-background-color: #5dade2; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;";

        btnInicio.setStyle(estiloNormal);
        btnLibros.setStyle(estiloNormal);
        btnUsuarios.setStyle(estiloNormal);
        btnPrestamos.setStyle(estiloNormal);
        btnReservas.setStyle(estiloNormal);
        btnReportes.setStyle(estiloNormal);

        botonSeleccionado.setStyle(estiloSeleccionado);
    }

    private void mostrarInicio() {
        VBox inicio = new VBox(40);
        inicio.setPadding(new Insets(50));
        inicio.setAlignment(Pos.TOP_CENTER);

        Label titulo = new Label("Panel de Control");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        titulo.setTextFill(Color.web("#2c3e50"));

        HBox cardsBox = new HBox(30);
        cardsBox.setAlignment(Pos.CENTER);

        int totalLibros = 0;
        int totalUsuarios = 0;
        int totalPrestamos = 0;
        int totalReservas = 0;

        try {
            totalLibros = gestorLibros.obtenerTodosLibros().size();
        } catch (Exception e) {
            totalLibros = 0;
        }

        try {
            totalUsuarios = gestorUsuarios.obtenerTodosUsuarios().size();
        } catch (Exception e) {
            totalUsuarios = 0;
        }

        try {
            totalPrestamos = gestorPrestamos.contarPrestamosActivos();
        } catch (Exception e) {
            totalPrestamos = 0;
        }

        try {
            totalReservas = gestorPrestamos.getGestorReservas().contarReservas();
        } catch (Exception e) {
            totalReservas = 0;
        }

        VBox cardLibros = crearCardEstadistica("📚", "Total Libros", String.valueOf(totalLibros), "#3498db");
        VBox cardUsuarios = crearCardEstadistica("👥", "Usuarios", String.valueOf(totalUsuarios), "#2ecc71");
        VBox cardPrestamos = crearCardEstadistica("📖", "Préstamos Activos", String.valueOf(totalPrestamos), "#e74c3c");
        VBox cardReservas = crearCardEstadistica("🔖", "Reservas", String.valueOf(totalReservas), "#f39c12");

        cardsBox.getChildren().addAll(cardLibros, cardUsuarios, cardPrestamos, cardReservas);
        VBox bienvenida = new VBox(15);
        bienvenida.setAlignment(Pos.CENTER);
        bienvenida.setMaxWidth(700);
        bienvenida.setPadding(new Insets(40));
        bienvenida.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        Label lblBienvenida = new Label("Bienvenido, Administrador del Sistema");
        lblBienvenida.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblBienvenida.setTextFill(Color.web("#2c3e50"));

        Label lblDescripcion = new Label(
                "Sistema de gestión de biblioteca con estructuras de datos avanzadas.\n" +
                        "Utiliza el menú lateral para navegar entre los diferentes módulos.");
        lblDescripcion.setFont(Font.font("Segoe UI", 16));
        lblDescripcion.setTextFill(Color.web("#7f8c8d"));
        lblDescripcion.setWrapText(true);
        lblDescripcion.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        bienvenida.getChildren().addAll(lblBienvenida, lblDescripcion);

        inicio.getChildren().addAll(titulo, cardsBox, bienvenida);

        contentArea.getChildren().clear();
        contentArea.getChildren().add(inicio);
    }

    private VBox crearCardEstadistica(String icono, String titulo, String valor, String color) {
        VBox card = new VBox(15);
        card.setPrefSize(240, 180);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        card.setOnMouseEntered(_ -> {
            card.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: 12; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 8); " +
                            "-fx-scale-x: 1.03; " +
                            "-fx-scale-y: 1.03;");
        });

        card.setOnMouseExited(_ -> {
            card.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: 12; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");
        });

        Label lblIcono = new Label(icono);
        lblIcono.setFont(Font.font(56));

        Label lblValor = new Label(valor);
        lblValor.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        lblValor.setTextFill(Color.web(color));

        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        lblTitulo.setTextFill(Color.web("#7f8c8d"));

        card.getChildren().addAll(lblIcono, lblValor, lblTitulo);

        return card;
    }

    private void mostrarLibros() {
        LibrosView librosView = new LibrosView(gestorLibros);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(librosView);
    }

    private void mostrarUsuarios() {
        UsuariosView usuariosView = new UsuariosView(gestorUsuarios);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(usuariosView);
    }

    private void mostrarPrestamos() {
        PrestamosView prestamosView = new PrestamosView(gestorPrestamos, gestorLibros, gestorUsuarios);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(prestamosView);
    }

    private void mostrarReservas() {
        ReservasView reservasView = new ReservasView(
                BibliotecaApp.getGestorReservas(),
                gestorLibros,
                gestorUsuarios);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(reservasView);
    }

    private void mostrarReportes() {
        ReportesView reportesView = new ReportesView(gestorLibros, gestorUsuarios, gestorPrestamos);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(reportesView);
    }

    private void cerrarSesion() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cerrar Sesión");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Está seguro que desea cerrar sesión?");
        gui.BibliotecaApp.aplicarIconoAAlert(confirmacion);

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                LoginController loginController = new LoginController(stage);
                loginController.show();
            }
        });
    }

}
