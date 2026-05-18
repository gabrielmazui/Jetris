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
            try {
                System.out.println("[UDP] Connecting attempt: " + retries);
                NetworkContext.udpState = ConnectionState.CONNECTING;

                connect();
                
                retries = 0; 
                System.out.println("[UDP] Connected");
                
                startReaderLoop();
                startPingLoop();

                while (NetworkContext.udpState == ConnectionState.CONNECTED) {
                    if (System.currentTimeMillis() - lastPongTime > DISCONNECT_TIME) {
                        System.out.println("[UDP] timeout -> disconnecting");
                        NetworkContext.udpState = ConnectionState.DISCONNECTED;
                        break;
                    }
                    Thread.sleep(500);
                }

            } catch (Exception e) {
                NetworkContext.udpState = ConnectionState.RECONNECTING;
            } finally {
                cleanup();
                retries++;
                
                if (retries < MAX_RETRIES) {
                    sleep(1000); 
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

        socket.setSoTimeout(1500);

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

        socket.setSoTimeout(1000);

        lastPingSent = System.currentTimeMillis();
        lastPongTime = System.currentTimeMillis();
        NetworkContext.udpState = ConnectionState.CONNECTED;
    }

    private void startReaderLoop() {
        Thread.startVirtualThread(() -> {
            while (NetworkContext.udpState == ConnectionState.CONNECTED) {
                try {
                    byte[] buffer = new byte[1024];
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
                    continue;
                } catch (PortUnreachableException e) {
                    System.out.println("[UDP] Port unreachable");
                    NetworkContext.udpState = ConnectionState.DISCONNECTED;
                    break;
                } catch (Exception e) {
                    System.out.println("[UDP] Reader error: " + e.getMessage());
                    NetworkContext.udpState = ConnectionState.DISCONNECTED;
                    break;
                }
            }
            System.out.println("[UDP] Reader stopped");
        });
    }

    private void startPingLoop() {
        Thread.startVirtualThread(() -> {
            while (NetworkContext.udpState == ConnectionState.CONNECTED) {
                try {
                    lastPingSent = System.currentTimeMillis();
                    send("PING");
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println("[UDP] Ping error");
                    NetworkContext.udpState = ConnectionState.DISCONNECTED;
                    break;
                }
            }
            System.out.println("[UDP] Ping stopped");
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

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}