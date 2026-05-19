package core;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ScreenManager {

    private static Stage stage;
    private static Scene scene;

    private static double xOffset;
    private static double yOffset;

    public static void init(Stage primaryStage, ui.screens.Screen firstScreen) {
        stage = primaryStage;
        scene = new Scene(firstScreen.getRoot());

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setFullScreenExitHint(""); 

        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);

        // FIX DEFINITIVO DO MINIMIZAR: Força o redesenho redimensionando 1 pixel inteiro
        stage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
            if (!isIconified) { // Quando a janela é restaurada
                Platform.runLater(() -> {
                    // Tira 1 pixel real da largura (ignora o arredondamento do Windows)
                    double width = stage.getWidth();
                    stage.setWidth(width - 1); 
                    
                    // No frame seguinte, devolve o pixel
                    Platform.runLater(() -> {
                        stage.setWidth(width);
                    });
                });
            }
        });

        stage.centerOnScreen();
        stage.show();
    }

    public static void setScreen(ui.screens.Screen screen) {
        if (scene != null && screen != null) {
            Platform.runLater(() -> scene.setRoot(screen.getRoot()));
        }
    }

    public static void fechar() {
        Platform.exit();
        System.exit(0);
    }

    public static void minimizar() {
        if (stage != null) {
            stage.setIconified(true);
        }
    }

    public static void alternarTelaCheia() {
        if (stage == null) return;
        stage.setFullScreen(!stage.isFullScreen());
    }

    public static void tornarArrastavel(Node node) {
        if (node == null) return;

        node.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                alternarTelaCheia();
            }
        });

        node.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        node.setOnMouseDragged(event -> {
            if (stage.isFullScreen()) {
                double xProportion = (event.getScreenX() - stage.getX()) / stage.getWidth();
                stage.setFullScreen(false); 
                xOffset = stage.getWidth() * xProportion;
                yOffset = event.getSceneY();
            }

            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    public static Stage getStage() {
        return stage;
    }
}