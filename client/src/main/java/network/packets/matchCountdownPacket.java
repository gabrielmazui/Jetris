package network.packets;

public class matchCountdownPacket extends Packet {

    public final String matchCode;
    public final int secondsLeft;

    public matchCountdownPacket(String matchCode, int secondsLeft) {
        super("MATCH_COUNTDOWN", 0, "", 0);
        this.matchCode = matchCode;
        this.secondsLeft = secondsLeft;
    }
}