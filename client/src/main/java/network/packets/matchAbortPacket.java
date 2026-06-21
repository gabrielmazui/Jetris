package network.packets;

public class matchAbortPacket extends Packet {

    public final String matchCode;
    public final String reason;

    public matchAbortPacket(String matchCode, String reason) {
        super("MATCH_ABORT", 0, reason, 0);
        this.matchCode = matchCode;
        this.reason = reason;
    }
}
