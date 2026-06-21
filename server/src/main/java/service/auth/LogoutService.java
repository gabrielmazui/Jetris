package service.auth;

import network.SessionManager;
import network.connection.TCPConnectionManager;
import network.packets.LogoutPack;

public class LogoutService {
    public static void handle(LogoutPack logout, String clientIp){
        String token = logout.getToken();

        Integer userId = SessionManager.getUserIdByToken(token);

        if (userId != null) {
            SessionManager.logout(userId);

            TCPConnectionManager.send(clientIp, "LOGOUT 0 " + logout.getCallbackCode() + " SUCCESS");
        } else {
            TCPConnectionManager.send(clientIp, "LOGOUT 0 " + logout.getCallbackCode() + " FAIL Already_logged_out");
        }
    }
}
