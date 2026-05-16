package network;
import java.util.concurrent.LinkedBlockingQueue;

import network.packets.Packet;
import network.tcp.TCPClient;
import network.udp.UDPClient;
import network.parser.PacketParser;

public class NetworkManager {
    static LinkedBlockingQueue<String> rawQueue = new LinkedBlockingQueue<>();
    static LinkedBlockingQueue<Packet> packetQueue = new LinkedBlockingQueue<>();
    static TCPClient tcp;
    static UDPClient udp; 
    static PacketParser packetParser;

    public NetworkManager(LinkedBlockingQueue<String> raw, LinkedBlockingQueue<Packet> packet){
        rawQueue = raw;
        packetQueue = packet;
    }

    public void start(){
        tcp = new TCPClient(rawQueue);
        packetParser = new PacketParser(rawQueue, packetQueue);

        try{
            Thread.startVirtualThread(tcp);
            Thread.startVirtualThread(packetParser);
            //dispatcher aqui

        }catch(Exception e){
            System.out.println("Error:");
            e.printStackTrace();
        }
    }

    //ping pong e verificao tcp se esta conectado
    //caso nao estiver faz retry
}