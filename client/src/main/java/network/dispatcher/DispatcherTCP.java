package network.dispatcher;

import network.NetworkCallback;
import network.NetworkContext;
import network.packets.*;

public class DispatcherTCP implements Runnable{

    @Override
    public void run(){
        while (!Thread.currentThread().isInterrupted()){
            try{
                Packet packet = NetworkContext.packetQueueTCP.take();
                int callbackCode = packet.callbackCode;
                NetworkCallback callback = NetworkContext.mapCallbacks.remove(callbackCode);
                if(packet instanceof loginPacket){
                    loginPacket login = (loginPacket)packet;
                    if(login.success){
                        callback.onSuccess(login.body);
                    }else{
                        callback.onFailure(login.body);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
            }
        } 
        
    }
}