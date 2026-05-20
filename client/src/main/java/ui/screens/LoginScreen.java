package ui.screens;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
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

public class LoginScreen implements Screen {

    private final StackPane root;
    private VBox loginBox;
    private VBox registerBox;
    
    // Labels para exibição de mensagens de erro
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
        root = new StackPane();
        root.setStyle("-fx-background-color: #0F0F14;");
        root.setAlignment(Pos.CENTER);

        inicializarLoginBox();
        inicializarRegisterBox();

        root.getChildren().addAll(registerBox, loginBox);
        executarAnimacaoEntrada();
    }

    private void inicializarLoginBox() {
        loginBox = new VBox(22); // Reduzi levemente o espaçamento para acomodar o label de erro
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
        passwordInput.setPromptText("Senha");
        passwordInput.setStyle(INPUT_STYLE);

        // Inicializa o feedback de erro oculto
        loginErrorLabel = new Label("");
        loginErrorLabel.setStyle(ERROR_LABEL_STYLE);
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

        // Inserido o error label logo acima do botão de ação
        loginBox.getChildren().addAll(title, usernameInput, passwordInput, loginErrorLabel, loginButton, footer);
    }

    private void inicializarRegisterBox() {
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

        // Inicializa o feedback de erro oculto do registro
        registerErrorLabel = new Label("");
        registerErrorLabel.setStyle(ERROR_LABEL_STYLE);
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

    // --- ANIMAÇÃO DE ERRO (Shake / Tremor Visual) ---
    private void dispararFeedbackErro(Label targetLabel, VBox targetBox, String mensagem) {
        Platform.runLater(() -> {
            targetLabel.setText(mensagem.toUpperCase());
            targetLabel.setManaged(true);
            targetLabel.setVisible(true);

            // Efeito tremor horizontal clássico de erro em jogos
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

    // --- VALIDAÇÕES E ENVIO ---
    private void fazerLogin(String username, String password) {
        limparErros();

        // Regra: Usuário precisa ter pelo menos 6 caracteres no Login
        if (username == null || username.trim().length() < 6) {
            dispararFeedbackErro(loginErrorLabel, loginBox, "Usuário deve ter pelo menos 6 caracteres");
            return;
        }

        System.out.println("Enviando requisição de Login para o servidor: " + username);
        
        /* 
         Quando seu POST falhar no servidor, você só precisa chamar:
         dispararFeedbackErro(loginErrorLabel, loginBox, "Credenciais Inválidas ou Servidor Offline");
        */
    }

    private void criarConta(String username, String password) {
        limparErros();

        // Validação de Usuário: entre 5 e 20 caracteres
        if (username == null || username.trim().length() < 5 || username.trim().length() > 20) {
            dispararFeedbackErro(registerErrorLabel, registerBox, "Usuário deve conter entre 5 e 20 caracteres");
            return;
        }

        // Validação de Senha: entre 6 e 30 caracteres
        if (password == null || password.length() < 6 || password.length() > 30) {
            dispararFeedbackErro(registerErrorLabel, registerBox, "Senha deve conter entre 6 e 30 caracteres");
            return;
        }

        System.out.println("Enviando POST de registro: " + username);

        /* 
         Se o servidor responder informando que o usuário já existe, trate no callback assim:
         dispararFeedbackErro(registerErrorLabel, registerBox, "Este nome de usuário já está em uso");
        */
    }

    // --- TRANSIÇÕES DE ALTERNÂNCIA DE TELA ---
    private void alternarParaRegistro() {
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

    private void alternarParaLogin() {
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