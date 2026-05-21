package core;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

import ui.controllers.Controller;

public class ScreenManager {

    public static Controller controller;
    public static ui.screens.Screen CurrScreen;

    private static Stage stage;
    private static Scene scene;

    private static double xOffset;
    private static double yOffset;

    public static void init(Stage primaryStage, ui.screens.Screen firstScreen) {
        stage = primaryStage;
        scene = new Scene(firstScreen.getRoot());

        String windowTitle = "Jetris";
        stage.setTitle(windowTitle); 

        try {
            stage.getIcons().add(new Image(ScreenManager.class.getResourceAsStream("/icon.png")));
        } catch (Exception e) {
            System.out.println("Ícone não encontrado: " + e.getMessage());
        }

        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);

        stage.centerOnScreen();
        stage.show();

        aplicarModoEscuroNativo(windowTitle);
    }

    private static void aplicarModoEscuroNativo(String title) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Platform.runLater(() -> {
                    HWND hwnd = User32.INSTANCE.FindWindow(null, title);
                    
                    if (hwnd != null) {
                        int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
                        int[] darkModeOn = {1};
                        
                        com.sun.jna.Function dwmSetWindowAttribute = com.sun.jna.Function.getFunction("dwmapi", "DwmSetWindowAttribute");
                        dwmSetWindowAttribute.invokeInt(new Object[]{
                            hwnd, 
                            DWMWA_USE_IMMERSIVE_DARK_MODE, 
                            darkModeOn, 
                            4
                        });
                    }
                });
            }
        } catch (Exception e) {
        }
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