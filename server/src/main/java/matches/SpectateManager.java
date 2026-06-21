package matches;

import db.DatabaseManager;
import network.SessionManager;
import network.connection.TCPConnectionManager;

public class SpectateManager {

    public static void joinSpectate(int userId, String matchCode, String clientIp, int callback) {
        MatchSession session = MatchManager.getMatch(matchCode);
        if (session != null && session.isActive() && !MatchManager.isUserInActiveMatch(userId)) {
            session.addSpectator(userId);
            TCPConnectionManager.send(clientIp, "SPECTATE 0 " + callback + " SUCCESS JOINED " + matchCode);
        } else {
            TCPConnectionManager.send(clientIp, "SPECTATE 0 " + callback + " FAIL Match_not_available");
        }
    }

    public static void handleChat(int senderId, String matchCode, String message, String clientIp) {
        MatchSession session = MatchManager.getMatch(matchCode);
        if (session == null) return;
        if (!session.containsUser(senderId) && !session.getSpectators().contains(senderId)) {
            return;
        }

        String safeMessage = message == null ? "" : message.replaceAll("\\s+", " ").trim();
        if (safeMessage.length() > 100) {
            safeMessage = safeMessage.substring(0, 100);
        }

        String senderName = resolveUsername(senderId);
        String chatPacket = "CHAT 0 0 " + matchCode + " " + senderId + " " + senderName + " " + safeMessage;

        if (session.getPlayer1() != null) {
            TCPConnectionManager.send(SessionManager.getIpByUserId(session.getPlayer1()), chatPacket);
        }
        if (session.getPlayer2() != null) {
            TCPConnectionManager.send(SessionManager.getIpByUserId(session.getPlayer2()), chatPacket);
        }

        for (Integer specId : session.getSpectators()) {
            TCPConnectionManager.send(SessionManager.getIpByUserId(specId), chatPacket);
        }
    }

    private static String resolveUsername(int userId) {
        try {
            String username = DatabaseManager.getUsernameById(userId);
            if (username == null || username.isBlank()) {
                return String.valueOf(userId);
            }
            return username.replaceAll("\\s+", "");
        } catch (Exception e) {
            return String.valueOf(userId);
        }
    }
}