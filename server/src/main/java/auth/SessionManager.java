package auth;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final ConcurrentHashMap<String, String> ipToUser = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> userToIp = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> tokenToUser = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> userToToken = new ConcurrentHashMap<>();

    public static boolean isSessionValid(String clientIp) {
        return ipToUser.containsKey(clientIp);
    }

    public static boolean isUserOnline(String username) {
        return userToIp.containsKey(username);
    }

    public static void registerSession(String clientIp, String username) {
        String oldUser = ipToUser.put(clientIp, username);
        if (oldUser != null && !oldUser.equals(username)) {
            userToIp.remove(oldUser);
        }
        String oldIp = userToIp.put(username, clientIp);
        if (oldIp != null && !oldIp.equals(clientIp)) {
            ipToUser.remove(oldIp);
        }
    }

    public static String generateToken(String username) {
        String oldToken = userToToken.remove(username);
        if (oldToken != null) {
            tokenToUser.remove(oldToken);
        }
        String token = UUID.randomUUID().toString();
        userToToken.put(username, token);
        tokenToUser.put(token, username);
        return token;
    }

    public static String getUsernameByToken(String token) {
        if (token == null) return null;
        return tokenToUser.get(token);
    }

    public static void removeSession(String clientIp) {
        String username = ipToUser.remove(clientIp);
        if (username != null) {
            userToIp.remove(username);
        }
    }

    public static void logout(String username) {
        String clientIp = userToIp.remove(username);
        if (clientIp != null) {
            ipToUser.remove(clientIp);
        }
    }

    public static String getUsername(String clientIp) {
        return ipToUser.get(clientIp);
    }

    public static String getIpByUsername(String username) {
        return userToIp.get(username);
    }
}