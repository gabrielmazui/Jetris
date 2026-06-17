package ui.screens;

import java.io.ByteArrayInputStream;

import config.UserSession;
import core.ScreenManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import network.NetworkContext;
import ui.service.LogoutService;

public class MainScreen implements Screen {

    private final StackPane root;
    private BorderPane mainLayout;
    private VBox matchesList;
    private Circle[] pingDots;
    private Label pingLabel;
    
    private Label activeMatchesLabel;
    private Label pageInfoLabel;
    private Button prevPageBtn;
    private Button nextPageBtn;
    
    private int currentPage = 1;
    private int totalPages = 1;
    private int totalActiveMatches = 0;

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
        -fx-transition: -fx-border-color 0.2s ease;
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
        
        UpdatePing(NetworkContext.ping);
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
        applyButtonEffects(usersBtn, "#2E2E38", "#3E3E4A");
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
        applyIconRotationEffect(settingsBtn);
        settingsBtn.setOnAction(e -> onSettingsClick());

        VBox profileBox = new VBox(4);
        profileBox.setAlignment(Pos.CENTER);
        profileBox.setStyle("-fx-cursor: hand;");
        applyProfileHoverEffect(profileBox);

        byte[] pfpBytes = UserSession.getPfp();
        Circle pfp = new Circle(18);

        if (pfpBytes != null) {
            Image avatarImage = new Image(
                new ByteArrayInputStream(pfpBytes)
            );

            pfp.setFill(new ImagePattern(avatarImage));
        } else {
            pfp.setFill(Color.web("#2E2E38"));
        }

        pfp.setStroke(Color.web("#00ADB5"));
        pfp.setStrokeWidth(2);

        String user = UserSession.getUsername();
        Label usernameLabel = new Label(user);
        usernameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold;");
        usernameLabel.setMaxWidth(100);

        profileBox.getChildren().addAll(pfp, usernameLabel);

        ContextMenu profileMenu = new ContextMenu();
        profileMenu.setStyle("-fx-background-color: #1E1E26; -fx-border-color: #2E2E38; -fx-border-radius: 4; -fx-background-radius: 4;-fx-cursor: hand;");
        Label lblProfile = new Label("Profile");
        
        lblProfile.setTextFill(Color.WHITE);
        MenuItem profileItem = new MenuItem("", lblProfile);
        profileItem.setOnAction(e -> {
            profileMenu.hide();
            onProfileClick();
        });
        
        Label lblSettings = new Label("Settings");
        lblSettings.setTextFill(Color.WHITE);
        MenuItem settingsItem = new MenuItem("", lblSettings);
        settingsItem.setOnAction(e -> {
            profileMenu.hide();
            onSettingsClick();
        });
        
        Label lblLogout = new Label("Logout");
        lblLogout.setTextFill(Color.web("#FF4A4A"));
        MenuItem logoutItem = new MenuItem("", lblLogout);
        logoutItem.setOnAction(e -> {
            profileMenu.hide();
            onLogoutClick();
        });
        
        profileMenu.getItems().addAll(profileItem, settingsItem, logoutItem);

        profileBox.setOnMouseClicked(e -> {
            if (profileMenu.isShowing()) {
                profileMenu.hide();
            } else {
                profileMenu.show(profileBox, Side.BOTTOM, 0, 5);
            }
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
        searchInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                searchInput.setStyle(INPUT_STYLE + "-fx-border-color: #00ADB5;");
            } else {
                searchInput.setStyle(INPUT_STYLE);
            }
        });

        Button findMatchBtn = new Button("Find Match");
        findMatchBtn.setStyle(PRIMARY_BUTTON_STYLE);
        applyButtonEffects(findMatchBtn, "#00ADB5", "#33BEC4");
        findMatchBtn.setOnAction(e -> onFindMatch(searchInput.getText()));

        Button createMatchBtn = new Button("Create Private Match");
        createMatchBtn.setStyle(SECONDARY_BUTTON_STYLE);
        applyButtonEffects(createMatchBtn, "#2E2E38", "#3E3E4A");
        createMatchBtn.setOnAction(e -> onCreatePrivateMatch());

        searchArea.getChildren().addAll(searchInput, findMatchBtn, createMatchBtn);

        VBox matchesSection = new VBox(15);
        
        HBox matchesHeader = new HBox();
        matchesHeader.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleAndCounterBox = new VBox(4);
        Label matchesTitle = new Label("LIVE MATCHES");
        matchesTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: 800; -fx-letter-spacing: 1px;");
        
        activeMatchesLabel = new Label("Current active matches: 0");
        activeMatchesLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold;");
        titleAndCounterBox.getChildren().addAll(matchesTitle, activeMatchesLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label refreshIcon = new Label("⟳");
        refreshIcon.setStyle("-fx-text-fill: #00ADB5; -fx-font-weight: bold;");
        
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setGraphic(refreshIcon);
        refreshBtn.setContentDisplay(ContentDisplay.RIGHT);
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00ADB5; -fx-font-weight: bold; -fx-cursor: hand;");
        
        applyRefreshButtonEffects(refreshBtn, refreshIcon);
        refreshBtn.setOnAction(e -> onRefreshMatches());
        
        matchesHeader.getChildren().addAll(titleAndCounterBox, spacer, refreshBtn);

        matchesList = new VBox(10);
        
        ScrollPane scrollPane = new ScrollPane(matchesList);
        matchesList.setPadding(new Insets(10, 25, 10, 10));
        matchesList.setPickOnBounds(false);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(SCROLL_PANE_STYLE);
        scrollPane.setPrefHeight(400);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        HBox paginationBox = new HBox(15);
        paginationBox.setAlignment(Pos.CENTER);
        paginationBox.setPadding(new Insets(10, 0, 0, 0));

        prevPageBtn = new Button("< Prev");
        prevPageBtn.setStyle(SECONDARY_BUTTON_STYLE);
        applyButtonEffects(prevPageBtn, "#2E2E38", "#3E3E4A");
        prevPageBtn.setOnAction(e -> onPreviousPage());

        pageInfoLabel = new Label("Page 1 of 1");
        pageInfoLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold;");

        nextPageBtn = new Button("Next >");
        nextPageBtn.setStyle(SECONDARY_BUTTON_STYLE);
        applyButtonEffects(nextPageBtn, "#2E2E38", "#3E3E4A");
        nextPageBtn.setOnAction(e -> onNextPage());

        paginationBox.getChildren().addAll(prevPageBtn, pageInfoLabel, nextPageBtn);

        matchesSection.getChildren().addAll(matchesHeader, scrollPane, paginationBox);

        centerBox.getChildren().addAll(searchArea, matchesSection);
        
        Platform.runLater(this::fetchBackendMatchesData);

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
        StackPane cardWrapper = new StackPane();
        cardWrapper.setMaxWidth(Double.MAX_VALUE);

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
        applyButtonEffects(spectateBtn, "#00ADB5", "#33BEC4");
        spectateBtn.setOnAction(e -> onSpectate(matchId));

        spectateInfo.getChildren().addAll(spectatorsLabel, spectateBtn);

        card.getChildren().addAll(matchInfo, spacer, spectateInfo);
        cardWrapper.getChildren().add(card);

        cardWrapper.setOnMouseEntered(e -> {
            if (root.isDisable()) return;
            card.setViewOrder(-1.0);
            cardWrapper.setViewOrder(-1.0);
            card.setStyle(MATCH_CARD_STYLE + "-fx-border-color: #00ADB5; -fx-background-color: #23232D;");
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), card);
            tt.setToX(12);
            tt.play();
        });

        cardWrapper.setOnMouseExited(e -> {
            if (root.isDisable()) return;
            card.setViewOrder(0.0);
            cardWrapper.setViewOrder(0.0);
            card.setStyle(MATCH_CARD_STYLE);
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), card);
            tt.setToX(0);
            tt.play();
        });

        matchesList.getChildren().add(cardWrapper);
    }

    private void applyButtonEffects(Button button, String normalBg, String hoverBg) {
        button.setOnMouseEntered(e -> {
            if (root.isDisable()) return;
            button.setStyle(button.getStyle() + "-fx-background-color: " + hoverBg + ";");
        });
        button.setOnMouseExited(e -> {
            if (root.isDisable()) return;
            button.setStyle(button.getStyle() + "-fx-background-color: " + normalBg + ";");
        });
        
        button.setOnMousePressed(e -> {
            if (root.isDisable()) return;
            ScaleTransition st = new ScaleTransition(Duration.millis(80), button);
            st.setToX(0.95);
            st.setToY(0.95);
            st.play();
        });
        
        button.setOnMouseReleased(e -> {
            if (root.isDisable()) return;
            ScaleTransition st = new ScaleTransition(Duration.millis(80), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void applyRefreshButtonEffects(Button button, Label icon) {
        button.setOnMouseEntered(e -> {
            if (root.isDisable()) return;
            button.setStyle(button.getStyle() + "-fx-text-fill: #33BEC4;");
            icon.setStyle(icon.getStyle() + "-fx-text-fill: #33BEC4;");
        });
        button.setOnMouseExited(e -> {
            if (root.isDisable()) return;
            button.setStyle(button.getStyle() + "-fx-text-fill: #00ADB5;");
            icon.setStyle(icon.getStyle() + "-fx-text-fill: #00ADB5;");
        });
        
        button.setOnMousePressed(e -> {
            if (root.isDisable()) return;
            RotateTransition rt = new RotateTransition(Duration.millis(300), icon);
            rt.setByAngle(360);
            
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(0.9);
            st.setToY(0.9);
            
            new ParallelTransition(rt, st).play();
        });
        
        button.setOnMouseReleased(e -> {
            if (root.isDisable()) return;
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void applyIconRotationEffect(Button button) {
        button.setOnMouseEntered(e -> {
            if (root.isDisable()) return;
            button.setStyle(button.getStyle() + "-fx-text-fill: #FFFFFF;");
            RotateTransition rt = new RotateTransition(Duration.millis(400), button);
            rt.setToAngle(45);
            rt.play();
        });
        button.setOnMouseExited(e -> {
            if (root.isDisable()) return;
            button.setStyle(button.getStyle() + "-fx-text-fill: #6E6E77;");
            RotateTransition rt = new RotateTransition(Duration.millis(400), button);
            rt.setToAngle(0);
            rt.play();
        });
    }

    private void applyProfileHoverEffect(Node node) {
        node.setOnMouseEntered(e -> {
            if (root.isDisable()) return;
            FadeTransition ft = new FadeTransition(Duration.millis(150), node);
            ft.setToValue(0.8);
            ft.play();
        });
        node.setOnMouseExited(e -> {
            if (root.isDisable()) return;
            FadeTransition ft = new FadeTransition(Duration.millis(150), node);
            ft.setToValue(1.0);
            ft.play();
        });
    }

    @Override
    public void UpdatePing(int ms) {
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
                    pingDots[i].setStroke(Color.web("#5C5C64"));
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

    private void fetchBackendMatchesData() {
        this.totalActiveMatches = onFetchTotalActiveMatchesCount();
        this.totalPages = onFetchTotalPagesCount();
        
        activeMatchesLabel.setText("Current active matches: " + totalActiveMatches);
        pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);
        
        prevPageBtn.setDisable(currentPage == 1);
        nextPageBtn.setDisable(currentPage == totalPages);
        
        populateMockMatches();
    }

    private void populateMockMatches() {
        matchesList.getChildren().clear();
        if (currentPage == 1) {
            addMatchCard("Faker", "Caps", 2, 1, 4, 1240, "match_001");
            addMatchCard("Gabriel", "Player2", 0, 0, 1, 5, "match_002");
            addMatchCard("DarkKnight", "ProSniper", 3, 2, 6, 42, "match_003");
        } else if (currentPage == 2) {
            addMatchCard("BetaTester", "AlphaPlayer", 1, 1, 2, 14, "match_004");
            addMatchCard("Shadow", "Light", 5, 4, 9, 89, "match_005");
        }
    }

    private void onPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            onPageRequested(currentPage);
            fetchBackendMatchesData();
        }
    }

    private void onNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            onPageRequested(currentPage);
            fetchBackendMatchesData();
        }
    }

    private int onFetchTotalActiveMatchesCount() {
        return 5;
    }

    private int onFetchTotalPagesCount() {
        return 2;
    }

    private void onPageRequested(int page) {
    }

    private void onFindMatch(String query) {
    }

    private void onCreatePrivateMatch() {
    }

    private void onSpectate(String matchId) {
    }

    private void onRefreshMatches() {
        fetchBackendMatchesData();
    }

    private void onProfileClick() {
        ScreenManager.setScreen(new ProfileScreen(UserSession.getUsername()));
    }

    private void onSettingsClick() {
        core.ScreenManager.setScreen(new ui.screens.SettingsScreen());
    }

    public void onLogoutClick() {
        root.setDisable(true);
        Screen.transitionToScreen(() -> {
            LogoutService.logout();
            Platform.runLater(() -> ScreenManager.setScreen(new LoginScreen()));
        }, mainLayout);
    }

    public void onUsersClick(){
        ScreenManager.setScreen(new UserSearchScreen());
    }

    @Override 
    public Boolean isPingDisplayed(){
        return true;
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}