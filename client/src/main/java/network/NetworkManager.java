package network;

import network.tcp.TCPClient;
import network.udp.UDPClient;
import network.parser.PacketParser;

import config.UserState;

public class NetworkManager {
    private static TCPClient tcp;
    private static UDPClient udp; 
    private static PacketParser packetParser;

    static public void start(){
        tcp = new TCPClient();
        packetParser = new PacketParser();
        udp = new UDPClient();

        try{
            Thread.startVirtualThread(tcp);
            Thread.startVirtualThread(packetParser);
            Thread.startVirtualThread(udp);
            //dispatcher aqui

        }catch(Exception e){
            System.out.println("Error:");
            e.printStackTrace();
        }
    }

    static public void shutdown(){
        tcp.shutdown();
    }

    public static UserState verifyTokenCache(){
        //verifica dentro do cache o token de sessao
        // manda pro server
        return null;
    }

    //sendtcp ( recebe packet )
    //sendudp ( recebe packet )
}