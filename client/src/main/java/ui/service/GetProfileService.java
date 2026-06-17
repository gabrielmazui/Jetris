package ui.service;

import config.UserSession;
import network.NetworkCallback;
import network.NetworkManager;

public class GetProfileService {

    public static void getProfile(String username, int page, int callbackId, NetworkCallback callback) {
        String token = UserSession.getToken();
        String command = "GETPROFILE 1 " + callbackId + " " + token + " " + username + " " + page;
        NetworkManager.sendTCP(command, callback);
    }
}