package matches;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

final class TetrisGame {
    private static final int WIDTH = 10;
    private static final int HEIGHT = 20;
    private static final int ROUND_COUNTDOWN_SECONDS = 5;
    private static final int MAX_ROUNDS = 5;
    private static final int WIN_ROUNDS = 3;
    private static final long GRAVITY_NANOS = 650_000_000L;

    private static final boolean[][][] BASE_SHAPES = new boolean[][][] {
        {
            {false, false, false, false},
            {true,  true,  true,  true },
            {false, false, false, false},
            {false, false, false, false}
        },
        {
            {false, true,  true,  false},
            {false, true,  true,  false},
            {false, false, false, false},
            {false, false, false, false}
        },
        {
            {false, true,  false, false},
            {true,  true,  true,  false},
            {false, false, false, false},
            {false, false, false, false}
        },
        {
            {false, true,  true,  false},
            {true,  true,  false, false},
            {false, false, false, false},
            {false, false, false, false}
        },
        {
            {true,  true,  false, false},
            {false, true,  true,  false},
            {false, false, false, false},
            {false, false, false, false}
        },
        {
            {true,  false, false, false},
            {true,  true,  true,  false},
            {false, false, false, false},
            {false, false, false, false}
        },
        {
            {false, false, true,  false},
            {true,  true,  true,  false},
            {false, false, false, false},
            {false, false, false, false}
        }
    };

    private final MatchSession session;
    private final PlayerState player1;
    private final PlayerState player2;

    private int currentRound = 0;
    private long roundCountdownStart = 0L;
    private long lastGravityTick = 0L;
    private boolean roundCountdownAnnounced = false;
    private boolean inRound = false;

    TetrisGame(MatchSession session) {
        this.session = session;
        this.player1 = new PlayerState();
        this.player2 = new PlayerState();
    }

    synchronized void startFirstRound() {
        currentRound = 1;
        startRoundCountdown();
    }

    synchronized void stop() {
        inRound = false;
    }

    synchronized void tick() {
        if (session.getState() == MatchState.STARTING) {
            tickCountdown();
            return;
        }

        if (session.getState() != MatchState.IN_PROGRESS) {
            return;
        }

        long now = System.nanoTime();
        if (lastGravityTick == 0L) {
            lastGravityTick = now;
        }

        if (now - lastGravityTick >= GRAVITY_NANOS) {
            lastGravityTick = now;
            stepGravity(player1, player2, session.getPlayer1(), session.getPlayer2());
            stepGravity(player2, player1, session.getPlayer2(), session.getPlayer1());
            broadcastState("IN_PROGRESS", buildProgressPayload());
        }
    }

    synchronized void handleInput(Integer userId, String action) {
        if (session.getState() != MatchState.IN_PROGRESS || userId == null) {
            return;
        }

        PlayerState state = resolveState(userId);
        if (state == null || !state.alive) {
            return;
        }

        String normalized = action == null ? "" : action.trim().toUpperCase();
        boolean changed = false;

        switch (normalized) {
            case "LEFT":
                changed = tryMove(state, -1, 0, state.rotation);
                break;
            case "RIGHT":
                changed = tryMove(state, 1, 0, state.rotation);
                break;
            case "DOWN":
                changed = softDrop(state);
                break;
            case "ROTATE":
                changed = tryRotate(state);
                break;
            case "DROP":
                changed = hardDrop(state);
                break;
            default:
                return;
        }

        if (changed) {
            broadcastState("IN_PROGRESS", buildProgressPayload());
        }
    }

    private void tickCountdown() {
        if (!roundCountdownAnnounced) {
            roundCountdownAnnounced = true;
            session.broadcastToParticipants("MATCH_STATE 0 0 " + session.getMatchCode() + " ROUND_START " + currentRound + "|" + ROUND_COUNTDOWN_SECONDS);
        }

        long elapsedSeconds = (System.nanoTime() - roundCountdownStart) / 1_000_000_000L;
        int remaining = ROUND_COUNTDOWN_SECONDS - (int) elapsedSeconds;
        if (remaining > 0) {
            session.broadcastToParticipants("MATCH_COUNTDOWN 0 0 " + session.getMatchCode() + " " + remaining);
            session.broadcastToParticipants("MATCH_STATE 0 0 " + session.getMatchCode() + " ROUND_START " + currentRound + "|" + remaining);
            return;
        }

        session.broadcastToParticipants("MATCH_COUNTDOWN 0 0 " + session.getMatchCode() + " 0");
        startRound();
    }

    private void startRound() {
        resetPlayers();
        inRound = true;
        lastGravityTick = System.nanoTime();
        session.setState(MatchState.IN_PROGRESS);
        spawnNext(player1);
        spawnNext(player2);
        broadcastState("IN_PROGRESS", buildProgressPayload());
    }

    private void startRoundCountdown() {
        inRound = false;
        roundCountdownStart = System.nanoTime();
        roundCountdownAnnounced = false;
        session.setState(MatchState.STARTING);
    }

    private void resetPlayers() {
        player1.reset();
        player2.reset();
    }

    private void stepGravity(PlayerState self, PlayerState opponent, Integer selfUserId, Integer opponentUserId) {
        if (session.getState() != MatchState.IN_PROGRESS) {
            return;
        }
        if (!self.alive || self.currentPiece == -1) {
            return;
        }

        applyPendingGarbage(self);

        if (tryMove(self, 0, 1, self.rotation)) {
            return;
        }

        lockPiece(self);
        int cleared = clearLines(self);
        if (cleared > 0) {
            opponent.garbagePending += Math.max(0, cleared - 1);
        }

        if (!spawnNext(self)) {
            self.alive = false;
            endRound(opponentUserId, selfUserId, "Topout");
        }
    }

    private boolean softDrop(PlayerState state) {
        if (tryMove(state, 0, 1, state.rotation)) {
            return true;
        }

        lockPiece(state);
        int cleared = clearLines(state);
        PlayerState opponent = state == player1 ? player2 : player1;
        if (cleared > 0) {
            opponent.garbagePending += Math.max(0, cleared - 1);
        }

        if (!spawnNext(state)) {
            state.alive = false;
            endRound(state == player1 ? session.getPlayer2() : session.getPlayer1(), state == player1 ? session.getPlayer1() : session.getPlayer2(), "Topout");
        }

        return true;
    }

    private boolean hardDrop(PlayerState state) {
        while (tryMove(state, 0, 1, state.rotation)) {
        }
        lockPiece(state);
        int cleared = clearLines(state);
        PlayerState opponent = state == player1 ? player2 : player1;
        if (cleared > 0) {
            opponent.garbagePending += Math.max(0, cleared - 1);
        }

        if (!spawnNext(state)) {
            state.alive = false;
            endRound(state == player1 ? session.getPlayer2() : session.getPlayer1(), state == player1 ? session.getPlayer1() : session.getPlayer2(), "Topout");
        }

        return true;
    }

    private boolean tryRotate(PlayerState state) {
        if (state.currentPiece == 1) return false;
        int nextRotation = (state.rotation + 1) % 4;
        int[] kicks = new int[] {0, -1, 1, -2, 2};
        for (int kick : kicks) {
            if (canPlace(state, state.x + kick, state.y, nextRotation)) {
                state.x += kick;
                state.rotation = nextRotation;
                return true;
            }
        }
        return false;
    }

    private boolean tryMove(PlayerState state, int dx, int dy, int rotation) {
        int nextX = state.x + dx;
        int nextY = state.y + dy;
        if (!canPlace(state, nextX, nextY, rotation)) {
            return false;
        }

        state.x = nextX;
        state.y = nextY;
        return true;
    }

    private boolean spawnNext(PlayerState state) {
        if (state.garbagePending > 0) {
            applyPendingGarbage(state);
        }

        state.currentPiece = state.nextPiece;
        state.nextPiece = randomPiece();
        state.rotation = 0;
        state.x = 3;
        state.y = 0;

        if (!canPlace(state, state.x, state.y, state.rotation)) {
            return false;
        }

        return true;
    }

    private void lockPiece(PlayerState state) {
        for (int[] cell : getCells(state.currentPiece, state.rotation)) {
            int boardX = state.x + cell[0];
            int boardY = state.y + cell[1];
            if (boardY >= 0 && boardY < HEIGHT && boardX >= 0 && boardX < WIDTH) {
                state.board[boardY][boardX] = state.currentPiece + 1;
            }
        }
    }

    private int clearLines(PlayerState state) {
        int cleared = 0;
        for (int row = HEIGHT - 1; row >= 0; row--) {
            boolean full = true;
            for (int col = 0; col < WIDTH; col++) {
                if (state.board[row][col] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                cleared++;
                for (int pull = row; pull > 0; pull--) {
                    state.board[pull] = Arrays.copyOf(state.board[pull - 1], WIDTH);
                }
                state.board[0] = new int[WIDTH];
                row++;
            }
        }
        state.lines += cleared;
        state.score += cleared * 100;
        return cleared;
    }

    private void applyPendingGarbage(PlayerState state) {
        while (state.garbagePending > 0) {
            state.garbagePending--;
            for (int row = 0; row < HEIGHT - 1; row++) {
                state.board[row] = Arrays.copyOf(state.board[row + 1], WIDTH);
            }
            int hole = ThreadLocalRandom.current().nextInt(WIDTH);
            int[] garbage = new int[WIDTH];
            Arrays.fill(garbage, 8);
            garbage[hole] = 0;
            state.board[HEIGHT - 1] = garbage;
        }
    }

    private void endRound(Integer winnerId, Integer loserId, String reason) {
        if (winnerId == null || loserId == null || session.getState() == MatchState.FINISHED) {
            return;
        }

        PlayerState winner = winnerId.equals(session.getPlayer1()) ? player1 : player2;
        winner.wins++;
        session.broadcastToParticipants("MATCH_STATE 0 0 " + session.getMatchCode() + " ROUND_END " + currentRound + "|" + winnerId + "|" + reason + "|" + player1.wins + "|" + player2.wins);

        if (winner.wins >= WIN_ROUNDS || currentRound >= MAX_ROUNDS) {
            session.setState(MatchState.FINISHED);
            MatchManager.notifyMatchResult(session.getMatchCode(), winnerId, loserId, reason);
            MatchManager.endMatch(session.getMatchCode());
            return;
        }

        currentRound++;
        startRoundCountdown();
    }

    private PlayerState resolveState(Integer userId) {
        if (userId == null) {
            return null;
        }
        if (userId.equals(session.getPlayer1())) {
            return player1;
        }
        if (userId.equals(session.getPlayer2())) {
            return player2;
        }
        return null;
    }

    private boolean canPlace(PlayerState state, int x, int y, int rotation) {
        for (int[] cell : getCells(state.currentPiece, rotation)) {
            int boardX = x + cell[0];
            int boardY = y + cell[1];
            if (boardX < 0 || boardX >= WIDTH || boardY < 0 || boardY >= HEIGHT) {
                return false;
            }
            if (state.board[boardY][boardX] != 0) {
                return false;
            }
        }
        return true;
    }

    private int[][] getCells(int pieceType, int rotation) {
        boolean[][] matrix = BASE_SHAPES[pieceType];
        for (int i = 0; i < rotation; i++) {
            matrix = rotate(matrix);
        }

        int count = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (matrix[r][c]) {
                    count++;
                }
            }
        }

        int[][] cells = new int[count][2];
        int idx = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (matrix[r][c]) {
                    cells[idx][0] = c;
                    cells[idx][1] = r;
                    idx++;
                }
            }
        }
        return cells;
    }

    private boolean[][] rotate(boolean[][] src) {
        boolean[][] out = new boolean[4][4];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                out[c][3 - r] = src[r][c];
            }
        }
        return out;
    }

    private int randomPiece() {
        return ThreadLocalRandom.current().nextInt(BASE_SHAPES.length);
    }

    private void broadcastState(String state, String payload) {
        session.broadcastToParticipants("MATCH_STATE 0 0 " + session.getMatchCode() + " " + state + " " + payload);
    }

    private String buildProgressPayload() {
        return currentRound + "|" + player1.wins + "|" + player2.wins + "|" + player1.score + "|" + player2.score + "|"
            + buildBoardPayload(player1) + "|" + buildBoardPayload(player2) + "|"
            + pieceName(player1.nextPiece) + "|" + pieceName(player2.nextPiece) + "|"
            + player1.garbagePending + "|" + player2.garbagePending;
    }

    private String buildBoardPayload(PlayerState state) {
        char[][] view = new char[HEIGHT][WIDTH];
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                view[row][col] = state.board[row][col] == 0 ? '.' : (char) ('0' + state.board[row][col]);
            }
        }

        if (state.alive && state.currentPiece >= 0) {
            for (int[] cell : getCells(state.currentPiece, state.rotation)) {
                int x = state.x + cell[0];
                int y = state.y + cell[1];
                if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
                    view[y][x] = (char) ('0' + (state.currentPiece + 1));
                }
            }
        }

        StringBuilder sb = new StringBuilder(WIDTH * HEIGHT);
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                sb.append(view[row][col]);
            }
        }
        return sb.toString();
    }

    private String pieceName(int piece) {
        if (piece < 0 || piece >= BASE_SHAPES.length) {
            return "0";
        }
        return String.valueOf(piece + 1);
    }

    private static final class PlayerState {
        final int[][] board = new int[HEIGHT][WIDTH];
        int currentPiece = -1;
        int nextPiece = ThreadLocalRandom.current().nextInt(BASE_SHAPES.length);
        int rotation = 0;
        int x = 3;
        int y = 0;
        int score = 0;
        int lines = 0;
        int garbagePending = 0;
        int wins = 0;
        boolean alive = true;

        void reset() {
            for (int row = 0; row < HEIGHT; row++) {
                Arrays.fill(board[row], 0);
            }
            currentPiece = -1;
            rotation = 0;
            x = 3;
            y = 0;
            score = 0;
            lines = 0;
            garbagePending = 0;
            alive = true;
        }
    }
}
