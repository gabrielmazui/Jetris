package ui.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import config.UserSession;
import network.NetworkCallback;
import network.NetworkContext;
import network.NetworkManager;

public class MatchListService {

    public static final class LiveMatch {
        public final String code;
        public final String player1;
        public final String player1Pfp;
        public final String player2;
        public final String player2Pfp;
        public final String state;
        public final int spectators;
        public final int player1Id;
        public final int player2Id;
        public final long startTimeMillis;

        public LiveMatch(String code, String player1, String player1Pfp, String player2, String player2Pfp, String state, int spectators, int player1Id, int player2Id, long startTimeMillis) {
            this.code = code;
            this.player1 = player1;
            this.player1Pfp = player1Pfp;
            this.player2 = player2;
            this.player2Pfp = player2Pfp;
            this.state = state;
            this.spectators = spectators;
            this.player1Id = player1Id;
            this.player2Id = player2Id;
            this.startTimeMillis = startTimeMillis;
        }
    }

    public interface MatchListCallback {
        void onSuccess(List<LiveMatch> matches);
        void onFailure(String reason);
    }

    private MatchListService() {}

    public static void fetchLiveMatches(String query, MatchListCallback callback) {
        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        String safeQuery = query == null ? "" : query.trim();
        String send = "MATCH 5 " + callbackId + " " + UserSession.getToken() + " " + safeQuery;

        NetworkManager.sendTCP(send, new NetworkCallback(callbackId) {
            @Override
            public void onSuccess(String resposta) {
                LiveMatch match = parseSingleMatch(resposta);
                callback.onSuccess(match == null ? Collections.emptyList() : Collections.singletonList(match));
            }

            @Override
            public void onFailure(String erro) {
                callback.onFailure(erro);
            }
        });
    }

    public static void fetchMatchInfo(String matchCode, MatchListCallback callback) {
        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        String safeCode = matchCode == null ? "" : matchCode.trim();
        String send = "MATCH 7 " + callbackId + " " + UserSession.getToken() + " " + safeCode;

        NetworkManager.sendTCP(send, new NetworkCallback(callbackId) {
            @Override
            public void onSuccess(String resposta) {
                LiveMatch match = parseSingleMatch(resposta);
                callback.onSuccess(match == null ? Collections.emptyList() : Collections.singletonList(match));
            }

            @Override
            public void onFailure(String erro) {
                callback.onFailure(erro);
            }
        });
    }

    private static List<LiveMatch> parseMatches(String resposta) {
        List<LiveMatch> matches = new ArrayList<>();
        String raw = resposta == null ? "" : resposta.trim();
        if (raw.isEmpty() || raw.equals("0")) {
            return matches;
        }

        String[] parts = raw.split(" ", 2);
        if (parts.length < 2) {
            return matches;
        }

        String entriesRaw = parts[1];
        String[] entries = entriesRaw.split("\\|\\|");
        for (String entry : entries) {
            String[] fields = entry.split("\\|", 10);
            if (fields.length < 7) {
                continue;
            }

            try {
                int player1Id = -1;
                int player2Id = -1;
                long startTimeMillis = 0L;
                if (fields.length >= 9) {
                    player1Id = Integer.parseInt(fields[7]);
                    player2Id = Integer.parseInt(fields[8]);
                }
                if (fields.length >= 10) {
                    startTimeMillis = Long.parseLong(fields[9]);
                }

                matches.add(new LiveMatch(
                    fields[0],
                    fields[1],
                    fields[2],
                    fields[3],
                    fields[4],
                    fields[5],
                    Integer.parseInt(fields[6]),
                    player1Id,
                    player2Id,
                    startTimeMillis
                ));
            } catch (NumberFormatException ignored) {
            }
        }

        return matches;
    }

    private static LiveMatch parseSingleMatch(String resposta) {
        String raw = resposta == null ? "" : resposta.trim();
        if (raw.isEmpty()) {
            return null;
        }

        String[] fields = raw.split("\\|", 10);
        if (fields.length < 7) {
            return null;
        }

        try {
            int player1Id = -1;
            int player2Id = -1;
            long startTimeMillis = 0L;
            if (fields.length >= 9) {
                player1Id = Integer.parseInt(fields[7]);
                player2Id = Integer.parseInt(fields[8]);
            }
            if (fields.length >= 10) {
                startTimeMillis = Long.parseLong(fields[9]);
            }

            return new LiveMatch(
                fields[0],
                fields[1],
                fields[2],
                fields[3],
                fields[4],
                fields[5],
                Integer.parseInt(fields[6]),
                player1Id,
                player2Id,
                startTimeMillis
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
