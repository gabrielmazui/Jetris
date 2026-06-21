package ui.screens;

import core.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MatchResultScreen implements Screen {
    private final StackPane root;

    public MatchResultScreen(String matchCode, String outcome, String reason) {
        this(matchCode, outcome, reason, 0L, 0L);
    }

    public MatchResultScreen(String matchCode, String outcome, String reason,
                             long startTimeMillis, long endTimeMillis) {
        root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0F0F14 0%, #09090D 100%);");

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 40, 36, 40));
        card.setMaxWidth(400);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setStyle("-fx-background-color: rgba(20, 20, 28, 0.96); -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1;");

        boolean spectate = "SPECTATE".equalsIgnoreCase(outcome);
        boolean won = "WIN".equalsIgnoreCase(outcome);
        String accentColor = spectate ? "#FFB74D" : (won ? "#00E676" : "#FF4A4A");
        String badgeText   = spectate ? "SPECTATE" : (won ? "VITÓRIA" : "DERROTA");
        String titleText   = spectate
            ? (reason != null && !reason.isBlank() ? reason + " Venceu!" : "Partida Encerrada!")
            : (won ? "Você Venceu!" : "Você Perdeu!");

        Label badge = new Label(badgeText);
        badge.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 11px; -fx-font-weight: bold; "
                + "-fx-letter-spacing: 3px; -fx-background-color: " + accentColor + "22; "
                + "-fx-background-radius: 999; -fx-padding: 5 14 5 14;");

        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 30px; -fx-font-weight: 900; -fx-font-family: 'Segoe UI';");

        Label codeLabel = new Label("Partida  " + matchCode);
        codeLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 12px; -fx-font-weight: bold;");

        card.getChildren().addAll(badge, title, codeLabel);

        if (startTimeMillis > 0 && endTimeMillis > 0) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            ZoneId zone = ZoneId.systemDefault();
            String startStr = LocalTime.ofInstant(Instant.ofEpochMilli(startTimeMillis), zone).format(fmt);
            String endStr   = LocalTime.ofInstant(Instant.ofEpochMilli(endTimeMillis),   zone).format(fmt);
            long durationSec = (endTimeMillis - startTimeMillis) / 1000;
            String duration = String.format("%d:%02d", durationSec / 60, durationSec % 60);

            Label timeLabel = new Label("Início: " + startStr + "  •  Fim: " + endStr + "  •  Duração: " + duration);
            timeLabel.setStyle("-fx-text-fill: #6E6E77; -fx-font-size: 11px; -fx-font-weight: bold;");
            card.getChildren().add(timeLabel);
        }

        Button back = new Button("VOLTAR AO MENU");
        back.setStyle("-fx-background-color: " + accentColor + "; -fx-text-fill: #0F0F14; -fx-font-weight: bold; "
                + "-fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 12 24 12 24; -fx-cursor: hand;");
        back.setOnAction(e -> ScreenManager.setScreen(new MainScreen()));
        card.getChildren().add(back);

        root.getChildren().add(card);
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}
