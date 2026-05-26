package ui.screens;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.scene.control.TextFormatter;
import java.util.function.UnaryOperator;

import core.ScreenManager;
import ui.controllers.LoginController;

public class LoginScreen implements Screen {

    private final StackPane root;
    private VBox loginBox;
    private VBox registerBox;

    private Label loginErrorLabel;
    private Label registerErrorLabel;

    private static final String INPUT_STYLE = """
        -fx-background-color: #1E1E26;
        -fx-text-fill: #FFFFFF;
        -fx-highlight-fill: #00ADB5;
        -fx-prompt-text-fill: #5C5C64;
        -fx-background-radius: 6;
        -fx-border-radius: 6;
        -fx-border-color: #2E2E38;
        -fx-border-width: 1;
        -fx-padding: 12 16 12 16;
        -fx-font-family: 'Segoe UI';
        -fx-font-size: 14px;
    """;

    private static final String PRIMARY_BUTTON_STYLE = """
        -fx-background-color: #00ADB5;
        -fx-text-fill: #0F0F14;
        -fx-font-family: 'Segoe UI';
        -fx-font-weight: bold;
        -fx-font-size: 14px;
        -fx-background-radius: 6;
        -fx-padding: 12;
        -fx-cursor: hand;
    """;

    private static final String LINK_BUTTON_STYLE = """
        -fx-background-color: transparent;
        -fx-text-fill: #00ADB5;
        -fx-font-family: 'Segoe UI';
        -fx-font-weight: bold;
        -fx-font-size: 13px;
        -fx-cursor: hand;
        -fx-padding: 0;
    """;

    private static final String ERROR_LABEL_STYLE = """
        -fx-text-fill: #FF4A4A;
        -fx-font-family: 'Segoe UI';
        -fx-font-weight: bold;
        -fx-font-size: 12px;
        -fx-alignment: center;
    """;

    public LoginScreen() {
        ScreenManager.controller = new LoginController();
        root = new StackPane();
        root.setStyle("-fx-background-color: #0F0F14;");
        root.setAlignment(Pos.CENTER);

        inicializarLoginBox();
        inicializarRegisterBox();

        root.getChildren().addAll(registerBox, loginBox);
        executarAnimacaoEntrada();

    }

    private void inicializarLoginBox() {
        UnaryOperator<TextFormatter.Change> usernameFilter = change -> {
            if (change.getControlNewText().matches("^[a-zA-Z0-9_]*$")) {
                return change;
            }
            return null;
        };

        UnaryOperator<TextFormatter.Change> passwordFilter = change -> {
            if (!change.getControlNewText().contains(" ")) {
                return change;
            }
            return null;
        };

        loginBox = new VBox(22);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setMaxWidth(360);
        loginBox.setStyle("-fx-padding: 40;");

        Label title = new Label("WELCOME BACK");
        title.setFont(Font.font("Segoe UI", 28));
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 800; -fx-letter-spacing: 2px;");
        title.setEffect(new DropShadow(15, Color.web("#00ADB5", 0.4)));

        TextField usernameInput = new TextField();
        usernameInput.setPromptText("User");
        usernameInput.setStyle(INPUT_STYLE);

        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("Password");
        passwordInput.setStyle(INPUT_STYLE);

        usernameInput.setTextFormatter(new TextFormatter<>(usernameFilter));
        passwordInput.setTextFormatter(new TextFormatter<>(passwordFilter));

        loginErrorLabel = new Label("");
        loginErrorLabel.setStyle(ERROR_LABEL_STYLE);
        loginErrorLabel.setWrapText(true);
        loginErrorLabel.setMaxWidth(280);
        loginErrorLabel.setManaged(false);
        loginErrorLabel.setVisible(false);

        Button loginButton = new Button("SIGN IN");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle(PRIMARY_BUTTON_STYLE);
        
        loginButton.setOnMouseEntered(e -> loginButton.setStyle(PRIMARY_BUTTON_STYLE + "-fx-background-color: #00cfda;"));
        loginButton.setOnMouseExited(e -> loginButton.setStyle(PRIMARY_BUTTON_STYLE));
        
        loginButton.setOnAction(e -> fazerLogin(usernameInput.getText(), passwordInput.getText()));

        Label noAccountLabel = new Label("Don't have an account?");
        noAccountLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");

        Button btnIrParaRegistro = new Button("Create one");
        btnIrParaRegistro.setStyle(LINK_BUTTON_STYLE);
        btnIrParaRegistro.setOnAction(e -> alternarParaRegistro());

        HBox footer = new HBox(6, noAccountLabel, btnIrParaRegistro);
        footer.setAlignment(Pos.CENTER);

        loginBox.getChildren().addAll(title, usernameInput, passwordInput, loginErrorLabel, loginButton, footer);
    }

    private void inicializarRegisterBox() {

        UnaryOperator<TextFormatter.Change> usernameFilter = change -> {
            if (change.getControlNewText().matches("^[a-zA-Z0-9_]*$")) {
                return change;
            }
            return null;
        };

        UnaryOperator<TextFormatter.Change> passwordFilter = change -> {
            if (!change.getControlNewText().contains(" ")) {
                return change;
            }
            return null;
        };

        registerBox = new VBox(22);
        registerBox.setAlignment(Pos.CENTER);
        registerBox.setMaxWidth(360);
        registerBox.setStyle("-fx-padding: 40;");
        
        registerBox.setOpacity(0.0);
        registerBox.setTranslateX(400.0); 
        registerBox.setVisible(false);

        Label title = new Label("CREATE ACCOUNT");
        title.setFont(Font.font("Segoe UI", 28));
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 800; -fx-letter-spacing: 2px;");

        TextField regUsername = new TextField();
        regUsername.setPromptText("User");
        regUsername.setStyle(INPUT_STYLE);

        PasswordField regPassword = new PasswordField();
        regPassword.setPromptText("Password");
        regPassword.setStyle(INPUT_STYLE);

        regUsername.setTextFormatter(new TextFormatter<>(usernameFilter));
        regPassword.setTextFormatter(new TextFormatter<>(passwordFilter));

        registerErrorLabel = new Label("");
        registerErrorLabel.setStyle(ERROR_LABEL_STYLE);
        registerErrorLabel.setWrapText(true);
        registerErrorLabel.setMaxWidth(280);
        registerErrorLabel.setManaged(false);
        registerErrorLabel.setVisible(false);

        Button registerButton = new Button("SIGN UP");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setStyle(PRIMARY_BUTTON_STYLE);
        
        registerButton.setOnMouseEntered(e -> registerButton.setStyle(PRIMARY_BUTTON_STYLE + "-fx-background-color: #00cfda;"));
        registerButton.setOnMouseExited(e -> registerButton.setStyle(PRIMARY_BUTTON_STYLE));
        
        registerButton.setOnAction(e -> criarConta(regUsername.getText(), regPassword.getText()));

        Label hasAccountLabel = new Label("Already have an account?");
        hasAccountLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");

        Button btnIrParaLogin = new Button("Sign in");
        btnIrParaLogin.setStyle(LINK_BUTTON_STYLE);
        btnIrParaLogin.setOnAction(e -> alternarParaLogin());

        HBox footer = new HBox(6, hasAccountLabel, btnIrParaLogin);
        footer.setAlignment(Pos.CENTER);

        registerBox.getChildren().addAll(title, regUsername, regPassword, registerErrorLabel, registerButton, footer);
    }

    private void dispararFeedbackErro(Label targetLabel, VBox targetBox, String mensagem) {
        Platform.runLater(() -> {
            targetLabel.setText(mensagem.toUpperCase());
            targetLabel.setManaged(true);
            targetLabel.setVisible(true);

            TranslateTransition t1 = new TranslateTransition(Duration.millis(50), targetBox);
            t1.setByX(-10);
            TranslateTransition t2 = new TranslateTransition(Duration.millis(50), targetBox);
            t2.setByX(20);
            TranslateTransition t3 = new TranslateTransition(Duration.millis(50), targetBox);
            t3.setByX(-20);
            TranslateTransition t4 = new TranslateTransition(Duration.millis(50), targetBox);
            t4.setByX(10);

            new SequentialTransition(t1, t2, t3, t4).play();
        });
    }

    private void limparErros() {
        loginErrorLabel.setVisible(false);
        loginErrorLabel.setManaged(false);
        registerErrorLabel.setVisible(false);
        registerErrorLabel.setManaged(false);
    }

    private void setInterfaceBloqueada(boolean bloqueado, VBox box) {
        box.setDisable(bloqueado);
    }

    private void animarTransicaoParaMainScreen() {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), root);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition scaleOut = new ScaleTransition(Duration.seconds(0.5), root);
        scaleOut.setToX(1.1);
        scaleOut.setToY(1.1);
        scaleOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition transicao = new ParallelTransition(fadeOut, scaleOut);
        transicao.setOnFinished(e -> {
            ScreenManager.setScreen(new MainScreen()); 
        });
        transicao.play();
    }

    private void fazerLogin(String username, String password) {
        limparErros();

        if (password == null || password.isEmpty()) {
            return;
        }

        if (username == null || username.trim().length() < 6) {
            dispararFeedbackErro(loginErrorLabel, loginBox, "Username must be at least 6 characters long");
            return;
        }

        setInterfaceBloqueada(true, loginBox);

        Thread.startVirtualThread(() -> {
            String ans = LoginController.login(username, password); 
            
            Platform.runLater(() -> {
                if ("SUCCESS".equals(ans)) {
                    animarTransicaoParaMainScreen();
                } else {
                    setInterfaceBloqueada(false, loginBox);
                    dispararFeedbackErro(loginErrorLabel, loginBox, ans);
                }
            });
        });
    }

    private void criarConta(String username, String password) {
        limparErros();

        if (username == null || username.length() < 5 || username.length() > 20) {
            dispararFeedbackErro(registerErrorLabel, registerBox, "Username must be between 5 and 20 characters");
            return;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            dispararFeedbackErro(registerErrorLabel, registerBox, "Username can only contain letters, numbers, and underscores");
            return;
        }

        if (password == null || password.length() < 6 || password.length() > 30) {
            dispararFeedbackErro(registerErrorLabel, registerBox, "Password must be between 6 and 30 characters");
            return;
        }

        if (password.contains(" ")) {
            dispararFeedbackErro(registerErrorLabel, registerBox, "Password cannot contain spaces");
            return;
        }

        setInterfaceBloqueada(true, registerBox);

        Thread.startVirtualThread(() -> {
            String ans = LoginController.register(username, password);
            
            Platform.runLater(() -> {
                if ("SUCCESS".equals(ans)) {
                    setInterfaceBloqueada(false, registerBox);
                    alternarParaLogin();
                } else {
                    setInterfaceBloqueada(false, registerBox);
                    dispararFeedbackErro(registerErrorLabel, registerBox, ans);
                }
            });
        });
    }

    public void alternarParaRegistro() {
        limparErros();
        registerBox.setVisible(true);

        FadeTransition loginFade = new FadeTransition(Duration.seconds(0.4), loginBox);
        loginFade.setToValue(0.0);
        TranslateTransition loginMove = new TranslateTransition(Duration.seconds(0.4), loginBox);
        loginMove.setToX(-400.0);
        loginMove.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition regFade = new FadeTransition(Duration.seconds(0.4), registerBox);
        regFade.setToValue(1.0);
        TranslateTransition regMove = new TranslateTransition(Duration.seconds(0.4), registerBox);
        regMove.setToX(0.0);
        regMove.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition transicao = new ParallelTransition(loginFade, loginMove, regFade, regMove);
        transicao.setOnFinished(e -> loginBox.setVisible(false));
        transicao.play();
    }

    public void alternarParaLogin() {
        limparErros();
        loginBox.setVisible(true);

        FadeTransition regFade = new FadeTransition(Duration.seconds(0.4), registerBox);
        regFade.setToValue(0.0);
        TranslateTransition regMove = new TranslateTransition(Duration.seconds(0.4), registerBox);
        regMove.setToX(400.0);
        regMove.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition loginFade = new FadeTransition(Duration.seconds(0.4), loginBox);
        loginFade.setToValue(1.0);
        TranslateTransition loginMove = new TranslateTransition(Duration.seconds(0.4), loginBox);
        loginMove.setToX(0.0);
        loginMove.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition transicao = new ParallelTransition(regFade, regMove, loginFade, loginMove);
        transicao.setOnFinished(e -> registerBox.setVisible(false));
        transicao.play();
    }

    private void executarAnimacaoEntrada() {
        loginBox.setOpacity(0.0);
        loginBox.setTranslateY(15.0);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.6), loginBox);
        fadeIn.setToValue(1.0);

        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(0.6), loginBox);
        moveUp.setToY(0.0);
        moveUp.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fadeIn, moveUp).play();
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}