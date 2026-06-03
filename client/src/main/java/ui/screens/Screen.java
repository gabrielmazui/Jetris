package ui.screens;

import core.ScreenManager;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.NetworkContext;
import network.NetworkManager;

public interface Screen {
    Parent getRoot();
    
    public default Boolean isPingDisplayed(){
        return false;
    }

    public default void UpdatePing(int ms){}

    public default void EnableRetryMenu() {
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

    public default void EnableEscMenu() {
        if (!ScreenManager.escMenu) {
            ScreenManager.escMenu = true;

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
                    overlay.setId("escOverlay");
                    
                    Pane blurPane = new Pane();
                    blurPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
                    blurPane.setEffect(new GaussianBlur(45));

                    overlay.setFocusTraversable(true);
                    overlay.setOnMouseClicked(event -> { overlay.requestFocus(); event.consume(); });
                    overlay.setOnMousePressed(event -> { overlay.requestFocus(); event.consume(); });
                    overlay.setOnKeyPressed(event -> event.consume());
                    overlay.focusedProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal && ScreenManager.escMenu) {
                            Platform.runLater(overlay::requestFocus);
                        }
                    });

                    VBox content = new VBox(15);
                    content.setAlignment(Pos.CENTER);

                    if (container instanceof Region) {
                        Region region = (Region) container;
                        overlay.prefWidthProperty().bind(region.widthProperty());
                        overlay.prefHeightProperty().bind(region.heightProperty());
                        blurPane.prefWidthProperty().bind(region.widthProperty());
                        blurPane.prefHeightProperty().bind(region.heightProperty());
                    }

                    Button btnSettings = new Button("Settings");
                    btnSettings.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-family: 'Arial';");
                    
                    Button btnLeave = new Button("Leave");
                    btnLeave.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff3333; -fx-font-size: 22px; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-family: 'Arial';");

                    btnSettings.setOnAction(e -> {
                    });

                    btnLeave.setOnAction(e -> {
                    });

                    content.getChildren().addAll(btnSettings, btnLeave);
                    overlay.getChildren().addAll(blurPane, content);
                    container.getChildren().add(overlay);
                    overlay.requestFocus();
                }
            });
        }
    }

    public default void DisableEscMenu() {
        if (ScreenManager.escMenu) {
            ScreenManager.escMenu = false;

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
                    container.getChildren().removeIf(node -> "escOverlay".equals(node.getId()));
                }
            });
        }
    }
}