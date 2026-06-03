import db.DatabaseManager;
import network.NetworkManager;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("Connecting to database");
        Boolean DBConnected = DatabaseManager.connect();
        if(!DBConnected){
            System.err.println("[Boot CRITICAL] Could not connect with database");
            System.exit(1);
        }
        System.out.println("Initiating TCP and UDP");
        NetworkManager.init();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {}
    }
}