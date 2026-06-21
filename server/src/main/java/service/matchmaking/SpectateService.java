package service.matchmaking;

import matches.SpectateManager;
import network.SessionManager;

public class SpectateService {
    public static void handle(int code, int callbackCode, String bodyRaw, String clientIp) {
        Integer userId = SessionManager.getUserId(clientIp);
        if (userId == null) return;

        if (code == 0) {
            String[] parts = bodyRaw.split(" ", 2);
            if (parts.length == 2) {
                SpectateManager.joinSpectate(userId, parts[1], clientIp, callbackCode);
            } else {
                SpectateManager.joinSpectate(userId, bodyRaw, clientIp, callbackCode);
            }
        } else if (code == 1) {
            String[] parts = bodyRaw.split(" ", 2);
            if (parts.length == 2) {
                SpectateManager.handleChat(userId, parts[0], parts[1], clientIp);
            }
        }
    }
}