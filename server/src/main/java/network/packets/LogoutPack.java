package network.packets;

public class LogoutPack extends Packet{
    private String token;
    
    public LogoutPack(int callbackCode, String token) {
        super("LOGOUT", 0, callbackCode);
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
