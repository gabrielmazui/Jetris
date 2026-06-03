package network.dispatcher;

import network.NetworkCallback;
import network.NetworkContext;
import network.packets.*;

public class DispatcherTCP implements Runnable {
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Packet packet = NetworkContext.packetQueueTCP.take();
                int callbackCode = packet.callbackCode;
                NetworkCallback callback = NetworkContext.mapCallbacks.remove(callbackCode);
                
                if (callback == null) {
                    continue;
                }

                if (packet instanceof loginPacket) {
                    loginPacket login = (loginPacket) packet;
                    if (login.success) {
                        callback.onSuccess(login.AUTH);
                    } else {
                        callback.onFailure(login.body);
                    }
                } else if (packet instanceof registerPacket) {
                    registerPacket register = (registerPacket) packet;
                    if (register.success) {
                        callback.onSuccess(register.body);
                    } else {
                        callback.onFailure(register.body);
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