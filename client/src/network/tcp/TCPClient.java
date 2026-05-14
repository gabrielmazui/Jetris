package network.tcp;

import java.net.*;
import java.io.*;
import io.github.cdimascio.dotenv.Dotenv;

public class TCPClient {
    public TCPClient(){
        Dotenv env = Dotenv.configure().directory("../").load();
        String HOST = env.get("SERVER_HOST");
        int PORT = Integer.parseInt(env.get("SERVER_PORT"));

        try (Socket socket = new Socket(HOST, PORT)){

            PrintWriter out = new PrintWriter(
                socket.getOutputStream(),
                true
            );

            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );

            // out.println("teste");

            // String response = in.readLine();

            // System.out.println("Servidor respondeu: " + response);

        }catch (IOException e) {

            System.out.println("Error:");
            e.printStackTrace();

        }
    }
}   
