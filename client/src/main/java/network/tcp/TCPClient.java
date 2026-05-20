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
            long startTime = System.currentTimeMillis();
            try {
                System.out.println("[TCP] Trying to connect to the server, attempt: " + (retries + 1));
                NetworkContext.tcpState = ConnectionState.CONNECTING;
                
                connect();
                retries = 0;

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
        
            } finally {

                if (NetworkContext.tcpState == ConnectionState.CONNECTED) {
                    NetworkContext.tcpState = ConnectionState.RECONNECTING;
                }
                
                cleanup();
                retries++;
                
                long timeSpent = System.currentTimeMillis() - startTime;      
                long remainingSleep = 2000 - timeSpent;

                if (remainingSleep > 0 && retries < MAX_RETRIES && NetworkContext.tcpState != ConnectionState.DISCONNECTED) {
                    try { 
                        Thread.sleep(remainingSleep); 
                    } catch (InterruptedException ignored) {}
                }
            }
        }

        NetworkContext.tcpState = ConnectionState.DISCONNECTED;
        System.out.println("[TCP] Max retries reached");
    }

    private void connect() throws IOException {
    socket = new Socket();
    socket.setSoTimeout(0); 
    SocketAddress socketAddress = new InetSocketAddress(NetworkContext.HOST, NetworkContext.PORT);

    socket.connect(socketAddress, 2000); 

    out = new PrintWriter(socket.getOutputStream(), true);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    lastPongTime = System.currentTimeMillis();
    NetworkContext.tcpState = ConnectionState.CONNECTED;
    System.out.println("[TCP] Connected");
}

    private void startPingLoop() {
        Thread.startVirtualThread(() -> {
            while (NetworkContext.tcpState == ConnectionState.CONNECTED) {
                try {
                    out.println("PING");
                    Thread.sleep(2000);
                    
                    if (System.currentTimeMillis() - lastPongTime > TIMEOUT_PONG) {
                        System.out.println("[TCP] Connection timeout (No PONG received within " + TIMEOUT_PONG + "ms)");
                        handleDisconnect();
                        break;
                    }
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
        NetworkContext.tcpState = ConnectionState.DISCONNECTED;
        cleanup();
    }

}