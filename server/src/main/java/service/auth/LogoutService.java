package service.auth;

import network.SessionManager;
import network.connection.TCPConnectionManager;
import network.packets.LogoutPack;

public class LogoutService {
    public static void handle(LogoutPack logout, String clientIp){
        // 1. Pegamos o token enviado pelo cliente
        String token = logout.getToken();
        
        // 2. Descobrimos o ID do usuário na memória RAM através do token
        Integer userId = SessionManager.getUserIdByToken(token);

        if (userId != null) {
            // 3. Se o usuário existir, limpamos todas as sessões e tokens dele
            SessionManager.logout(userId);
            
            // 4. Envia o sucesso para o IP do cliente (Garantindo a ordem: IP primeiro, depois a mensagem)
            TCPConnectionManager.send(clientIp, "LOGOUT 0 " + logout.getCallbackCode() + " SUCCESS");
        } else {
            // Se o token já era inválido (ou o cara já caiu), avisa o cliente para ele poder voltar pra tela de login
            TCPConnectionManager.send(clientIp, "LOGOUT 0 " + logout.getCallbackCode() + " FAIL Already_logged_out");
        }
    }
}
