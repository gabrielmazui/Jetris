package auth;

import network.connection.TCPConnectionManager;
import network.packets.LoginPacket;
import db.DatabaseManager;
import exceptions.DBException;

public class LoginController {

    public static void handle(LoginPacket packet, String clientIp) {
        int loginType = packet.getCode();
        int callback = packet.getCallbackCode();

        if (loginType == 0) {
            String token = packet.getToken();
            String username = SessionManager.getUsernameByToken(token);

            if (username != null) {
                SessionManager.registerSession(clientIp, username);
                TCPConnectionManager.send(clientIp, "LOGIN 0 " + callback + " SUCCESS " + token + " " + username);
            } else {
                TCPConnectionManager.send(clientIp, "LOGIN 0 " + callback + " FAIL Token_expired_or_invalid");
            }
            return;
        }

        if (loginType == 1) {
            String username = packet.getUsername();
            String password = packet.getPassword();

            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " FAIL Invalid_fields");
                return;
            }

            if (username.length() < 6 || username.length() > 25 || !username.matches("^[a-zA-Z0-9_]*$")) {
                TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " FAIL Invalid_username_format");
                return;
            }

            if (password.length() < 1 || password.length() > 30 || password.contains(" ")) {
                TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " FAIL Invalid_password_format");
                return;
            }

            try {
                if (!DatabaseManager.validateCredentials(username, password)) {
                    TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " FAIL Invalid_credentials");
                    return;
                }
            } catch (DBException d) {
                System.out.println(d);
                TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " FAIL Database_error");
                return;
            }
            
            if (SessionManager.isUserOnline(username)) {
                String currentIpOfUser = SessionManager.getIpByUsername(username);

                if (!clientIp.equals(currentIpOfUser)) {
                    TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " FAIL User_already_online_elsewhere");
                    return;
                }
            }

            SessionManager.registerSession(clientIp, username);
            String token = SessionManager.generateToken(username);
            TCPConnectionManager.send(clientIp, "LOGIN 1 " + callback + " SUCCESS " + token);
        }
    }
}