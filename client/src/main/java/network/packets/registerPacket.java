package network.packets;

public class registerPacket extends Packet {
    public Boolean success;
    public String body;

    public registerPacket(int cod, String body, Boolean b, int codeCallback){
        super("REGISTER", cod, body, codeCallback);
        success = b;
    }
}