package network;

import network.tcp.TCPClient;
import network.udp.UDPClient;
import network.parser.PacketParserTCP;
import network.dispatcher.DispatcherTCP;

public class NetworkManager {
    private static TCPClient tcp;
    private static UDPClient udp; 
    private static PacketParserTCP parserTCP;
    private static DispatcherTCP dispatcherTCP;

    static public void start(){
        tcp = new TCPClient();
        udp = new UDPClient();
        parserTCP = new PacketParserTCP();
        dispatcherTCP = new DispatcherTCP();

        try{
            Thread.startVirtualThread(tcp);
            Thread.startVirtualThread(parserTCP);
            Thread.startVirtualThread(dispatcherTCP);

            Thread.startVirtualThread(udp);

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

    public static void sendTCP(String toSend, NetworkCallback callback){
        NetworkContext.mapCallbacks.put(callback.code, callback);
        tcp.send(toSend);
    }

    public static void sendUDP(String toSend){
        udp.send(toSend);
    }

}