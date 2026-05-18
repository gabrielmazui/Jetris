package network.udp;

import java.io.IOException;
import java.net.*;
import network.NetworkContext;
import network.NetworkContext.ConnectionState;

public class UDPClient implements Runnable {

    private DatagramSocket socket;
    private InetAddress address;

    private volatile long lastPingSent;
    private volatile long lastPongTime;

    @Override
    public void run() {
        try {

            NetworkContext.udpState = ConnectionState.CONNECTING;
            connect();
            startReaderLoop();
            startPingLoop();

            while (true) {
                if (System.currentTimeMillis() - lastPongTime > 5000) {
                    NetworkContext.udpState = ConnectionState.DISCONNECTED;
                }
                Thread.sleep(1000);
            }

        } catch(Exception e) {

            System.out.println("Error:");
            e.printStackTrace();

            NetworkContext.udpState = ConnectionState.DISCONNECTED;
        }
    }

    private void connect() throws Exception {
        address =
            InetAddress.getByName(
                NetworkContext.HOST
            );

        socket = new DatagramSocket();
        socket.connect(
            address,
            NetworkContext.PORT
        );

        lastPongTime = System.currentTimeMillis();
        NetworkContext.udpState = ConnectionState.CONNECTED;
    }

    private void startReaderLoop() {
        Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet =
                        new DatagramPacket(
                            buffer,
                            buffer.length
                        );

                    socket.receive(packet);
                    String msg =
                        new String(
                            packet.getData(),
                            0,
                            packet.getLength()
                        );

                    if (msg.equals("PONG")) {
                        lastPongTime = System.currentTimeMillis();
                        network.NetworkContext.ping = (int)(lastPongTime - lastPingSent);
                        continue;
                    }
                    NetworkContext.rawQueueUDP.add(msg);

                } catch(Exception e) {

                    System.out.println("Error:");
                    e.printStackTrace();
                    NetworkContext.udpState = ConnectionState.DISCONNECTED;
                }
            }
        });
    }

    public void send(String msg) {
        try {
            byte[] data = msg.getBytes();

            DatagramPacket packet =
                new DatagramPacket(
                    data,
                    data.length
                );

            socket.send(packet);

        } catch(Exception e) {

            System.out.println("Error:");
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        NetworkContext.udpState = ConnectionState.DISCONNECTED;
    }

    private void startPingLoop() {
        Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    lastPingSent = System.currentTimeMillis();

                    send("PING");
                    Thread.sleep(1000);

                } catch(Exception e) {

                    System.out.println("Error:");
                    e.printStackTrace();

                    NetworkContext.udpState = ConnectionState.DISCONNECTED;
                }
            }
        });
    }
}