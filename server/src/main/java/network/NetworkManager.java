package network;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import matches.GameLoopManager;
import network.core.TCPServer;
import network.core.UDPServer;

public class NetworkManager {
    
    public static void init() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("src/main/resources/config")
                    .filename(".env")
                    .load();
                    
            String tcpPortStr = dotenv.get("TCP_PORT");
            String udpPortStr = dotenv.get("UDP_PORT");

            if (tcpPortStr == null || udpPortStr == null) {
                throw new IllegalArgumentException(".env error");
            }

            NetworkContext.TCP_PORT = Integer.parseInt(tcpPortStr);
            NetworkContext.UDP_PORT = Integer.parseInt(udpPortStr);


        } catch (DotenvException e) {
            System.err.println("[Network CRITICAL] error .env" + e.getMessage());
            System.exit(1);
        } catch (NumberFormatException e) {
            System.err.println("[Network CRITICAL]" + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("[Network CRITICAL]" + e.getMessage());
            System.exit(1);
        }

        Thread.startVirtualThread(TCPServer::start);
        Thread.startVirtualThread(UDPServer::start);
        GameLoopManager.start();
    }
}