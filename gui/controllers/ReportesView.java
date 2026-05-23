package gui.controllers;

import controlador.GestorLibros;
import controlador.GestorUsuarios;
import controlador.GestorPrestamos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import utilidades.GeneradorReportes;

public class ReportesView extends VBox {

    private GeneradorReportes generadorReportes;
    private TextArea areaReporte;

    public ReportesView(GestorLibros gestorLibros, GestorUsuarios gestorUsuarios, GestorPrestamos gestorPrestamos) {
        this.generadorReportes = new GeneradorReportes(gestorLibros, gestorUsuarios, gestorPrestamos);
        inicializar();
    }

    private void inicializar() {
        this.setPadding(new Insets(24));
        this.setSpacing(20);

        Label titulo = new Label("📊 Reportes del Sistema");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        titulo.setTextFill(Color.web("#2c3e50"));

        areaReporte = new TextArea();
        areaReporte.setEditable(false);
        areaReporte.setFont(Font.font("Consolas", 13));
        areaReporte.setStyle(
                "-fx-background-color: #fafbfc; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 16;");
        VBox.setVgrow(areaReporte, Priority.ALWAYS);
        HBox panelBotones = crearPanelBotones();

        this.getChildren().addAll(titulo, panelBotones, areaReporte);
    }

    private HBox crearPanelBotones() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(16));
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        Button btnGeneral = crearBoton("📋 Reporte General", "#5dade2");
        Button btnLibros = crearBoton("📚 Reporte de Libros", "#52c98f");
        Button btnUsuarios = crearBoton("👥 Reporte de Usuarios", "#ec7063");
        Button btnPrestamos = crearBoton("📖 Reporte de Préstamos", "#f8b739");
        Button btnTop5 = crearBoton("🏆 Top 5 Usuarios", "#9b7ab8");
        Button btnMultas = crearBoton("💰 Estado Financiero", "#e67e22");
        Button btnCompleto = crearBoton("📊 Reporte Completo", "#34495e");
        Button btnLimpiar = crearBoton("🗑️ Limpiar", "#95a5a6");

        btnGeneral.setOnAction(_ -> mostrarReporteGeneral());
        btnLibros.setOnAction(_ -> mostrarReporteLibros());
        btnUsuarios.setOnAction(_ -> mostrarReporteUsuarios());
        btnPrestamos.setOnAction(_ -> mostrarReportePrestamos());
        btnTop5.setOnAction(_ -> mostrarTop5Usuarios());
        btnMultas.setOnAction(_ -> mostrarEstadoFinanciero());
        btnCompleto.setOnAction(_ -> mostrarReporteCompleto());
        btnLimpiar.setOnAction(_ -> limpiarReporte());

        panel.getChildren().addAll(
                btnGeneral, btnLibros, btnUsuarios, btnPrestamos,
                btnTop5, btnMultas, btnCompleto, btnLimpiar);

        return panel;
    }

    private Button crearBoton(String texto, String color) {
        Button btn = new Button(texto);
        String colorHover = calcularColorOscurecido(color, 0.85);

        javafx.scene.effect.DropShadow sombra = new javafx.scene.effect.DropShadow();
        sombra.setRadius(5.0);
        sombra.setOffsetX(0.0);
        sombra.setOffsetY(3.0);
        sombra.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.2));

        String estiloBase = String.format(
                "-fx-background-color: %s; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 12 20; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-family: 'Segoe UI'; " +
                        "-fx-cursor: hand;",
                color);

        btn.setStyle(estiloBase);
        btn.setEffect(sombra);

        btn.setOnMouseEntered(_ -> {
            btn.setStyle(estiloBase.replace(color, colorHover));
            javafx.scene.effect.DropShadow sombraHover = new javafx.scene.effect.DropShadow();
            sombraHover.setRadius(8.0);
            sombraHover.setOffsetX(0.0);
            sombraHover.setOffsetY(4.0);
            sombraHover.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.3));
            btn.setEffect(sombraHover);
        });

        btn.setOnMouseExited(_ -> {
            btn.setStyle(estiloBase);
            btn.setEffect(sombra);
        });

        return btn;
    }

    private String calcularColorOscurecido(String hexColor, double factor) {
        hexColor = hexColor.replace("#", "");
        int r = Integer.parseInt(hexColor.substring(0, 2), 16);
        int g = Integer.parseInt(hexColor.substring(2, 4), 16);
        int b = Integer.parseInt(hexColor.substring(4, 6), 16);

        r = (int) (r * factor);
        g = (int) (g * factor);
        b = (int) (b * factor);

        return String.format("#%02x%02x%02x", r, g, b);
    }

    private void mostrarReporteGeneral() {
        areaReporte.clear();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        java.io.PrintStream old = System.out;
        System.setOut(ps);

        generadorReportes.generarReporteGeneral();

        System.out.flush();
        System.setOut(old);

        areaReporte.setText(baos.toString());
    }

    private void mostrarReporteLibros() {
        areaReporte.clear();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        java.io.PrintStream old = System.out;
        System.setOut(ps);

        generadorReportes.generarReporteLibros();

        System.out.flush();
        System.setOut(old);

        areaReporte.setText(baos.toString());
    }

    private void mostrarReporteUsuarios() {
        areaReporte.clear();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        java.io.PrintStream old = System.out;
        System.setOut(ps);

        generadorReportes.generarReporteUsuarios();

        System.out.flush();
        System.setOut(old);

        areaReporte.setText(baos.toString());
    }

    private void mostrarReportePrestamos() {
        areaReporte.clear();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        java.io.PrintStream old = System.out;
        System.setOut(ps);

        generadorReportes.generarReportePrestamos();

        System.out.flush();
        System.setOut(old);

        areaReporte.setText(baos.toString());
    }

    private void mostrarTop5Usuarios() {
        areaReporte.clear();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        java.io.PrintStream old = System.out;
        System.setOut(ps);

        generadorReportes.mostrarTop5UsuariosActivos();

        System.out.flush();
        System.setOut(old);

        areaReporte.setText(baos.toString());
    }

    private void mostrarEstadoFinanciero() {
        areaReporte.clear();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        java.io.PrintStream old = System.out;
        System.setOut(ps);

        generadorReportes.mostrarEstadoFinanciero();

        System.out.flush();
        System.setOut(old);

        areaReporte.setText(baos.toString());
    }

    private void mostrarReporteCompleto() {
        areaReporte.clear();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        java.io.PrintStream old = System.out;
        System.setOut(ps);

        generadorReportes.generarReporteCompleto();

        System.out.flush();
        System.setOut(old);

        areaReporte.setText(baos.toString());
    }

    private void limpiarReporte() {
        areaReporte.clear();
        areaReporte.setPromptText("Seleccione un tipo de reporte para visualizar...");
    }
}
