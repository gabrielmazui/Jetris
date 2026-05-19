package ui.screens;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
public class LoginScreen implements Screen{

    @Override
    public Parent getRoot(){
        Label label = new Label("Carregando...");
        ProgressBar bar = new ProgressBar(0);

        VBox root = new VBox(10, label, bar);
        root.setStyle("-fx-alignment: center; -fx-padding: 40;");

        new Thread(() -> {

            for (int i = 0; i <= 100; i++) {

                double progress = i / 100.0;

                Platform.runLater(() -> bar.setProgress(progress));

                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {}
            }

            // Platform.runLater(() ->
                
            // );

        }).start();

        return root;
    }
}