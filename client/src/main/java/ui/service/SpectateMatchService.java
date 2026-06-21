package ui.service;

import config.UserSession;
import network.NetworkCallback;
import network.NetworkContext;
import network.NetworkManager;

public class SpectateMatchService {

    public interface SpectateCallback {
        void onSuccess(String matchCode);
        void onFailure(String reason);
    }

    private SpectateMatchService() {}

    public static void spectate(String matchCode, SpectateCallback callback) {
        int callbackId = NetworkContext.requestCallbackID.incrementAndGet();
        String send = "SPECTATE 0 " + callbackId + " " + UserSession.getToken() + " " + matchCode;

        NetworkManager.sendTCP(send, new NetworkCallback(callbackId) {
            @Override
            public void onSuccess(String resposta) {
                String[] parts = resposta.split(" ", 2);
                String code = parts.length > 1 ? parts[1] : matchCode;
                callback.onSuccess(code);
            }

            @Override
            public void onFailure(String erro) {
                callback.onFailure(erro);
            }
        });
    }
}
