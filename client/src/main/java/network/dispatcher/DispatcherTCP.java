package network.dispatcher;

import network.NetworkContext;
import network.packets.*;

public class DispatcherTCP implements Runnable{

    @Override
    public void run(){
        while (!Thread.currentThread().isInterrupted()){
            try{
                Packet packet = NetworkContext.packetQueueTCP.take();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
            }
        } 
        
    }
}