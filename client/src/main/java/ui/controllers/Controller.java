package ui.controllers;

public interface Controller{
    public static void escHandler(){
        // show esc menu (exceptions main screen login and disconnect)
    }
    public static void disconnectHandler(){
        // recconecting screen (except main menu and disconnect menu)
        // manda pro disconnect, parametro screen atual
        // ui.screens.Screen CurrScreen usa isso
    }
}