package network.packets;

public class setPfpPacket extends Packet{
    public String error;
    public Boolean success;

    public setPfpPacket(int cod, String body, int codeCallback, Boolean s){
        super("SETPFP", cod, body, codeCallback);
        this.error = body;
        this.success = s;
    }
}
