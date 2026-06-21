package ui.service;

import config.UserSession;
import network.NetworkManager;
import exceptions.ConnectionException;

public class MatchChatService {
    public static final int MAX_CHAT_LENGTH = 100;

    private MatchChatService() {}

    public static String sanitize(String message) {
        String safe = message == null ? "" : message.replaceAll("\\s+", " ").trim();
        if (safe.length() > MAX_CHAT_LENGTH) {
            safe = safe.substring(0, MAX_CHAT_LENGTH);
        }
        return safe;
    }

    public static void send(String matchCode, String message) throws ConnectionException {
        String safe = sanitize(message);
        if (safe.isEmpty()) {
            return;
        }

        NetworkManager.sendTCP("CHAT 0 0 " + UserSession.getToken() + " " + matchCode + " " + safe);
    }
}
