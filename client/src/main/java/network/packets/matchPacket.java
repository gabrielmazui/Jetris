package network.packets;

public class matchPacket extends Packet {

    public boolean success;
    public String action;
    public String matchCode;
    public int secondsLeft; 

    public matchPacket(int code, String body, int callbackCode, boolean success, String action, String matchCode) {
        super("MATCH", code, body, callbackCode);
        this.success = success;
        this.action = action;
        this.matchCode = matchCode;
        this.secondsLeft = -1;
    }
}