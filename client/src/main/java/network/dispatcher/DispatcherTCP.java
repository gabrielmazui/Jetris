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
                        callback.onSuccess(login.AUTH + " " + login.pfpBase64);
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
                } else if (packet instanceof logoutPacket) {
                    callback.onSuccess("SUCCESS");
                } else if (packet instanceof getUserPacket) {
                    getUserPacket getUser = (getUserPacket) packet;
                    if (getUser.code == 0) {
                        callback.onSuccess(getUser.quant + " " + getUser.users);
                    } else if (getUser.code == 1) {
                        if (getUser.username == null) {
                            callback.onFailure("EMPTY");
                        } else {
                            String raw = getUser.username + " " + getUser.pfpBase64 + " "
                                + getUser.wins + " " + getUser.losses + " "
                                + getUser.totalMatches + " " + getUser.totalPages + " "
                                + getUser.currentPage + " " + getUser.matchCount
                                + (getUser.matchesRaw != null && !getUser.matchesRaw.isBlank()
                                    ? " " + getUser.matchesRaw : "");
                            callback.onSuccess(raw);
                        }
                    }
                }else if (packet instanceof setPfpPacket) {
                    setPfpPacket setpfp = (setPfpPacket) packet;
                    if(setpfp.success){
                        callback.onSuccess(setpfp.body);
                    }else{
                        if(setpfp.error.length() > 0){
                            callback.onFailure(setpfp.error);
                        }else{
                            callback.onFailure("error");
                        }
                    }
                } else if (packet instanceof deleteAccountPacket) {
                    deleteAccountPacket del = (deleteAccountPacket) packet;
                    if (del.success) {
                        callback.onSuccess("SUCCESS");
                    } else {
                        callback.onFailure(del.error.length() > 0 ? del.error : "Unknown error");
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