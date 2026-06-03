package network.clientHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;

import network.connection.TCPConnectionManager;
import network.connection.UDPConnectionManager;
import network.parser.TCPPacketParser;

public class TCPClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String clientIp;
    private static final int MAX_PACKET_SIZE = 1024;

    public TCPClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientIp = clientSocket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String rawData;
            while ((rawData = reader.readLine()) != null) {
                if (rawData.length() > MAX_PACKET_SIZE) {
                    System.err.println("[TCP Handler] Warning: Packet from " + clientIp + " dropped. Size exceeded limit (" + rawData.length() + "/" + MAX_PACKET_SIZE + " bytes).");
                    break;
                }
                TCPPacketParser.parse(rawData, clientIp);
            }
        } catch (IOException e) {
            System.err.println("[TCP Handler] Connection error with " + clientIp + ": " + e.getMessage());
        } finally {
            TCPConnectionManager.unregisterClient(clientIp);
            UDPConnectionManager.unregisterClientPort(clientIp);
            try {
                clientSocket.close();
                System.out.println("[TCP Handler] Connection closed for: " + clientIp);
            } catch (IOException e) {
                System.err.println("[TCP Handler] Error closing socket for " + clientIp + ": " + e.getMessage());
            }
        }
    }
}