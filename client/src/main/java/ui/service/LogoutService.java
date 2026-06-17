package ui.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import config.UserSession;
import network.NetworkCallback;
import network.NetworkContext;
import network.NetworkManager;

public class LogoutService {

    public static void logout() {
        long TIMEOUT_MS = 5000;
        int callbackCode = NetworkContext.requestCallbackID.incrementAndGet();
        String send = "LOGOUT 0 " + callbackCode + " " + UserSession.getToken();

        CountDownLatch trava = new CountDownLatch(1);

        NetworkCallback c = new NetworkCallback(callbackCode) {
            @Override
            public void onSuccess(String s) {
                LogoutService.logoutProcedure(trava);
            }

            @Override
            public void onFailure(String s) {
                LogoutService.logoutProcedure(trava);
            }
        };
        NetworkManager.sendTCP(send, c);

        try {
            boolean respondeuEmTempo = trava.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!respondeuEmTempo) {
                LogoutService.logoutProcedure(trava);
            }
        } catch (InterruptedException e) {
            LogoutService.logoutProcedure(trava);
        }

        return;
    }

    public static void logoutProcedure(CountDownLatch trava){
        UserSession.limparSessao();
        trava.countDown();
    }
}
