import network.NetworkManager;
import network.packets.Packet;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientMain {
    static LinkedBlockingQueue<String> rawQueue = new LinkedBlockingQueue<>();
    static LinkedBlockingQueue<Packet> packetQueue = new LinkedBlockingQueue<>();
    
    public static void main(String[] args) {
        NetworkManager net = new NetworkManager(rawQueue, packetQueue);
        net.start();
    }
}
