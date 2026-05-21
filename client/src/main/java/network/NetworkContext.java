package network;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.github.cdimascio.dotenv.Dotenv;
import network.packets.Packet;

public class NetworkContext {
    private static Dotenv env = Dotenv.configure().directory("/config").load();

    public static String HOST = env.get("SERVER_HOST");
    public static int PORT = Integer.parseInt(env.get("SERVER_PORT"));
    public static int ping = 0;

    public static ConnectionState tcpState = ConnectionState.DISCONNECTED;
    public static ConnectionState udpState = ConnectionState.DISCONNECTED;

    public static final BlockingQueue<String> rawQueueTCP =
        new LinkedBlockingQueue<>();
    public static final BlockingQueue<String> rawQueueUDP =
        new LinkedBlockingQueue<>();
    public static final BlockingQueue<Packet> packetQueue =
        new LinkedBlockingQueue<>();
}