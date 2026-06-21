package service.auth;

import java.util.Base64;

import network.SessionManager;
import network.connection.TCPConnectionManager;
import network.packets.LoginPacket;
import db.DatabaseManager;
import exceptions.DBException;

public class LoginService {

    public static void handle(LoginPacket packet, String clientIp) {
        int loginType = packet.getCode();
        int callback = packet.getCallbackCode();

        if (loginType == 0) {
            String token = packet.getToken();
            Integer userId = SessionManager.getUserIdByToken(token);

            if (userId != null) {
                Integer onlineUserAtIp = SessionManager.getUserId(clientIp);
                if (onlineUserAtIp != null) {
                    TCPConnectionManager.send(
                        clientIp,
                        "LOGIN 0 " + callback + " FAIL IP_already_in_use"
                    );
                    return;
                }

                if (SessionManager.isUserOnline(userId)) {
                    TCPConnectionManager.send(
                        clientIp,
                        "LOGIN 0 " + callback + " FAIL User_already_online"
                    );
                    return;
                }

                SessionManager.registerSession(clientIp, userId);

                String pfpBase64 = getPfpBase64(userId);

                TCPConnectionManager.send(
                    clientIp,
                    "LOGIN 0 " + callback + " SUCCESS " + token + " " + pfpBase64
                );
            } else {
                TCPConnectionManager.send(
                    clientIp,
                    "LOGIN 0 " + callback + " FAIL Token_expired_or_invalid"
                );
            }
            return;
        }

        if (loginType == 1) {
            String username = packet.getUsername();
            String password = packet.getPassword();

            if (username == null || username.trim().isEmpty()
                    || password == null || password.trim().isEmpty()) {

                TCPConnectionManager.send(
                    clientIp,
                    "LOGIN 1 " + callback + " FAIL Invalid_fields"
                );
                return;
            }

            if (username.length() < 6
                    || username.length() > 25
                    || !username.matches("^[a-zA-Z0-9_]*$")) {

                TCPConnectionManager.send(
                    clientIp,
                    "LOGIN 1 " + callback + " FAIL Invalid_username_format"
                );
                return;
            }

            if (password.length() < 1
                    || password.length() > 30
                    || password.contains(" ")) {

                TCPConnectionManager.send(
                    clientIp,
                    "LOGIN 1 " + callback + " FAIL Invalid_password_format"
                );
                return;
            }

            int userId;

            try {
                userId = DatabaseManager.validateCredentials(username, password);

                if (userId == -1) {
                    TCPConnectionManager.send(
                        clientIp,
                        "LOGIN 1 " + callback + " FAIL Invalid_credentials"
                    );
                    return;
                }
            } catch (DBException d) {
                System.out.println(d);

                TCPConnectionManager.send(
                    clientIp,
                    "LOGIN 1 " + callback + " FAIL Database_error"
                );
                return;
            }

            if (SessionManager.getUserId(clientIp) != null) {
                TCPConnectionManager.send(
                    clientIp,
                    "LOGIN 1 " + callback + " FAIL IP_already_in_use"
                );
                return;
            }

            if (SessionManager.isUserOnline(userId)) {
                TCPConnectionManager.send(
                    clientIp,
                    "LOGIN 1 " + callback + " FAIL User_already_online"
                );
                return;
            }

            String token = SessionManager.generateToken(userId);
            SessionManager.registerSession(clientIp, userId);

            String pfpBase64 = getPfpBase64(userId);

            TCPConnectionManager.send(
                clientIp,
                "LOGIN 1 " + callback + " SUCCESS " + token + " " + pfpBase64
            );
        }
    }

    private static String getPfpBase64(int userId) {
        try {
            byte[] pfp = DatabaseManager.getUserPfp(userId);

            if (pfp != null) {
                return Base64.getEncoder().encodeToString(pfp);
            }
        } catch (DBException e) {
            e.printStackTrace();
        }

        return "";
    }
    }