package network.middleware;

import auth.SessionManager;

public class SessionMiddleware extends Middleware {
    @Override
    public boolean check(String type, int code, int callbackCode, String body, String clientIp) {
        if (type.equals("LOGIN") || type.equals("PING") || type.equals("REGISTER")) {
            return checkNext(type, code, callbackCode, body, clientIp);
        }

        if (!SessionManager.isSessionValid(clientIp)) {
            System.err.println("[SessionMiddleware] Blocked: No active session for " + clientIp);
            return false;
        }

        return checkNext(type, code, callbackCode, body, clientIp);
    }
}