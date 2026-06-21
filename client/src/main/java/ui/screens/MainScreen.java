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
import ui.service.MatchListService;
import ui.service.LogoutService;
import ui.service.MatchMakingService;
import ui.service.SpectateMatchService;

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

    private Button findMatchBtn;
    private Button searchMatchBtn;
    private Button joinPrivateBtn;
    private Button createMatchBtn;
    private Label searchStatusLabel;
    private TextField matchSearchInput;
    private TextField privateCodeInput;
    private StackPane matchOverlay;
    private String pendingPrivateMatchCode = "";
    private String currentMatchQuery = "";

    private enum PendingMatchType {
        NONE,
        QUEUE,
        PRIVATE
    }

    private PendingMatchType pendingMatchType = PendingMatchType.NONE;

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
        topBar.setMinHeight(Region.USE_PREF_SIZE);
        topBar.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("JETRIS");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 24));
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-letter-spacing: 2px;");
        title.setEffect(new DropShadow(10, Color.web("#00ADB5", 0.5)));

        Button usersBtn = new Button("Users");
        usersBtn.setStyle(SECONDARY_BUTTON_STYLE);
        usersBtn.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
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
        pingBox.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Button settingsBtn = new Button("⚙");
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6E6E77; -fx-font-size: 20px; -fx-cursor: hand;");
        settingsBtn.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        applyIconRotationEffect(settingsBtn);
        settingsBtn.setOnAction(e -> onSettingsClick());

        VBox profileBox = new VBox(4);
        profileBox.setAlignment(Pos.CENTER);
        profileBox.setStyle("-fx-cursor: hand;");
        profileBox.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
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
        rightControls.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        topBar.getChildren().addAll(title, usersBtn, spacer1, rightControls);
        return topBar;
    }

    private ScrollPane createCenterContent() {
        VBox centerBox = new VBox(14);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setPadding(new Insets(15, 0, 0, 0)); 
        centerBox.setMaxWidth(1080);

        VBox.setVgrow(centerBox, Priority.ALWAYS);

        VBox heroCard = new VBox(6);
        heroCard.setAlignment(Pos.CENTER_LEFT);
        heroCard.setPadding(new Insets(14, 20, 14, 20));
        heroCard.setStyle("-fx-background-color: linear-gradient(to right, rgba(32, 32, 44, 0.95), rgba(16, 16, 22, 0.95)); -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: rgba(255, 255, 255, 0.06); -fx-border-width: 1;");

        Label heroEyebrow = new Label("MATCH LOBBY");
        heroEyebrow.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 3px;");

        Label heroTitle = new Label("Choose how you want to play");
        heroTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 20px; -fx-font-weight: 900;");

        Label heroSubtitle = new Label("Find a quick match, create a private room, or join one with a code.");
        heroSubtitle.setWrapText(true);
        heroSubtitle.setStyle("-fx-text-fill: #A3A3B0; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px;");

        heroCard.getChildren().addAll(heroEyebrow, heroTitle, heroSubtitle);

        HBox actionsRow = new HBox(14);
        actionsRow.setAlignment(Pos.CENTER);

        VBox quickCard = new VBox(12);
        quickCard.setAlignment(Pos.CENTER_LEFT);
        quickCard.setPadding(new Insets(16));
        quickCard.setStyle("-fx-background-color: #17171F; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #2A2A36; -fx-border-width: 1;");
        HBox.setHgrow(quickCard, Priority.ALWAYS);

        Label quickTitle = new Label("Quick Match");
        quickTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 15px; -fx-font-weight: 800;");
        Label quickBody = new Label("Jump into the public queue and get matched automatically.");
        quickBody.setWrapText(true);
        quickBody.setStyle("-fx-text-fill: #A3A3B0; -fx-font-size: 11px;");

        HBox quickButtons = new HBox(10);
        quickButtons.setAlignment(Pos.CENTER_LEFT);

        Button findMatchBtn = new Button("Find Match");
        findMatchBtn.setStyle(PRIMARY_BUTTON_STYLE);
        applyButtonEffects(findMatchBtn, "#00ADB5", "#33BEC4");
        findMatchBtn.setOnAction(e -> onFindMatch());
        this.findMatchBtn = findMatchBtn;

        Button createMatchBtn = new Button("Create Private Match");
        createMatchBtn.setStyle(SECONDARY_BUTTON_STYLE);
        applyButtonEffects(createMatchBtn, "#2E2E38", "#3E3E4A");
        createMatchBtn.setOnAction(e -> onCreatePrivateMatch());
        this.createMatchBtn = createMatchBtn;

        quickButtons.getChildren().addAll(findMatchBtn, createMatchBtn);
        quickCard.getChildren().addAll(quickTitle, quickBody, quickButtons);

        VBox privateCard = new VBox(12);
        privateCard.setAlignment(Pos.CENTER_LEFT);
        privateCard.setPadding(new Insets(16));
        privateCard.setStyle("-fx-background-color: #17171F; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #2A2A36; -fx-border-width: 1;");
        HBox.setHgrow(privateCard, Priority.ALWAYS);

        Label privateTitle = new Label("Private Match");
        privateTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 15px; -fx-font-weight: 800;");
        Label privateBody = new Label("Join a room using a 6-character code.");
        privateBody.setWrapText(true);
        privateBody.setStyle("-fx-text-fill: #A3A3B0; -fx-font-size: 11px;");

        privateCodeInput = new TextField();
        privateCodeInput.setPromptText("Enter private code");
        privateCodeInput.setStyle(INPUT_STYLE);
        privateCodeInput.setPrefWidth(180);
        privateCodeInput.setOnAction(e -> onJoinPrivateMatch());
        privateCodeInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                privateCodeInput.setStyle(INPUT_STYLE + "-fx-border-color: #00ADB5;");
            } else {
                privateCodeInput.setStyle(INPUT_STYLE);
            }
        });

        joinPrivateBtn = new Button("Join Private");
        joinPrivateBtn.setStyle(PRIMARY_BUTTON_STYLE);
        applyButtonEffects(joinPrivateBtn, "#00ADB5", "#33BEC4");
        joinPrivateBtn.setOnAction(e -> onJoinPrivateMatch());

        HBox privateActions = new HBox(10, privateCodeInput, joinPrivateBtn);
        privateActions.setAlignment(Pos.CENTER_LEFT);

        privateCard.getChildren().addAll(privateTitle, privateBody, privateActions);

        actionsRow.getChildren().addAll(quickCard, privateCard);

        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(2, 0, 0, 0));

        matchSearchInput = new TextField();
        matchSearchInput.setPromptText("Search by code or user...");
        matchSearchInput.setStyle(INPUT_STYLE);
        HBox.setHgrow(matchSearchInput, Priority.ALWAYS);
        matchSearchInput.setOnAction(e -> onSearchMatches());
        matchSearchInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                matchSearchInput.setStyle(INPUT_STYLE + "-fx-border-color: #00ADB5;");
            } else {
                matchSearchInput.setStyle(INPUT_STYLE);
            }
        });

        searchMatchBtn = new Button("Search");
        searchMatchBtn.setStyle(PRIMARY_BUTTON_STYLE);
        applyButtonEffects(searchMatchBtn, "#00ADB5", "#33BEC4");
        searchMatchBtn.setOnAction(e -> onSearchMatches());

        Button clearSearchBtn = new Button("Clear");
        clearSearchBtn.setStyle(SECONDARY_BUTTON_STYLE);
        applyButtonEffects(clearSearchBtn, "#2E2E38", "#3E3E4A");
        clearSearchBtn.setOnAction(e -> {
            if (matchSearchInput != null) {
                matchSearchInput.clear();
            }
            currentMatchQuery = "";
            fetchBackendMatchesData("");
        });

        searchBar.getChildren().addAll(matchSearchInput, searchMatchBtn, clearSearchBtn);

        searchStatusLabel = new Label("");
        searchStatusLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox statusBox = new HBox(searchStatusLabel);
        statusBox.setAlignment(Pos.CENTER);

        VBox matchesSection = new VBox(10);
        matchesSection.setMinHeight(320);
        matchesSection.setPadding(new Insets(14, 16, 14, 16));
        matchesSection.setStyle("-fx-background-color: #14141C; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #2A2A36; -fx-border-width: 1;");
        VBox.setVgrow(matchesSection, Priority.ALWAYS);
        
        HBox matchesHeader = new HBox();
        matchesHeader.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleAndCounterBox = new VBox(2);
        Label matchesTitle = new Label("LIVE MATCHES");
        matchesTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 15px; -fx-font-weight: 800; -fx-letter-spacing: 1px;");
        
        activeMatchesLabel = new Label("Current active matches: 0");
        activeMatchesLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold;");
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

        matchesList = new VBox(12); 
        matchesList.setPadding(new Insets(4, 12, 4, 4));
        matchesList.setPickOnBounds(false);
        
        ScrollPane scrollPane = new ScrollPane(matchesList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(SCROLL_PANE_STYLE);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        matchesSection.getChildren().addAll(matchesHeader, scrollPane);

        centerBox.getChildren().addAll(heroCard, actionsRow, searchBar, statusBox, matchesSection);
        
        Platform.runLater(this::fetchBackendMatchesData);

        StackPane centeredWrapper = new StackPane(centerBox);
        centeredWrapper.setAlignment(Pos.TOP_CENTER);
        centeredWrapper.setMaxWidth(Double.MAX_VALUE);

        ScrollPane centerScrollPane = new ScrollPane(centeredWrapper);
        centerScrollPane.setFitToWidth(true);
        centerScrollPane.setFitToHeight(true);
        centerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        centerScrollPane.setStyle(SCROLL_PANE_STYLE);
        return centerScrollPane;
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

    private void addMatchCard(String user1, String user1Pfp, String user2, String user2Pfp, String state, int spectators, String matchId, long startTimeMillis) {
        StackPane cardWrapper = new StackPane();
        cardWrapper.setMaxWidth(Double.MAX_VALUE);

        HBox card = new HBox(20);
        card.setStyle(MATCH_CARD_STYLE);
        card.setAlignment(Pos.CENTER_LEFT);

        VBox matchInfo = new VBox(8);
        HBox playersRow = new HBox(10);
        playersRow.setAlignment(Pos.CENTER_LEFT);

        Circle p1Avatar = buildAvatar(user1Pfp, Color.web("#00ADB5"));
        Circle p2Avatar = buildAvatar(user2Pfp, Color.web("#FF4A4A"));

        Label vsLabel = new Label("VS");
        vsLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 11px; -fx-font-weight: 900; -fx-letter-spacing: 2px;");
        playersRow.getChildren().addAll(p1Avatar, vsLabel, p2Avatar);

        Label playersLabel = new Label(user1 + " x " + user2);
        playersLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 16px;");

        String stateText = "State: " + state;
        if (startTimeMillis > 0) {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            String startStr = java.time.LocalTime.ofInstant(java.time.Instant.ofEpochMilli(startTimeMillis), java.time.ZoneId.systemDefault()).format(fmt);
            stateText += "  •  Início: " + startStr;
        }
        Label stateLabel = new Label(stateText);
        stateLabel.setStyle("-fx-text-fill: #00ADB5; -fx-font-size: 12px; -fx-font-weight: bold;");

        matchInfo.getChildren().addAll(playersLabel, playersRow, stateLabel);

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

    private Circle buildAvatar(String pfpBase64, Color borderColor) {
        Circle avatar = new Circle(16, Color.web("#2E2E38"));
        avatar.setStroke(borderColor);
        avatar.setStrokeWidth(2);

        if (pfpBase64 != null && !pfpBase64.isBlank()) {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(pfpBase64.trim());
                Image image = new Image(new ByteArrayInputStream(bytes));
                if (!image.isError()) {
                    avatar.setFill(new ImagePattern(image));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return avatar;
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
        fetchBackendMatchesData(currentMatchQuery);
    }

    private void fetchBackendMatchesData(String query) {
        currentMatchQuery = query == null ? "" : query.trim();
        matchesList.getChildren().setAll(createLoadingRow("Loading live matches"));
        activeMatchesLabel.setText(currentMatchQuery.isEmpty()
            ? "Current active matches: ..."
            : "Current active matches for \"" + currentMatchQuery + "\": ...");

        MatchListService.fetchLiveMatches(currentMatchQuery, new MatchListService.MatchListCallback() {
            @Override
            public void onSuccess(java.util.List<MatchListService.LiveMatch> matches) {
                Platform.runLater(() -> renderLiveMatches(matches));
            }

            @Override
            public void onFailure(String reason) {
                Platform.runLater(() -> {
                    matchesList.getChildren().setAll(createLoadingRow(
                        "Request_timeout".equals(reason)
                            ? "Server took too long to respond."
                            : "Could not load live matches: " + reason
                    ));
                    activeMatchesLabel.setText("Current active matches: --");
                });
            }
        });
    }

    private VBox createLoadingRow(String text) {
        VBox row = new VBox();
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(20, 10, 20, 10));

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 12px; -fx-font-weight: bold;");
        row.getChildren().add(label);
        return row;
    }

    private void renderLiveMatches(java.util.List<MatchListService.LiveMatch> matches) {
        matchesList.getChildren().clear();
        totalActiveMatches = matches.size();
        totalPages = 1;
        currentPage = 1;

        activeMatchesLabel.setText("Current active matches: " + totalActiveMatches);
        if (matches.isEmpty()) {
            matchesList.getChildren().add(createLoadingRow("No live matches right now."));
            return;
        }

        for (MatchListService.LiveMatch match : matches) {
            addMatchCard(match.player1, match.player1Pfp, match.player2, match.player2Pfp, match.state, match.spectators, match.code, match.startTimeMillis);
        }
    }

    private void onFindMatch() {
        setSearchControlsDisabled(true);
        searchStatusLabel.setText("Searching for an opponent...");
        MatchMakingService.findMatch(new MatchMakingService.MatchResultCallback() {
            @Override
            public void onSuccess(String action, String matchCode) {
                Platform.runLater(() -> {
                    if ("START".equalsIgnoreCase(action)) {
                        goToGameScreen(matchCode);
                    } else {
                        searchStatusLabel.setText("Waiting in queue for an opponent...");
                        showPendingMatchOverlay(
                            PendingMatchType.QUEUE,
                            "SEARCHING MATCH",
                            "You are in the queue. The screen is blocked until a match starts.",
                            "",
                            "CANCEL QUEUE"
                        );
                        armQueueListener();
                    }
                });
            }

            @Override
            public void onFailure(String reason) {
                Platform.runLater(() -> {
                    searchStatusLabel.setText("Request_timeout".equals(reason)
                        ? "Server took too long to respond."
                        : "Couldn't find a match: " + reason);
                    setSearchControlsDisabled(false);
                });
            }
        });
    }

    private void onJoinPrivateMatch() {
        String code = privateCodeInput == null ? "" : privateCodeInput.getText();
        String trimmed = code == null ? "" : code.trim();

        if (trimmed.isEmpty()) {
            searchStatusLabel.setText("Enter a private match code first.");
            return;
        }

        setSearchControlsDisabled(true);
        searchStatusLabel.setText("Joining private match " + trimmed + "...");

        MatchMakingService.joinPrivateMatch(trimmed, new MatchMakingService.MatchResultCallback() {
            @Override
            public void onSuccess(String action, String matchCode) {
                Platform.runLater(() -> goToGameScreen(matchCode));
            }

            @Override
            public void onFailure(String reason) {
                Platform.runLater(() -> {
                    searchStatusLabel.setText("Request_timeout".equals(reason)
                        ? "Server took too long to respond."
                        : "Couldn't join: " + reason);
                    setSearchControlsDisabled(false);
                });
            }
        });
    }

    private void armQueueListener() {
        MatchMakingService.listenForCountdown(new MatchMakingService.CountdownListener() {
            @Override
            public void onTick(String matchCode, int secondsLeft) {
                Platform.runLater(() -> {
                    if (pendingMatchType == PendingMatchType.QUEUE) {
                        goToGameScreen(matchCode);
                    } else if (pendingMatchType == PendingMatchType.PRIVATE && matchCode.equals(pendingPrivateMatchCode)) {
                        goToGameScreen(matchCode);
                    }
                });
            }

            @Override
            public void onMatchStarted(String matchCode) {
                Platform.runLater(() -> {
                    if (pendingMatchType == PendingMatchType.QUEUE) {
                        goToGameScreen(matchCode);
                    } else if (pendingMatchType == PendingMatchType.PRIVATE && matchCode.equals(pendingPrivateMatchCode)) {
                        goToGameScreen(matchCode);
                    }
                });
            }

            @Override
            public void onMatchCancelled(String matchCode, String reason) {
                Platform.runLater(() -> {
                    if (pendingMatchType == PendingMatchType.QUEUE) {
                        clearPendingMatchState();
                        searchStatusLabel.setText("Match cancelled: " + reason);
                    } else if (pendingMatchType == PendingMatchType.PRIVATE && matchCode.equals(pendingPrivateMatchCode)) {
                        clearPendingMatchState();
                        searchStatusLabel.setText("Private match closed: " + reason);
                    }
                });
            }
        });
    }

    private void goToGameScreen(String matchCode) {
        clearPendingMatchState();
        Screen.transitionToScreen(() -> {
            Platform.runLater(() -> ScreenManager.setScreen(new GameScreen(matchCode)));
        }, mainLayout);
    }

    private void setSearchControlsDisabled(boolean disabled) {
        findMatchBtn.setDisable(disabled);
        if (searchMatchBtn != null) {
            searchMatchBtn.setDisable(disabled);
        }
        createMatchBtn.setDisable(disabled);
        joinPrivateBtn.setDisable(disabled);
        if (matchSearchInput != null) {
            matchSearchInput.setDisable(disabled);
        }
        if (privateCodeInput != null) {
            privateCodeInput.setDisable(disabled);
        }
    }

    private void onSearchMatches() {
        String query = matchSearchInput == null ? "" : matchSearchInput.getText();
        fetchBackendMatchesData(query);
    }

    private void onCreatePrivateMatch() {
        setSearchControlsDisabled(true);
        searchStatusLabel.setText("Creating private match...");

        MatchMakingService.createPrivateMatch(new MatchMakingService.MatchResultCallback() {
            @Override
            public void onSuccess(String action, String matchCode) {
                Platform.runLater(() -> {
                    pendingPrivateMatchCode = matchCode;
                    searchStatusLabel.setText("Match created! Code: " + matchCode + " — waiting for opponent...");
                    showPendingMatchOverlay(
                        PendingMatchType.PRIVATE,
                        "PRIVATE MATCH CREATED",
                        "Share the code below and wait for an opponent.",
                        matchCode,
                        "CANCEL PRIVATE MATCH"
                    );
                    armQueueListener();
                });
            }

            @Override
            public void onFailure(String reason) {
                Platform.runLater(() -> {
                    searchStatusLabel.setText("Couldn't create match: " + reason);
                    setSearchControlsDisabled(false);
                });
            }
        });
    }

    private void showPendingMatchOverlay(PendingMatchType type, String title, String body, String matchCode, String cancelLabel) {
        pendingMatchType = type;
        pendingPrivateMatchCode = matchCode == null ? "" : matchCode;

        if (matchOverlay != null) {
            root.getChildren().remove(matchOverlay);
        }

        matchOverlay = new StackPane();
        matchOverlay.setStyle("-fx-background-color: rgba(10, 10, 16, 0.7);"); 
        matchOverlay.prefWidthProperty().bind(root.widthProperty());
        matchOverlay.prefHeightProperty().bind(root.heightProperty());
        matchOverlay.setAlignment(Pos.CENTER);

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(380);
        card.setMinWidth(320);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #1E1E2A, #14141C); -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(0, 173, 181, 0.3); -fx-border-width: 1.5;");
        card.setEffect(new DropShadow(25, Color.web("#000000", 0.6)));

        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER); 

        Label eyebrow = new Label(type == PendingMatchType.QUEUE ? "MATCHMAKING" : "PRIVATE MATCH");
        eyebrow.setStyle("-fx-text-fill: #00ADB5; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");

        Label overlayTitle = new Label(title);
        overlayTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 20px; -fx-font-weight: 900;");

        Label overlayBody = new Label(body);
        overlayBody.setWrapText(true);
        overlayBody.setAlignment(Pos.CENTER);
        overlayBody.setStyle("-fx-text-fill: #A3A3B0; -fx-font-size: 13px; -fx-line-spacing: 3px;");

        Label codeLabel = new Label(matchCode == null || matchCode.isBlank() ? "" : matchCode);
        codeLabel.setVisible(matchCode != null && !matchCode.isBlank());
        codeLabel.setManaged(matchCode != null && !matchCode.isBlank());
        codeLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: #17171F; -fx-padding: 10 20 10 20; -fx-background-radius: 8; -fx-font-size: 24px; -fx-font-weight: 900; -fx-letter-spacing: 5px; -fx-border-color: #2A2A36; -fx-border-radius: 8;");
        
        VBox.setMargin(codeLabel, new Insets(10, 0, 10, 0));

        Label overlayHint = new Label(type == PendingMatchType.QUEUE ? "Press ESC to cancel queue" : "Press ESC to cancel match");
        overlayHint.setStyle("-fx-text-fill: #5C5C64; -fx-font-size: 11px;");

        Button cancelButton = new Button(cancelLabel);
        cancelButton.setStyle("-fx-background-color: #FF4A4A; -fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 6; -fx-padding: 10 20 10 20; -fx-cursor: hand;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(cancelButton.getStyle() + "-fx-background-color: #FF6B6B;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle(cancelButton.getStyle() + "-fx-background-color: #FF4A4A;"));
        cancelButton.setOnAction(e -> cancelPendingMatch());

        content.getChildren().addAll(eyebrow, overlayTitle, overlayBody, codeLabel, overlayHint);
        card.getChildren().addAll(content, cancelButton);
        matchOverlay.getChildren().add(card);
        root.getChildren().add(matchOverlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), matchOverlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        ScaleTransition cardScale = new ScaleTransition(Duration.millis(180), card);
        cardScale.setFromX(0.9);
        cardScale.setFromY(0.9);
        cardScale.setToX(1.0);
        cardScale.setToY(1.0);
        cardScale.setInterpolator(Interpolator.EASE_OUT);
        cardScale.play();
    }

    private void cancelPendingMatch() {
        if (pendingMatchType == PendingMatchType.NONE) {
            return;
        }

        if (pendingMatchType == PendingMatchType.QUEUE) {
            MatchMakingService.cancelQueue(new MatchMakingService.MatchResultCallback() {
                @Override
                public void onSuccess(String action, String matchCode) {
                    Platform.runLater(MainScreen.this::clearPendingMatchState);
                }

                @Override
                public void onFailure(String reason) {
                    Platform.runLater(MainScreen.this::clearPendingMatchState);
                }
            });
            return;
        }

        if (pendingPrivateMatchCode == null || pendingPrivateMatchCode.isBlank()) {
            clearPendingMatchState();
            return;
        }

        MatchMakingService.cancelPrivateMatch(pendingPrivateMatchCode, new MatchMakingService.MatchResultCallback() {
            @Override
            public void onSuccess(String action, String matchCode) {
                Platform.runLater(MainScreen.this::clearPendingMatchState);
            }

            @Override
            public void onFailure(String reason) {
                Platform.runLater(MainScreen.this::clearPendingMatchState);
            }
        });
    }

    private void clearPendingMatchState() {
        MatchMakingService.stopListeningForCountdown();
        pendingMatchType = PendingMatchType.NONE;
        pendingPrivateMatchCode = "";
        setSearchControlsDisabled(false);
        searchStatusLabel.setText("");
        mainLayout.setDisable(false);

        if (matchOverlay != null) {
            root.getChildren().remove(matchOverlay);
            matchOverlay = null;
        }
    }

    private void onSpectate(String matchId) {
        setSearchControlsDisabled(true);
        SpectateMatchService.spectate(matchId, new SpectateMatchService.SpectateCallback() {
            @Override
            public void onSuccess(String code) {
                Platform.runLater(() -> Screen.transitionToScreen(() -> {
                    Platform.runLater(() -> ScreenManager.setScreen(new SpectatorScreen(code)));
                }, mainLayout));
            }

            @Override
            public void onFailure(String reason) {
                Platform.runLater(() -> {
                    searchStatusLabel.setText("Could not spectate: " + reason);
                    setSearchControlsDisabled(false);
                });
            }
        });
    }

    private void onRefreshMatches() {
        fetchBackendMatchesData(currentMatchQuery);
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
    public void onEscapeKeyPressed() {
        if (pendingMatchType != PendingMatchType.NONE) {
            cancelPendingMatch();
            return;
        }

        Screen.super.onEscapeKeyPressed();
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