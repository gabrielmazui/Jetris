package ui.service;

import config.UserSession;
import network.NetworkCallback;
import network.NetworkContext;
import network.NetworkManager;

public class MatchResultService {
    private MatchResultService() {}

    public interface MatchResultCallback {
        void onSuccess(String matchCode, String outcome, String reason);
        void onFailure(String reason);
    }

    public static void fetch(String matchCode, MatchResultCallback callback) {
        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        String safeCode = matchCode == null ? "" : matchCode.trim();
        String send = "MATCH 8 " + callbackId + " " + UserSession.getToken() + " " + safeCode;

        NetworkManager.sendTCP(send, new NetworkCallback(callbackId) {
            @Override
            public void onSuccess(String resposta) {
                String raw = resposta == null ? "" : resposta.trim();
                if (raw.isEmpty() || raw.equals("EMPTY")) {
                    callback.onFailure("EMPTY");
                    return;
                }

                String[] fields = raw.split("\\|", 3);
                if (fields.length < 3) {
                    callback.onFailure("Invalid_result");
                    return;
                }

                callback.onSuccess(fields[0], fields[1], fields[2]);
            }

            @Override
            public void onFailure(String erro) {
                callback.onFailure(erro);
            }
        });
    }
}
