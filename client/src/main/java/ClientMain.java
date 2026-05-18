import network.NetworkManager;

public class ClientMain {
    
    public static void main(String[] args) {
        NetworkManager.start();
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {}
    }
}
