package network.packets;

public class getUserPacket extends Packet {
    public int quant;
    public String users;

    public String username;
    public String pfpBase64;
    public int wins;
    public int losses;
    public int totalMatches;
    public int totalPages;
    public int currentPage;
    public int matchCount;
    public String matchesRaw;

    public getUserPacket(int cod, String body, int q, int codeCallback, String users) {
        super("GETUSER", cod, body, codeCallback);
        quant = q;
        this.users = users;
    }
}