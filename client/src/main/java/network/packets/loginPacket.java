package network.packets;

public class loginPacket extends Packet{
    public String AUTH;
    public Boolean success;

    public loginPacket(int cod, String auth, String body, Boolean b, int codeCallback){
        super("LOGIN", cod, body, codeCallback);
        AUTH = auth;
        success = b;
    }
}