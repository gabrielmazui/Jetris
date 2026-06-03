package network;

import network.tcp.TCPClient;
import network.udp.UDPClient;
import network.parser.PacketParserTCP;
import core.ScreenManager;
import exceptions.ConnectionException;
import network.dispatcher.DispatcherTCP;

public class NetworkManager {
    private static TCPClient tcp;
    private static UDPClient udp; 
    private static PacketParserTCP parserTCP;
    private static DispatcherTCP dispatcherTCP;

    static public void start(){
        tcp = new TCPClient();
        udp = new UDPClient();
        parserTCP = new PacketParserTCP();
        dispatcherTCP = new DispatcherTCP();

        try{
            Thread.startVirtualThread(tcp);
            Thread.startVirtualThread(parserTCP);
            Thread.startVirtualThread(dispatcherTCP);
            Thread.startVirtualThread(udp);

            Thread.startVirtualThread(() -> updatePingLoop());
            Thread.startVirtualThread(() -> downHandlerLoop());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    static public void shutdown(){
        tcp.shutdown();
        udp.shutdown();
    }

    static private void downHandlerLoop(){
        try{
            while (true) {
                Thread.sleep(1000);
                if(NetworkContext.tcpState == ConnectionState.RECONNECTING || NetworkContext.udpState == ConnectionState.RECONNECTING || NetworkContext.tcpState == ConnectionState.DISCONNECTED || NetworkContext.udpState == ConnectionState.DISCONNECTED){
                    retryConnection();
                    ScreenManager.CurrScreen.UpdatePing(-1);
                }
                if(NetworkContext.tcpState == ConnectionState.CONNECTED && NetworkContext.udpState == ConnectionState.CONNECTED){
                    ScreenManager.CurrScreen.DisableRetryMenu();
                }
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    static public void retryConnection(){
        if(NetworkContext.tcpState != ConnectionState.CONNECTED && !NetworkContext.isAttemptingTCP){
            Thread.startVirtualThread(tcp);
            ScreenManager.CurrScreen.EnableRetryMenu();
        }
        if(NetworkContext.udpState != ConnectionState.CONNECTED && !NetworkContext.isAttemptingUDP){
            Thread.startVirtualThread(udp);
            ScreenManager.CurrScreen.EnableRetryMenu();
        }
    }

    public static void sendTCP(String toSend, NetworkCallback callback){
        if(NetworkContext.tcpState == ConnectionState.CONNECTED){
            NetworkContext.mapCallbacks.put(callback.code, callback);
            try{
                tcp.send(toSend);
            }catch(ConnectionException e){
                retryConnection();
            }
        }
    }

    public static void sendUDP(String toSend, NetworkCallback callback){
        if(NetworkContext.udpState == ConnectionState.CONNECTED){
            NetworkContext.mapCallbacks.put(callback.code, callback);
            try{
                udp.send(toSend);
            }catch(Exception e){
                retryConnection();
            }
        }
    }

    private static void updatePingLoop(){
        try{
            while(true){
                Thread.sleep(1000);
                if(NetworkContext.udpState != ConnectionState.CONNECTED || NetworkContext.tcpState != ConnectionState.CONNECTED){
                    ScreenManager.CurrScreen.UpdatePing(-1);
                }
                if(ScreenManager.CurrScreen.isPingDisplayed()){
                    ScreenManager.CurrScreen.UpdatePing(NetworkContext.ping);
                }
            }
        }catch(InterruptedException e){}
    }
}