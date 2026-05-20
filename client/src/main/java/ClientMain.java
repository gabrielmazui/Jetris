import core.ScreenManager;
import javafx.application.Application;
import javafx.stage.Stage;

import network.NetworkManager;
import ui.screens.LoadingScreen;

public class ClientMain extends Application {
    
    @Override
    public void start(Stage stage){
        ScreenManager.init(stage, new LoadingScreen());
    }
    public static void main(String[] args) {
        NetworkManager.start();
        launch();
    }
}