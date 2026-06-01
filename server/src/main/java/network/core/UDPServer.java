package network.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

import network.NetworkContext;
import network.clientHandler.UDPClientHandler;
import network.connection.UDPConnectionManager;

public class UDPServer {
    private static boolean running = true;
    private static final int MAX_PACKET_SIZE = 1024;

    public static void start() {
        System.out.println("[UDP Server] Starting");
        
        while (running) {
            try (DatagramSocket datagramSocket = new DatagramSocket(NetworkContext.UDP_PORT)) {
                UDPConnectionManager.setServerSocket(datagramSocket);
                
                
                System.out.println("[UDP Server] Listening for packets on port " + NetworkContext.UDP_PORT);

                while (running) {
                    byte[] buffer = new byte[MAX_PACKET_SIZE + 256];
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(receivePacket);

                    String clientIp = receivePacket.getAddress().getHostAddress();
                    int clientPort = receivePacket.getPort();
                    int packetLength = receivePacket.getLength();

                    if (!NetworkContext.isAddressAllowed(clientIp)) {
                        System.out.println("[UDP Server] Packet dropped (Blacklisted IP): " + clientIp);
                        continue;
                    }

                    if (packetLength > MAX_PACKET_SIZE) {
                        System.err.println("[UDP Server] Warning: Packet from " + clientIp + " dropped. Size exceeded limit.");
                        continue;
                    }

                    UDPConnectionManager.registerClientPort(clientIp, clientPort);

                    byte[] dataCopy = Arrays.copyOfRange(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getOffset() + packetLength);
                    
                    Thread.ofVirtual().start(new UDPClientHandler(dataCopy, clientIp, clientPort));
                }
            } catch (IOException e) {
                System.err.println("[UDP Server] Socket error: " + e.getMessage());
                if (running) {
                    System.out.println("[UDP Server] Retrying in 5 seconds");
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
    }

    public static void stop() {
        System.out.println("[UDP Server] Stopping server");
        running = false;
    }
}