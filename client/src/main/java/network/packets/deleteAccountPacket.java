package network.packets;

public class deleteAccountPacket extends Packet {
    public String body;
    public boolean success;
    public String error;

    public deleteAccountPacket(int code, String body, int callbackCode, boolean success, String error) {
        super("DELETE_ACCOUNT", code, body, callbackCode);
        this.body = body;
        this.success = success;
        this.error = error;
    }
}