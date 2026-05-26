package ui.screens;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class MainScreen implements Screen {

    private final StackPane root;
    private BorderPane mainLayout;
    private VBox matchesList;
    private Circle[] pingDots;
    private Label pingLabel;

    private static final String INPUT_STYLE = """
        -fx-background-color: #1E1E26;
        -fx-text-fill: #FFFFFF;
        -fx-prompt-text-fill: #5C5C64;
        -fx-background-radius: 6;
        -fx-border-radius: 6;
        -fx-border-color: #2E2E38;
        -fx-border-width: 1;
        -fx-padding: 10 14 10 14;
        -fx-font-family: 'Segoe UI';
        -fx-font-size: 13px;
    """;

    private static final String PRIMARY_BUTTON_STYLE = """
        -fx-background-color: #00ADB5;
        -fx-text-fill: #0F0F14;
        -fx-font-family: 'Segoe UI';
        -fx-font-weight: bold;
        -fx-font-size: 13px;
        -fx-background-radius: 6;
        -fx-padding: 10 16 10 16;
        -fx-cursor: hand;
    """;

    private static final String SECONDARY_BUTTON_STYLE = """
        -fx-background-color: #2E2E38;
        -fx-text-fill: #FFFFFF;
        -fx-font-family: 'Segoe UI';
        -fx-font-weight: bold;
        -fx-font-size: 13px;
        -fx-background-radius: 6;
        -fx-padding: 10 16 10 16;
        -fx-cursor: hand;
    """;

    private static final String MATCH_CARD_STYLE = """
        -fx-background-color: #1E1E26;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: #2E2E38;
        -fx-border-width: 1;
        -fx-padding: 15;
    """;

    private static final String SCROLL_PANE_STYLE = """
        -fx-background-color: transparent;
        -fx-background: #0F0F14;
    """;

    public MainScreen() {
        root = new StackPane();
        root.setStyle("-fx-background-color: #0F0F14;");

        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20, 30, 20, 30));

        mainLayout.setTop(createTopBar());
        mainLayout.setCenter(createCenterContent());
        mainLayout.setBottom(createBottomBar());

        root.getChildren().add(mainLayout);

        executarAnimacaoEntrada();
        
        updatePing(45);
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER);
        topBar.setSpacing(20);

        Label title = new Label("JETRIS");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 24));
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-letter-spacing: 2px;");
        title.setEffect(new DropShadow(10, Color.web("#00ADB5", 0.5)));

        Button usersBtn = new Button("Users");
        usersBtn.setStyle(SECONDARY_BUTTON_STYLE);
        usersBtn.setOnAction(e -> onUsersClick());

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        pingDots = new Circle[3];
        HBox dotsBox = new HBox(3);
        dotsBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            pingDots[i] = new Circle(3.5);
            pingDots[i].setFill(Color.TRANSPARENT);
            pingDots[i].setStroke(Color.web("#5C5C64"));
            pingDots[i].setStrokeWidth(1);
            dotsBox.getChildren().add(pingDots[i]);
        }

        pingLabel = new Label("📶 -- ms");
        pingLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox pingBox = new HBox(6, pingLabel, dotsBox);
        pingBox.setAlignment(Pos.CENTER);

        Button settingsBtn = new Button("⚙");
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6E6E77; -fx-font-size: 20px; -fx-cursor: hand;");
        settingsBtn.setOnAction(e -> onSettingsClick());

        VBox profileBox = new VBox(4);
        profileBox.setAlignment(Pos.CENTER);
        profileBox.setStyle("-fx-cursor: hand;");

        Circle pfp = new Circle(18, Color.web("#2E2E38"));
        pfp.setStroke(Color.web("#00ADB5"));
        pfp.setStrokeWidth(2);

        Label usernameLabel = new Label("PlayerOne");
        usernameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold;");
        usernameLabel.setMaxWidth(100);

        profileBox.getChildren().addAll(pfp, usernameLabel);

        ContextMenu profileMenu = new ContextMenu();
        profileMenu.setStyle("-fx-background-color: #1E1E26;");
        
        Label lblProfile = new Label("Profile");
        lblProfile.setTextFill(Color.WHITE);
        MenuItem profileItem = new MenuItem("", lblProfile);
        profileItem.setOnAction(e -> onProfileClick());
        
        Label lblSettings = new Label("Settings");
        lblSettings.setTextFill(Color.WHITE);
        MenuItem settingsItem = new MenuItem("", lblSettings);
        settingsItem.setOnAction(e -> onSettingsClick());
        
        Label lblLogout = new Label("Logout");
        lblLogout.setTextFill(Color.WHITE);
        MenuItem logoutItem = new MenuItem("", lblLogout);
        logoutItem.setOnAction(e -> onLogoutClick());
        
        profileMenu.getItems().addAll(profileItem, settingsItem, logoutItem);

        profileBox.setOnMouseClicked(e -> {
            profileMenu.show(profileBox, e.getScreenX(), e.getScreenY());
        });

        HBox rightControls = new HBox(20, pingBox, settingsBtn, profileBox);
        rightControls.setAlignment(Pos.CENTER_RIGHT);

        topBar.getChildren().addAll(title, usersBtn, spacer1, rightControls);
        return topBar;
    }

    private VBox createCenterContent() {
        VBox centerBox = new VBox(30);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setPadding(new Insets(40, 0, 0, 0));
        centerBox.setMaxWidth(800);

        HBox searchArea = new HBox(10);
        searchArea.setAlignment(Pos.CENTER);

        TextField searchInput = new TextField();
        searchInput.setPromptText("Search match by code or user...");
        searchInput.setStyle(INPUT_STYLE);
        searchInput.setPrefWidth(400);

        Button findMatchBtn = new Button("Find Match");
        findMatchBtn.setStyle(PRIMARY_BUTTON_STYLE);
        findMatchBtn.setOnAction(e -> onFindMatch(searchInput.getText()));

        Button createMatchBtn = new Button("Create Private Match");
        createMatchBtn.setStyle(SECONDARY_BUTTON_STYLE);
        createMatchBtn.setOnAction(e -> onCreatePrivateMatch());

        searchArea.getChildren().addAll(searchInput, findMatchBtn, createMatchBtn);

        VBox matchesSection = new VBox(15);
        
        HBox matchesHeader = new HBox();
        matchesHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label matchesTitle = new Label("LIVE MATCHES");
        matchesTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: 800; -fx-letter-spacing: 1px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button refreshBtn = new Button("Refresh ⟳");
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00ADB5; -fx-font-weight: bold; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> onRefreshMatches());
        
        matchesHeader.getChildren().addAll(matchesTitle, spacer, refreshBtn);

        matchesList = new VBox(10);
        
        ScrollPane scrollPane = new ScrollPane(matchesList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(SCROLL_PANE_STYLE);
        scrollPane.setPrefHeight(400);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        matchesSection.getChildren().addAll(matchesHeader, scrollPane);

        centerBox.getChildren().addAll(searchArea, matchesSection);
        
        Platform.runLater(this::populateMockMatches);

        return centerBox;
    }

    private HBox createBottomBar() {
        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        Label credits = new Label("Created by Gabriel Mazui");
        credits.setStyle("-fx-text-fill: #5C5C64; -fx-font-family: 'Segoe UI'; -fx-font-size: 11px;");

        bottomBar.getChildren().add(credits);
        return bottomBar;
    }

    private void addMatchCard(String user1, String user2, int score1, int score2, int round, int spectators, String matchId) {
        HBox card = new HBox(20);
        card.setStyle(MATCH_CARD_STYLE);
        card.setAlignment(Pos.CENTER_LEFT);

        VBox matchInfo = new VBox(5);
        Label playersLabel = new Label(user1 + " x " + user2);
        playersLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 16px;");
        
        Label scoreLabel = new Label("Score: " + score1 + " - " + score2 + "  |  Round " + round);
        scoreLabel.setStyle("-fx-text-fill: #00ADB5; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        matchInfo.getChildren().addAll(playersLabel, scoreLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox spectateInfo = new VBox(8);
        spectateInfo.setAlignment(Pos.CENTER_RIGHT);
        
        Label spectatorsLabel = new Label("👁 " + spectators + " Spectators");
        spectatorsLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 12px;");
        
        Button spectateBtn = new Button("Spectate");
        spectateBtn.setStyle(PRIMARY_BUTTON_STYLE);
        spectateBtn.setOnAction(e -> onSpectate(matchId));

        spectateInfo.getChildren().addAll(spectatorsLabel, spectateBtn);

        card.getChildren().addAll(matchInfo, spacer, spectateInfo);
        matchesList.getChildren().add(card);
    }

    public void updatePing(int ms) {
        Platform.runLater(() -> {
            int activeDots = 0;
            Color dotColor = Color.TRANSPARENT;

            if (ms < 0 || ms >= 1000) {
                pingLabel.setText("📶 -- ms");
                activeDots = 0; 
            } else if (ms < 100) {
                pingLabel.setText("📶 " + ms + " ms");
                activeDots = 3; 
                dotColor = Color.web("#00E676"); 
            } else if (ms < 200) {
                pingLabel.setText("📶 " + ms + " ms");
                activeDots = 2; 
                dotColor = Color.web("#FF9100"); 
            } else {
                pingLabel.setText("📶 " + ms + " ms");
                activeDots = 1; 
                dotColor = Color.web("#FF4A4A"); 
            }

            for (int i = 0; i < 3; i++) {
                if (i < activeDots) {
                    pingDots[i].setFill(dotColor);
                    pingDots[i].setStroke(dotColor);
                } else {
                    pingDots[i].setFill(Color.TRANSPARENT);
                    pingDots[i].setStroke(Color.web("#5C5C64")); // Borda cinza pros pontos vazios
                }
            }
        });
    }

    private void executarAnimacaoEntrada() {
        mainLayout.setOpacity(0.0);
        mainLayout.setTranslateY(20.0);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.6), mainLayout);
        fadeIn.setToValue(1.0);

        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(0.6), mainLayout);
        moveUp.setToY(0.0);
        moveUp.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fadeIn, moveUp).play();
    }

    private void populateMockMatches() {
        matchesList.getChildren().clear();
        addMatchCard("Faker", "Caps", 2, 1, 4, 1240, "match_001");
        addMatchCard("Gabriel", "Player2", 0, 0, 1, 5, "match_002");
        addMatchCard("DarkKnight", "ProSniper", 3, 2, 6, 42, "match_003");
    }

    private void onProfileClick() {
    }

    private void onSettingsClick() {
    }

    private void onLogoutClick() {
    }

    private void onUsersClick() {
    }

    private void onFindMatch(String query) {
    }

    private void onCreatePrivateMatch() {
    }

    private void onSpectate(String matchId) {
    }

    private void onRefreshMatches() {
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}