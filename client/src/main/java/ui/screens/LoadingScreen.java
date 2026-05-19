package ui.screens;

import core.ScreenManager;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class LoadingScreen implements Screen {

    private final VBox root;

    public LoadingScreen() {
        // --- BARRA DE TÍTULO ULTRA-MODERNA (Estilo Windows 11 / Fluent Dark) ---
        HBox windowBar = new HBox(5);
        windowBar.setAlignment(Pos.CENTER_RIGHT);
        windowBar.setPadding(new Insets(6, 12, 6, 16));
        windowBar.setStyle("-fx-background-color: #111116;"); // Header mais escuro integrado

        Label gameTitle = new Label("Jetris");
        gameTitle.setStyle("""
            -fx-text-fill: #8A8A93;
            -fx-font-family: 'Segoe UI', system-ui;
            -fx-font-size: 13px;
            -fx-font-weight: bold;
        """);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Botões minimalistas modernos (Estilo Windows Moderno)
        Button btnMin = criarBotaoControle("—", "#2A2A35", "#FFFFFF");
        Button btnMax = criarBotaoControle("🗖", "#2A2A35", "#FFFFFF");
        Button btnClose = criarBotaoControle("✕", "#E81123", "#FFFFFF");

        btnMin.setOnAction(e -> ScreenManager.minimizar());
        btnMax.setOnAction(e -> ScreenManager.alternarTelaCheia());
        btnClose.setOnAction(e -> ScreenManager.fechar());

        windowBar.getChildren().addAll(gameTitle, spacer, btnMin, btnMax, btnClose);
        ScreenManager.tornarArrastavel(windowBar);

        // --- CONTEÚDO CENTRAL ---
        VBox centerContent = new VBox(30);
        centerContent.setAlignment(Pos.CENTER);
        VBox.setVgrow(centerContent, Priority.ALWAYS);

        Label title = new Label("JETRIS");
        title.setFont(Font.font("Segoe UI Black", 56));
        title.setStyle("""
            -fx-text-fill: linear-gradient(to bottom, #FFFFFF, #D6D6D6);
            -fx-font-weight: 900;
            -fx-letter-spacing: 10px;
        """);

        DropShadow softShadow = new DropShadow();
        softShadow.setRadius(20);
        softShadow.setSpread(0.15);
        softShadow.setColor(Color.rgb(0, 173, 181, 0.45));
        title.setEffect(softShadow);

        // Spinner customizado
        Circle spinner = new Circle(26);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#00ADB5"));
        spinner.setStrokeWidth(4);
        spinner.getStrokeDashArray().addAll(70.0, 45.0);

        RotateTransition rotate = new RotateTransition(Duration.seconds(1.4), spinner);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.play();

        StackPane spinnerContainer = new StackPane(spinner);

        Label status = new Label("Connecting to the servers...");
        status.setStyle("""
            -fx-text-fill: #9A9AA3;
            -fx-font-family: 'Segoe UI';
            -fx-font-size: 14px;
            -fx-letter-spacing: 1px;
        """);

        centerContent.getChildren().addAll(title, spinnerContainer, status);

        // --- ROOT CONTAINER ---
        root = new VBox();
        root.setStyle("-fx-background-color: #18181F;"); // Fundo dark do app
        root.getChildren().addAll(windowBar, centerContent);

        aplicarAnimacoes(title, spinnerContainer, status);
        simularCarregamento();
    }

    private Button criarBotaoControle(String texto, String hoverBg, String hoverText) {
        Button btn = new Button(texto);
        
        String estiloNormal = """
            -fx-background-color: transparent;
            -fx-text-fill: #A0A0A8;
            -fx-font-size: 11px;
            -fx-font-family: 'Segoe UI', Symbol;
            -fx-min-width: 46px;
            -fx-min-height: 32px;
            -fx-background-radius: 0; 
        """;

        String estiloHover = """
            -fx-background-color: %s;
            -fx-text-fill: %s;
            -fx-font-size: 11px;
            -fx-font-family: 'Segoe UI', Symbol;
            -fx-min-width: 46px;
            -fx-min-height: 32px;
            -fx-background-radius: 0;
        """.formatted(hoverBg, hoverText);

        btn.setStyle(estiloNormal);
        btn.setOnMouseEntered(e -> btn.setStyle(estiloHover));
        btn.setOnMouseExited(e -> btn.setStyle(estiloNormal));
        return btn;
    }

    private void aplicarAnimacoes(Label title, StackPane spinner, Label status) {
        title.setOpacity(0);
        spinner.setOpacity(0);
        status.setOpacity(0);

        FadeTransition titleFade = new FadeTransition(Duration.seconds(1.4), title);
        titleFade.setFromValue(0);
        titleFade.setToValue(1);

        ScaleTransition titleScale = new ScaleTransition(Duration.seconds(1.6), title);
        titleScale.setFromX(0.96);
        titleScale.setFromY(0.96);
        titleScale.setToX(1);
        titleScale.setToY(1);

        FadeTransition spinnerFade = new FadeTransition(Duration.seconds(1), spinner);
        spinnerFade.setDelay(Duration.seconds(0.7));
        spinnerFade.setFromValue(0);
        spinnerFade.setToValue(1);

        FadeTransition statusFade = new FadeTransition(Duration.seconds(1), status);
        statusFade.setDelay(Duration.seconds(1));
        statusFade.setFromValue(0);
        statusFade.setToValue(1);

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2.2), title);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.015);
        pulse.setToY(1.015);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);

        ParallelTransition intro = new ParallelTransition(titleFade, titleScale, spinnerFade, statusFade);
        intro.setOnFinished(e -> pulse.play());
        intro.play();
    }

    private void simularCarregamento() {
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(15000000);
                Platform.runLater(() -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.8), root);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> System.out.println("Servidor conectado!"));
                    fadeOut.play();
                });
            } catch (InterruptedException ignored) {}
        });
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}