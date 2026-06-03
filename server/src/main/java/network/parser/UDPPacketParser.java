package network.parser;

import network.connection.UDPConnectionManager;
import network.middleware.AddressMiddleware;
import network.middleware.Middleware;
import network.middleware.RateLimitMiddleware;
import network.middleware.SessionMiddleware;

public class UDPPacketParser {

    private static final Middleware chain = Middleware.link(
        new AddressMiddleware(),
        new RateLimitMiddleware(10),
        new SessionMiddleware()
    );

    public static void parse(String rawData, String clientIp, int clientPort) {
        if (rawData == null || rawData.isBlank()) {
            return;
        }

        String cleanData = rawData.trim();

        if (cleanData.equalsIgnoreCase("PING")) {
            UDPConnectionManager.send(clientIp, clientPort, "PONG");
            return;
        }

        String[] partes = cleanData.split(" ", 4);
        
        if (partes.length < 4) {
            System.err.println("[UDP Parser] Invalid packet format from " + clientIp + ": " + rawData);
            return;
        }

        String type = partes[0].toUpperCase();
        int code;
        int callbackCode;

        try {
            code = Integer.parseInt(partes[1]);
            callbackCode = Integer.parseInt(partes[2]);
        } catch (NumberFormatException e) {
            System.err.println("[UDP Parser] Error parsing codes from " + clientIp);
            return;
        }

        String bodyRaw = partes[3].trim();

        if (!chain.check(type, code, callbackCode, bodyRaw, clientIp)) {
            return;
        }

        switch (type) {
            case "MOVE":
                break;

            default:
                System.err.println("[UDP Parser] Unknown Type (" + type + ") from " + clientIp);
                break;
        }
    }
}