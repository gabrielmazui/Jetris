package network.tcp;

import java.net.*;
import java.io.*;
import exceptions.ConnectionException;
import network.NetworkContext;
import network.ConnectionState;

public class TCPClient implements Runnable {
    private BufferedReader in;  
    private PrintWriter out;
    private Socket socket;

    private volatile long lastPongTime;
    private int retries = 0;
    private static final int MAX_RETRIES = 30;
    private static final int TIMEOUT_PONG = 8000;

    @Override
    public void run() {
        retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                System.out.println("[TCP] Trying to connect to the server, attempt: " + retries);
                NetworkContext.tcpState = ConnectionState.CONNECTING;
                
                connect();
                retries = 0;

                startReaderLoop();
                startPingLoop();

                while (NetworkContext.tcpState == ConnectionState.CONNECTED) {
                    long currTime = System.currentTimeMillis();
                    if (currTime - lastPongTime > TIMEOUT_PONG) {
                        System.out.println("[TCP] Connection timeout (No PONG received)");
                        handleDisconnect();
                        break;
                    }
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                System.out.println("[TCP] Connection failed: " + e.getMessage());
                NetworkContext.tcpState = ConnectionState.RECONNECTING;
            } finally {
                cleanup();
                retries++;
                
                if (retries < MAX_RETRIES) {
                    sleep(2000);
                }
            }
        }

        NetworkContext.tcpState = ConnectionState.DISCONNECTED;
        System.out.println("[TCP] Max retries reached");
    }

    private void connect() throws IOException {
        socket = new Socket(NetworkContext.HOST, NetworkContext.PORT);
        
        socket.setSoTimeout(10000); 

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        lastPongTime = System.currentTimeMillis();
        NetworkContext.tcpState = ConnectionState.CONNECTED;
        System.out.println("[TCP] Connected");
    }

    private void startReaderLoop() {
        Thread.startVirtualThread(() -> {
            try {
                String msg;
                while (NetworkContext.tcpState == ConnectionState.CONNECTED && (msg = in.readLine()) != null) {

                    if (msg.equals("PONG")) {
                        lastPongTime = System.currentTimeMillis();
                        continue;
                    }

                    NetworkContext.rawQueueTCP.add(msg);
                }
            } catch (IOException e) {
                System.out.println("[TCP] Reader loop error: " + e.getMessage());
            } finally {
                handleDisconnect();
            }
        });
    }

    private void startPingLoop() {
        Thread.startVirtualThread(() -> {
            while (NetworkContext.tcpState == ConnectionState.CONNECTED) {
                try {
                    out.println("PING");
                    Thread.sleep(2000);
                } catch (Exception e) {
                    handleDisconnect();
                    break;
                }
            }
        });
    }

    public void send(String message) {
        if (NetworkContext.tcpState != ConnectionState.CONNECTED) {
            throw new ConnectionException("TCP connection does not exist");
        }
        try {
            out.println(message);
        } catch (Exception e) {
            System.out.println("[TCP] Send error:");
            e.printStackTrace();
            handleDisconnect();
        }
    }

    private void handleDisconnect() {
        if (NetworkContext.tcpState == ConnectionState.CONNECTED) {
            NetworkContext.tcpState = ConnectionState.RECONNECTING;
        }
        cleanup();
    }

    private void cleanup() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
        in = null;
        out = null;
        socket = null;
    }

    public void shutdown() {
        NetworkContext.tcpState = ConnectionState.DISCONNECTED;
        cleanup();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}