package network.packets;

public class loginPacket extends Packet{
    public String AUTH;
    public Boolean success;

    public loginPacket(int cod, String auth, String error, Boolean b, int codeCallback){
        super("LOGIN", cod, error, codeCallback);
        AUTH = auth;
        success = b;
    }
}