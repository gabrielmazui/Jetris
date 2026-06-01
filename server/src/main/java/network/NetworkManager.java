package network;

import io.github.cdimascio.dotenv.Dotenv;
import network.core.TCPServer;
import network.core.UDPServer;

public class NetworkManager {
    
    public static void init() {
        System.out.println("Starting server");
        Dotenv dotenv = Dotenv.configure()
                .directory("src/main/resources/config")
                .filename(".env")
                .load();
                
        NetworkContext.TCP_PORT = Integer.parseInt(dotenv.get("TCP_PORT"));
        NetworkContext.UDP_PORT = Integer.parseInt(dotenv.get("UDP_PORT"));

        System.out.println("Ports from .env configured");

        Thread.startVirtualThread(TCPServer::start);
        System.out.println("Starting TCP");
        Thread.startVirtualThread(UDPServer::start);
        System.out.println("Starting UDP");
    }
}