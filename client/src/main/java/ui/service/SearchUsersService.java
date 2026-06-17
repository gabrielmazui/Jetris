package ui.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import config.UserSession;
import network.NetworkCallback;
import network.NetworkContext;
import network.NetworkManager;

public class SearchUsersService implements Service {
    public static List<String[]> searchUsernames(String query) {
        long TIMEOUT_MS = 5000;
        int callbackCode = NetworkContext.requestCallbackID.incrementAndGet();
        
        String send = "GETPROFILE 0 " + callbackCode + " " + UserSession.getToken() + " " + query;

        CountDownLatch trava = new CountDownLatch(1);
        AtomicReference<List<String[]>> resultados = new AtomicReference<>(new ArrayList<>());

        NetworkCallback c = new NetworkCallback(callbackCode) {
            @Override
            public void onSuccess(String resposta) {
               
                List<String[]> lista = new ArrayList<>();
                String limpa = resposta.trim();
                
                if (!limpa.equals("EMPTY")) {
                    String[] tokens = limpa.split(" ");
                    if (tokens.length > 1) {
                        try {
                            int quant = Integer.parseInt(tokens[0]);
                            
                            for (int i = 1; lista.size() < quant && (i + 1) < tokens.length; i += 2) {
                                String username = tokens[i];
                                String pfp = tokens[i + 1];

                                if ("NULL".equals(pfp)) {
                                    pfp = "";
                                }

                                lista.add(new String[]{username, pfp});
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Erro ao converter quantidade de usuarios.");
                        }
                    }
                }
                
                resultados.set(lista);
                trava.countDown();
            }

            @Override
            public void onFailure(String mensagemErro) {
                trava.countDown();
            }
        };
        
        NetworkManager.sendTCP(send, c);

        try {
            boolean respondeuEmTempo = trava.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!respondeuEmTempo) {
                NetworkContext.mapCallbacks.remove(callbackCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return resultados.get();
    } 
}