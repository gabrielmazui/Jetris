package ui.service;

import network.NetworkManager;
import network.NetworkCallback;
import network.NetworkContext;
import config.UserSession;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LoginService implements Service {

    public static String login(String username, String password) {
        long TIMEOUT_MS = 5000;
        int callbackCode = NetworkContext.requestCallbackID.incrementAndGet();
        String send = "LOGIN 1 " + callbackCode + " " + username + " " + password;

        CountDownLatch trava = new CountDownLatch(1);
        AtomicReference<String> resultadoLogin = new AtomicReference<>("Timeout: Server did not answer in time");

        NetworkCallback c = new NetworkCallback(callbackCode) {
            @Override
            public void onSuccess(String resposta) {
                String[] dados = resposta.split(" ", 2);
                String token = dados[0];
                String pfpBase64 = dados.length > 1 ? dados[1] : "";

                UserSession.iniciarESalvarSessao(token, username);

                if (!pfpBase64.isEmpty()) {
                    UserSession.setPfp(
                        Base64.getDecoder().decode(pfpBase64)
                    );
                }

                UserSession.logged = true;

                resultadoLogin.set("SUCCESS");
                trava.countDown();
            }

            @Override
            public void onFailure(String mensagemErro) {
                resultadoLogin.set(mensagemErro);
                trava.countDown();
            }
        };
        NetworkManager.sendTCP(send, c);

        try {
            boolean respondeuEmTempo = trava.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!respondeuEmTempo) {
                NetworkContext.mapCallbacks.remove(callbackCode);
                return "Login failed. Please try again";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Internal error";
        }

        return resultadoLogin.get();
    }

    public static String register(String username, String password) {
        long TIMEOUT_MS = 5000;
        int callbackCode = NetworkContext.requestCallbackID.incrementAndGet();
        String send = "REGISTER 0 " + callbackCode + " " + username + " " + password;

        CountDownLatch trava = new CountDownLatch(1);
        AtomicReference<String> resRegister = new AtomicReference<>("Timeout: Server did not answer in time");

        NetworkCallback c = new NetworkCallback(callbackCode) {
            @Override
            public void onSuccess(String resposta) {
                resRegister.set("SUCCESS");
                trava.countDown();
            }

            @Override
            public void onFailure(String mensagemErro) {
                resRegister.set(mensagemErro);
                trava.countDown();
            }
        };
        NetworkManager.sendTCP(send, c);

        try {
            boolean respondeuEmTempo = trava.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!respondeuEmTempo) {
                NetworkContext.mapCallbacks.remove(callbackCode);
                return "Register failed. Please try again";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Internal error";
        }

        return resRegister.get();
    }

    public static Boolean verifyTokenCache() {
        long TIMEOUT_MS = 5000;
        String tok = UserSession.getToken();
        if (tok == null || tok.isEmpty()) {
            return false;
        }

        int callbackCode = NetworkContext.requestCallbackID.incrementAndGet();
        String toSend = "LOGIN 0 " + callbackCode + " " + tok;

        AtomicBoolean logged = new AtomicBoolean(false);
        CountDownLatch trava = new CountDownLatch(1);

        NetworkManager.sendTCP(toSend, new NetworkCallback(callbackCode) {
            @Override
            public void onSuccess(String resposta) {
                String[] dados = resposta.split(" ", 2);
                String pfpBase64 = dados.length > 1 ? dados[1] : dados[0];

                if (!pfpBase64.isEmpty()) {
                    try {
                        UserSession.setPfp(Base64.getDecoder().decode(pfpBase64));
                    } catch (IllegalArgumentException ignored) {}
                }

                logged.set(true);
                UserSession.logged = true;
                trava.countDown();
            }

            @Override
            public void onFailure(String erro) {
                logged.set(false);
                trava.countDown();
            }
        });

        System.out.println("Trying to login with auth token");
        try {
            trava.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (logged.get()) {
            System.out.println("Connected by auth token");
        } else {
            System.out.println("Could not login with auth token");
        }

        return logged.get();
    }
}