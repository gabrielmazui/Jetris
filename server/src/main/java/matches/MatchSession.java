package matches;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import network.SessionManager;
import network.connection.TCPConnectionManager;

public class MatchSession {

    public static final int COUNTDOWN_SECONDS = 5;

    private final String matchCode;
    private Integer player1;
    private Integer player2;
    private volatile MatchState state;
    private final Set<Integer> spectators;
    private final long startTimeMillis = System.currentTimeMillis();

    private long countdownStartNanos;
    private final AtomicInteger lastBroadcastSecond = new AtomicInteger(-1);
    private final TetrisGame game;

    public MatchSession(String matchCode, Integer player1) {
        this.matchCode = matchCode;
        this.player1 = player1;
        this.state = MatchState.WAITING_FOR_PLAYERS;
        this.spectators = ConcurrentHashMap.newKeySet();
        this.game = new TetrisGame(this);
    }


    public void startMatch() {
        this.game.startFirstRound();
    }

    public void stopMatch() {
        this.state = MatchState.FINISHED;
        this.game.stop();
    }

    public void abortMatch(String reason) {
        broadcastAbort(reason);
        this.state = MatchState.FINISHED;
        this.game.stop();
    }

    public void forfeit(Integer loserUserId, String reason) {
        Integer winnerId = getOpponentId(loserUserId);
        if (winnerId != null && player1 != null && player2 != null) {
            MatchManager.notifyMatchResult(matchCode, winnerId, loserUserId, reason);
        }

        broadcastAbort(reason);
        this.state = MatchState.FINISHED;
        this.game.stop();
    }

    public void tick() {
        game.tick();
    }

    public void handleInput(Integer userId, String action) {
        game.handleInput(userId, action);
    }

    private void broadcastAbort(String reason) {
        String packet = "MATCH_ABORT 0 0 " + matchCode + " " + reason;
        sendToPlayer(player1, packet);
        sendToPlayer(player2, packet);
    }

    private void sendToPlayer(Integer userId, String packet) {
        if (userId == null) return;
        String ip = SessionManager.getIpByUserId(userId);
        if (ip != null) {
            TCPConnectionManager.send(ip, packet);
        }
    }

    private void tickGame() {
        processInputs();
        updatePhysicsAndState();
        checkWinCondition();
        broadcastGameState();
    }

    private void initGameLogic() {

    }

    private void processInputs() {

    }

    private void updatePhysicsAndState() {

    }

    private void checkWinCondition() {

    }

    private void broadcastGameState() {
        String payload = "MATCH_STATE 0 0 " + matchCode + " " + state.name() + " " + spectators.size();
        sendToPlayer(player1, payload);
        sendToPlayer(player2, payload);
        for (Integer specId : spectators) {
            sendToPlayer(specId, payload);
        }
    }

    public long getStartTimeMillis() { return startTimeMillis; }
    public String getMatchCode() { return matchCode; }
    public Integer getPlayer1() { return player1; }
    public void setPlayer1(Integer player1) { this.player1 = player1; }
    public Integer getPlayer2() { return player2; }
    public void setPlayer2(Integer player2) { this.player2 = player2; }
    public MatchState getState() { return state; }
    public void setState(MatchState state) { this.state = state; }
    public Set<Integer> getSpectators() { return spectators; }
    public void addSpectator(Integer userId) { this.spectators.add(userId); }
    public void removeSpectator(Integer userId) { this.spectators.remove(userId); }

    void broadcastToParticipants(String packet) {
        sendToPlayer(player1, packet);
        sendToPlayer(player2, packet);
        for (Integer specId : spectators) {
            sendToPlayer(specId, packet);
        }
    }

    public boolean containsUser(Integer userId) {
        return userId != null && (userId.equals(player1) || userId.equals(player2));
    }

    public boolean isHost(Integer userId) {
        return userId != null && userId.equals(player1);
    }

    public Integer getOpponentId(Integer userId) {
        if (userId == null) {
            return null;
        }

        if (userId.equals(player1)) {
            return player2;
        }

        if (userId.equals(player2)) {
            return player1;
        }

        return null;
    }

    public boolean isActive() {
        return state == MatchState.STARTING || state == MatchState.IN_PROGRESS;
    }
}