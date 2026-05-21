package network.packets;

import network.packets.Packet;

public class loginPacket extends Packet{
    final public String type = "LOGIN";
    int code;
    public String USERNAME;
    public String PASSWORD;

    public loginPacket(int cod, String usr, String pass){
        code = cod;
        USERNAME = usr;
        PASSWORD = pass;
    }
}