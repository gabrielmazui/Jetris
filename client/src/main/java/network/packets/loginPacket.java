package network.packets;

public class loginPacket extends Packet {
    public String AUTH;
    public String pfpBase64;
    public Boolean success;

    public loginPacket(int cod, String auth, String pfp, String body, Boolean b, int codeCallback) {
        super("LOGIN", cod, body, codeCallback);

        AUTH = auth;
        pfpBase64 = pfp;
        success = b;
    }
}