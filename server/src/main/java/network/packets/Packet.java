package network.packets;

public abstract class Packet {
    protected final String type;
    protected final int code;
    protected final int callbackCode;

    public Packet(String type, int code, int callbackCode) {
        this.type = type;
        this.code = code;
        this.callbackCode = callbackCode;
    }

    public String getType() { 
        return type; 
    }
    
    public int getCode() { 
        return code; 
    }
    
    public int getCallbackCode() { 
        return callbackCode; 
    }
}