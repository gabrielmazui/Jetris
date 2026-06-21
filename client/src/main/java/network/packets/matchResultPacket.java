package network.packets;

public class matchResultPacket extends Packet {
    public final String matchCode;
    public final String outcome;
    public final String reason;
    public final long startTimeMillis;
    public final long endTimeMillis;

    public matchResultPacket(String matchCode, String outcome, String reason,
                             long startTimeMillis, long endTimeMillis) {
        super("MATCHRESULT", 0, reason, 0);
        this.matchCode = matchCode;
        this.outcome = outcome;
        this.reason = reason;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
    }
}
