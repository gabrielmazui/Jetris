package ui.screens;

import config.UserSession;
import core.ScreenManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import network.NetworkCallback;
import network.NetworkContext;
import ui.service.GetProfileService;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ProfileScreen implements Screen {

    private final StackPane root;
    private BorderPane mainLayout;
    private Circle[] pingDots;
    private Label pingLabel;

    private final String targetUsername;
    private final boolean isOwnProfile;

    private Circle pfpCircle;
    private Label usernameLabel;
    private VBox winsBox;
    private VBox lossesBox;
    private VBox playedBox;
    private VBox winRateBox;
    private VBox matchHistoryList;
    private Label pageInfoLabel;
    private Button prevBtn;
    private Button nextBtn;
    private Label statusLabel;

    private int currentPage = 1;
    private int totalPages  = 1;

    private static final int TIMEOUT_SECONDS = 5;

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

    private static final String WIN_CARD_STYLE = """
        -fx-background-color: #1A2A1E;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: #2E6B3A;
        -fx-border-width: 1;
        -fx-padding: 16;
    """;

    private static final String LOSS_CARD_STYLE = """
        -fx-background-color: #2A1A1A;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: #6B2E2E;
        -fx-border-width: 1;
        -fx-padding: 16;
    """;

    private static final String SCROLL_PANE_STYLE = """
        -fx-background-color: transparent;
        -fx-background: #0F0F14;
    """;

    public ProfileScreen(String targetUsername) {
        this.targetUsername = targetUsername;
        this.isOwnProfile   = targetUsername.equals(UserSession.getUsername());

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

        Platform.runLater(() -> loadProfile(1));
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER);
        topBar.setSpacing(20);

        Label title = new Label("JETRIS");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 24));
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-letter-spacing: 2px;");
        title.setEffect(new DropShadow(10, Color.web("#00ADB5", 0.5)));

        Button backBtn = new Button("Main Screen");
        backBtn.setStyle(SECONDARY_BUTTON_STYLE);
        applyButtonEffects(backBtn, "#2E2E38", "#3E3E4A");
        backBtn.setOnAction(e -> ScreenManager.setScreen(new MainScreen()));

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
        pingLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox pingBox = new HBox(6, pingLabel, dotsBox);
        pingBox.setAlignment(Pos.CENTER);

        topBar.getChildren().addAll(title, backBtn, spacer, pingBox);
        return topBar;
    }

    private VBox createCenterContent() {
        VBox center = new VBox(24);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(30, 0, 0, 0));
        center.setMaxWidth(700);

        statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(600);
        statusLabel.setStyle("-fx-text-fill: #5C5C64; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold;");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        HBox profileCard = new HBox(24);
        profileCard.setStyle("""
            -fx-background-color: #1E1E26;
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-border-color: #2E2E38;
            -fx-border-width: 1;
            -fx-padding: 28;
        """);
        profileCard.setMaxWidth(600);
        profileCard.setAlignment(Pos.CENTER_LEFT);

        pfpCircle = new Circle(48);
        pfpCircle.setFill(Color.web("#2E2E38"));
        pfpCircle.setStroke(Color.web("#00ADB5"));
        pfpCircle.setStrokeWidth(3);

        VBox infoBox = new VBox(12);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        usernameLabel = new Label("Loading...");
        usernameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 22px; -fx-font-weight: 800;");

        HBox statsRow = new HBox(24);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        winsBox    = makeStat("Wins",     "--", "#00E676");
        lossesBox  = makeStat("Losses",   "--", "#FF4A4A");
        playedBox  = makeStat("Played",   "--", "#00ADB5");
        winRateBox = makeStat("Win Rate", "--", "#FF9100");

        statsRow.getChildren().addAll(winsBox, lossesBox, playedBox, winRateBox);
        infoBox.getChildren().addAll(usernameLabel, statsRow);
        profileCard.getChildren().addAll(pfpCircle, infoBox);

        VBox historySection = new VBox(12);
        historySection.setMaxWidth(600);

        Label histTitle = new Label("MATCH HISTORY");
        histTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 800; -fx-letter-spacing: 1px;");

        matchHistoryList = new VBox(8);

        ScrollPane scroll = new ScrollPane(matchHistoryList);
        scroll.setFitToWidth(true);
        scroll.setStyle(SCROLL_PANE_STYLE);
        scroll.setPrefHeight(360);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        HBox pagination = new HBox(14);
        pagination.setAlignment(Pos.CENTER);
        pagination.setPadding(new Insets(8, 0, 0, 0));

        prevBtn = new Button("< Prev");
        prevBtn.setStyle(SECONDARY_BUTTON_STYLE);
        applyButtonEffects(prevBtn, "#2E2E38", "#3E3E4A");
        prevBtn.setDisable(true);
        prevBtn.setOnAction(e -> { if (currentPage > 1) loadProfile(currentPage - 1); });

        pageInfoLabel = new Label("Page 1 of 1");
        pageInfoLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold;");

        nextBtn = new Button("Next >");
        nextBtn.setStyle(SECONDARY_BUTTON_STYLE);
        applyButtonEffects(nextBtn, "#2E2E38", "#3E3E4A");
        nextBtn.setDisable(true);
        nextBtn.setOnAction(e -> { if (currentPage < totalPages) loadProfile(currentPage + 1); });

        pagination.getChildren().addAll(prevBtn, pageInfoLabel, nextBtn);
        historySection.getChildren().addAll(histTitle, scroll, pagination);

        VBox wrapper = new VBox(20, statusLabel, profileCard, historySection);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(600);

        ScrollPane outerScroll = new ScrollPane(wrapper);
        outerScroll.setFitToWidth(true);
        outerScroll.setStyle(SCROLL_PANE_STYLE);
        outerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        center.getChildren().add(outerScroll);
        VBox.setVgrow(outerScroll, Priority.ALWAYS);

        return center;
    }

    private HBox createBottomBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 0, 0, 0));
        Label credits = new Label("Created by Gabriel Mazui");
        credits.setStyle("-fx-text-fill: #5C5C64; -fx-font-family: 'Segoe UI'; -fx-font-size: 11px;");
        bar.getChildren().add(credits);
        return bar;
    }

    private void loadProfile(int page) {
        setStatus("Loading profile...", "#5C5C64");
        root.setDisable(true);

        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        CountDownLatch trava = new CountDownLatch(1);
        AtomicBoolean sucesso = new AtomicBoolean(false);
        AtomicReference<String> erroRef = new AtomicReference<>("TIMEOUT");

        GetProfileService.getProfile(targetUsername, page, callbackId, new NetworkCallback(callbackId) {
            @Override
            public void onSuccess(String res) {
                sucesso.set(true);
                erroRef.set(res);
                trava.countDown();
            }

            @Override
            public void onFailure(String err) {
                erroRef.set(err);
                trava.countDown();
            }
        });

        Thread.startVirtualThread(() -> {
            try { trava.await(TIMEOUT_SECONDS, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                root.setDisable(false);
                if (!sucesso.get()) {
                    setStatus("⚠ " + ("TIMEOUT".equals(erroRef.get())
                        ? "Request timed out. Please go back and try again."
                        : erroRef.get()), "#BA3C3C");
                    return;
                }

                String raw = erroRef.get();
                if (raw == null || raw.trim().equals("EMPTY")) {
                    setStatus("⚠ User not found.", "#BA3C3C");
                    return;
                }

                parseAndRender(raw);
            });
        });
    }

    private void parseAndRender(String raw) {
        try {
            String[] f = raw.trim().split(" ", 9);
            if (f.length < 8) {
                setStatus("⚠ Invalid response from server.", "#BA3C3C");
                return;
            }

            String username  = f[0];
            String pfpB64    = f[1];
            int wins         = Integer.parseInt(f[2]);
            int losses       = Integer.parseInt(f[3]);
            int totalMatches = Integer.parseInt(f[4]);
            int tPages       = Integer.parseInt(f[5]);
            int cPage        = Integer.parseInt(f[6]);
            int matchCount   = Integer.parseInt(f[7]);
            String matchesRaw = f.length > 8 ? f[8] : "";

            this.totalPages  = tPages;
            this.currentPage = cPage;

            usernameLabel.setText(username + (isOwnProfile ? "  (You)" : ""));

            if (!"NULL".equals(pfpB64) && !pfpB64.isBlank()) {
                try {
                    byte[] bytes = Base64.getDecoder().decode(pfpB64);
                    pfpCircle.setFill(new ImagePattern(new Image(new ByteArrayInputStream(bytes))));
                } catch (Exception ignored) {
                    pfpCircle.setFill(Color.web("#2E2E38"));
                }
            } else {
                pfpCircle.setFill(Color.web("#2E2E38"));
            }

            int played = wins + losses;
            double wr  = played > 0 ? (wins * 100.0 / played) : 0;

            updateStat(winsBox,    String.valueOf(wins));
            updateStat(lossesBox,  String.valueOf(losses));
            updateStat(playedBox,  String.valueOf(totalMatches));
            updateStat(winRateBox, String.format("%.0f%%", wr));

            pageInfoLabel.setText("Page " + cPage + " of " + tPages);
            prevBtn.setDisable(cPage <= 1);
            nextBtn.setDisable(cPage >= tPages);

            matchHistoryList.getChildren().clear();

            if (matchCount == 0 || matchesRaw.isBlank()) {
                Label empty = new Label("No matches played yet.");
                empty.setStyle("-fx-text-fill: #5C5C64; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
                matchHistoryList.getChildren().add(empty);
                hideStatus();
                return;
            }

            String[] tokens = matchesRaw.trim().split(" ");
            for (int i = 0; i + 5 < tokens.length; i += 6) {
                String  p1       = tokens[i];
                String  p2       = tokens[i + 1];
                int     duration = Integer.parseInt(tokens[i + 2]);
                int     score1   = Integer.parseInt(tokens[i + 3]);
                int     score2   = Integer.parseInt(tokens[i + 4]);
                boolean won      = Boolean.parseBoolean(tokens[i + 5]);
                matchHistoryList.getChildren().add(buildMatchCard(p1, p2, duration, score1, score2, won));
            }

            hideStatus();

        } catch (Exception e) {
            setStatus("⚠ Failed to parse profile data.", "#BA3C3C");
        }
    }

    private HBox buildMatchCard(String p1, String p2, int duration, int score1, int score2, boolean won) {
        HBox card = new HBox(16);
        card.setStyle(won ? WIN_CARD_STYLE : LOSS_CARD_STYLE);
        card.setAlignment(Pos.CENTER_LEFT);

        Label result = new Label(won ? "WIN" : "LOSS");
        result.setMinWidth(44);
        result.setStyle("-fx-text-fill: " + (won ? "#00E676" : "#FF4A4A") + "; " +
                        "-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: 800;");

        VBox matchInfo = new VBox(4);
        Label players = new Label(p1 + "  vs  " + p2);
        players.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label score = new Label("Score: " + score1 + " — " + score2);
        score.setStyle("-fx-text-fill: #00ADB5; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold;");

        matchInfo.getChildren().addAll(players, score);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dur = new Label(String.format("⏱ %d:%02d", duration / 60, duration % 60));
        dur.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px;");

        card.getChildren().addAll(result, matchInfo, spacer, dur);
        return card;
    }

    private VBox makeStat(String labelText, String value, String color) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Segoe UI'; -fx-font-size: 18px; -fx-font-weight: 800;");
        val.setUserData("val");

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #5C5C64; -fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold;");

        box.getChildren().addAll(val, lbl);
        return box;
    }

    private void updateStat(VBox box, String newValue) {
        for (var node : box.getChildren()) {
            if ("val".equals(node.getUserData())) {
                ((Label) node).setText(newValue);
                return;
            }
        }
    }

    private void setStatus(String msg, String color) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void applyButtonEffects(Button button, String normalBg, String hoverBg) {
        String baseStyle = button.getStyle();
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + "-fx-background-color: " + hoverBg + ";"));
        button.setOnMouseExited(e  -> button.setStyle(baseStyle + "-fx-background-color: " + normalBg + ";"));
        button.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), button);
            st.setToX(0.95); st.setToY(0.95); st.play();
        });
        button.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), button);
            st.setToX(1.0); st.setToY(1.0); st.play();
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

    @Override
    public void UpdatePing(int ms) {
        Platform.runLater(() -> {
            int activeDots  = 0;
            Color dotColor  = Color.TRANSPARENT;
            if (ms < 0 || ms >= 1000) {
                pingLabel.setText("📶 -- ms");
            } else if (ms < 100) {
                pingLabel.setText("📶 " + ms + " ms"); activeDots = 3; dotColor = Color.web("#00E676");
            } else if (ms < 200) {
                pingLabel.setText("📶 " + ms + " ms"); activeDots = 2; dotColor = Color.web("#FF9100");
            } else {
                pingLabel.setText("📶 " + ms + " ms"); activeDots = 1; dotColor = Color.web("#FF4A4A");
            }
            for (int i = 0; i < 3; i++) {
                if (i < activeDots) { pingDots[i].setFill(dotColor);              pingDots[i].setStroke(dotColor); }
                else                { pingDots[i].setFill(Color.TRANSPARENT);     pingDots[i].setStroke(Color.web("#5C5C64")); }
            }
        });
    }

    @Override public Boolean isPingDisplayed() { return true; }
    @Override public Parent  getRoot()          { return root; }
}