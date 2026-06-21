package network.packets;

public class chatPacket extends Packet {
    public final String matchCode;
    public final int senderId;
    public final String senderName;
    public final String message;

    public chatPacket(String matchCode, int senderId, String senderName, String message) {
        super("CHAT", 0, message, 0);
        this.matchCode = matchCode;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
    }
}
