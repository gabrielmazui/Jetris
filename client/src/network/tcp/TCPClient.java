package network.tcp;

import java.net.*;
import java.io.*;

import exceptions.ConnectionException;
import network.NetworkContext;
import network.NetworkContext.ConnectionState;


public class TCPClient implements Runnable {
    private BufferedReader in;  
    private PrintWriter out;
    private Socket socket;

    private volatile long lastPongTime;

    private int retries = 0;

    static private final int MAX_RETRIES = 20;

   @Override
    public void run() {
        retries = 0;
        while(retries < MAX_RETRIES) {
            try {
                NetworkContext.tcpState = ConnectionState.CONNECTING;
                connect();
                retries = 0;

                startReaderLoop();
                startPingLoop();

                while(NetworkContext.tcpState == ConnectionState.CONNECTED) {
                    Thread.sleep(1000);
                }

                retries++;
                Thread.sleep(2000);

            } catch(Exception e) {

                retries++;
                e.printStackTrace();
            }
        }

        NetworkContext.tcpState = ConnectionState.DISCONNECTED;
    }

    private void connect() throws IOException {
        socket = new Socket(
            NetworkContext.HOST,
            NetworkContext.PORT
        );

        out = new PrintWriter(
            socket.getOutputStream(),
            true
        );

        in = new BufferedReader(
            new InputStreamReader(
                socket.getInputStream()
            )
        );

        NetworkContext.tcpState = ConnectionState.CONNECTED;
    }

    private void startReaderLoop() {
        Thread.startVirtualThread(() -> {
            try {

                String msg;

                while (NetworkContext.tcpState == ConnectionState.CONNECTED && (msg = in.readLine()) != null) {

                    if(msg.equals("PONG")) {
                        lastPongTime = System.currentTimeMillis();
                        continue;
                    }

                    NetworkContext.rawQueueTCP.add(msg);
                }
                handleDisconnect();

            } catch(IOException e) {
                handleDisconnect();
            }
        });
    }

    public void shutdown() {
        NetworkContext.tcpState = ConnectionState.DISCONNECTED;
        try {

            socket.close();

        } catch(IOException e) {

            e.printStackTrace();
        }
    }

    private void handleDisconnect() {
        if(NetworkContext.tcpState != ConnectionState.CONNECTED) {
            return;
        }

        NetworkContext.tcpState = ConnectionState.RECONNECTING;
        try {

            socket.close();

        } catch(IOException e) {

            e.printStackTrace();
        }
    }
    
    public void send(String message){
        if(NetworkContext.tcpState != ConnectionState.CONNECTED){
            throw new ConnectionException("TCP connection does not exists");
        }
        try{
            out.println(message);
        }
        catch(Exception e){
            System.out.println("Error:");
            e.printStackTrace();
        }
    }

    private void startPingLoop() {
        Thread.startVirtualThread(() -> {
            lastPongTime = System.currentTimeMillis();
            while(NetworkContext.tcpState == ConnectionState.CONNECTED) {
                try {
                    out.println("PING");
                    Thread.sleep(2000);
                    handlePingLoop();
                } catch(Exception e) {

                    handleDisconnect();
                }
            }
        });
    }

    private void handlePingLoop() {
        Thread.startVirtualThread(() -> {
            while(NetworkContext.tcpState == ConnectionState.CONNECTED){
                long currTime = System.currentTimeMillis();
                if(currTime - lastPongTime > 8000){
                    handleDisconnect();
                }
            }
        });
    }
}   