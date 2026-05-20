package ui.screens;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;

import core.ScreenManager;

public class LoadingScreen implements Screen {

    private final StackPane root; 

    public LoadingScreen() {
        // --- CONTEÚDO CENTRAL ---
        VBox centerContent = new VBox(35); 
        centerContent.setAlignment(Pos.CENTER);

        Label title = new Label("JETRIS");
        title.setFont(Font.font("Segoe UI Black", 64)); 
        title.setStyle("""
            -fx-text-fill: linear-gradient(to bottom, #FFFFFF 30%, #A3A3A3 100%);
            -fx-font-weight: 900;
            -fx-letter-spacing: 4px;
            """);

        // Brilho neon azul marcante
        DropShadow glow = new DropShadow();
        glow.setRadius(25);
        glow.setSpread(0.2);
        glow.setColor(Color.web("#00ADB5", 0.6));
        title.setEffect(glow);

        // Spinner customizado
        Circle spinner = new Circle(28);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#00ADB5"));
        spinner.setStrokeWidth(4.5);
        spinner.getStrokeDashArray().addAll(80.0, 50.0);

        // Rotação contínua linear perfeita
        RotateTransition rotate = new RotateTransition(Duration.seconds(1.2), spinner);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);
        rotate.play();

        StackPane spinnerContainer = new StackPane(spinner);

        Label status = new Label("CONNECTING TO SERVERS");
        status.setStyle("""
            -fx-text-fill: #6E6E77;
            -fx-font-family: 'Segoe UI';
            -fx-font-weight: bold;
            -fx-font-size: 11px;
            -fx-letter-spacing: 3px;
        """);

        centerContent.getChildren().addAll(title, spinnerContainer, status);

        // --- ROOT CONTAINER ---
        root = new StackPane(centerContent);
        root.setStyle("-fx-background-color: #0F0F14;"); // Fundo dark elegante e fixo

        // Dispara a animação passando os elementos e o container interno
        executarCoreografia(title, spinnerContainer, status, centerContent);
    }

    private void executarCoreografia(Label title, StackPane spinner, Label status, VBox centerContent) {
        // Estado inicial (Elementos escondidos e preparados para a entrada)
        title.setOpacity(0.0);
        title.setTranslateY(20.0);
        title.setScaleX(0.9);
        title.setScaleY(0.9);

        spinner.setOpacity(0.0);
        spinner.setScaleX(0.6);
        spinner.setScaleY(0.6);

        status.setOpacity(0.0);

        // --- 1. COREOGRAFIA DE ENTRADA ---
        FadeTransition titleFade = new FadeTransition(Duration.seconds(0.8), title);
        titleFade.setToValue(1.0);
        
        TranslateTransition titleMove = new TranslateTransition(Duration.seconds(0.9), title);
        titleMove.setToY(0.0);
        titleMove.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition titleScale = new ScaleTransition(Duration.seconds(0.9), title);
        titleScale.setToX(1.0);
        titleScale.setToY(1.0);
        titleScale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition spinnerFade = new FadeTransition(Duration.seconds(0.6), spinner);
        spinnerFade.setDelay(Duration.seconds(0.3));
        spinnerFade.setToValue(1.0);

        ScaleTransition spinnerScale = new ScaleTransition(Duration.seconds(0.7), spinner);
        spinnerScale.setDelay(Duration.seconds(0.3));
        spinnerScale.setToX(1.0);
        spinnerScale.setToY(1.0);
        spinnerScale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition statusFade = new FadeTransition(Duration.seconds(0.5), status);
        statusFade.setDelay(Duration.seconds(0.6));
        statusFade.setToValue(1.0);

        ParallelTransition introAnimation = new ParallelTransition(
            titleFade, titleMove, titleScale, 
            spinnerFade, spinnerScale, 
            statusFade
        );

        // --- 2. ANIMAÇÃO DE PULSO (LOOP) ---
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2.0), title);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.02);
        pulse.setToY(1.02);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setInterpolator(Interpolator.EASE_BOTH);

        introAnimation.setOnFinished(e -> {
            pulse.play();
            aguardarCarregamento(title, spinner, status, pulse, centerContent);
        });

        introAnimation.play();
    }

    private void aguardarCarregamento(Label title, StackPane spinner, Label status, ScaleTransition pulse, VBox centerContent) {
        Thread.startVirtualThread(() -> {
            try {
                // Mantém os 3 segundos que você queria de loading
                Thread.sleep(3000); 

                Platform.runLater(() -> {
                    pulse.stop();

                    // --- 3. COREOGRAFIA DE SAÍDA (EFEITO PREMIUM) ---
                    // O título expande sutilmente sumindo no escuro
                    FadeTransition titleFadeOut = new FadeTransition(Duration.seconds(0.4), title);
                    titleFadeOut.setToValue(0.0);
                    
                    ScaleTransition titleScaleOut = new ScaleTransition(Duration.seconds(0.4), title);
                    titleScaleOut.setToX(1.1);
                    titleScaleOut.setToY(1.1);
                    titleScaleOut.setInterpolator(Interpolator.EASE_IN);

                    // O spinner encolhe rapidamente para o centro
                    FadeTransition spinnerFadeOut = new FadeTransition(Duration.seconds(0.35), spinner);
                    spinnerFadeOut.setToValue(0.0);
                    
                    ScaleTransition spinnerScaleOut = new ScaleTransition(Duration.seconds(0.35), spinner);
                    spinnerScaleOut.setToX(0.4);
                    spinnerScaleOut.setToY(0.4);
                    spinnerScaleOut.setInterpolator(Interpolator.EASE_IN);

                    // O status apaga suavemente
                    FadeTransition statusFadeOut = new FadeTransition(Duration.seconds(0.3), status);
                    statusFadeOut.setToValue(0.0);

                    ParallelTransition outroAnimation = new ParallelTransition(
                        titleFadeOut, titleScaleOut,
                        spinnerFadeOut, spinnerScaleOut,
                        statusFadeOut
                    );

                    // Quando todos os elementos sumirem no fundo preto, fazemos o switch
                    outroAnimation.setOnFinished(evt -> {
                        ScreenManager.setScreen(new LoginScreen()); 
                    });

                    outroAnimation.play();
                });
            } catch (InterruptedException ignored) {}
        });
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}