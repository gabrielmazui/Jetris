package network.packets;

public class matchStatePacket extends Packet {

    public final String matchCode;
    public final String state;
    public final String payload;

    public matchStatePacket(String matchCode, String state, String payload) {
        super("MATCH_STATE", 0, payload, 0);
        this.matchCode = matchCode;
        this.state = state;
        this.payload = payload;
    }
}
