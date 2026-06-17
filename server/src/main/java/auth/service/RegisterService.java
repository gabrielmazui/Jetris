package auth.service;

import network.connection.TCPConnectionManager;
import network.packets.RegisterPacket;
import db.DatabaseManager;
import exceptions.DBException;

public class RegisterService {

    public static void handle(RegisterPacket packet, String clientIp) {
        String username = packet.getUsername();
        String password = packet.getPassword();
        int callback = packet.getCallbackCode();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            TCPConnectionManager.send(clientIp, "REGISTER " + "0 " + callback + " FAIL Invalid_fields");
            return;
        }

        if (username.length() < 5 || username.length() > 25 || !username.matches("^[a-zA-Z0-9_]*$")) {
            TCPConnectionManager.send(clientIp, "REGISTER " + "0 " + callback + " FAIL Invalid_username_format");
            return;
        }

        if (password.length() < 6 || password.length() > 30 || password.contains(" ")) {
            TCPConnectionManager.send(clientIp, "REGISTER " + "0 " + callback + " FAIL Invalid_password_format");
            return;
        }
        try{
            if (DatabaseManager.userExists(username)) {
                TCPConnectionManager.send(clientIp, "REGISTER " + "0 " + callback + " FAIL Username_already_taken");
                return;
            }
            String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
            boolean success = DatabaseManager.createUser(username, hashedPassword);
            if (success) {
                TCPConnectionManager.send(clientIp, "REGISTER " + "0 " + callback + " SUCCESS");
            } else {
                TCPConnectionManager.send(clientIp, "REGISTER " + "0 " + callback + " FAIL Database_error");
            }
        }catch(DBException d){
            System.out.println(d);
        }
    }
}