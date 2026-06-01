import network.NetworkManager;

public class ServerMain {
    public static void main(String[] args) {
        NetworkManager.init();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            
        }
    }
}