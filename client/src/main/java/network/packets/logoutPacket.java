package network.packets;

public class logoutPacket extends Packet {
    public Boolean success;
    public String body;

    public logoutPacket(String body, Boolean b, int codeCallback){
        super("LOGOUT", 0, body, codeCallback);
        success = b;
    }
}

