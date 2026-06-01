package auth;

import network.connection.TCPConnectionManager;
import network.packets.LoginPacket;

public class LoginController {

    public static void handle(LoginPacket packet, String clientIp) {
        int loginType = packet.getCode();
        int callback = packet.getCallbackCode();

        if (loginType == 0) {
            String token = packet.getToken();
            String username = SessionManager.getUsernameByToken(token);

            if (username != null) {
                SessionManager.registerSession(clientIp, username);
                TCPConnectionManager.send(clientIp, "LOGIN 0 " + callback + " SUCCESS " + username);
            } else {
                TCPConnectionManager.send(clientIp, "LOGIN 0 " + callback + " FAIL Token_expired_or_invalid");
            }
            return;
        }

        if (loginType == 1) {
            String username = packet.getUsername();
            String password = packet.getPassword();

            if (SessionManager.isUserOnline(username)) {
                String currentIpOfUser = SessionManager.getIpByUsername(username);

                if (clientIp.equals(currentIpOfUser)) {
                    SessionManager.registerSession(clientIp, username);
                    String token = SessionManager.generateToken(username);
                    TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " SUCCESS " + token);
                } else {
                    TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " FAIL User_already_online_elsewhere");
                }
                return;
            }

            if (username.equals("gabriel") && password.equals("1234")) {
                SessionManager.registerSession(clientIp, username);
                String token = SessionManager.generateToken(username);
                TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " SUCCESS " + token);
            } else {
                TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " FAIL Invalid_credentials");
            }
        }
    }
}