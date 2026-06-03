package network.middleware;

import auth.SessionManager;

public class AuthMiddleware extends Middleware {
    @Override
    public boolean check(String type, int code, int callbackCode, String body, String clientIp) {
        if (type.equals("LOGIN") || type.equals("REGISTER")) {
            if (SessionManager.isSessionValid(clientIp)) {
                return false;
            }
        }
        return checkNext(type, code, callbackCode, body, clientIp);
    }
}