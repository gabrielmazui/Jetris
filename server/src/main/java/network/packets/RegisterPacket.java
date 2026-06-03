package network.packets;

public class RegisterPacket extends Packet {
    private final String username;
    private final String password;

    public RegisterPacket(int code, int callbackCode, String username, String password) {
        super("REGISTER", code, callbackCode);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}