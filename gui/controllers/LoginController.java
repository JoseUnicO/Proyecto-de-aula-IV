package gui.controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import modelo.Admin;
import utilidades.PersistenciaArchivos;

import java.io.IOException;

public class LoginController {

    private Stage stage;
    private Admin admin;
    private int intentosRestantes = 3;

    public LoginController(Stage stage) {
        this.stage = stage;
        cargarAdmin();
    }

    private void cargarAdmin() {
        try {
            this.admin = PersistenciaArchivos.cargarAdmin();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo cargar el administrador", Alert.AlertType.ERROR);
        }
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #5dade2 0%, #8e44ad 100%);");

        VBox centerBox = new VBox();
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPrefHeight(Double.MAX_VALUE);

        VBox loginBox = crearPanelLogin();
        centerBox.getChildren().add(loginBox);

        root.setCenter(centerBox);

        Scene scene = new Scene(root, 900, 700);
        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/icon1.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("No se pudo cargar el icono");
        }

        stage.setScene(scene);
        stage.setTitle("Biblioteca - Inicio de Sesión");
        stage.show();
    }

    private VBox crearPanelLogin() {
        VBox loginBox = new VBox(25);
        loginBox.getStyleClass().add("login-box");
        loginBox.setAlignment(Pos.TOP_CENTER);
        loginBox.setPadding(new Insets(50, 60, 50, 60));
        loginBox.setMaxWidth(520);
        loginBox.setMaxHeight(680);

        ImageView logoIcono = new ImageView();
        logoIcono.getStyleClass().add("login-logo");
        try {
            Image logoImage = new Image(getClass().getResourceAsStream("/resources/icon1.png"));
            logoIcono.setImage(logoImage);
            logoIcono.setFitWidth(90);
            logoIcono.setFitHeight(90);
            logoIcono.setPreserveRatio(true);
        } catch (Exception e) {
            Label fallback = new Label("📚");
            fallback.setFont(Font.font("Arial", 70));
        }

        HBox logoBox = new HBox();
        logoBox.setAlignment(Pos.CENTER);
        logoBox.getChildren().add(logoIcono);

        Label titulo = new Label("BIBLIOTECA");
        titulo.getStyleClass().add("login-title");

        Label subtitulo = new Label("Sistema de Gestión Profesional");
        subtitulo.getStyleClass().add("login-subtitle");

        Region separador = new Region();
        separador.setPrefHeight(40);

        Label lblUsuario = new Label("Usuario");
        lblUsuario.getStyleClass().add("login-label");

        TextField txtUsuario = new TextField();
        txtUsuario.getStyleClass().add("login-field");
        txtUsuario.setPromptText("Ingrese su usuario");
        txtUsuario.setPrefHeight(50);

        Label lblPassword = new Label("Contraseña");
        lblPassword.getStyleClass().add("login-label");

        PasswordField txtPassword = new PasswordField();
        txtPassword.getStyleClass().add("login-field");
        txtPassword.setPromptText("Ingrese su contraseña");
        txtPassword.setPrefHeight(50);

        Label lblError = new Label();
        lblError.getStyleClass().add("login-error");
        lblError.setWrapText(true);
        lblError.setVisible(false);

        Label lblIntentos = new Label("Intentos restantes: " + intentosRestantes);
        lblIntentos.setFont(Font.font("Segoe UI", 12));
        lblIntentos.setTextFill(Color.web("#7f8c8d"));

        Button btnLogin = new Button("Iniciar Sesión");
        btnLogin.getStyleClass().add("login-button");
        btnLogin.setPrefWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(50);
        ;

        javafx.scene.effect.DropShadow sombra = new javafx.scene.effect.DropShadow();
        sombra.setRadius(5.0);
        sombra.setOffsetX(0.0);
        sombra.setOffsetY(3.0);
        sombra.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.2));

        String estiloNormal = "-fx-background-color: linear-gradient(to right, #5dade2 0%, #8e44ad 100%); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 12;";

        String estiloHover = "-fx-background-color: linear-gradient(to right, #4a9dd5 0%, #7d3c98 100%); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 12;";

        btnLogin.setStyle(estiloNormal);
        btnLogin.setEffect(sombra);

        btnLogin.setOnMouseEntered(_ -> {
            btnLogin.setStyle(estiloHover);
            javafx.scene.effect.DropShadow sombraHover = new javafx.scene.effect.DropShadow();
            sombraHover.setRadius(7.0);
            sombraHover.setOffsetX(0.0);
            sombraHover.setOffsetY(4.0);
            sombraHover.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.3));
            btnLogin.setEffect(sombraHover);
        });

        btnLogin.setOnMouseExited(_ -> {
            btnLogin.setStyle(estiloNormal);
            btnLogin.setEffect(sombra);
        });

        btnLogin.setOnMouseEntered(_ -> {
            btnLogin.setStyle(
                    "-fx-background-color: linear-gradient(to right, #4a9dd5 0%, #7d3c98 100%); " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 16px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-padding: 12;");
        });

        btnLogin.setOnMouseExited(_ -> {
            btnLogin.setStyle(
                    "-fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%); " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 16px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-padding: 12;");
        });

        btnLogin.setOnAction(_ -> {
            String usuario = txtUsuario.getText().trim();
            String password = txtPassword.getText().trim();

            lblError.setVisible(false);

            if (usuario.isEmpty() || password.isEmpty()) {
                lblError.setText("⚠️ Por favor complete todos los campos");
                lblError.setVisible(true);
                return;
            }

            if (admin != null && admin.validarCredenciales(usuario, password)) {
                abrirDashboard();
            } else {
                intentosRestantes--;
                lblIntentos.setText("Intentos restantes: " + intentosRestantes);

                if (intentosRestantes > 0) {
                    lblError.setText("❌ Credenciales incorrectas");
                    lblError.setVisible(true);
                    txtPassword.clear();
                } else {
                    lblError.setText("❌ Ha excedido el número de intentos. La aplicación se cerrará.");
                    lblError.setVisible(true);
                    btnLogin.setDisable(true);
                    txtUsuario.setDisable(true);
                    txtPassword.setDisable(true);
                    PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(3));
                    pause.setOnFinished(_ -> stage.close());
                    pause.play();
                }
            }
        });

        txtPassword.setOnAction(_ -> btnLogin.fire());

        Label footer = new Label("© 2025 - Sistema de Gestión de Biblioteca v2.0");
        footer.getStyleClass().add("login-footer");

        loginBox.getChildren().addAll(
                logoBox, titulo, subtitulo, separador,
                lblUsuario, txtUsuario,
                lblPassword, txtPassword,
                lblError, lblIntentos,
                btnLogin,
                new Region(),
                footer);

        return loginBox;
    }

    private void abrirDashboard() {
        DashboardController dashboard = new DashboardController(stage, admin);
        dashboard.show();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        gui.BibliotecaApp.aplicarIconoAAlert(alert);
        alert.showAndWait();
    }

}
