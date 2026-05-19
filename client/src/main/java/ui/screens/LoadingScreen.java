package ui.screens;

import core.ScreenManager;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class LoadingScreen implements Screen {

    private final VBox root;

    public LoadingScreen() {
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
        root.setStyle("-fx-background-color: #18181F;"); // Mantém o fundo dark elegante do app
        root.getChildren().addAll(centerContent);

        aplicarAnimacoes(title, spinnerContainer, status);
        simularCarregamento();
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
                // Apenas um aviso: 15.000.000ms são mais de 4 horas de loading! 
                // Ajuste esse valor depois para testes rápidos (ex: 3000 para 3 segundos)
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