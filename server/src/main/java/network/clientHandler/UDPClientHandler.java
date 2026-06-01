package network.clientHandler;

import network.parser.UDPPacketParser;

public class UDPClientHandler implements Runnable {
    private final byte[] data;
    private final String clientIp;
    private final int clientPort; // ADICIONADO

    public UDPClientHandler(byte[] data, String clientIp, int clientPort) {
        this.data = data;
        this.clientIp = clientIp;
        this.clientPort = clientPort;
    }

    @Override
    public void run() {
        String rawData = new String(data);
        System.out.println("[UDP Handler] Processing packet from " + clientIp + ":" + clientPort + " (" + data.length + " bytes): " + rawData);
        UDPPacketParser.parse(rawData, clientIp, clientPort);
    }
}