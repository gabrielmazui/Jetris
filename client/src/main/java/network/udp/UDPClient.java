package network.udp;

import java.net.*;
import network.NetworkContext;
import network.ConnectionState;

public class UDPClient implements Runnable {

    private DatagramSocket socket;
    private InetAddress address;

    private volatile long lastPingSent;
    private volatile long lastPongTime;

    private static final int DISCONNECT_TIME = 5000;
    private static final int MAX_RETRIES = 60;

    private volatile int retries = 0;

    @Override
    public void run() {
        while (retries < MAX_RETRIES) {
            long startTime = System.currentTimeMillis();
            try {
                System.out.println("[UDP] Connecting attempt: " + retries);
                if (retries == 0) {
                    NetworkContext.udpState = ConnectionState.CONNECTING;
                } else {
                    NetworkContext.udpState = ConnectionState.RECONNECTING;
                }

                connect();
                retries = 0; 
                System.out.println("[UDP] Connected");
                
                startPingLoop();

                byte[] buffer = new byte[1024];
                while (NetworkContext.udpState == ConnectionState.CONNECTED) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        String msg = new String(packet.getData(), 0, packet.getLength());

                        if (msg.equals("PONG")) {
                            lastPongTime = System.currentTimeMillis();
                            NetworkContext.ping = (int)(lastPongTime - lastPingSent);
                            continue;
                        }

                        NetworkContext.rawQueueUDP.add(msg);

                    } catch (SocketTimeoutException e) {
                        if (System.currentTimeMillis() - lastPongTime > DISCONNECT_TIME) {
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                if (NetworkContext.udpState == ConnectionState.CONNECTED) {
                    NetworkContext.udpState = ConnectionState.RECONNECTING;
                }
            } finally {
                if (NetworkContext.udpState != ConnectionState.DISCONNECTED) {
                    NetworkContext.udpState = ConnectionState.RECONNECTING;
                }
                cleanup();
                retries++;
                
                long timeSpent = System.currentTimeMillis() - startTime;      
                long remainingSleep = 1000 - timeSpent;

                if (remainingSleep > 0 && retries < MAX_RETRIES && NetworkContext.udpState != ConnectionState.DISCONNECTED) {
                    try { 
                        Thread.sleep(remainingSleep); 
                    } catch (InterruptedException ignored) {}
                }
            }
        }

        NetworkContext.udpState = ConnectionState.DISCONNECTED;
        System.out.println("[UDP] Max retries reached");
    }

    private void connect() throws Exception {
        address = InetAddress.getByName(NetworkContext.HOST);
        socket = new DatagramSocket();
        socket.connect(address, NetworkContext.PORT);

        socket.setSoTimeout(1000);

        byte[] pingData = "PING".getBytes();
        DatagramPacket handshakePing = new DatagramPacket(pingData, pingData.length);
        socket.send(handshakePing);

        byte[] buffer = new byte[1024];
        DatagramPacket handshakePong = new DatagramPacket(buffer, buffer.length);
        
        socket.receive(handshakePong); 

        String response = new String(handshakePong.getData(), 0, handshakePong.getLength());
        if (!response.equals("PONG")) {
            throw new Exception("Invalid handshake response from server");
        }

        lastPingSent = System.currentTimeMillis();
        lastPongTime = System.currentTimeMillis();
        NetworkContext.udpState = ConnectionState.CONNECTED;
    }

    private void startPingLoop() {
        Thread.startVirtualThread(() -> {
            while (NetworkContext.udpState == ConnectionState.CONNECTED) {
                try {
                    lastPingSent = System.currentTimeMillis();
                    send("PING");
                    Thread.sleep(1000); // Envia ping a cada 1 segundo
                } catch (Exception e) {
                    System.out.println("[UDP] Ping stopped due to error");
                    break;
                }
            }
        });
    }

    public void send(String msg) {
        try {
            if (socket != null && !socket.isClosed()) {
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.send(packet);
            }
        } catch (Exception e) {
            System.out.println("[UDP] Send error: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {}
        socket = null;
    }

    public void shutdown() {
        NetworkContext.udpState = ConnectionState.DISCONNECTED;
        cleanup();
    }
}