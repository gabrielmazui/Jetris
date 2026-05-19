package core;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ScreenManager {

    private static Stage stage;
    private static Scene scene;

    private static double xOffset;
    private static double yOffset;

    public static void init(Stage primaryStage, ui.screens.Screen firstScreen) {
        stage = primaryStage;
        scene = new Scene(firstScreen.getRoot());

        // O título da janela nativa (aparece na barra do Windows/Linux)
        stage.setTitle("Jetris"); 

        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);

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

    // Mantido caso você precise arrastar algum elemento interno no futuro, 
    // mas não é mais necessário para mover a janela principal.
    public static void tornarArrastavel(Node node) {
        if (node == null) return;

        node.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        node.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    public static Stage getStage() {
        return stage;
    }
}