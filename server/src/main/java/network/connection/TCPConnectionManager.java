package network.connection;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

import auth.SessionManager;

public class TCPConnectionManager {
    private static final ConcurrentHashMap<String, PrintWriter> clientWriters = new ConcurrentHashMap<>();

    public static void registerClient(String clientIp, PrintWriter writer) {
        clientWriters.put(clientIp, writer);
    }

    public static void unregisterClient(String clientIp) {
        clientWriters.remove(clientIp);
        SessionManager.removeSession(clientIp);
    }

    public static void send(String clientIp, String message) {
        PrintWriter writer = clientWriters.get(clientIp);
        if (writer != null) {
            writer.println(message);

            if (!message.equals("PONG")) {
                String preview = message.length() > 100
                        ? message.substring(0, 100) + "..."
                        : message;

                System.out.println(
                    "[TCP] Sent (" + message.length() + " chars): ["
                    + preview + "] ClientIP: [" + clientIp + "]"
                );
            }
        }
    }
}