package network.connection;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

import network.SessionManager;

public class TCPConnectionManager {
    private static final ConcurrentHashMap<String, PrintWriter> clientWriters = new ConcurrentHashMap<>();

    public static boolean registerClientIfAbsent(String clientIp, PrintWriter writer) {
        return clientWriters.putIfAbsent(clientIp, writer) == null;
    }

    public static boolean hasClient(String clientIp) {
        return clientWriters.containsKey(clientIp);
    }

    public static void unregisterClient(String clientIp) {
        clientWriters.remove(clientIp);
        SessionManager.removeSession(clientIp);
    }

    public static void send(String clientIp, String message) {
        PrintWriter writer = clientWriters.get(clientIp);
        if (writer != null) {
            writer.println(message);

            if (!message.equals("PONG") && !message.startsWith("MATCH_STATE ") && !message.startsWith("CHAT ")) {
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