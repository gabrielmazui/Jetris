package ui.service;

import config.UserSession;
import network.NetworkCallback;
import network.NetworkContext;
import network.NetworkManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class MatchMakingService {

    public interface MatchResultCallback {
        void onSuccess(String action, String matchCode);
        void onFailure(String reason);
    }


    public interface CountdownListener {
        void onTick(String matchCode, int secondsLeft);
        void onMatchStarted(String matchCode);
        default void onMatchCancelled(String matchCode, String reason) {}
    }

    private static final long MATCH_REQUEST_TIMEOUT_MS = 8000L;
    private static final ScheduledExecutorService timeoutScheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "matchmaking-timeouts");
            t.setDaemon(true);
            return t;
        });

    private MatchMakingService() {}

    private static void sendMatchRequest(String message, final int callbackId, final MatchResultCallback uiCallback, final java.util.function.Consumer<String> onSuccess) {
        final AtomicBoolean completed = new AtomicBoolean(false);

        NetworkCallback netCallback = new NetworkCallback(callbackId) {
            @Override
            public void onSuccess(String resposta) {
                if (!completed.compareAndSet(false, true)) {
                    return;
                }
                onSuccess.accept(resposta);
            }

            @Override
            public void onFailure(String erro) {
                if (!completed.compareAndSet(false, true)) {
                    return;
                }
                uiCallback.onFailure(erro);
            }
        };

        NetworkManager.sendTCP(message, netCallback);

        timeoutScheduler.schedule(() -> {
            if (completed.compareAndSet(false, true)) {
                NetworkContext.mapCallbacks.remove(callbackId);
                uiCallback.onFailure("Request_timeout");
            }
        }, MATCH_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    public static void findMatch(MatchResultCallback uiCallback) {
        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        sendMatchRequest(
            "MATCH 0 " + callbackId + " " + UserSession.getToken(),
            callbackId,
            uiCallback,
            resposta -> {
                String[] parts = resposta.split(" ", 2);
                String action = parts[0];
                String matchCode = parts.length > 1 ? parts[1] : "";
                uiCallback.onSuccess(action, matchCode);
            }
        );
    }

    public static void cancelQueue(MatchResultCallback uiCallback) {
        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        sendMatchRequest(
            "MATCH 1 " + callbackId + " " + UserSession.getToken(),
            callbackId,
            uiCallback,
            resposta -> uiCallback.onSuccess("LEAVED", "")
        );
    }

    public static void createPrivateMatch(MatchResultCallback uiCallback) {
        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        sendMatchRequest(
            "MATCH 2 " + callbackId + " " + UserSession.getToken(),
            callbackId,
            uiCallback,
            resposta -> {
                String[] parts = resposta.split(" ", 2);
                String matchCode = parts.length > 1 ? parts[1] : "";
                uiCallback.onSuccess("CREATED", matchCode);
            }
        );
    }

    public static void cancelPrivateMatch(String matchCode, MatchResultCallback uiCallback) {
        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();

        if (matchCode == null || matchCode.trim().isEmpty()) {
            uiCallback.onSuccess("LEAVED", "");
            return;
        }
        sendMatchRequest(
            "MATCH 4 " + callbackId + " " + UserSession.getToken() + " " + matchCode,
            callbackId,
            uiCallback,
            resposta -> uiCallback.onSuccess("LEAVED", "")
        );
    }

    public static void joinPrivateMatch(String matchCode, MatchResultCallback uiCallback) {
        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        sendMatchRequest(
            "MATCH 3 " + callbackId + " " + UserSession.getToken() + " " + matchCode,
            callbackId,
            uiCallback,
            resposta -> {
                String[] parts = resposta.split(" ", 2);
                String code = parts.length > 1 ? parts[1] : matchCode;
                uiCallback.onSuccess("START", code);
            }
        );
    }

    public static void leaveCurrentMatch(MatchResultCallback uiCallback) {
        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        sendMatchRequest(
            "MATCH 6 " + callbackId + " " + UserSession.getToken(),
            callbackId,
            uiCallback,
            resposta -> uiCallback.onSuccess("LEAVED", "")
        );
    }

    
    public static void listenForCountdown(CountdownListener uiListener) {
        NetworkContext.matchEventListener = new NetworkContext.MatchEventListener() {
            @Override
            public void onCountdown(String matchCode, int secondsLeft) {
                uiListener.onTick(matchCode, secondsLeft);
            }

            @Override
            public void onMatchStarted(String matchCode) {
                uiListener.onMatchStarted(matchCode);
            }

            @Override
            public void onMatchCancelled(String matchCode, String reason) {
                uiListener.onMatchCancelled(matchCode, reason);
            }
        };
    }

    public static void stopListeningForCountdown() {
        NetworkContext.matchEventListener = null;
    }
}