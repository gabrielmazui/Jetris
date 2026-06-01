package network;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkContext {
    public static int TCP_PORT;
    public static int UDP_PORT;

    private static final Set<String> blacklistedIps = ConcurrentHashMap.newKeySet();

    public static boolean isAddressAllowed(String ipAddress) {
        return !blacklistedIps.contains(ipAddress);
    }

    public static void blacklistIp(String ipAddress) {
        blacklistedIps.add(ipAddress);
    }

    public static void whitelistIp(String ipAddress) {
        blacklistedIps.remove(ipAddress);
    }
}