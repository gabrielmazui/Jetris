package service.auth;

import db.DatabaseManager;
import exceptions.DBException;
import network.SessionManager;
import network.connection.TCPConnectionManager;
import network.packets.DeleteAccountPacket;

public class DeleteService {

    public static void handle(DeleteAccountPacket packet, String clientIp) {
        String token = packet.token;
        int cb = packet.callbackCode;

        Integer userId = SessionManager.getUserIdByToken(token);
        if (userId == null) {
            send(clientIp, cb, "INVALID_TOKEN");
            return;
        }

        try {
            boolean deleted = DatabaseManager.deleteUser(userId);
            if (!deleted) {
                send(clientIp, cb, "TRY_AGAIN_LATER");
                return;
            }

            SessionManager.logout(userId);
            send(clientIp, cb, "SUCCESS");

        } catch (DBException e) {
            System.err.println("[DeleteService] DB error: " + e.getMessage());
            send(clientIp, cb, "TRY_AGAIN_LATER");
        }
    }

    private static void send(String clientIp, int callbackCode, String status) {
        TCPConnectionManager.send(clientIp, "DELETE_ACCOUNT 0 " + callbackCode + " " + status);
    }
}