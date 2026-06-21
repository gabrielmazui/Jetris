package service.matchmaking;

import matches.MatchManager;
import network.SessionManager;
import network.connection.TCPConnectionManager;

public class MatchmakingService {
    public static void handle(int code, int callbackCode, String bodyRaw, String clientIp) {
        Integer userId = SessionManager.getUserId(clientIp);
        if (userId == null) return;

        switch (code) {
            case 0:
                MatchManager.joinQueue(userId, clientIp, callbackCode);
                break;
            case 1:
                MatchManager.leaveQueue(userId, clientIp, callbackCode);
                break;
            case 2:
                MatchManager.createPrivateMatch(userId, clientIp, callbackCode);
                break;
            case 3:
                MatchManager.joinPrivateMatch(userId, bodyRaw, clientIp, callbackCode);
                break;
            case 4:
                MatchManager.cancelPrivateMatch(userId, bodyRaw, clientIp, callbackCode);
                break;
            case 5:
                TCPConnectionManager.send(clientIp, "MATCH 5 " + callbackCode + " SUCCESS " + MatchManager.buildMatchListPayload(bodyRaw));
                break;
            case 6:
                MatchManager.leaveActiveMatch(userId, clientIp, callbackCode);
                break;
            case 7:
                TCPConnectionManager.send(clientIp, "MATCH 7 " + callbackCode + " SUCCESS " + MatchManager.buildMatchInfoPayload(bodyRaw));
                break;
            case 8:
                TCPConnectionManager.send(clientIp, "MATCHRESULT 0 " + callbackCode + " SUCCESS " + MatchManager.buildMatchResultPayload(bodyRaw, userId));
                break;
        }
    }
}