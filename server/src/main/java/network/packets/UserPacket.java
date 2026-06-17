package network.packets;

public class UserPacket extends Packet{
    private String username;
    private int page;

    public UserPacket(String u, int cd, int callback, int page){
        super("GETPROFILE", cd, callback);
        this.username = u;
        this.page = page;
    }

    public String getUsername(){return this.username;}
    public int getPage() { return page; }

}
