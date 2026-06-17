package auth;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    public static class UserSession {
        final int userId; 
        final String token;
        volatile String clientIp; 

        public UserSession(int userId, String token, String clientIp) {
            this.userId = userId;
            this.token = token;
            this.clientIp = clientIp;
        }
    }

    private static final ConcurrentHashMap<Integer, UserSession> sessionsByUserId = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> ipToUserId = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> tokenToUserId = new ConcurrentHashMap<>();

    public static boolean isSessionValid(String clientIp) {
        return clientIp != null && ipToUserId.containsKey(clientIp);
    }

    public static boolean isUserOnline(int userId) {
        UserSession session = sessionsByUserId.get(userId);
        return session != null && session.clientIp != null;
    }

    public static Integer getUserIdByToken(String token) {
        return token == null ? null : tokenToUserId.get(token);
    }

    public static Integer getUserId(String clientIp) {
        return clientIp == null ? null : ipToUserId.get(clientIp);
    }

    public static String getIpByUserId(int userId) {
        UserSession session = sessionsByUserId.get(userId);
        return session == null ? null : session.clientIp;
    }

    public static String generateToken(int userId) {
        String token = UUID.randomUUID().toString();
        
        sessionsByUserId.compute(userId, (key, oldSession) -> {
            if (oldSession != null) {
                tokenToUserId.remove(oldSession.token);
                if (oldSession.clientIp != null) {
                    ipToUserId.remove(oldSession.clientIp);
                }
            }
            tokenToUserId.put(token, userId);
            return new UserSession(userId, token, null);
        });

        return token;
    }

    public static void registerSession(String clientIp, int userId) {
        if (clientIp == null) return;

        sessionsByUserId.compute(userId, (key, currentSession) -> {
            
            if (currentSession == null) {
                ipToUserId.put(clientIp, userId);
                return new UserSession(userId, null, clientIp);
            }

            if (currentSession.clientIp != null) {
                ipToUserId.remove(currentSession.clientIp);
            }

            currentSession.clientIp = clientIp;
            ipToUserId.put(clientIp, userId);
            
            return currentSession;
        });
    }

    public static void removeSession(String clientIp) {
        if (clientIp == null) return;

        Integer userId = ipToUserId.remove(clientIp);
        if (userId != null) {
            sessionsByUserId.computeIfPresent(userId, (key, currentSession) -> {
                if (clientIp.equals(currentSession.clientIp)) {
                    currentSession.clientIp = null; 
                }
                return currentSession;
            });
        }
    }

    public static void logout(int userId) {
        UserSession session = sessionsByUserId.remove(userId);

        if (session != null) {
            tokenToUserId.remove(session.token);
            if (session.clientIp != null) {
                ipToUserId.remove(session.clientIp);
            }
        }
    }
}