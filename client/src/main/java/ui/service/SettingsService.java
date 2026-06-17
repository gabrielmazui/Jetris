package ui.service;

import config.UserSession;
import network.NetworkManager;
import network.NetworkCallback;

import java.util.Base64;

public class SettingsService {

    public static void uploadProfilePicture(byte[] pngBytes, int callbackId, NetworkCallback callback) {
        String base64Image = Base64.getEncoder().encodeToString(pngBytes);
        String command = "SETPFP 0 " + callbackId + " " + UserSession.getToken() + " " + UserSession.getUsername()+ " " + base64Image;
        NetworkManager.sendTCP(command, callback);
    }

    public static void deleteAccount(int callbackId, NetworkCallback callback) {
        String command = "DELETE_ACCOUNT 0 " + callbackId + " " + UserSession.getToken();
        NetworkManager.sendTCP(command, callback);
    }
}