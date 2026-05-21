package network;

import network.tcp.TCPClient;
import network.udp.UDPClient;
import network.parser.PacketParser;
import network.NetworkContext;
import network.ConnectionState;
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

    static public void retryConnection(){
        try{
            if(NetworkContext.tcpState == ConnectionState.DISCONNECTED){
                Thread.startVirtualThread(tcp);
            }

            if(NetworkContext.udpState == ConnectionState.DISCONNECTED){
                Thread.startVirtualThread(udp);
            }

        }catch(Exception e){
            System.out.println("Error:");
            e.printStackTrace();
        }
    }

    public static Boolean verifyTokenCache(){
        //verifica dentro do cache o token de sessao
        //manda pro server
        return false;
    }

    public static void sendTCP(String s){
        tcp.send(s);
    }

    public static void sendUDP(String s){
        udp.send(s);
    }

}