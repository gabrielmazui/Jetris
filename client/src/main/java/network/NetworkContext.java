package network;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.cdimascio.dotenv.Dotenv;
import network.packets.Packet;

public class NetworkContext {
    private static Dotenv env = Dotenv.configure().directory("/config").load();

    public static String HOST = env.get("SERVER_HOST");
    public static int PORT_TCP = Integer.parseInt(env.get("SERVER_PORT_TCP"));
    public static int PORT_UDP = Integer.parseInt(env.get("SERVER_PORT_UDP"));
    public static int ping = 0;

    public static final Map<Integer, NetworkCallback> mapCallbacks = new HashMap<>();
    public static final AtomicInteger requestCallbackID = new AtomicInteger(0);

    public static ConnectionState tcpState = ConnectionState.CONNECTING;
    public static ConnectionState udpState = ConnectionState.CONNECTING;

    public static volatile Boolean isAttemptingTCP = true;
    public static volatile Boolean isAttemptingUDP = true;

    public static final BlockingQueue<String> rawQueueTCP =
        new LinkedBlockingQueue<>();
    public static final BlockingQueue<String> rawQueueUDP =
        new LinkedBlockingQueue<>();
    public static final BlockingQueue<Packet> packetQueueTCP =
        new LinkedBlockingQueue<>();
    public static final BlockingQueue<Packet> packetQueueUDP =
        new LinkedBlockingQueue<>();
}