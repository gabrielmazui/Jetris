package network.tcp;

import java.net.*;
import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;

import io.github.cdimascio.dotenv.Dotenv;

import exceptions.ConnectionException;

public class TCPClient implements Runnable {
    private Boolean connected = false;
    private BufferedReader in;  
    private PrintWriter out;
    private Socket socket;

    private Dotenv env = Dotenv.configure().directory("../").load();
    private String HOST = env.get("SERVER_HOST");
    private int PORT = Integer.parseInt(env.get("SERVER_PORT"));

    private int retries = 0;

    static final int MAX_RETRIES = 10;
    static LinkedBlockingQueue<String> rawQueue;

    public TCPClient(LinkedBlockingQueue<String> raw){
        rawQueue = raw;
    }

    @Override
    public void run(){
        if (connected) {
            throw new ConnectionException("TCP connection already exists");
        }

        while (!connected && (retries < MAX_RETRIES)) {
            try {
                socket = new Socket(HOST, PORT);

                out = new PrintWriter(
                    socket.getOutputStream(),
                    true
                );

                in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                );

                connected = true;
                retries = 0;

                startReaderLoop();

            } catch (IOException e) {
                retries++;
                System.out.println("Error:");
                e.printStackTrace();
            }
        }

        if (!connected) {
            throw new ConnectionException("Failed to connect after retries");
        }
    }

    private void startReaderLoop(){
        String msg; 
        try{
            while ((msg = in.readLine()) != null) { 
                rawQueue.add(msg); 
            }
        }catch(IOException e){
            System.out.println("Error:");
            e.printStackTrace();
        }
    }

    public void shutdown() throws ConnectionException{
        if(!connected){
            throw new ConnectionException("TCP connection does not exists");
        }
        try{
            socket.close();
            connected = false;
        }
        catch(IOException e){
            System.out.println("Error:");
            e.printStackTrace();
        }
    }

    public Boolean isConnected(){
        return connected;
    }
    
    public void send(String message){
        try{
            out.println(message);
        }
        catch(Exception e){
            System.out.println("Error:");
            e.printStackTrace();
        }
    }

}   