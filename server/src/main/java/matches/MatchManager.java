package matches;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import db.DatabaseManager;
import exceptions.DBException;
import network.SessionManager;
import network.connection.TCPConnectionManager;

public class MatchManager {
    private static final ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<String, MatchSession> pendingPrivateMatches = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MatchSession> activeMatches = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MatchResult> finishedMatches = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, PendingDisconnect> pendingDisconnects = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService disconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "match-disconnect-grace");
        t.setDaemon(true);
        return t;
    });

    private static final int DISCONNECT_GRACE_SECONDS = 30;

    private static final class PendingDisconnect {
        final int userId;
        final String matchCode;
        volatile ScheduledFuture<?> future;

        PendingDisconnect(int userId, String matchCode) {
            this.userId = userId;
            this.matchCode = matchCode;
        }
    }

    public static final class MatchResult {
        public final String matchCode;
        public final Integer winnerId;
        public final Integer loserId;
        public final String reason;
        public final long startTimeMillis;
        public final long endTimeMillis;

        public MatchResult(String matchCode, Integer winnerId, Integer loserId, String reason,
                           long startTimeMillis, long endTimeMillis) {
            this.matchCode = matchCode;
            this.winnerId = winnerId;
            this.loserId = loserId;
            this.reason = reason == null ? "" : reason;
            this.startTimeMillis = startTimeMillis;
            this.endTimeMillis = endTimeMillis;
        }
    }

    public static void joinQueue(int userId, String clientIp, int callback) {
        if (queue.contains(userId)) {
            TCPConnectionManager.send(clientIp, "MATCH 0 " + callback + " FAIL Already_in_queue");
            return;
        }

        Integer opponentId = queue.poll();

        if (opponentId != null && opponentId != userId) {
            String matchCode = UUID.randomUUID().toString().substring(0, 8);
            MatchSession session = new MatchSession(matchCode, opponentId);
            session.setPlayer2(userId);

            activeMatches.put(matchCode, session);
            session.startMatch();

            String oppIp = SessionManager.getIpByUserId(opponentId);

            TCPConnectionManager.send(oppIp, "MATCH 0 0 SUCCESS START " + matchCode);
            TCPConnectionManager.send(clientIp, "MATCH 0 " + callback + " SUCCESS START " + matchCode);
        } else {
            queue.add(userId);
            TCPConnectionManager.send(clientIp, "MATCH 0 " + callback + " SUCCESS QUEUED");
        }
    }

    public static void leaveQueue(int userId, String clientIp, int callback) {
        queue.remove(userId);
        TCPConnectionManager.send(clientIp, "MATCH 1 " + callback + " SUCCESS LEAVED");
    }

    public static void createPrivateMatch(int userId, String clientIp, int callback) {
        String matchCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MatchSession session = new MatchSession(matchCode, userId);
        pendingPrivateMatches.put(matchCode, session);
        TCPConnectionManager.send(clientIp, "MATCH 2 " + callback + " SUCCESS CREATED " + matchCode);
    }

    public static void joinPrivateMatch(int userId, String matchCode, String clientIp, int callback) {
        MatchSession session = pendingPrivateMatches.get(matchCode);

        if (session != null && session.getState() == MatchState.WAITING_FOR_PLAYERS) {
            pendingPrivateMatches.remove(matchCode, session);
            session.setPlayer2(userId);

            activeMatches.put(matchCode, session);
            session.startMatch();

            String hostIp = SessionManager.getIpByUserId(session.getPlayer1());
            TCPConnectionManager.send(hostIp, "MATCH 3 0 SUCCESS START " + matchCode);
            TCPConnectionManager.send(clientIp, "MATCH 3 " + callback + " SUCCESS START " + matchCode);
        } else {
            TCPConnectionManager.send(clientIp, "MATCH 3 " + callback + " FAIL Invalid_or_full");
        }
    }

    public static void cancelPrivateMatch(int userId, String matchCode, String clientIp, int callback) {
        MatchSession session = pendingPrivateMatches.get(matchCode);

        if (session != null && session.isHost(userId) && session.getState() == MatchState.WAITING_FOR_PLAYERS) {
            pendingPrivateMatches.remove(matchCode, session);
            session.stopMatch();
            TCPConnectionManager.send(clientIp, "MATCH 4 " + callback + " SUCCESS LEAVED");
            return;
        }

        TCPConnectionManager.send(clientIp, "MATCH 4 " + callback + " FAIL Not_found");
    }

    public static void handleUserDeparture(int userId, String clientIp) {
        handleUserDeparture(userId, clientIp, false);
    }

    public static void handleUserDeparture(int userId, String clientIp, boolean immediate) {
        if (userId < 0) {
            return;
        }

        cancelPendingDeparture(userId);
        queue.remove(userId);

        pendingPrivateMatches.entrySet().removeIf(entry -> {
            MatchSession session = entry.getValue();
            if (session != null && session.isHost(userId)) {
                session.abortMatch("Player_left");
                return true;
            }
            if (session != null) {
                session.removeSpectator(userId);
            }
            return false;
        });

        activeMatches.entrySet().removeIf(entry -> {
            MatchSession session = entry.getValue();
            if (session != null && session.containsUser(userId)) {
                if (immediate) {
                    session.forfeit(userId, "Player_left");
                    return true;
                }
                scheduleForfeit(session, userId);
                return false;
            }
            if (session != null) {
                session.removeSpectator(userId);
            }
            return false;
        });
    }

    public static void leaveActiveMatch(int userId, String clientIp, int callback) {
        cancelPendingDeparture(userId);
        final boolean[] removedSpectator = new boolean[]{false};
        boolean handled = activeMatches.entrySet().removeIf(entry -> {
            MatchSession session = entry.getValue();
            if (session == null) {
                return false;
            }
            if (session.containsUser(userId)) {
                session.forfeit(userId, "Player_left");
                return true;
            }
            if (session.getSpectators().contains(userId)) {
                session.removeSpectator(userId);
                removedSpectator[0] = true;
                return false;
            }
            return false;
        });

        if (handled || removedSpectator[0]) {
            TCPConnectionManager.send(clientIp, "MATCH 6 " + callback + " SUCCESS LEAVED");
        } else {
            TCPConnectionManager.send(clientIp, "MATCH 6 " + callback + " FAIL Not_in_active_match");
        }
    }

    public static MatchSession getMatch(String matchCode) {
        MatchSession active = activeMatches.get(matchCode);
        if (active != null) {
            return active;
        }
        return pendingPrivateMatches.get(matchCode);
    }

    public static Collection<MatchSession> getAllActiveMatches() {
        return activeMatches.values();
    }

    public static void endMatch(String matchCode) {
        MatchSession session = activeMatches.remove(matchCode);
        if (session != null) {
            session.stopMatch();
        }
        pendingDisconnects.entrySet().removeIf(entry -> matchCode.equals(entry.getValue().matchCode));
    }

    public static void storeMatchResult(String matchCode, Integer winnerId, Integer loserId, String reason,
                                        long startTimeMillis, long endTimeMillis) {
        if (matchCode == null) {
            return;
        }
        finishedMatches.put(matchCode, new MatchResult(matchCode, winnerId, loserId, reason,
                startTimeMillis, endTimeMillis));
    }

    public static String buildMatchResultPayload(String matchCode, Integer userId) {
        MatchResult result = finishedMatches.get(matchCode);
        if (result == null || userId == null) {
            return "EMPTY";
        }

        String outcome = userId.equals(result.winnerId) ? "WIN" : "LOSE";
        return matchCode + "|" + outcome + "|" + safeValue(result.reason)
                + "|" + result.startTimeMillis + "|" + result.endTimeMillis;
    }

    public static void notifyMatchResult(String matchCode, Integer winnerId, Integer loserId, String reason) {
        long endTimeMillis = System.currentTimeMillis();

        MatchSession session = getMatch(matchCode);
        long startTimeMillis = session != null ? session.getStartTimeMillis() : endTimeMillis;
        int durationSeconds = (int) ((endTimeMillis - startTimeMillis) / 1000);

        if (session != null && winnerId != null && loserId != null
                && session.getPlayer1() != null && session.getPlayer2() != null) {
            try {
                DatabaseManager.saveMatchResult(session.getPlayer1(), session.getPlayer2(),
                        durationSeconds, 0, 0, winnerId);
            } catch (DBException e) {
                System.err.println("[MatchManager] Could not save match result: " + e.getMessage());
            }
        }

        storeMatchResult(matchCode, winnerId, loserId, reason, startTimeMillis, endTimeMillis);
        sendMatchResultToUser(winnerId, matchCode);
        sendMatchResultToUser(loserId, matchCode);
    }

    private static void sendMatchResultToUser(Integer userId, String matchCode) {
        if (userId == null) {
            return;
        }
        String ip = SessionManager.getIpByUserId(userId);
        if (ip != null) {
            String payload = buildMatchResultPayload(matchCode, userId);
            if (!"EMPTY".equals(payload)) {
                TCPConnectionManager.send(ip, "MATCHRESULT 0 0 SUCCESS " + payload);
            }
        }
    }

    public static String buildMatchListPayload(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<String> entries = new ArrayList<>();

        for (MatchSession session : activeMatches.values()) {
            if (session == null) {
                continue;
            }

            String code = session.getMatchCode();
            String player1 = resolveUsername(session.getPlayer1());
            String player2 = resolveUsername(session.getPlayer2());
            String player1Pfp = resolvePfp(session.getPlayer1());
            String player2Pfp = resolvePfp(session.getPlayer2());
            String spectators = String.valueOf(session.getSpectators().size());
            String state = session.getState().name();

            if (!normalizedQuery.isEmpty()) {
                boolean matchesCode = code != null && code.toLowerCase().contains(normalizedQuery);
                boolean matchesPlayers = (player1 != null && player1.toLowerCase().contains(normalizedQuery))
                    || (player2 != null && player2.toLowerCase().contains(normalizedQuery));
                if (!matchesCode && !matchesPlayers) {
                    continue;
                }
            }

            entries.add(code + "|" + safeValue(player1) + "|" + safeValue(player1Pfp) + "|" + safeValue(player2) + "|" + safeValue(player2Pfp) + "|" + state + "|" + spectators + "|" + safeInt(session.getPlayer1()) + "|" + safeInt(session.getPlayer2()) + "|" + session.getStartTimeMillis());
        }

        return entries.size() + (entries.isEmpty() ? "" : " " + String.join("|||", entries));
    }

    public static String buildMatchInfoPayload(String matchCode) {
        MatchSession session = getMatch(matchCode);
        if (session == null) {
            return "EMPTY";
        }

        String code = session.getMatchCode();
        String player1 = resolveUsername(session.getPlayer1());
        String player2 = resolveUsername(session.getPlayer2());
        String player1Pfp = resolvePfp(session.getPlayer1());
        String player2Pfp = resolvePfp(session.getPlayer2());
        String state = session.getState().name();
        String spectators = String.valueOf(session.getSpectators().size());

        return code + "|" + safeValue(player1) + "|" + safeValue(player1Pfp) + "|" + safeValue(player2) + "|" + safeValue(player2Pfp) + "|" + state + "|" + spectators + "|" + safeInt(session.getPlayer1()) + "|" + safeInt(session.getPlayer2()) + "|" + session.getStartTimeMillis();
    }

    private static String resolveUsername(Integer userId) {
        if (userId == null) {
            return "";
        }

        try {
            String username = DatabaseManager.getUsernameById(userId);
            return username == null ? "" : username;
        } catch (Exception e) {
            return "";
        }
    }

    private static String safeValue(String value) {
        return value == null ? "" : value;
    }

    private static String safeInt(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static boolean isUserInActiveMatch(Integer userId) {
        if (userId == null) {
            return false;
        }
        for (MatchSession session : activeMatches.values()) {
            if (session != null && session.containsUser(userId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPendingDisconnectForMatch(String matchCode) {
        if (matchCode == null) {
            return false;
        }
        for (PendingDisconnect pending : pendingDisconnects.values()) {
            if (pending != null && matchCode.equals(pending.matchCode)) {
                return true;
            }
        }
        return false;
    }

    public static void cancelPendingDeparture(Integer userId) {
        if (userId == null) {
            return;
        }
        PendingDisconnect pending = pendingDisconnects.remove(userId);
        if (pending != null && pending.future != null) {
            pending.future.cancel(false);
        }
    }

    public static void handleUserReconnected(Integer userId) {
        cancelPendingDeparture(userId);
    }

    private static void scheduleForfeit(MatchSession session, int userId) {
        if (session == null) {
            return;
        }

        PendingDisconnect existing = pendingDisconnects.remove(userId);
        if (existing != null && existing.future != null) {
            existing.future.cancel(false);
        }

        PendingDisconnect pending = new PendingDisconnect(userId, session.getMatchCode());
        ScheduledFuture<?> future = disconnectScheduler.schedule(() -> {
            PendingDisconnect current = pendingDisconnects.get(userId);
            if (current == null || current != pending) {
                return;
            }

            if (SessionManager.isUserOnline(userId)) {
                cancelPendingDeparture(userId);
                return;
            }

            MatchSession active = activeMatches.get(session.getMatchCode());
            if (active != null && active.containsUser(userId)) {
                active.forfeit(userId, "Player_timeout");
                activeMatches.remove(active.getMatchCode(), active);
            }

            cancelPendingDeparture(userId);
        }, DISCONNECT_GRACE_SECONDS, TimeUnit.SECONDS);

        pending.future = future;
        pendingDisconnects.put(userId, pending);
    }

    private static String resolvePfp(Integer userId) {
        if (userId == null) {
            return "";
        }

        try {
            byte[] pfpBytes = DatabaseManager.getUserPfp(userId);
            if (pfpBytes == null || pfpBytes.length == 0) {
                return "";
            }
            return Base64.getEncoder().encodeToString(pfpBytes);
        } catch (Exception e) {
            return "";
        }
    }
}