package network.middleware;

import network.NetworkContext;

public class AddressMiddleware extends Middleware {
    @Override
    public boolean check(String type, int code, int callbackCode, String body, String clientIp) {
        if (!NetworkContext.isAddressAllowed(clientIp)) {
            System.err.println("[AddressMiddleware] Blocked IP: " + clientIp);
            return false;
        }
        return checkNext(type, code, callbackCode, body, clientIp);
    }
}