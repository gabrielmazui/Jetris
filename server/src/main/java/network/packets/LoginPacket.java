package network.packets;

public class LoginPacket extends Packet {
    private final String username;
    private final String password;
    private final String token;

    public LoginPacket(int code, int callbackCode, String username, String password, String tok) {
        super("LOGIN", code, callbackCode);
        this.username = username;
        this.password = password;
        this.token = tok;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }
}