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
import modelo.Admin;
import seguridad.Autenticacion;


public class LoginController {

    private Stage stage;
    private Admin admin;

    public LoginController(Stage stage) {
        this.stage = stage;

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
        scene.getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
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
        VBox loginBox = new VBox(12);
        loginBox.getStyleClass().add("login-box");
        loginBox.setAlignment(Pos.TOP_CENTER);
        loginBox.setPadding(new Insets(24, 40, 24, 40));
        loginBox.setMaxWidth(520);
        loginBox.setMaxHeight(680);

        ImageView logoIcono = new ImageView();
        logoIcono.getStyleClass().add("login-logo");
        try {
            Image logoImage = new Image(getClass().getResourceAsStream("/resources/icon1.png"));
            logoIcono.setImage(logoImage);
            logoIcono.setFitWidth(72);
            logoIcono.setFitHeight(72);
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
        separador.setPrefHeight(8);

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

        Label lblIntentos = new Label("Cinco intentos fallidos bloquean el acceso durante 15 minutos");
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
            String password = txtPassword.getText();

            lblError.setVisible(false);

            if (usuario.isEmpty() || password.isEmpty()) {
                lblError.setText("⚠️ Por favor complete todos los campos");
                lblError.setVisible(true);
                return;
            }

            try {
                admin=gui.TareasBD.invocar(()->{
                    seguridad.Sesion sesion=new Autenticacion().ingresar(usuario,password.toCharArray());
                    gui.BibliotecaApp.abrir(sesion);return sesion.administrador();
                });
                txtPassword.clear();abrirDashboard();
            }catch(Exception e){
                txtPassword.clear();lblError.setText(e.getMessage());lblError.setVisible(true);
            }
        });

        txtPassword.setOnAction(_ -> btnLogin.fire());

        Label footer = new Label("Sistema de Gestión de Biblioteca · MySQL");
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



}
