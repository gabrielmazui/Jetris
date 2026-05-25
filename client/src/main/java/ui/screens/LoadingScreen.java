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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;

import network.ConnectionState;
import core.ScreenManager;
import network.NetworkContext;
import network.NetworkManager;
import ui.controllers.LoginController;
import config.UserSession;

public class LoadingScreen implements Screen {

    private final StackPane root;
    private VBox centerContent;
    private Label title;
    private StackPane spinnerContainer;
    private Label status;
    private ScaleTransition pulse;
    private RotateTransition rotate;

    private static final String RETRY_BUTTON_STYLE = """
        -fx-background-color: #00ADB5;
        -fx-text-fill: #0F0F14;
        -fx-font-family: 'Segoe UI';
        -fx-font-weight: bold;
        -fx-font-size: 13px;
        -fx-background-radius: 6;
        -fx-padding: 10 24 10 24;
        -fx-cursor: hand;
    """;

    public LoadingScreen() {
        root = new StackPane();
        root.setStyle("-fx-background-color: #0F0F14;");
        root.setAlignment(Pos.CENTER);
        
        inicializarComponentes();
    }

    private void inicializarComponentes() {
        root.getChildren().clear();

        centerContent = new VBox(35);
        centerContent.setAlignment(Pos.CENTER);

        title = new Label("JETRIS");
        title.setFont(Font.font("Segoe UI Black", 64));
        title.setStyle("""
            -fx-text-fill: linear-gradient(to bottom, #FFFFFF 30%, #A3A3A3 100%);
            -fx-font-weight: 900;
            -fx-letter-spacing: 4px;
            """);

        DropShadow glow = new DropShadow();
        glow.setRadius(25);
        glow.setSpread(0.2);
        glow.setColor(Color.web("#00ADB5", 0.6));
        title.setEffect(glow);

        Circle spinner = new Circle(28);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#00ADB5"));
        spinner.setStrokeWidth(4.5);
        spinner.getStrokeDashArray().addAll(80.0, 50.0);

        rotate = new RotateTransition(Duration.seconds(1.2), spinner);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);
        rotate.play();

        spinnerContainer = new StackPane(spinner);

        status = new Label("CONNECTING TO THE SERVER");
        status.setStyle("""
            -fx-text-fill: #6E6E77;
            -fx-font-family: 'Segoe UI';
            -fx-font-weight: bold;
            -fx-font-size: 11px;
            -fx-letter-spacing: 3px;
        """);

        centerContent.getChildren().addAll(title, spinnerContainer, status);
        root.getChildren().add(centerContent);

        executarCoreografia();
    }

    private void executarCoreografia() {
        title.setOpacity(0.0);
        title.setTranslateY(20.0);
        title.setScaleX(0.9);
        title.setScaleY(0.9);

        spinnerContainer.setOpacity(0.0);
        spinnerContainer.setScaleX(0.6);
        spinnerContainer.setScaleY(0.6);

        status.setOpacity(0.0);

        FadeTransition titleFade = new FadeTransition(Duration.seconds(0.8), title);
        titleFade.setToValue(1.0);

        TranslateTransition titleMove = new TranslateTransition(Duration.seconds(0.9), title);
        titleMove.setToY(0.0);
        titleMove.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition titleScale = new ScaleTransition(Duration.seconds(0.9), title);
        titleScale.setToX(1.0);
        titleScale.setToY(1.0);
        titleScale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition spinnerFade = new FadeTransition(Duration.seconds(0.6), spinnerContainer);
        spinnerFade.setDelay(Duration.seconds(0.3));
        spinnerFade.setToValue(1.0);

        ScaleTransition spinnerScale = new ScaleTransition(Duration.seconds(0.7), spinnerContainer);
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

        pulse = new ScaleTransition(Duration.seconds(2.0), title);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.02);
        pulse.setToY(1.02);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setInterpolator(Interpolator.EASE_BOTH);

        introAnimation.setOnFinished(e -> {
            pulse.play();
            aguardarCarregamento();
        });

        introAnimation.play();
    }

    private void aguardarCarregamento() {
        Thread.startVirtualThread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                    ConnectionState estadoAtual = NetworkContext.tcpState;

                    if (estadoAtual == ConnectionState.CONNECTED) {
                        aguardarAuth();
                        break;
                    } 
                    else if (estadoAtual == ConnectionState.RECONNECTING || estadoAtual == ConnectionState.CONNECTING) {
                        continue;
                    } 
                    else {
                        Platform.runLater(this::exibirErroConexao);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    private void aguardarAuth(){
        Thread.startVirtualThread(() ->{
            UserSession.carregarDoArquivo();
            Platform.runLater(this::<LoginScreen>transicaoParaProximaTela);
        });
    }

    private <T> void transicaoParaProximaTela() {
        pulse.stop();
        rotate.stop();

        FadeTransition titleFadeOut = new FadeTransition(Duration.seconds(0.4), title);
        titleFadeOut.setToValue(0.0);

        ScaleTransition titleScaleOut = new ScaleTransition(Duration.seconds(0.4), title);
        titleScaleOut.setToX(1.1);
        titleScaleOut.setToY(1.1);
        titleScaleOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition spinnerFadeOut = new FadeTransition(Duration.seconds(0.35), spinnerContainer);
        spinnerFadeOut.setToValue(0.0);

        ScaleTransition spinnerScaleOut = new ScaleTransition(Duration.seconds(0.35), spinnerContainer);
        spinnerScaleOut.setToX(0.4);
        spinnerScaleOut.setToY(0.4);
        spinnerScaleOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition statusFadeOut = new FadeTransition(Duration.seconds(0.3), status);
        statusFadeOut.setToValue(0.0);

        ParallelTransition outroAnimation = new ParallelTransition(
            titleFadeOut, titleScaleOut,
            spinnerFadeOut, spinnerScaleOut,
            statusFadeOut
        );

        
        // if(LoginController.verifyTokenCache()){
        //     // pula para a home screen
        //     // outroAnimation.setOnFinished(evt -> ScreenManager.setScreen(new HomeScreen()));
        //     // outroAnimation.play();
        // }else{
        //     outroAnimation.setOnFinished(evt -> ScreenManager.setScreen(new LoginScreen()));
        //     outroAnimation.play();
        // }
        
    }

    private void exibirErroConexao() {
        rotate.stop();

        FadeTransition spinnerFadeOut = new FadeTransition(Duration.seconds(0.3), spinnerContainer);
        spinnerFadeOut.setToValue(0.0);

        FadeTransition statusFadeOut = new FadeTransition(Duration.seconds(0.3), status);
        statusFadeOut.setToValue(0.0);

        ParallelTransition fadeOutAntigos = new ParallelTransition(spinnerFadeOut, statusFadeOut);
        
        fadeOutAntigos.setOnFinished(e -> {
            centerContent.getChildren().removeAll(spinnerContainer, status);

            Label errorLabel = new Label("COULD NOT CONNECT TO THE SERVER");
            errorLabel.setStyle("""
                -fx-text-fill: #FF4A4A;
                -fx-font-family: 'Segoe UI';
                -fx-font-weight: bold;
                -fx-font-size: 12px;
                -fx-letter-spacing: 2px;
            """);
            errorLabel.setOpacity(0.0);

            Button retryButton = new Button("TRY AGAIN");
            retryButton.setStyle(RETRY_BUTTON_STYLE);
            retryButton.setOpacity(0.0);
            
            retryButton.setOnMouseEntered(evt -> retryButton.setStyle(RETRY_BUTTON_STYLE + "-fx-background-color: #00cfda;"));
            retryButton.setOnMouseExited(evt -> retryButton.setStyle(RETRY_BUTTON_STYLE));
            retryButton.setOnAction(evt -> {
                pulse.stop();
                NetworkManager.retryConnection();
                inicializarComponentes();
            });

            centerContent.getChildren().addAll(errorLabel, retryButton);

            FadeTransition errorFadeIn = new FadeTransition(Duration.seconds(0.4), errorLabel);
            errorFadeIn.setToValue(1.0);

            FadeTransition buttonFadeIn = new FadeTransition(Duration.seconds(0.4), retryButton);
            buttonFadeIn.setToValue(1.0);

            new ParallelTransition(errorFadeIn, buttonFadeIn).play();
        });

        fadeOutAntigos.play();
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}