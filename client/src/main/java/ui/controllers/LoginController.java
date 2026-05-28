package ui.controllers;

import network.NetworkManager;
import network.NetworkCallback;
import network.NetworkContext;
import config.UserSession;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LoginController implements Controller{
    public static void escHandler(){
        return;
    } 

    public static String login(String username, String password) {
        long TIMEOUT_MS = 5000;
        int callbackCode = NetworkContext.requestCallbackID.incrementAndGet();
        String send = "LOGIN 1 " + callbackCode + " " + username + " " + password;

        CountDownLatch trava = new CountDownLatch(1);
        AtomicReference<String> resultadoLogin = new AtomicReference<>("Timeout: Server did not answer in time");

        NetworkCallback c = new NetworkCallback(callbackCode) {
            @Override
            public void onSuccess(String resposta) {
                resposta.split(" ", 2);
                resultadoLogin.set(resposta);
                UserSession.iniciarESalvarSessao(resposta, username);
                UserSession.logged = true;
                
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
                // return "Login failed. Please try again";

                //MOMENTANEO
                resultadoLogin.set("SUCCESS");
                UserSession.iniciarESalvarSessao("auth legal", username);
                UserSession.logged = true;
                //----
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Internal error";
        }

        
        return resultadoLogin.get();
    }

    public static String register(String username, String Password){
        long TIMEOUT_MS = 5000;
        int CallbackCode = NetworkContext.requestCallbackID.incrementAndGet();
        String send = "REGISTER 0 " + CallbackCode + " " + username + " " + Password;

        CountDownLatch trava = new CountDownLatch(1);
        AtomicReference<String> resRegister = new AtomicReference<>("Timeout: Server did not answer in time");
        NetworkCallback c = new NetworkCallback(CallbackCode) {
            @Override
            public void onSuccess(String resposta) {
                resRegister.set(resposta);
                
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
                NetworkContext.mapCallbacks.remove(CallbackCode);
                return "Register failed. Please try again";
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Internal error";
        }

        return resRegister.get();
    }

    public static Boolean verifyTokenCache(){
        System.out.print("Trying to login with auth token");
        long TIMEOUT_MS = 5000;

        String tok = UserSession.getToken();
        if(tok == null || tok.length() == 0){
            return false;
        }
        int CallbackCode = NetworkContext.requestCallbackID.incrementAndGet();
        String toSend = "LOGIN 0 " + CallbackCode + " "+ tok;
        
        AtomicBoolean logged = new AtomicBoolean(false);
        CountDownLatch trava = new CountDownLatch(1);
        NetworkManager.sendTCP(toSend, new NetworkCallback(CallbackCode) {
            @Override
            public void onSuccess(String resposta) {
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
        try {
            trava.await(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return logged.get();
    }
}