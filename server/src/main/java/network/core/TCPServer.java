package network.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import network.NetworkContext;
import network.clientHandler.TCPClientHandler;
import network.connection.TCPConnectionManager;

public class TCPServer {
    private static boolean running = true;

    public static void start() {
        System.out.println("[TCP Server] Starting");
        
        while (running) {
            try (ServerSocket serverSocket = new ServerSocket(NetworkContext.TCP_PORT)) {
                System.out.println("[TCP Server] Listening for connections on port " + NetworkContext.TCP_PORT);
                
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();

                    if (NetworkContext.isAddressAllowed(clientIp)) {
                        System.out.println("[TCP Server] Connection accepted from: " + clientIp);
                        Thread.startVirtualThread(new TCPClientHandler(clientSocket));
                        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                        TCPConnectionManager.registerClient(clientIp, writer);
                    } else {
                        System.out.println("[TCP Server] Connection rejected (Blacklisted IP): " + clientIp);
                        clientSocket.close();
                    }
                }
            } catch (IOException e) {
                System.err.println("[TCP Server] Connection lost or failed to bind: " + e.getMessage());
                if (running) {
                    System.out.println("[TCP Server] Retrying connection in 5 seconds");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    public static void stop() {
        System.out.println("[TCP Server] Stopping server");
        running = false;
    }
}