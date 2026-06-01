package network.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

public class UDPConnectionManager {
    private static DatagramSocket serverSocket;
    private static final ConcurrentHashMap<String, Integer> clientPorts = new ConcurrentHashMap<>();

    public static void setServerSocket(DatagramSocket socket) {
        serverSocket = socket;
    }

    public static void registerClientPort(String clientIp, int port) {
        clientPorts.put(clientIp, port);
    }

    public static void unregisterClientPort(String clientIp) {
        clientPorts.remove(clientIp);
    }
    
    public static void send(String clientIp, int port, String message) {
        if (serverSocket == null || serverSocket.isClosed()) return;

        try {
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(clientIp);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            serverSocket.send(packet);
        } catch (IOException e) {
            System.err.println("[UDP Send Error - Direct] " + e.getMessage());
        }
    }

    public static void send(String clientIp, String message) {
        if (serverSocket == null) return;
        
        Integer port = clientPorts.get(clientIp);
        if (port == null) {
            System.err.println("[UDP Send Error] No port registered for IP: " + clientIp);
            return;
        }

        send(clientIp, port, message);
    }
}