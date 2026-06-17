package ui.screens;

import core.ScreenManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import network.NetworkContext;
import network.NetworkManager;

public interface Screen {
    Parent getRoot();
    
    default Boolean isPingDisplayed(){
        return false;
    }

    default void UpdatePing(int ms){}
    default void onMainButtonClick(){}

    public static void transitionToScreen(Runnable onFinished, BorderPane mainLayout) {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.4), mainLayout);
        fadeOut.setToValue(0.0);

        TranslateTransition moveDown = new TranslateTransition(Duration.seconds(0.4), mainLayout);
        moveDown.setToY(20.0);
        moveDown.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition pt = new ParallelTransition(fadeOut, moveDown);
        pt.setOnFinished(e -> onFinished.run());
        pt.play();
    }

    default void EnableRetryMenu() {
        if (!ScreenManager.retryMenu) {
            ScreenManager.retryMenu = true;

            Platform.runLater(() -> {
                Pane container = null;
                if (getRoot().getParent() instanceof Pane) {
                    container = (Pane) getRoot().getParent();
                } else {
                    Scene scene = getRoot().getScene();
                    if (scene != null && scene.getRoot() instanceof Pane) {
                        container = (Pane) scene.getRoot();
                    }
                }

                if (container != null) {
                    StackPane overlay = new StackPane();
                    overlay.setId("retryOverlay");
                    
                    Pane blurPane = new Pane();
                    blurPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);");
                    blurPane.setEffect(new GaussianBlur(15));

                    overlay.setFocusTraversable(true);
                    overlay.setOnMouseClicked(event -> { overlay.requestFocus(); event.consume(); });
                    overlay.setOnMousePressed(event -> { overlay.requestFocus(); event.consume(); });
                    overlay.setOnKeyPressed(event -> event.consume());
                    overlay.focusedProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal && ScreenManager.retryMenu) {
                            Platform.runLater(overlay::requestFocus);
                        }
                    });

                    VBox content = new VBox(20);
                    content.setAlignment(Pos.CENTER);

                    if (container instanceof Region) {
                        Region region = (Region) container;
                        overlay.prefWidthProperty().bind(region.widthProperty());
                        overlay.prefHeightProperty().bind(region.heightProperty());
                        blurPane.prefWidthProperty().bind(region.widthProperty());
                        blurPane.prefHeightProperty().bind(region.heightProperty());
                    }

                    ProgressIndicator progress = new ProgressIndicator();
                    progress.setMinSize(60, 60);
                    progress.setMaxSize(60, 60);

                    Label label = new Label("Trying to reconnect to the server");
                    label.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Arial';");

                    content.getChildren().addAll(progress, label);
                    overlay.getChildren().addAll(blurPane, content);
                    container.getChildren().add(overlay);
                    overlay.requestFocus();

                    Thread.startVirtualThread(() -> {
                        while(true){
                            try{
                                Thread.sleep(2000);
                            }catch(InterruptedException e){}
                            if((NetworkContext.tcpState == network.ConnectionState.DISCONNECTED && NetworkContext.isAttemptingTCP) && (NetworkContext.udpState == network.ConnectionState.DISCONNECTED && NetworkContext.isAttemptingUDP)){
                                ScreenManager.setScreen(new LoadingScreen());
                                NetworkContext.isAttemptingTCP = false;
                                NetworkContext.isAttemptingUDP = false;
                                NetworkManager.retryConnection();
                                break;
                            }
                        }
                    });
                }
            });
        }
    }

    public default void DisableRetryMenu() {
        if (ScreenManager.retryMenu) {
            ScreenManager.retryMenu = false;

            Platform.runLater(() -> {
                Pane container = null;
                if (getRoot().getParent() instanceof Pane) {
                    container = (Pane) getRoot().getParent();
                } else {
                    Scene scene = getRoot().getScene();
                    if (scene != null && scene.getRoot() instanceof Pane) {
                        container = (Pane) scene.getRoot();
                    }
                }

                if (container != null) {
                    container.getChildren().removeIf(node -> "retryOverlay".equals(node.getId()));
                }
            });
        }
    }

    default void onEscapeKeyPressed() {
        javafx.scene.layout.Pane container = null;
        if (getRoot().getParent() instanceof javafx.scene.layout.Pane) {
            container = (javafx.scene.layout.Pane) getRoot().getParent();
        } else {
            javafx.scene.Scene scene = getRoot().getScene();
            if (scene != null && scene.getRoot() instanceof javafx.scene.layout.Pane) {
                container = (javafx.scene.layout.Pane) scene.getRoot();
            }
        }

        if (container != null) {
            javafx.scene.Node existing = null;
            for (javafx.scene.Node node : container.getChildren()) {
                if ("pauseOverlay".equals(node.getId())) {
                    existing = node;
                    break;
                }
            }
            if (existing != null) {
                DisablePauseMenu();
                return;
            }
        }
        EnablePauseMenu();
    }

    default void EnablePauseMenu() {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.layout.Pane container = null;
            if (getRoot().getParent() instanceof javafx.scene.layout.Pane) {
                container = (javafx.scene.layout.Pane) getRoot().getParent();
            } else {
                javafx.scene.Scene scene = getRoot().getScene();
                if (scene != null && scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    container = (javafx.scene.layout.Pane) scene.getRoot();
                }
            }

            if (container != null) {
                for (javafx.scene.Node node : container.getChildren()) {
                    if ("pauseOverlay".equals(node.getId())) {
                        return;
                    }
                }

                javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
                overlay.setId("pauseOverlay");

                javafx.scene.layout.Pane blurPane = new javafx.scene.layout.Pane();
                blurPane.setStyle("-fx-background-color: rgba(7, 7, 10, 0.65);");
                blurPane.setEffect(new javafx.scene.effect.GaussianBlur(20));

                overlay.setFocusTraversable(true);
                overlay.setOnMouseClicked(event -> event.consume());
                overlay.setOnMousePressed(event -> event.consume());
                overlay.setOnMouseReleased(event -> event.consume());
                overlay.setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        DisablePauseMenu();
                    }
                    event.consume();
                });

                if (container instanceof javafx.scene.layout.Region) {
                    javafx.scene.layout.Region region = (javafx.scene.layout.Region) container;
                    overlay.prefWidthProperty().bind(region.widthProperty());
                    overlay.prefHeightProperty().bind(region.heightProperty());
                    blurPane.prefWidthProperty().bind(region.widthProperty());
                    blurPane.prefHeightProperty().bind(region.heightProperty());
                }

                
                javafx.scene.layout.VBox menuCard = new javafx.scene.layout.VBox(25);
                menuCard.setAlignment(javafx.geometry.Pos.CENTER);
                menuCard.setMaxSize(320, 360);
                menuCard.setStyle("-fx-background-color: rgba(20, 20, 28, 0.65); "
                                + "-fx-padding: 40 45; "
                                + "-fx-background-radius: 24; "
                                + "-fx-border-radius: 24; "
                                + "-fx-border-color: rgba(255, 255, 255, 0.08); "
                                + "-fx-border-width: 1.2;");

                javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("PAUSE");
                titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 26px; -fx-font-weight: 900; -fx-letter-spacing: 4px; -fx-padding: 0 0 10 0;");

                javafx.scene.control.Button resumeBtn = new javafx.scene.control.Button("RESUME");
                resumeBtn.setStyle("-fx-background-color: rgba(0, 173, 181, 0.04); -fx-border-color: #00ADB5; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #00ADB5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 220px; -fx-padding: 13 0; -fx-cursor: hand;");
                resumeBtn.setOnAction(e -> DisablePauseMenu());
                applyMenuButtonEffects(resumeBtn, "rgba(0, 173, 181, 0.04)", "#00ADB5", "#00ADB5", "#14141E", "#008C94", "#14141E");

                javafx.scene.control.Button leaveBtn = new javafx.scene.control.Button("LEAVE GAME");
                leaveBtn.setStyle("-fx-background-color: rgba(255, 75, 75, 0.04); -fx-border-color: #FF4B4B; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #FF4B4B; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 220px; -fx-padding: 13 0; -fx-cursor: hand;");
                leaveBtn.setOnAction(e -> core.ScreenManager.fechar());
                applyMenuButtonEffects(leaveBtn, "rgba(255, 75, 75, 0.04)", "#FF4B4B", "#FF4B4B", "#FFFFFF", "#D33A3A", "#FFFFFF");

                menuCard.getChildren().addAll(titleLabel, resumeBtn, leaveBtn);
                overlay.getChildren().addAll(blurPane, menuCard);
                container.getChildren().add(overlay);
                overlay.requestFocus();

                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(Duration.millis(120), overlay);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);

                javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(Duration.millis(180), menuCard);
                scaleIn.setFromX(0.9);
                scaleIn.setFromY(0.9);
                scaleIn.setToX(1.0);
                scaleIn.setToY(1.0);
                scaleIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

                new javafx.animation.ParallelTransition(fadeIn, scaleIn).play();
            }
        });
    }

    default void DisablePauseMenu() {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.layout.Pane container = null;
            if (getRoot().getParent() instanceof javafx.scene.layout.Pane) {
                container = (javafx.scene.layout.Pane) getRoot().getParent();
            } else {
                javafx.scene.Scene scene = getRoot().getScene();
                if (scene != null && scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    container = (javafx.scene.layout.Pane) scene.getRoot();
                }
            }

            if (container != null) {
                javafx.scene.Node overlayNode = null;
                for (javafx.scene.Node node : container.getChildren()) {
                    if ("pauseOverlay".equals(node.getId())) {
                        overlayNode = node;
                        break;
                    }
                }

                if (overlayNode instanceof javafx.scene.layout.StackPane) {
                    javafx.scene.layout.StackPane overlay = (javafx.scene.layout.StackPane) overlayNode;
                    if (overlay.getOpacity() < 0.5) return;

                    javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(Duration.millis(100), overlay);
                    fadeOut.setToValue(0.0);

                    if (overlay.getChildren().size() > 1) {
                        javafx.scene.Node cardNode = overlay.getChildren().get(1);
                        javafx.animation.ScaleTransition scaleOut = new javafx.animation.ScaleTransition(Duration.millis(100), cardNode);
                        scaleOut.setToX(0.9);
                        scaleOut.setToY(0.9);

                        javafx.scene.layout.Pane finalContainer = container;
                        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(fadeOut, scaleOut);
                        pt.setOnFinished(e -> finalContainer.getChildren().remove(overlay));
                        pt.play();
                    } else {
                        javafx.scene.layout.Pane finalContainer = container;
                        fadeOut.setOnFinished(e -> finalContainer.getChildren().remove(overlay));
                        fadeOut.play();
                    }
                }
            }
        });
    }

    default void applyMenuButtonEffects(javafx.scene.control.Button btn, String bgBase, String borderBase, String bgHover, String textHover, String bgPressed, String textPressed) {
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(80), btn);
        
        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: " + bgHover + "; -fx-border-color: " + bgHover + "; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + textHover + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 220px; -fx-padding: 13 0; -fx-cursor: hand;");
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: " + bgBase + "; -fx-border-color: " + borderBase + "; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + borderBase + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 220px; -fx-padding: 13 0; -fx-cursor: hand;");
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        btn.setOnMousePressed(e -> {
            btn.setStyle("-fx-background-color: " + bgPressed + "; -fx-border-color: " + bgPressed + "; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + textPressed + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 220px; -fx-padding: 13 0; -fx-cursor: hand;");
            st.setToX(0.98);
            st.setToY(0.98);
            st.play();
        });

        btn.setOnMouseReleased(e -> {
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
            if (btn.isHover()) {
                btn.setStyle("-fx-background-color: " + bgHover + "; -fx-border-color: " + bgHover + "; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + textHover + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 220px; -fx-padding: 13 0; -fx-cursor: hand;");
            } else {
                btn.setStyle("-fx-background-color: " + bgBase + "; -fx-border-color: " + borderBase + "; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + borderBase + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 220px; -fx-padding: 13 0; -fx-cursor: hand;");
            }
        });
    }
}