package network.parser;

import service.auth.*;
import service.matchmaking.MatchmakingService;
import service.matchmaking.SpectateService;
import network.connection.TCPConnectionManager;
import network.middleware.*;
import network.packets.*;
import service.ProfileService;
import network.SessionManager;
import matches.SpectateManager;

public class TCPPacketParser {

    private static final Middleware chain = Middleware.link(
        new AddressMiddleware(),
        new RateLimitMiddleware(200),
        new AuthMiddleware(),
        new SessionMiddleware()
    );

    public static void parse(String rawData, String clientIp) {
        if (rawData == null || rawData.isBlank()) {
            return;
        }

        String cleanData = rawData.trim();

        if (cleanData.equalsIgnoreCase("PING")) {
            TCPConnectionManager.send(clientIp, "PONG");
            return;
        }

        String[] partes = cleanData.split(" ", 4);
        
        if (partes.length < 4) {
            System.err.println("[TCP Parser] Invalid packet format from " + clientIp + ": " + rawData);
            return;
        }

        String type = partes[0].toUpperCase();
        int code;
        int callbackCode;

        try {
            code = Integer.parseInt(partes[1]);
            callbackCode = Integer.parseInt(partes[2]);
        } catch (NumberFormatException e) {
            System.err.println("[TCP Parser] Error parsing codes from " + clientIp);
            return;
        }

        String bodyRaw = partes[3].trim();

        if (!chain.check(type, code, callbackCode, bodyRaw, clientIp)) {
            return;
        }

        switch (type) {
            case "LOGIN":
                System.out.println("[TCP] Received from IP: " + clientIp);
                System.out.println("[TCP] ----> " + rawData);
                {
                    String[] credentials = bodyRaw.split(" ", 2);

                    String username = credentials[0];
                    String password = credentials.length > 1 ? credentials[1] : "";
            
                    LoginPacket loginPacket = new LoginPacket(code, callbackCode, username, password, credentials.length == 1 ? credentials[0] : "");
                    LoginService.handle(loginPacket, clientIp);
                }
                break;

            case "REGISTER":
                System.out.println("[TCP] Received from IP: " + clientIp);
                System.out.println("[TCP] ----> " + rawData);
                {
                    String[] credentials = bodyRaw.split(" ", 2);
                    String username = credentials[0];
                    String password = credentials.length > 1 ? credentials[1] : "";
                    
                    RegisterPacket registerPacket = new RegisterPacket(code, callbackCode, username, password);
                    RegisterService.handle(registerPacket, clientIp);
                }
                break;

            case "LOGOUT":
                System.out.println("[TCP] Received from IP: " + clientIp);
                System.out.println("[TCP] ----> " + rawData);
                {
                    String token = bodyRaw; 
                    LogoutPack logoutPack = new LogoutPack(callbackCode, token);
                    LogoutService.handle(logoutPack, clientIp);
                }
                break;

            case "GETPROFILE":
                {
                    String[] profileArgs = bodyRaw.split(" ", 3);
                    if (profileArgs.length < 2) {
                        System.err.println("[TCP Parser] Invalid GETPROFILE body from " + clientIp);
                        return;
                    }
                    String targetUsername = profileArgs[1];
                    int page = profileArgs.length >= 3 ? Integer.parseInt(profileArgs[2]) : 1;

                    if (code == 0) {
                        UserPacket profilePacket = new UserPacket(targetUsername, code, callbackCode, 0);
                        ProfileService.searchUsernamesHandle(profilePacket, clientIp);
                    } else if (code == 1) {
                        UserPacket profilePacket = new UserPacket(targetUsername, code, callbackCode, page);
                        ProfileService.getUserProfileHandle(profilePacket, clientIp);
                    }
                }
                break;

            case "SETPFP":
                System.out.println("[TCP] PFP Upload: " + clientIp);
                {
                    String[] pfpArgs = bodyRaw.split(" ", 3);
                    if (pfpArgs.length < 3) {
                        TCPConnectionManager.send(clientIp, "SETPFP 0 " + callbackCode + " INVALID_FORMAT");
                        System.err.println("[TCP Parser] Formato SETPFP invalido " + clientIp);
                        break;
                    }
                    String username = pfpArgs[1];
                    String base64Image = pfpArgs[2];
                    
                    ProfileService.updatePfpHandle(username, base64Image, callbackCode, clientIp);
                }
                break;
            case "DELETE_ACCOUNT":
                System.out.println("[TCP] Delete account request from IP: " + clientIp);
                System.out.println("[TCP] ----> " + rawData);
                {
                    String token = bodyRaw;
                    DeleteAccountPacket deletePacket = new DeleteAccountPacket(code, callbackCode, token);
                    DeleteService.handle(deletePacket, clientIp);
                }
                break;

            case "MATCH":
                {
                    String[] b = bodyRaw.split(" ", 2);
                    String s = "";
                    if(b.length > 1){
                        s = b[1];
                    }
                    MatchmakingService.handle(code, callbackCode, s, clientIp);
                    break;
                }

            case "SPECTATE":
                {
                    String[] b = bodyRaw.split(" ", 2);
                    String s = "";
                    if(b.length > 1){
                        s = b[1];
                    }
                    SpectateService.handle(code, callbackCode, s, clientIp);
                    break;
                }

            case "CHAT":
                {
                    String[] b = bodyRaw.split(" ", 3);
                    if (b.length >= 3) {
                        String token = b[0];
                        String matchCode = b[1];
                        String message = b[2];
                        Integer senderId = SessionManager.getUserIdByToken(token);
                        if (senderId != null) {
                            SpectateManager.handleChat(senderId, matchCode, message, clientIp);
                        }
                    }
                    break;
                }

            default:
                System.err.println("[TCP Parser] Unknown Type (" + type + ") from " + clientIp);
                break;
        }
    }
}