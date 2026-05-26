package network.packets;

public abstract class Packet {
    public String body;
    public final String type;
    public int code;
    public int callbackCode;

    public Packet(String s, int i, String b, int call){
        this.type = s;
        this.code = i;
        this.body = b;
        this.callbackCode = call;
    }
}