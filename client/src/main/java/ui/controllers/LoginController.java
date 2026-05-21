package ui.controllers;

import ui.controllers.Controller;
import network.NetworkManager;
import network.packets.loginPacket;

public class LoginController implements Controller{
    public static void escHandler(){
        return;
    } 

    public static String login(String username, String Password){
        String send = "LOGIN 1 " + username + " " + Password;
        NetworkManager.sendTCP(send);
        return "bah";
    }

    public static String register(String username, String Password){
        String send = "REGISTER 0 " + username + " " + Password;
        NetworkManager.sendUDP(send);
        return "bah";
    }
}