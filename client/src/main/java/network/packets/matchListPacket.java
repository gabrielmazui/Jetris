package network.packets;

public class matchListPacket extends Packet {

    public final boolean success;
    public final String payload;

    public matchListPacket(int code, String body, int callbackCode, boolean success, String payload) {
        super("MATCHLIST", code, body, callbackCode);
        this.success = success;
        this.payload = payload;
    }
}
