package ui.screens;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import config.UserSession;
import core.ScreenManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import network.NetworkContext;
import network.NetworkManager;
import network.ConnectionState;
import ui.service.MatchListService;
import ui.service.MatchMakingService;
import ui.service.MatchChatService;

public class GameScreen implements Screen {

    private static final int BOARD_COLUMNS = 10;
    private static final int BOARD_ROWS = 20;
    private static final int DAS_MS = 100;
    private static final int ARR_MS = 33;

    private final StackPane root;
    private final BorderPane mainLayout;
    private final String matchCode;
    private final boolean spectatorMode;

    private StackPane countdownOverlay;
    private Label countdownNumber;
    private Label roundLabel;
    private Label countdownGetReadyLabel;
    private Label roundResultLabel;
    private Label matchStateLabel;
    private Label boardStatusLabel;
    private Label matchStartTimeLabel;
    private VBox boardArea;
    private GridPane mainBoardGrid;
    private GridPane opponentBoardGrid;
    private final List<Rectangle> mainBoardCells = new ArrayList<>();
    private final List<Rectangle> opponentBoardCells = new ArrayList<>();
    private StackPane pauseOverlay;
    private Button leaveButton;
    private Circle player1Avatar;
    private Circle player2Avatar;
    private Label player1NameLabel;
    private Label player2NameLabel;
    private Label board1TitleLabel;
    private Label board2TitleLabel;
    private Circle board1Avatar;
    private Circle board2Avatar;
    private VBox chatMessagesBox;
    private TextField chatInputField;
    private volatile boolean matchWatchdogRunning = true;
    private volatile boolean leavingForMatchLoss = false;
    private volatile boolean matchStarted = false;
    private volatile boolean leavingInProgress = false;
    private long matchStartWallTime = 0L;

    private Circle[] pingDots;
    private Label pingLabel;

    private boolean isPlayer2 = false;
    private Label p1ScoreLabel;
    private Label p2ScoreLabel;
    private Label spectatorCountLabel;
    private ParallelTransition currentCountdownAnim;
    private FadeTransition dismissFadeAnim;
    private int lastDisplayedCountdownNumber = -1;

    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private Timeline dasTimer;
    private Timeline arrTimer;

    public GameScreen(String matchCode) {
        this(matchCode, false);
    }

    protected GameScreen(String matchCode, boolean spectatorMode) {
        this.matchCode = matchCode;
        this.spectatorMode = spectatorMode;

        root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0F0F14 0%, #0B0B10 100%);");

        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(12, 18, 12, 18));

        mainLayout.setTop(createTopBar());
        mainLayout.setCenter(createCenterContent());

        root.getChildren().add(mainLayout);
        root.setFocusTraversable(true);
        root.setOnMouseClicked(e -> root.requestFocus());
        root.setOnKeyPressed(e -> onKeyDown(e.getCode()));
        root.setOnKeyReleased(e -> onKeyUp(e.getCode()));
        Platform.runLater(root::requestFocus);

        countdownOverlay = buildCountdownOverlay();
        root.getChildren().add(countdownOverlay);

        executarAnimacaoEntrada();
        wireCountdownListener();
        wireMatchStateListener();
        wireChatListener();
        wireMatchResultListener();
        loadMatchDetails();
        startMatchWatchdog();
        updateMatchState("Waiting for match countdown", "#6E6E77");

        UpdatePing(NetworkContext.ping);
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(14);
        topBar.setAlignment(Pos.CENTER);
        topBar.setMinHeight(Region.USE_PREF_SIZE);
        topBar.setMaxWidth(Double.MAX_VALUE);

        Button leaveBtn = new Button(spectatorMode ? "Leave Spectate" : "Leave Match");
        leaveBtn.setStyle("-fx-background-color: #2E2E38; -fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6; -fx-padding: 7 14 7 14; -fx-cursor: hand;");
        leaveBtn.setOnAction(e -> leaveMatch());

        Label codeLabel = new Label("CODE: " + matchCode);
        codeLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        matchStartTimeLabel = new Label("");
        matchStartTimeLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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
        pingLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold;");
        HBox pingBox = new HBox(5, pingLabel, dotsBox);
        pingBox.setAlignment(Pos.CENTER);
        pingBox.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        VBox profileBox = new VBox(3);
        profileBox.setAlignment(Pos.CENTER);
        profileBox.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        byte[] pfpBytes = UserSession.getPfp();
        Circle pfp = new Circle(16);
        if (pfpBytes != null) {
            Image avatarImage = new Image(new ByteArrayInputStream(pfpBytes));
            pfp.setFill(new ImagePattern(avatarImage));
        } else {
            pfp.setFill(Color.web("#2E2E38"));
        }
        pfp.setStroke(Color.web("#00ADB5"));
        pfp.setStrokeWidth(2);

        String user = UserSession.getUsername();
        Label usernameLabel = new Label(user);
        usernameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold;");
        usernameLabel.setMaxWidth(100);

        profileBox.getChildren().addAll(pfp, usernameLabel);

        HBox rightControls = new HBox(14, pingBox, profileBox);
        rightControls.setAlignment(Pos.CENTER_RIGHT);
        rightControls.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        topBar.getChildren().addAll(leaveBtn, codeLabel, matchStartTimeLabel, spacer, rightControls);
        return topBar;
    }

    private ScrollPane createCenterContent() {
        if (spectatorMode) {
            return createSpectatorCenterContent();
        }

        VBox layout = new VBox(12);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(6, 0, 0, 0));
        VBox.setVgrow(layout, Priority.ALWAYS);

        layout.getChildren().add(createPlayersHeader());

        HBox gameArea = new HBox(20);
        gameArea.setAlignment(Pos.CENTER);
        VBox.setVgrow(gameArea, Priority.ALWAYS);

        VBox chatArea = buildChatArea();
        boardArea = buildMainBoardArea();
        VBox opponentArea = buildOpponentBoardArea();

        gameArea.getChildren().addAll(chatArea, boardArea, opponentArea);
        layout.getChildren().add(gameArea);

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scrollPane;
    }

    private ScrollPane createSpectatorCenterContent() {
        VBox layout = new VBox(12);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(6, 0, 0, 0));
        VBox.setVgrow(layout, Priority.ALWAYS);

        layout.getChildren().add(createPlayersHeader());

        HBox gameArea = new HBox(24);
        gameArea.setAlignment(Pos.CENTER);
        VBox.setVgrow(gameArea, Priority.ALWAYS);

        VBox chatArea = buildChatArea();
        VBox boardsArea = buildSpectatorBoardsArea();

        gameArea.getChildren().addAll(chatArea, boardsArea);
        layout.getChildren().add(gameArea);

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scrollPane;
    }

    private VBox createPlayersHeader() {
        VBox container = new VBox(8);
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(34);
        header.setAlignment(Pos.CENTER);

        HBox p1Box = new HBox(15);
        p1Box.setAlignment(Pos.CENTER_LEFT);

        player1Avatar = new Circle(22, Color.web("#2E2E38"));
        player1Avatar.setStroke(Color.web("#00ADB5"));
        player1Avatar.setStrokeWidth(2.2);

        VBox p1Info = new VBox(2);
        p1Info.setAlignment(Pos.CENTER_LEFT);
        player1NameLabel = new Label("Player 1");
        player1NameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16px; -fx-font-weight: 900; -fx-font-family: 'Segoe UI';");
        p1ScoreLabel = new Label("0 x 0");
        p1ScoreLabel.setStyle("-fx-text-fill: #00ADB5; -fx-font-size: 13px; -fx-font-weight: bold;");
        p1Info.getChildren().addAll(player1NameLabel, p1ScoreLabel);
        p1Box.getChildren().addAll(player1Avatar, p1Info);

        Label vsLabel = new Label("VS");
        vsLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 18px; -fx-font-weight: 900; -fx-font-style: italic;");

        HBox p2Box = new HBox(15);
        p2Box.setAlignment(Pos.CENTER_RIGHT);

        VBox p2Info = new VBox(2);
        p2Info.setAlignment(Pos.CENTER_RIGHT);
        player2NameLabel = new Label("Player 2");
        player2NameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16px; -fx-font-weight: 900; -fx-font-family: 'Segoe UI';");
        p2ScoreLabel = new Label("0 x 0");
        p2ScoreLabel.setStyle("-fx-text-fill: #FF4A4A; -fx-font-size: 13px; -fx-font-weight: bold;");
        p2Info.getChildren().addAll(player2NameLabel, p2ScoreLabel);

        player2Avatar = new Circle(22, Color.web("#2E2E38"));
        player2Avatar.setStroke(Color.web("#FF4A4A"));
        player2Avatar.setStrokeWidth(2.2);
        p2Box.getChildren().addAll(p2Info, player2Avatar);

        header.getChildren().addAll(p1Box, vsLabel, p2Box);

        matchStateLabel = new Label();
        matchStateLabel.setStyle("-fx-background-color: rgba(0, 173, 181, 0.12); -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-color: rgba(0, 173, 181, 0.35); -fx-text-fill: #00ADB5; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 14 6 14;");

        spectatorCountLabel = new Label("0 spectators");
        spectatorCountLabel.setStyle("-fx-background-color: rgba(255, 183, 77, 0.10); -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-color: #FFB74D44; -fx-text-fill: #FFB74D; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;");
        spectatorCountLabel.setVisible(false);

        HBox statusRow = new HBox(12, matchStateLabel, spectatorCountLabel);
        statusRow.setAlignment(Pos.CENTER);

        container.getChildren().addAll(header, statusRow);
        return container;
    }

    private VBox buildChatArea() {
        VBox chat = new VBox(10);
        chat.setPrefWidth(280);
        chat.setMaxWidth(280);
        chat.setPadding(new Insets(12));
        chat.setStyle("-fx-background-color: #14141C; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #2E2E38; -fx-border-width: 1;");

        Label title = new Label("CHAT");
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 800; -fx-letter-spacing: 1px;");

        chatMessagesBox = new VBox(8);
        chatMessagesBox.setPadding(new Insets(6));

        ScrollPane scroll = new ScrollPane(chatMessagesBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0D0D12; -fx-background-color: transparent; -fx-border-color: #2B2B36; -fx-border-radius: 8; -fx-background-radius: 8;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox inputArea = new HBox(8);
        chatInputField = new TextField();
        chatInputField.setPromptText("Message...");
        chatInputField.setStyle("-fx-background-color: #1E1E26; -fx-text-fill: #FFFFFF; -fx-prompt-text-fill: #5C5C64; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #2E2E38; -fx-border-width: 1; -fx-padding: 7;");
        chatInputField.textProperty().addListener((obs, oldValue, newValue) -> {
            String normalized = normalizeChatDraft(newValue);
            if (!normalized.equals(newValue)) {
                chatInputField.setText(normalized);
            }
        });
        HBox.setHgrow(chatInputField, Priority.ALWAYS);

        Button send = new Button("→");
        send.setStyle("-fx-background-color: #00ADB5; -fx-text-fill: #0F0F14; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;");
        send.setOnAction(e -> sendChatMessage());
        chatInputField.setOnAction(e -> sendChatMessage());

        inputArea.getChildren().addAll(chatInputField, send);
        chat.getChildren().addAll(title, scroll, inputArea);

        return chat;
    }

    private VBox buildMainBoardArea() {
        VBox wrapper = new VBox(8);
        wrapper.setAlignment(Pos.CENTER);

        Label myLabel = new Label("YOU");
        myLabel.setStyle("-fx-text-fill: #00ADB5; -fx-font-size: 11px; -fx-font-weight: 800; -fx-letter-spacing: 2px;");

        StackPane boardFrame = new StackPane();
        boardFrame.setMinSize(300, 600);
        boardFrame.setMaxSize(300, 600);
        boardFrame.setStyle("-fx-background-color: #0D0D12; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #00ADB5; -fx-border-width: 2;");

        GridPane grid = buildBoardGrid(28, mainBoardCells);
        mainBoardGrid = grid;

        boardStatusLabel = new Label("Awaiting start");
        boardStatusLabel.setStyle("-fx-text-fill: #00ADB5; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: rgba(13, 13, 18, 0.85); -fx-padding: 10 18 10 18; -fx-background-radius: 8;");

        StackPane overlay = new StackPane(boardStatusLabel);

        boardFrame.getChildren().addAll(grid, overlay);
        boardFrame.setClip(createBoardClip(300, 600, 12));
        wrapper.getChildren().addAll(myLabel, boardFrame);
        return wrapper;
    }

    private VBox buildOpponentBoardArea() {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #14141C; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #2E2E38; -fx-border-width: 1;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        board2Avatar = new Circle(14, Color.web("#2E2E38"));
        board2Avatar.setStroke(Color.web("#FF4A4A"));
        board2Avatar.setStrokeWidth(2);

        VBox labels = new VBox(2);
        Label roleLabel = new Label("OPONENTE");
        roleLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");
        board2TitleLabel = new Label("Player 2");
        board2TitleLabel.setStyle("-fx-text-fill: #FF4A4A; -fx-font-size: 13px; -fx-font-weight: bold;");
        labels.getChildren().addAll(roleLabel, board2TitleLabel);

        header.getChildren().addAll(board2Avatar, labels);

        StackPane boardFrame = new StackPane();
        boardFrame.setMinSize(220, 440);
        boardFrame.setMaxSize(220, 440);
        boardFrame.setStyle("-fx-background-color: #0D0D12; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #FF4A4A44; -fx-border-width: 2;");

        GridPane smallGrid = buildBoardGrid(20, opponentBoardCells);
        opponentBoardGrid = smallGrid;
        boardFrame.getChildren().add(smallGrid);
        boardFrame.setClip(createBoardClip(220, 440, 10));

        card.getChildren().addAll(header, boardFrame);
        return card;
    }

    private VBox buildSpectatorBoardsArea() {
        VBox boardsArea = new VBox(18);
        boardsArea.setAlignment(Pos.CENTER);

        HBox boardsRow = new HBox(22);
        boardsRow.setAlignment(Pos.CENTER);

        boardsRow.getChildren().addAll(
            buildSpectatorBoardCard("PLAYER 1", "Player 1", Color.web("#00ADB5"), true),
            buildSpectatorBoardCard("PLAYER 2", "Player 2", Color.web("#FF4A4A"), false)
        );

        boardsArea.getChildren().add(boardsRow);
        return boardsArea;
    }

    private VBox buildSpectatorBoardCard(String role, String nameText, Color accent, boolean firstBoard) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.setPrefWidth(318);
        card.setStyle("-fx-background-color: #14141C; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #2E2E38; -fx-border-width: 1;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Circle avatar = new Circle(16, Color.web("#2E2E38"));
        avatar.setStroke(accent);
        avatar.setStrokeWidth(2);

        VBox labelsBox = new VBox(2);
        Label roleLabel = new Label(role);
        roleLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");
        Label nameLabel = new Label(nameText);
        nameLabel.setStyle("-fx-text-fill: " + toHex(accent) + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        labelsBox.getChildren().addAll(roleLabel, nameLabel);

        header.getChildren().addAll(avatar, labelsBox);

        StackPane boardFrame = new StackPane();
        boardFrame.setMinSize(300, 600);
        boardFrame.setMaxSize(300, 600);
        boardFrame.setStyle("-fx-background-color: #0D0D12; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #2B2B36; -fx-border-width: 2;");
        boardFrame.setClip(createBoardClip(300, 600, 12));

        List<Rectangle> cellList = firstBoard ? mainBoardCells : opponentBoardCells;
        boardFrame.getChildren().add(buildBoardGrid(28, cellList));

        if (firstBoard) {
            board1TitleLabel = nameLabel;
            board1Avatar = avatar;
        } else {
            board2TitleLabel = nameLabel;
            board2Avatar = avatar;
        }

        card.getChildren().addAll(header, boardFrame);
        return card;
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int) Math.round(color.getRed() * 255),
            (int) Math.round(color.getGreen() * 255),
            (int) Math.round(color.getBlue() * 255));
    }

    private GridPane buildBoardGrid(double cellSize, List<Rectangle> cellStore) {
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(4));

        for (int i = 0; i < BOARD_COLUMNS; i++) {
            ColumnConstraints column = new ColumnConstraints(cellSize);
            column.setMinWidth(cellSize);
            column.setPrefWidth(cellSize);
            column.setMaxWidth(cellSize);
            grid.getColumnConstraints().add(column);
        }

        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLUMNS; col++) {
                Rectangle cell = new Rectangle(cellSize, cellSize);
                cell.setArcWidth(3);
                cell.setArcHeight(3);
                cell.setFill(Color.web((row + col) % 2 == 0 ? "#17171F" : "#14141B"));
                cell.setStroke(Color.web("#23232D"));
                cell.setStrokeWidth(0.5);
                cellStore.add(cell);
                grid.add(cell, col, row);
            }
        }

        return grid;
    }

    private Rectangle createBoardClip(double width, double height, double radius) {
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        return clip;
    }

    private StackPane buildCountdownOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(8, 8, 12, 0.85);");

        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);

        roundLabel = new Label("ROUND 1");
        roundLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 26px; -fx-font-weight: 900; -fx-letter-spacing: 3px;");

        countdownGetReadyLabel = new Label("GET READY");
        countdownGetReadyLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 800; -fx-letter-spacing: 3px;");

        countdownNumber = new Label("5");
        countdownNumber.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 80));
        countdownNumber.setStyle("-fx-text-fill: #00ADB5;");
        countdownNumber.setEffect(new DropShadow(30, Color.web("#00ADB5", 0.6)));

        roundResultLabel = new Label("");
        roundResultLabel.setStyle("-fx-text-fill: #FFB74D; -fx-font-size: 15px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        roundResultLabel.setVisible(false);

        content.getChildren().addAll(roundLabel, countdownGetReadyLabel, countdownNumber, roundResultLabel);
        overlay.getChildren().add(content);
        return overlay;
    }

    private void wireCountdownListener() {
        MatchMakingService.listenForCountdown(new MatchMakingService.CountdownListener() {
            @Override
            public void onTick(String code, int secondsLeft) {
                if (!matchCode.equals(code)) return;
                Platform.runLater(() -> {
                    animateCountdownNumber(secondsLeft);
                    updateMatchState("Starts in " + secondsLeft + "s", "#00ADB5");
                    if (boardStatusLabel != null) boardStatusLabel.setText("Countdown");
                });
            }

            @Override
            public void onMatchStarted(String code) {
                if (!matchCode.equals(code)) return;
                Platform.runLater(() -> {
                    boolean wasStarted = matchStarted;
                    matchStarted = true;
                    if (!wasStarted) {
                        matchStartWallTime = System.currentTimeMillis();
                        showStartTime(matchStartWallTime);
                    }
                    updateMatchState("Match live", "#00E676");
                    if (boardStatusLabel != null) boardStatusLabel.setVisible(false);
                    dismissCountdownOverlay();
                });
            }

            @Override
            public void onMatchCancelled(String code, String reason) {
                if (!matchCode.equals(code)) return;
                Platform.runLater(() -> {
                    if (matchWatchdogRunning) leaveMatch();
                });
            }
        });
    }

    private void showStartTime(long startMs) {
        if (matchStartTimeLabel == null) return;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        String startStr = LocalTime.ofInstant(Instant.ofEpochMilli(startMs), ZoneId.systemDefault()).format(fmt);
        matchStartTimeLabel.setText("Início: " + startStr);
    }

    private void wireMatchStateListener() {
        NetworkContext.matchStateListener = (stateMatchCode, state, payload) -> {
            if (!matchCode.equals(stateMatchCode)) return;
            Platform.runLater(() -> applyMatchState(state, payload));
        };
    }

    private void applyMatchState(String state, String payload) {
        String normalizedState = state == null ? "" : state.trim().toUpperCase();
        String raw = payload == null ? "" : payload.trim();

        if ("ROUND_START".equals(normalizedState)) {
            String[] parts = raw.split("\\|", 2);
            String round = parts.length > 0 ? parts[0] : "1";
            String seconds = parts.length > 1 ? parts[1] : "5";
            lastDisplayedCountdownNumber = parseIntSafe(seconds);
            updateMatchState("ROUND " + round, "#00ADB5");
            if (boardStatusLabel != null) boardStatusLabel.setVisible(false);
            roundLabel.setText("ROUND " + round);
            countdownGetReadyLabel.setVisible(true);
            countdownNumber.setText(String.valueOf(parseIntSafe(seconds)));
            countdownNumber.setScaleX(1.0);
            countdownNumber.setScaleY(1.0);
            countdownNumber.setOpacity(1.0);
            countdownNumber.setVisible(true);
            roundResultLabel.setVisible(false);
            showCountdownOverlay();
            return;
        }

        if ("IN_PROGRESS".equals(normalizedState)) {
            if (!matchStarted) {
                matchStarted = true;
                if (matchStartWallTime == 0L) {
                    matchStartWallTime = System.currentTimeMillis();
                    showStartTime(matchStartWallTime);
                }
            }
            dismissCountdownOverlay();
            updateMatchState("Match live", "#00E676");
            applyProgressPayload(raw);
            return;
        }

        if ("ROUND_END".equals(normalizedState)) {
            String[] parts = raw.split("\\|", 5);
            String round = parts.length > 0 ? parts[0] : "";
            String winner = parts.length > 1 ? parts[1] : "";
            String reason = parts.length > 2 ? parts[2] : "";
            String wins1 = parts.length > 3 ? parts[3] : "0";
            String wins2 = parts.length > 4 ? parts[4] : "0";
            updateMatchState("Round " + round + " ended", "#FFB74D");
            if (boardStatusLabel != null) boardStatusLabel.setVisible(false);
            String resultText = "Winner: " + winner + "   " + wins1 + " x " + wins2;
            if (reason != null && !reason.isBlank()) resultText += "   •   " + reason.replace("_", " ");
            roundLabel.setText("ROUND " + round + " ENDED");
            roundResultLabel.setText(resultText);
            countdownGetReadyLabel.setVisible(false);
            countdownNumber.setVisible(false);
            roundResultLabel.setVisible(true);
            showCountdownOverlay();
        }
    }

    private void applyProgressPayload(String payload) {
        String[] parts = payload.split("\\|", 11);
        if (parts.length < 11) return;

        String round = parts[0];
        String wins1 = parts[1];
        String wins2 = parts[2];
        String board1 = parts[5];
        String board2 = parts[6];

        updateMatchState("Round " + round, "#00E676");
        if (boardStatusLabel != null) boardStatusLabel.setVisible(false);
        if (p1ScoreLabel != null) p1ScoreLabel.setText(wins1 + " WINS");
        if (p2ScoreLabel != null) p2ScoreLabel.setText(wins2 + " WINS");

        if (spectatorMode) {
            renderBoard(mainBoardCells, board1);
            renderBoard(opponentBoardCells, board2);
        } else if (isPlayer2) {
            renderBoard(mainBoardCells, board2);
            renderBoard(opponentBoardCells, board1);
        } else {
            renderBoard(mainBoardCells, board1);
            renderBoard(opponentBoardCells, board2);
        }
    }

    private void renderBoard(List<Rectangle> cells, String board) {
        if (cells == null || board == null) return;

        Color[] palette = new Color[] {
            Color.TRANSPARENT,
            Color.web("#00ADB5"),
            Color.web("#FFD166"),
            Color.web("#9B5DE5"),
            Color.web("#00E676"),
            Color.web("#FF4A4A"),
            Color.web("#3A86FF"),
            Color.web("#F8961E"),
            Color.web("#6C757D")
        };

        for (int i = 0; i < cells.size() && i < board.length(); i++) {
            char ch = board.charAt(i);
            int idx = ch >= '0' && ch <= '9' ? ch - '0' : 0;
            if (idx < 0 || idx >= palette.length) idx = 0;
            cells.get(i).setFill(palette[idx]);
        }
    }

    private int parseIntSafe(String value) {
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return 0; }
    }

    private void wireChatListener() {
        NetworkContext.chatListener = (incomingMatchCode, senderId, senderName, message) -> {
            if (!matchCode.equalsIgnoreCase(incomingMatchCode)) return;
            Platform.runLater(() -> appendChatMessage(senderId, senderName, message));
        };
    }

    private void wireMatchResultListener() {
        NetworkContext.matchResultListener = (incomingMatchCode, outcome, reason, startMs, endMs) -> {
            if (!matchCode.equalsIgnoreCase(incomingMatchCode)) return;
            Platform.runLater(() -> {
                matchWatchdogRunning = false;
                ScreenManager.setScreen(new MatchResultScreen(matchCode, outcome, reason, startMs, endMs));
            });
        };
    }

    private void loadMatchDetails() {
        MatchListService.fetchMatchInfo(matchCode, new MatchListService.MatchListCallback() {
            @Override
            public void onSuccess(java.util.List<MatchListService.LiveMatch> matches) {
                if (matches == null || matches.isEmpty()) return; // details are cosmetic — don't abort the match
                MatchListService.LiveMatch finalSelected = matches.get(0);
                Platform.runLater(() -> applyMatchDetails(finalSelected));
            }

            @Override
            public void onFailure(String reason) { /* details fetch failed — not fatal, match continues */ }
        });
    }

    private void startMatchWatchdog() {
        Thread.startVirtualThread(() -> {
            while (matchWatchdogRunning) {
                try { Thread.sleep(4000); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); return;
                }
                if (!matchWatchdogRunning || leavingForMatchLoss) return;
                if (!matchStarted) continue;
                if (NetworkContext.tcpState != ConnectionState.CONNECTED) { switchToLoading(); return; }

                MatchListService.fetchMatchInfo(matchCode, new MatchListService.MatchListCallback() {
                    @Override
                    public void onSuccess(java.util.List<MatchListService.LiveMatch> matches) {
                        if (!matchWatchdogRunning || leavingForMatchLoss) return;
                        if (matches == null || matches.isEmpty()) { switchToLoading(); return; }
                        MatchListService.LiveMatch m = matches.get(0);
                        Platform.runLater(() -> updateSpectatorCount(m.spectators));
                    }
                    @Override
                    public void onFailure(String reason) {
                        if (!matchWatchdogRunning || leavingForMatchLoss) return;
                        switchToLoading();
                    }
                });
            }
        });
    }

    private void switchToLoading() {
        if (leavingForMatchLoss) return;
        leavingForMatchLoss = true;
        matchWatchdogRunning = false;
        MatchMakingService.stopListeningForCountdown();
        network.NetworkContext.matchStateListener = null;
        network.NetworkContext.chatListener = null;
        network.NetworkManager.notifyConnectionDrop();
    }

    private void applyMatchDetails(MatchListService.LiveMatch match) {
        if (match == null) return;
        String me = config.UserSession.getUsername();
        isPlayer2 = me != null && me.equalsIgnoreCase(match.player2);
        updateHeaderSlot(player1NameLabel, player1Avatar, match.player1, match.player1Pfp, "Player 1", Color.web("#00ADB5"));
        updateHeaderSlot(player2NameLabel, player2Avatar, match.player2, match.player2Pfp, "Player 2", Color.web("#FF4A4A"));
        updateBoardSlot(board1TitleLabel, board1Avatar, match.player1, match.player1Pfp, Color.web("#00ADB5"));
        updateBoardSlot(board2TitleLabel, board2Avatar, match.player2, match.player2Pfp, Color.web("#FF4A4A"));
        if (!spectatorMode) {
            String opponentName = isPlayer2 ? match.player1 : match.player2;
            String opponentPfp  = isPlayer2 ? match.player1Pfp : match.player2Pfp;
            updateBoardSlot(board2TitleLabel, board2Avatar, opponentName, opponentPfp, Color.web("#FF4A4A"));
        }
        if (match.startTimeMillis > 0) showStartTime(match.startTimeMillis);
        updateSpectatorCount(match.spectators);
    }

    private void updateSpectatorCount(int count) {
        if (spectatorCountLabel == null) return;
        String text = count == 1 ? "1 spectator" : count + " spectators";
        spectatorCountLabel.setText(text);
        spectatorCountLabel.setVisible(true);
    }

    private void updateHeaderSlot(Label nameLabel, Circle avatar, String name, String base64, String fallbackName, Color accent) {
        if (nameLabel != null) nameLabel.setText(name != null && !name.isBlank() ? name : fallbackName);
        applyAvatar(avatar, base64, Color.web("#2E2E38"));
        if (avatar != null) avatar.setStroke(accent);
    }

    private void updateBoardSlot(Label nameLabel, Circle avatar, String name, String base64, Color accent) {
        if (nameLabel != null) nameLabel.setText(name != null && !name.isBlank() ? name : "Unknown");
        applyAvatar(avatar, base64, Color.web("#2E2E38"));
        if (avatar != null) avatar.setStroke(accent);
    }

    private void applyAvatar(Circle avatar, String base64, Color fallbackColor) {
        if (avatar == null) return;
        if (base64 != null && !base64.isBlank()) {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(base64.trim());
                Image avatarImage = new Image(new ByteArrayInputStream(bytes));
                if (!avatarImage.isError()) { avatar.setFill(new ImagePattern(avatarImage)); return; }
            } catch (IllegalArgumentException ignored) {}
        }
        avatar.setFill(fallbackColor);
    }

    private void appendChatMessage(int senderId, String senderName, String message) {
        if (chatMessagesBox == null) return;
        if (senderId == 0) return;
        String safeMessage = MatchChatService.sanitize(message);
        if (safeMessage.isEmpty()) return;

        String currentUser = UserSession.getUsername();
        boolean isOwnMessage = currentUser != null && senderName != null && currentUser.equalsIgnoreCase(senderName);
        String senderLabel = isOwnMessage ? "You" : resolveSenderName(senderId, senderName);
        Label line = new Label(senderLabel + ": " + safeMessage);
        line.setWrapText(true);
        line.setMaxWidth(Double.MAX_VALUE);
        if (isOwnMessage) {
            line.setStyle("-fx-text-fill: #06191A; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: linear-gradient(to right, rgba(0,173,181,0.95), rgba(0,231,255,0.85)); -fx-background-radius: 12; -fx-padding: 9 12 9 12;");
        } else {
            line.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-background-color: #1A1A22; -fx-background-radius: 12; -fx-padding: 9 12 9 12;");
        }
        chatMessagesBox.getChildren().add(line);
    }

    private String resolveSenderName(int senderId, String senderName) {
        if (senderName != null && !senderName.isBlank()) return senderName;
        if (senderId <= 0) return "System";
        return "Player " + senderId;
    }

    private String normalizeChatDraft(String raw) {
        String safe = raw == null ? "" : raw.replaceAll("\\s{2,}", " ");
        safe = safe.replaceFirst("^\\s+", "");
        if (safe.length() > MatchChatService.MAX_CHAT_LENGTH) safe = safe.substring(0, MatchChatService.MAX_CHAT_LENGTH);
        return safe;
    }

    private void sendChatMessage() {
        if (chatInputField == null) return;
        String text = chatInputField.getText();
        String safe = MatchChatService.sanitize(text);
        if (safe.isEmpty()) return;
        try {
            MatchChatService.send(matchCode, safe);
            chatInputField.clear();
        } catch (Exception e) {
            // send failure silently ignored
        }
    }

    private void onKeyDown(KeyCode keyCode) {
        if (chatInputField != null && chatInputField.isFocused()) return;
        if (!matchStarted || leavingForMatchLoss || spectatorMode) return;

        if (pressedKeys.add(keyCode)) {
            handleGameKey(keyCode);
            if (keyCode == KeyCode.LEFT || keyCode == KeyCode.RIGHT || keyCode == KeyCode.DOWN) {
                startDAS(keyCode);
            }
        }
    }

    private void onKeyUp(KeyCode keyCode) {
        pressedKeys.remove(keyCode);
        if (keyCode == KeyCode.LEFT || keyCode == KeyCode.RIGHT || keyCode == KeyCode.DOWN) {
            stopDAS();
        }
    }

    private void startDAS(KeyCode key) {
        stopDAS();
        if (key == KeyCode.DOWN) {
            startARR(key);
            return;
        }
        dasTimer = new Timeline(new KeyFrame(Duration.millis(DAS_MS), e -> startARR(key)));
        dasTimer.setCycleCount(1);
        dasTimer.play();
    }

    private void startARR(KeyCode key) {
        arrTimer = new Timeline(new KeyFrame(Duration.millis(ARR_MS), e -> {
            if (pressedKeys.contains(key) && matchStarted && !leavingForMatchLoss) {
                handleGameKey(key);
            } else {
                stopDAS();
            }
        }));
        arrTimer.setCycleCount(Timeline.INDEFINITE);
        arrTimer.play();
    }

    private void stopDAS() {
        if (dasTimer != null) { dasTimer.stop(); dasTimer = null; }
        if (arrTimer != null) { arrTimer.stop(); arrTimer = null; }
    }

    private void handleGameKey(KeyCode keyCode) {
        String action = null;
        if (keyCode == KeyCode.LEFT)       action = "LEFT";
        else if (keyCode == KeyCode.RIGHT) action = "RIGHT";
        else if (keyCode == KeyCode.DOWN)  action = "DOWN";
        else if (keyCode == KeyCode.R)     action = "ROTATE";
        else if (keyCode == KeyCode.SPACE) action = "DROP";
        if (action != null) sendGameAction(action);
    }

    private void sendGameAction(String action) {
        String token = UserSession.getToken();
        if (token == null || token.isBlank()) return;
        NetworkManager.sendUDP("MOVE 0 0 " + token + " " + matchCode + " " + action);
    }

    private void animateCountdownNumber(int secondsLeft) {
        if (secondsLeft == lastDisplayedCountdownNumber) return;
        lastDisplayedCountdownNumber = secondsLeft;

        if (currentCountdownAnim != null) {
            currentCountdownAnim.stop();
            currentCountdownAnim = null;
        }
        countdownNumber.setScaleX(1.0);
        countdownNumber.setScaleY(1.0);
        countdownNumber.setOpacity(1.0);
        countdownNumber.setText(String.valueOf(secondsLeft));

        ScaleTransition pop = new ScaleTransition(Duration.millis(300), countdownNumber);
        pop.setFromX(1.4); pop.setFromY(1.4);
        pop.setToX(1.0);  pop.setToY(1.0);
        pop.setInterpolator(Interpolator.EASE_OUT);

        currentCountdownAnim = new ParallelTransition(pop);
        currentCountdownAnim.setOnFinished(e -> currentCountdownAnim = null);
        currentCountdownAnim.play();
    }

    private void dismissCountdownOverlay() {
        if (countdownOverlay == null || !root.getChildren().contains(countdownOverlay)) return;
        if (dismissFadeAnim != null) dismissFadeAnim.stop();
        dismissFadeAnim = new FadeTransition(Duration.millis(350), countdownOverlay);
        dismissFadeAnim.setToValue(0.0);
        dismissFadeAnim.setOnFinished(e -> { root.getChildren().remove(countdownOverlay); dismissFadeAnim = null; });
        dismissFadeAnim.play();
    }

    private void showCountdownOverlay() {
        if (dismissFadeAnim != null) { dismissFadeAnim.stop(); dismissFadeAnim = null; }
        if (countdownOverlay != null) {
            countdownOverlay.setOpacity(1.0);
            if (!root.getChildren().contains(countdownOverlay)) root.getChildren().add(countdownOverlay);
        }
    }

    private void updateMatchState(String text, String accentColor) {
        matchStateLabel.setText(text);
        matchStateLabel.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.12); -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-color: "
            + accentColor + "55; -fx-text-fill: " + accentColor + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 14 6 14;"
        );
    }

    private void executarAnimacaoEntrada() {
        mainLayout.setOpacity(0.0);
        mainLayout.setTranslateY(20.0);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.45), mainLayout);
        fadeIn.setToValue(1.0);
        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(0.45), mainLayout);
        moveUp.setToY(0.0);
        moveUp.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fadeIn, moveUp).play();
    }

    public void leaveMatch() {
        if (leavingInProgress) return;
        leavingInProgress = true;
        if (leaveButton != null) Platform.runLater(() -> leaveButton.setDisable(true));
        stopDAS();
        matchWatchdogRunning = false;
        MatchMakingService.stopListeningForCountdown();
        network.NetworkContext.matchStateListener = null;
        network.NetworkContext.chatListener = null;
        NetworkContext.retryPaused = false;
        hidePauseOverlay();
        MatchMakingService.leaveCurrentMatch(new MatchMakingService.MatchResultCallback() {
            @Override
            public void onSuccess(String action, String matchCode) {
                Platform.runLater(() -> ScreenManager.setScreen(new MainScreen()));
            }
            @Override
            public void onFailure(String reason) {
                Platform.runLater(() -> ScreenManager.setScreen(new MainScreen()));
            }
        });
    }

    private void showPauseOverlay() {
        if (pauseOverlay != null && root.getChildren().contains(pauseOverlay)) return;

        pauseOverlay = new StackPane();
        pauseOverlay.setStyle("-fx-background-color: rgba(8, 8, 12, 0.70);");
        pauseOverlay.setAlignment(Pos.CENTER);

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));
        card.setMaxWidth(300);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setStyle("-fx-background-color: rgba(20, 20, 28, 0.92); -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-width: 1;");

        Label title = new Label("PAUSED");
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 26px; -fx-font-weight: 900; -fx-letter-spacing: 4px;");

        Label subtitle = new Label("The match is waiting.");
        subtitle.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 12px;");

        Button resumeButton = new Button("RESUME");
        resumeButton.setStyle("-fx-background-color: #00ADB5; -fx-text-fill: #0F0F14; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 26 12 26; -fx-cursor: hand;");
        resumeButton.setOnAction(e -> hidePauseOverlay());

        leaveButton = new Button("LEAVE MATCH");
        leaveButton.setStyle("-fx-background-color: #2E2E38; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 26 12 26; -fx-cursor: hand;");
        leaveButton.setOnAction(e -> leaveMatch());

        card.getChildren().addAll(title, subtitle, resumeButton, leaveButton);
        pauseOverlay.getChildren().add(card);
        root.getChildren().add(pauseOverlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(120), pauseOverlay);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0); fadeIn.play();
    }

    private void hidePauseOverlay() {
        if (pauseOverlay == null || !root.getChildren().contains(pauseOverlay)) return;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(120), pauseOverlay);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> root.getChildren().remove(pauseOverlay));
        fadeOut.play();
    }

    @Override
    public void UpdatePing(int ms) {
        Platform.runLater(() -> {
            int activeDots = 0;
            Color dotColor = Color.TRANSPARENT;
            if (ms < 0 || ms >= 1000) {
                pingLabel.setText("📶 -- ms"); activeDots = 0;
            } else if (ms < 100) {
                pingLabel.setText("📶 " + ms + " ms"); activeDots = 3; dotColor = Color.web("#00E676");
            } else if (ms < 200) {
                pingLabel.setText("📶 " + ms + " ms"); activeDots = 2; dotColor = Color.web("#FF9100");
            } else {
                pingLabel.setText("📶 " + ms + " ms"); activeDots = 1; dotColor = Color.web("#FF4A4A");
            }
            if (pingDots != null) {
                for (int i = 0; i < 3; i++) {
                    if (i < activeDots) {
                        pingDots[i].setFill(dotColor); pingDots[i].setStroke(dotColor);
                    } else {
                        pingDots[i].setFill(Color.TRANSPARENT); pingDots[i].setStroke(Color.web("#5C5C64"));
                    }
                }
            }
        });
    }

    @Override
    public void onEscapeKeyPressed() {
        if (pauseOverlay != null && root.getChildren().contains(pauseOverlay)) { hidePauseOverlay(); return; }
        showPauseOverlay();
    }

    public VBox getBoardArea() { return boardArea; }

    @Override
    public Boolean isPingDisplayed() { return true; }

    @Override
    public Parent getRoot() { return root; }
}
