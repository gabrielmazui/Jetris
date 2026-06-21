package ui.screens;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

public interface Screen {
    Parent getRoot();

    default Boolean isPingDisplayed() {
        return false;
    }

    default void UpdatePing(int ms) {}

    default void onMainButtonClick() {}

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

    default void EnableRetryMenu() {}

    default void DisableRetryMenu() {}

    default void onEscapeKeyPressed() {
        EnablePauseMenu();
    }

    default void EnablePauseMenu() {}

    default void DisablePauseMenu() {}
}
