package network.middleware;

import network.SessionManager;

public class AuthMiddleware extends Middleware {
    @Override
    public boolean check(String type, int code, int callbackCode, String body, String clientIp) {
        if (type.equals("LOGIN") || type.equals("REGISTER") || type.equals("PING")) {
            return checkNext(type, code, callbackCode, body, clientIp);
        }

        if (body == null || body.isBlank()) {
            System.err.println("[AuthMiddleware] Blocked: Empty body on protected route from " + clientIp);
            return false;
        }

        String[] parts = body.trim().split(" ", 2);
        String token = parts[0];

        Integer userId = SessionManager.getUserIdByToken(token);
        if (userId == null) {
            System.err.println("[AuthMiddleware] Blocked: Invalid Token from " + clientIp);
            return false; 
        }

        return checkNext(type, code, callbackCode, body, clientIp);
    }
}