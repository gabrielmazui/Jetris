package network.clientHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;

import network.connection.TCPConnectionManager;
import network.connection.UDPConnectionManager;
import network.parser.TCPPacketParser;

public class TCPClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String clientIp;
    
    private static final int DEFAULT_MAX_PACKET_SIZE = 1024;
    private static final int MAX_PFP_IMAGE_SIZE_MB = 5;
    private static final int MAX_PFP_BASE64_SIZE = ((MAX_PFP_IMAGE_SIZE_MB * 1024 * 1024) * 4) / 3;

    public TCPClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientIp = clientSocket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String rawData;
            while ((rawData = reader.readLine()) != null) {
                int currentLimit = DEFAULT_MAX_PACKET_SIZE;
                boolean isImageUpload = rawData.startsWith("SETPFP"); 

                if (isImageUpload) {
                    currentLimit = MAX_PFP_BASE64_SIZE;
                }

                if (rawData.length() > currentLimit) {
                    if (isImageUpload) {
                        System.err.println("[TCP Handler] PFP Upload rejected from " + clientIp + ". Exceeded max allowed size of " + MAX_PFP_IMAGE_SIZE_MB + " MB.");
                        
                        writer.println("ERROR_PFP_SIZE_EXCEEDED|Image exceeds maximum limit of " + MAX_PFP_IMAGE_SIZE_MB + "MB.");
                    } else {
                        System.err.println("[TCP Handler] Warning: Standard packet from " + clientIp + " dropped. Size exceeded limit (" + rawData.length() + "/" + DEFAULT_MAX_PACKET_SIZE + " bytes).");
                        writer.println("ERROR_PACKET_SIZE_EXCEEDED");
                        break;
                    }
                    continue;
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