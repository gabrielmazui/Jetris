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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import network.NetworkCallback;
import network.NetworkContext;
import ui.service.SettingsService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SettingsScreen implements Screen {

    private final StackPane root;
    private BorderPane mainLayout;
    private Circle[] pingDots;
    private Label pingLabel;
    private Circle largePfpPreview;
    private Label statusLabel;
    private HBox statusBox;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int UPLOAD_TIMEOUT_SECONDS = 5;

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

    private static final String DANGER_BUTTON_STYLE = """
        -fx-background-color: #BA3C3C;
        -fx-text-fill: #FFFFFF;
        -fx-font-family: 'Segoe UI';
        -fx-font-weight: bold;
        -fx-font-size: 13px;
        -fx-background-radius: 6;
        -fx-padding: 10 16 10 16;
        -fx-cursor: hand;
    """;

    private static final String CARD_STYLE = """
        -fx-background-color: #1E1E26;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: #2E2E38;
        -fx-border-width: 1;
        -fx-padding: 25;
    """;

    private static final String DANGER_CARD_STYLE = """
        -fx-background-color: #1E1E26;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: #BA3C3C;
        -fx-border-width: 1;
        -fx-padding: 25;
    """;

    private static final String SCROLL_PANE_STYLE = """
        -fx-background-color: transparent;
        -fx-background: #0F0F14;
    """;

    public SettingsScreen() {
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

        Button backBtn = new Button("Main Screen");
        backBtn.setStyle(SECONDARY_BUTTON_STYLE);
        applyButtonEffects(backBtn, "#2E2E38", "#3E3E4A");
        backBtn.setOnAction(e -> onBackClick());

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

        topBar.getChildren().addAll(title, backBtn, spacer1, pingBox);
        return topBar;
    }

    private VBox createCenterContent() {
        VBox centerBox = new VBox(25);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setPadding(new Insets(30, 0, 0, 0));
        centerBox.setMaxWidth(700);

        Label pageTitle = new Label("ACCOUNT SETTINGS");
        pageTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: 800; -fx-letter-spacing: 1px;");

        VBox sectionsBox = new VBox(20);
        sectionsBox.setMaxWidth(600);
        sectionsBox.getChildren().addAll(
                createProfileCard(),
                createDangerZoneCard()
        );

        statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setMaxWidth(600);
        statusBox.setVisible(false);
        statusBox.setManaged(false);
        statusBox.setPadding(new Insets(12, 16, 12, 16));
        statusBox.setStyle(CARD_STYLE + "-fx-padding: 12 16 12 16;");

        statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold;");
        statusBox.getChildren().add(statusLabel);

        VBox content = new VBox(20, pageTitle, statusBox, sectionsBox);
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(600);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(SCROLL_PANE_STYLE);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        centerBox.getChildren().addAll(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return centerBox;
    }

    private VBox createProfileCard() {
        VBox card = new VBox(15);
        card.setStyle(CARD_STYLE);
        card.setAlignment(Pos.CENTER);

        Label sectionTitle = new Label("PROFILE PICTURE");
        sectionTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 800; -fx-letter-spacing: 1px;");
        sectionTitle.setMaxWidth(Double.MAX_VALUE);
        sectionTitle.setAlignment(Pos.CENTER_LEFT);

        largePfpPreview = new Circle(50);
        largePfpPreview.setFill(Color.web("#2E2E38"));
        largePfpPreview.setStroke(Color.web("#00ADB5"));
        largePfpPreview.setStrokeWidth(3);
        carregarPfpAtual();

        String user = UserSession.getUsername();
        Label usernameLabel = new Label(user);
        usernameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 15px; -fx-font-weight: bold;");

        Button changePfpBtn = new Button("Upload New Picture");
        changePfpBtn.setStyle(PRIMARY_BUTTON_STYLE);
        applyButtonEffects(changePfpBtn, "#00ADB5", "#33BEC4");
        changePfpBtn.setOnAction(e -> onSelecionarPfp());

        Label infoLabel = new Label("PNG or JPG format  •  Maximum file size: 5 MB");
        infoLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold;");

        VBox uploadBox = new VBox(8, changePfpBtn, infoLabel);
        uploadBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(sectionTitle, largePfpPreview, usernameLabel, uploadBox);
        return card;
    }

    private VBox createDangerZoneCard() {
        VBox card = new VBox(15);
        card.setStyle(DANGER_CARD_STYLE);
        card.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = new Label("DANGER ZONE");
        sectionTitle.setStyle("-fx-text-fill: #FF4A4A; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 800; -fx-letter-spacing: 1px;");

        Label description = new Label("Deleting your account is permanent and cannot be undone. All your matches, stats and profile data will be permanently removed.");
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px;");

        Button deleteAccountBtn = new Button("Delete Account");
        deleteAccountBtn.setStyle(DANGER_BUTTON_STYLE);
        applyButtonEffects(deleteAccountBtn, "#BA3C3C", "#D44E4E");
        deleteAccountBtn.setOnAction(e -> onDeleteAccountClick());

        card.getChildren().addAll(sectionTitle, description, deleteAccountBtn);
        return card;
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

    private void showStatus(String message, StatusType type) {
        String color;
        String icon;
        switch (type) {
            case SUCCESS -> { color = "#00ADB5"; icon = "✓ "; }
            case ERROR   -> { color = "#BA3C3C"; icon = "⚠ "; }
            case INFO    -> { color = "#5C5C64"; icon = "ℹ "; }
            default      -> { color = "#2E2E38"; icon = ""; }
        }

        statusLabel.setText(icon + message);
        statusBox.setStyle("""
            -fx-background-color: #1E1E26;
            -fx-background-radius: 8;
            -fx-border-radius: 8;
            -fx-border-color: %s;
            -fx-border-width: 1;
            -fx-padding: 12 16 12 16;
        """.formatted(color));
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold;");

        statusBox.setVisible(true);
        statusBox.setManaged(true);

        statusBox.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), statusBox);
        ft.setToValue(1.0);
        ft.play();
    }

    private void hideStatus() {
        statusBox.setVisible(false);
        statusBox.setManaged(false);
    }

    private enum StatusType { SUCCESS, ERROR, INFO }

    private void showDeleteConfirmOverlay() {
        Rectangle backdrop = new Rectangle();
        backdrop.setFill(Color.web("#000000", 0.6));
        backdrop.widthProperty().bind(root.widthProperty());
        backdrop.heightProperty().bind(root.heightProperty());

        VBox dialog = new VBox(20);
        dialog.setAlignment(Pos.CENTER);
        dialog.setMaxWidth(420);
        dialog.setStyle("""
            -fx-background-color: #1E1E26;
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-border-color: #BA3C3C;
            -fx-border-width: 2;
            -fx-padding: 32 28 28 28;
        """);
        dialog.setEffect(new DropShadow(30, Color.web("#000000", 0.6)));

        Label icon = new Label("⚠");
        icon.setStyle("-fx-text-fill: #BA3C3C; -fx-font-size: 32px;");

        Label title = new Label("Delete Account");
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 18px; -fx-font-weight: 800;");

        Label body = new Label("Are you absolutely sure? This action is permanent and cannot be undone. All your matches, stats and profile data will be permanently deleted.");
        body.setWrapText(true);
        body.setMaxWidth(360);
        body.setStyle("-fx-text-fill: #6E6E77; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
        body.setAlignment(Pos.CENTER);

        Button confirmBtn = new Button("Yes, delete my account");
        confirmBtn.setStyle(DANGER_BUTTON_STYLE);
        confirmBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(SECONDARY_BUTTON_STYLE);
        cancelBtn.setMaxWidth(Double.MAX_VALUE);

        VBox buttons = new VBox(10, confirmBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER);
        buttons.setMaxWidth(280);

        dialog.getChildren().addAll(icon, title, body, buttons);

        StackPane overlay = new StackPane(backdrop, dialog);
        overlay.setAlignment(Pos.CENTER);
        overlay.setOpacity(0);

        root.getChildren().add(overlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), overlay);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        cancelBtn.setOnAction(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), overlay);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> root.getChildren().remove(overlay));
            fadeOut.play();
        });

        confirmBtn.setOnAction(e -> {
            root.getChildren().remove(overlay);
            executeFinalAccountDeletion();
        });

        applyButtonEffects(confirmBtn, "#BA3C3C", "#D44E4E");
        applyButtonEffects(cancelBtn, "#2E2E38", "#3E3E4A");
    }

    private void onSelecionarPfp() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select a profile picture");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "Image Files",
            "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif", "*.webp"
        ));

        File file = fc.showOpenDialog(root.getScene().getWindow());
        if (file == null) return;

        String nameLower = file.getName().toLowerCase();
        if (!nameLower.endsWith(".png") && !nameLower.endsWith(".jpg")
                && !nameLower.endsWith(".jpeg") && !nameLower.endsWith(".bmp")
                && !nameLower.endsWith(".gif") && !nameLower.endsWith(".webp")) {
            showStatus("Invalid file type.", StatusType.ERROR);
            return;
        }

        if (file.length() > MAX_FILE_SIZE) {
            double sizeMb = file.length() / (1024.0 * 1024.0);
            showStatus(String.format("File too large (%.2f MB). Maximum allowed size is 5 MB.", sizeMb), StatusType.ERROR);
            return;
        }

        try {
            BufferedImage bimg = ImageIO.read(file);
            if (bimg == null) {
                showStatus("This file is not a valid image.", StatusType.ERROR);
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bimg, "png", baos);
            byte[] pngBytes = baos.toByteArray();

            if (pngBytes.length > MAX_FILE_SIZE) {
                showStatus("File too large after processing. Maximum allowed size is 5 MB.", StatusType.ERROR);
                return;
            }

            root.setDisable(true);
            showStatus("Uploading profile picture... Please wait.", StatusType.INFO);

            int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
            CountDownLatch trava = new CountDownLatch(1);
            AtomicReference<String> resultado = new AtomicReference<>("TIMEOUT");
            AtomicBoolean sucesso = new AtomicBoolean(false);

            SettingsService.uploadProfilePicture(pngBytes, callbackId, new NetworkCallback(callbackId) {
                @Override
                public void onSuccess(String res) {
                    sucesso.set(true);
                    resultado.set("SUCCESS");
                    UserSession.setPfp(pngBytes);
                    trava.countDown();
                }

                @Override
                public void onFailure(String err) {
                    resultado.set(err);
                    trava.countDown();
                }
            });

            Thread.startVirtualThread(() -> {
                try {
                    trava.await(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}

                Platform.runLater(() -> {
                    root.setDisable(false);
                    if (sucesso.get()) {
                        largePfpPreview.setFill(new ImagePattern(new Image(new ByteArrayInputStream(pngBytes))));
                        showStatus("Profile picture updated successfully!", StatusType.SUCCESS);
                    } else if ("TIMEOUT".equals(resultado.get())) {
                        showStatus("Upload timed out. Please try again.", StatusType.ERROR);
                    } else {
                        showStatus("Failed to update profile picture: " + resultado.get(), StatusType.ERROR);
                    }
                });
            });

        } catch (IOException e) {
            showStatus("An error occurred while reading the selected file.", StatusType.ERROR);
        }
    }

    private void onDeleteAccountClick() {
        showDeleteConfirmOverlay();
    }

    private void executeFinalAccountDeletion() {
        showStatus("Deleting account...", StatusType.INFO);
        root.setDisable(true);

        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        CountDownLatch trava = new CountDownLatch(1);
        AtomicBoolean sucesso = new AtomicBoolean(false);
        AtomicReference<String> erro = new AtomicReference<>("TIMEOUT");

        SettingsService.deleteAccount(callbackId, new NetworkCallback(callbackId) {
            @Override
            public void onSuccess(String res) {
                sucesso.set(true);
                trava.countDown();
            }

            @Override
            public void onFailure(String err) {
                erro.set(err);
                trava.countDown();
            }
        });

        Thread.startVirtualThread(() -> {
            try {
                trava.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                if (sucesso.get()) {
                    UserSession.limparSessao();
                    ScreenManager.setScreen(new LoginScreen());
                } else {
                    root.setDisable(false);
                    if ("TIMEOUT".equals(erro.get())) {
                        showStatus("Request timed out. Please try again.", StatusType.ERROR);
                    } else {
                        showStatus("Failed to delete account: " + erro.get(), StatusType.ERROR);
                    }
                }
            });
        });
    }

    private void onBackClick() {
        ScreenManager.setScreen(new MainScreen());
    }

    private void carregarPfpAtual() {
        byte[] bytes = UserSession.getPfp();
        if (bytes != null) {
            largePfpPreview.setFill(new ImagePattern(new Image(new ByteArrayInputStream(bytes))));
        } else {
            largePfpPreview.setFill(Color.web("#2E2E38"));
        }
    }

    private void applyButtonEffects(Button button, String normalBg, String hoverBg) {
        String baseStyle = button.getStyle();

        button.setOnMouseEntered(e -> {
            if (root.isDisable()) return;
            button.setStyle(baseStyle + "-fx-background-color: " + hoverBg + ";");
        });
        button.setOnMouseExited(e -> {
            if (root.isDisable()) return;
            button.setStyle(baseStyle + "-fx-background-color: " + normalBg + ";");
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

    @Override
    public Boolean isPingDisplayed() {
        return true;
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}