package network;

import network.tcp.TCPClient;
import network.udp.UDPClient;
import network.parser.PacketParser;

public class NetworkManager {
    private static TCPClient tcp;
    private static UDPClient udp; 
    private static PacketParser packetParser;

    static public void start(){
        tcp = new TCPClient();
        packetParser = new PacketParser();

        try{
            Thread.startVirtualThread(tcp);
            Thread.startVirtualThread(packetParser);
            //dispatcher aqui

        }catch(Exception e){
            System.out.println("Error:");
            e.printStackTrace();
        }
    }

    static public void shutdown(){
        tcp.shutdown();
    }

    //sendtcp ( recebe packet )
    //sendudp ( recebe packet )
}