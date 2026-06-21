package network.udp;

import java.net.*;
import network.NetworkManager;
import network.NetworkContext;
import network.ConnectionState;

public class UDPClient implements Runnable {
    private static final int MAX_RECONNECT_ATTEMPTS = 20;

    private DatagramSocket socket;
    private InetAddress address;
    private volatile long lastPingSent;
    private volatile long lastPongTime;

    private static final int DISCONNECT_TIME = 3000;
    private volatile boolean running = true;

    @Override
    public void run() {
        NetworkContext.isAttemptingUDP = true;
        boolean firstAttempt = true;
        int reconnectAttempts = 0;
        while (running && !Thread.currentThread().isInterrupted()) {
            long startTime = System.currentTimeMillis();
            boolean exhausted = false;
            try {
                System.out.println("[UDP] Attempt " + (reconnectAttempts + 1) + "/" + MAX_RECONNECT_ATTEMPTS);
                NetworkContext.udpState = firstAttempt ? ConnectionState.CONNECTING : ConnectionState.RECONNECTING;

                connect();
                firstAttempt = false;
                System.out.println("[UDP] Connected");
                reconnectAttempts = 0;
                
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
                            NetworkManager.notifyConnectionDrop();
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                if (running && NetworkContext.udpState != ConnectionState.DISCONNECTED) {
                    reconnectAttempts++;
                    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                        exhausted = true;
                        System.out.println("[UDP] Reconnect attempts exhausted");
                    }
                }
                if (NetworkContext.udpState == ConnectionState.CONNECTED) {
                    NetworkContext.udpState = ConnectionState.RECONNECTING;
                }
            } finally {
                if (exhausted) {
                    NetworkContext.udpState = ConnectionState.DISCONNECTED;
                    NetworkContext.isAttemptingUDP = false;
                    cleanup();
                    break;
                }
                if (running && NetworkContext.udpState != ConnectionState.DISCONNECTED) {
                    NetworkContext.udpState = ConnectionState.RECONNECTING;
                }
                cleanup();
                if (running && NetworkContext.udpState != ConnectionState.DISCONNECTED) {
                    NetworkManager.notifyConnectionDrop();
                }
                
                long timeSpent = System.currentTimeMillis() - startTime;      
                long remainingSleep = 1000 - timeSpent;

                if (remainingSleep > 0 && running && NetworkContext.udpState != ConnectionState.DISCONNECTED) {
                    try { 
                        Thread.sleep(remainingSleep); 
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        NetworkContext.udpState = ConnectionState.DISCONNECTED;
        NetworkContext.isAttemptingUDP = false;
    }

    private void connect() throws Exception {
        address = InetAddress.getByName(NetworkContext.HOST);
        socket = new DatagramSocket();
        socket.connect(address, NetworkContext.PORT_UDP);

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
            while (!Thread.currentThread().isInterrupted() && NetworkContext.udpState == ConnectionState.CONNECTED) {
                try {
                    Thread.sleep(1000); 
                    lastPingSent = System.currentTimeMillis();
                    send("PING");
                } catch (Exception e) {
                    System.out.println("[UDP] Ping stopped due to error");
                    break;
                }
            }
        });
    }
 
    public void send(String msg) throws Exception {
        if(NetworkContext.udpState != ConnectionState.CONNECTED){
            throw new ConnectException("UDP is not Connected");
        }
        if (socket != null && !socket.isClosed()) {
            byte[] data = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length);
            socket.send(packet);
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
        running = false;
        NetworkContext.udpState = ConnectionState.DISCONNECTED;
        NetworkContext.isAttemptingUDP = false;
        cleanup();
    }
}