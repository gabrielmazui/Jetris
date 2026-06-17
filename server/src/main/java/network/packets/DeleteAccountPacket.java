package network.packets;

public class DeleteAccountPacket {
    public int code;
    public int callbackCode;
    public String token;

    public DeleteAccountPacket(int code, int callbackCode, String token) {
        this.code = code;
        this.callbackCode = callbackCode;
        this.token = token;
    }
}