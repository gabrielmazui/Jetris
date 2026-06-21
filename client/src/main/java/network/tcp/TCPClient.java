package network.tcp;

import java.net.*;
import java.io.*;
import exceptions.ConnectionException;
import network.NetworkManager;
import network.NetworkContext;
import network.ConnectionState;

public class TCPClient implements Runnable {
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private BufferedReader in;  
    private PrintWriter out;
    private Socket socket;

    private volatile long lastPongTime;
    private static final int TIMEOUT_PONG = 6000;
    private volatile boolean running = true;

    @Override
    public void run() {
        NetworkContext.isAttemptingTCP = true;
        boolean firstAttempt = true;
        int reconnectAttempts = 0;
        while (running && !Thread.currentThread().isInterrupted()) {
            long startTime = System.currentTimeMillis();
            boolean exhausted = false;
            try {
                System.out.println("[TCP] Attempt " + (reconnectAttempts + 1) + "/" + MAX_RECONNECT_ATTEMPTS);
                NetworkContext.tcpState = firstAttempt ? ConnectionState.CONNECTING : ConnectionState.RECONNECTING;
                
                connect();
                NetworkContext.isAttemptingTCP = false;
                firstAttempt = false;
                reconnectAttempts = 0;
                startPingLoop();

                String msg;
                while (NetworkContext.tcpState == ConnectionState.CONNECTED && (msg = in.readLine()) != null) {
                    if (msg.equals("PONG")) {
                        lastPongTime = System.currentTimeMillis();
                        continue;
                    }
                    NetworkContext.rawQueueTCP.add(msg);
                }

            } catch (Exception e) {
                if (running && NetworkContext.tcpState != ConnectionState.DISCONNECTED) {
                    reconnectAttempts++;
                    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                        exhausted = true;
                        System.out.println("[TCP] Reconnect attempts exhausted");
                    }
                }
            } finally {

                if (NetworkContext.tcpState == ConnectionState.CONNECTED) {
                    NetworkContext.tcpState = ConnectionState.RECONNECTING;
                }
                
                cleanup();
                if (exhausted) {
                    NetworkContext.tcpState = ConnectionState.DISCONNECTED;
                    NetworkContext.isAttemptingTCP = false;
                    break;
                }
                if (running && NetworkContext.tcpState != ConnectionState.DISCONNECTED) {
                    NetworkManager.notifyConnectionDrop();
                }
                
                long timeSpent = System.currentTimeMillis() - startTime;      
                long remainingSleep = 2000 - timeSpent;

                if (remainingSleep > 0 && running && NetworkContext.tcpState != ConnectionState.DISCONNECTED) {
                    try { 
                        Thread.sleep(remainingSleep); 
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        NetworkContext.tcpState = ConnectionState.DISCONNECTED;
    }

    private void connect() throws IOException {
        socket = new Socket();
        socket.setSoTimeout(0); 
        SocketAddress socketAddress = new InetSocketAddress(NetworkContext.HOST, NetworkContext.PORT_TCP);

        socket.connect(socketAddress, 2000); 

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        lastPongTime = System.currentTimeMillis();
        NetworkContext.tcpState = ConnectionState.CONNECTED;
        System.out.println("[TCP] Connected");
    }

    private void startPingLoop() {
        Thread.startVirtualThread(() -> {
            while (!Thread.currentThread().isInterrupted() && NetworkContext.tcpState == ConnectionState.CONNECTED) {
                try {
                    out.println("PING");
                    Thread.sleep(2000);
                    
                    if (System.currentTimeMillis() - lastPongTime > TIMEOUT_PONG) {
                        System.out.println("[TCP] Connection timeout (No PONG received within " + TIMEOUT_PONG + "ms)");
                        handleDisconnect();
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    handleDisconnect();
                    break;
                }
            }
        });
    }

    public void send(String message) throws ConnectionException {
        if (NetworkContext.tcpState != ConnectionState.CONNECTED) {
            throw new ConnectionException("TCP connection does not exist");
        }
        try {
            out.println(message);
        } catch (Exception e) {
            System.out.println("[TCP] Send error: " + e.getMessage());
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
        running = false;
        NetworkContext.tcpState = ConnectionState.DISCONNECTED;
        cleanup();
    }
}