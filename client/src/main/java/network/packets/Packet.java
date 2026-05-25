package network.packets;

public abstract class Packet {
    public String error;
    public final String type;
    public int code;
    public int callbackCode;

    public Packet(String s, int i, String e, int call){
        this.type = s;
        this.code = i;
        this.error = e;
        this.callbackCode = call;
    }
}