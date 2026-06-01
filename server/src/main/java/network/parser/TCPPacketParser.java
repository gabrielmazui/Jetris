package network.parser;

import network.connection.TCPConnectionManager;
import network.middleware.AddressMiddleware;
import network.middleware.AuthMiddleware;
import network.middleware.Middleware;
import network.middleware.RateLimitMiddleware;
import network.middleware.SessionMiddleware;
import network.packets.LoginPacket;

import auth.LoginController;

public class TCPPacketParser {

    private static final Middleware chain = Middleware.link(
        new AddressMiddleware(),
        new RateLimitMiddleware(200),
        new AuthMiddleware(),
        new SessionMiddleware()
    );

    public static void parse(String rawData, String clientIp) {
        if (rawData == null || rawData.isBlank()) {
            return;
        }

        String cleanData = rawData.trim();

        if (cleanData.equalsIgnoreCase("PING")) {
            TCPConnectionManager.send(clientIp, "PONG");
            return;
        }

        String[] partes = cleanData.split(" ", 4);
        
        if (partes.length < 4) {
            System.err.println("[TCP Parser] Invalid packet format from " + clientIp + ": " + rawData);
            return;
        }

        String type = partes[0].toUpperCase();
        int code;
        int callbackCode;

        try {
            code = Integer.parseInt(partes[1]);
            callbackCode = Integer.parseInt(partes[2]);
        } catch (NumberFormatException e) {
            System.err.println("[TCP Parser] Error parsing codes from " + clientIp);
            return;
        }

        String bodyRaw = partes[3].trim();

        if (!chain.check(type, code, callbackCode, bodyRaw, clientIp)) {
            return;
        }

        switch (type) {
            case "LOGIN":
                if(code == 1){
                    String[] credentials = bodyRaw.split(" ", 2);
                    String username = credentials[0];
                    String password = credentials.length > 1 ? credentials[1] : "";
                    
                    LoginPacket loginPacket = new LoginPacket(code, callbackCode, username, password, "");
                    LoginController.handle(loginPacket, clientIp);
                }
                break;

            default:
                System.err.println("[TCP Parser] Unknown Type (" + type + ") from " + clientIp);
                break;
        }
    }
}