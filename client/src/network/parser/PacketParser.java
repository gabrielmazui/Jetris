package network.parser;

import java.util.concurrent.LinkedBlockingQueue;

import network.packets.Packet;

public class PacketParser implements Runnable {
    static LinkedBlockingQueue<String> rawQueue;
    static LinkedBlockingQueue<Packet> packetQueue;

    public PacketParser(LinkedBlockingQueue<String> raw, LinkedBlockingQueue<Packet> packet){
        rawQueue = raw;
        packetQueue = packet;
    }
    @Override
    public void run(){
        
    }
}
