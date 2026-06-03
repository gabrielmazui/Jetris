package network.clientHandler;

import network.parser.UDPPacketParser;

public class UDPClientHandler implements Runnable {
    private final byte[] data;
    private final String clientIp;
    private final int clientPort;

    public UDPClientHandler(byte[] data, String clientIp, int clientPort) {
        this.data = data;
        this.clientIp = clientIp;
        this.clientPort = clientPort;
    }

    @Override
    public void run() {
        String rawData = new String(data);
        UDPPacketParser.parse(rawData, clientIp, clientPort);
    }
}