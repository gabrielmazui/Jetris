package ui.screens;

import config.UserSession;
import core.ScreenManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
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
import ui.service.SearchUsersService;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

public class UserSearchScreen implements Screen {

    private final StackPane root;
    private BorderPane mainLayout;
    private Circle[] pingDots;
    private Label pingLabel;
    
    private VBox searchResultsBox;
    private Label searchStatusLabel;
    private PauseTransition searchCooldown;

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

    private static final String USER_CARD_STYLE = """
        -fx-background-color: #1E1E26;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: #2E2E38;
        -fx-border-width: 1;
        -fx-padding: 15;
        -fx-cursor: hand;
    """;

    private static final String SCROLL_PANE_STYLE = """
        -fx-background-color: transparent;
        -fx-background: #0F0F14;
    """;

    public UserSearchScreen() {
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

        Button mainScreenBtn = new Button("Main Screen");
        mainScreenBtn.setStyle(SECONDARY_BUTTON_STYLE);
        applyButtonEffects(mainScreenBtn, "#2E2E38", "#3E3E4A");
        mainScreenBtn.setOnAction(e -> onMainScreenClick());

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

        Circle pfp = new Circle(18, Color.web("#2E2E38"));
        pfp.setStroke(Color.web("#00ADB5"));
        pfp.setStrokeWidth(2);
        
        // Carrega a foto do usuário logado na barra do topo de forma segura
        byte[] pfpBytes = UserSession.getPfp();

        if (pfpBytes != null) {
            Image avatarImage = new Image(
                new ByteArrayInputStream(pfpBytes)
            );

            if (!avatarImage.isError()) {
                pfp.setFill(new ImagePattern(avatarImage));
            }
        }

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

        topBar.getChildren().addAll(title, mainScreenBtn, spacer1, rightControls);
        return topBar;
    }

    private VBox createCenterContent() {
        VBox centerBox = new VBox(30);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setPadding(new Insets(40, 0, 0, 0));
        centerBox.setMaxWidth(800);

        Label sectionTitle = new Label("USER SEARCH");
        sectionTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI'; -fx-font-size: 18px; -fx-font-weight: 800; -fx-letter-spacing: 1px;");

        TextField searchInput = new TextField();
        searchInput.setPromptText("Type a username to search...");
        searchInput.setStyle(INPUT_STYLE);
        searchInput.setPrefWidth(600);
        searchInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                searchInput.setStyle(INPUT_STYLE + "-fx-border-color: #00ADB5;");
            } else {
                searchInput.setStyle(INPUT_STYLE);
            }
        });

        searchStatusLabel = new Label();
        searchStatusLabel.setStyle("-fx-text-fill: #00ADB5; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold;");
        searchStatusLabel.setVisible(false);

        searchResultsBox = new VBox(10);
        searchResultsBox.setPadding(new Insets(10, 25, 10, 10));
        searchResultsBox.setPickOnBounds(false);

        ScrollPane scrollPane = new ScrollPane(searchResultsBox);
        scrollPane.setPickOnBounds(false);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(SCROLL_PANE_STYLE);
        scrollPane.setPrefHeight(450);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        searchCooldown = new PauseTransition(Duration.millis(500));
        searchCooldown.setOnFinished(e -> performSearch(searchInput.getText()));

        searchInput.textProperty().addListener((obs, oldText, newText) -> {
            searchResultsBox.getChildren().clear();
            if (newText == null || newText.trim().isEmpty()) {
                searchCooldown.stop();
                searchStatusLabel.setVisible(false);
            } else {
                searchStatusLabel.setText("Loading...");
                searchStatusLabel.setStyle("-fx-text-fill: #00ADB5; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold;");
                searchStatusLabel.setVisible(true);
                searchCooldown.playFromStart();
            }
        });

        centerBox.getChildren().addAll(sectionTitle, searchInput, searchStatusLabel, scrollPane);

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

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchStatusLabel.setVisible(false);
            return;
        }

        Thread.startVirtualThread(() -> {
            List<String[]> results = fetchUsersFromServer(query);

            Platform.runLater(() -> {
                searchResultsBox.getChildren().clear();

                if (results.isEmpty()) {
                    searchStatusLabel.setText("No users found");
                    searchStatusLabel.setStyle("-fx-text-fill: #FF4A4A; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold;");
                    searchStatusLabel.setVisible(true);
                } else {
                    searchStatusLabel.setVisible(false);
                    for (String[] userData : results) {
                        String username = userData[0];
                        String pfpData = userData.length > 1 ? userData[1] : null;
                        addUserCard(username, pfpData);
                    }
                }
            });
        });
    }

    private List<String[]> fetchUsersFromServer(String query) {
        return SearchUsersService.searchUsernames(query);
    }

    private void addUserCard(String username, String pfpData) {
        StackPane cardWrapper = new StackPane();
        cardWrapper.setMaxWidth(Double.MAX_VALUE);

        HBox card = new HBox(20);
        card.setStyle(USER_CARD_STYLE);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 20, 10, 10));

        Circle userIcon = new Circle(15, Color.web("#2E2E38"));
        userIcon.setStroke(Color.web("#00ADB5"));
        userIcon.setStrokeWidth(1.5);

        if (pfpData != null && !pfpData.trim().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(pfpData.trim());
                Image img = new Image(new ByteArrayInputStream(imageBytes));
                if (!img.isError()) {
                    userIcon.setFill(new ImagePattern(img));
                }
            } catch (Exception e) {
                userIcon.setFill(Color.web("#2E2E38"));
            }
        }

        Label nameLabel = new Label(username);
        nameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 15px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label viewProfileLabel = new Label("View Profile ➔");
        viewProfileLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 12px; -fx-font-weight: bold;");

        card.getChildren().addAll(userIcon, nameLabel, spacer, viewProfileLabel);
        cardWrapper.getChildren().add(card);

        cardWrapper.setOnMouseEntered(e -> {
            if (root.isDisable()) return;
            card.setViewOrder(-1.0);
            cardWrapper.setViewOrder(-1.0);
            card.setStyle(USER_CARD_STYLE + "-fx-border-color: #00ADB5; -fx-background-color: #23232D;");
            viewProfileLabel.setStyle("-fx-text-fill: #00ADB5; -fx-font-size: 12px; -fx-font-weight: bold;");
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), card);
            tt.setToX(12);
            tt.play();
        });

        cardWrapper.setOnMouseExited(e -> {
            if (root.isDisable()) return;
            card.setViewOrder(0.0);
            cardWrapper.setViewOrder(0.0);
            card.setStyle(USER_CARD_STYLE);
            viewProfileLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 12px; -fx-font-weight: bold;");
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), card);
            tt.setToX(0);
            tt.play();
        });

        cardWrapper.setOnMouseClicked(e -> {
            if (root.isDisable()) return;
            onUserClicked(username);
        });

        searchResultsBox.getChildren().add(cardWrapper);
    }

    private void onUserClicked(String username) {
        ScreenManager.setScreen(new ProfileScreen(username));
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
    
    public void transitionToScreen(Runnable onFinished) {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.4), mainLayout);
        fadeOut.setToValue(0.0);

        TranslateTransition moveDown = new TranslateTransition(Duration.seconds(0.4), mainLayout);
        moveDown.setToY(20.0);
        moveDown.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition pt = new ParallelTransition(fadeOut, moveDown);
        pt.setOnFinished(e -> onFinished.run());
        pt.play();
    }

    private void onMainScreenClick() {
        ScreenManager.setScreen(new MainScreen());
    }

    private void onProfileClick() {
        ScreenManager.setScreen(new ProfileScreen(UserSession.getUsername()));
    }
    
    private void onSettingsClick() {
        core.ScreenManager.setScreen(new ui.screens.SettingsScreen());
    }

    private void onLogoutClick() {
        root.setDisable(true);
        Screen.transitionToScreen(() -> {
            LogoutService.logout();
            Platform.runLater(() -> ScreenManager.setScreen(new LoginScreen()));
        }, mainLayout);
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