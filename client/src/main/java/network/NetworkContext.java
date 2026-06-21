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
    public static volatile Boolean retryMenuRequested = false;
    public static volatile Boolean retryPaused = false;
    public static volatile Boolean retryExhausted = false;
    public static volatile boolean fullReconnectInProgress = false;
    public static final BlockingQueue<String> rawQueueTCP =
        new LinkedBlockingQueue<>();
    public static final BlockingQueue<String> rawQueueUDP =
        new LinkedBlockingQueue<>();
    public static final BlockingQueue<Packet> packetQueueTCP =
        new LinkedBlockingQueue<>();
    public static final BlockingQueue<Packet> packetQueueUDP =
        new LinkedBlockingQueue<>();

    public interface MatchEventListener {
        void onCountdown(String matchCode, int secondsLeft);
        void onMatchStarted(String matchCode);
        default void onMatchCancelled(String matchCode, String reason) {}
    }
    public static volatile MatchEventListener matchEventListener;

    public interface MatchStateListener {
        void onState(String matchCode, String state, String payload);
    }
    public static volatile MatchStateListener matchStateListener;

    public interface ChatListener {
        void onMessage(String matchCode, int senderId, String senderName, String message);
    }
    public static volatile ChatListener chatListener;

    public interface MatchResultListener {
        void onResult(String matchCode, String outcome, String reason, long startTimeMillis, long endTimeMillis);
    }
    public static volatile MatchResultListener matchResultListener;

}