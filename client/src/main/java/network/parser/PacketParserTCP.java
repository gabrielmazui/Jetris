package network.parser;

import network.NetworkContext;
import network.packets.*;

public class PacketParserTCP implements Runnable {

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String raw = NetworkContext.rawQueueTCP.take();
                String[] partes = raw.trim().split(" ", 4);
                
                if (partes.length < 4) {
                    continue;
                }
                
                String type = partes[0];
                int code;
                int callbackCode;
                try {
                    code = Integer.parseInt(partes[1]);
                    callbackCode = Integer.parseInt(partes[2]);
                } catch (NumberFormatException e) {
                    continue;
                }
                
                String bodyRaw = partes[3].trim();
                Packet packet = null;

                switch (type) {
                    case "LOGIN":
                        String[] body = bodyRaw.split(" ", 2);
                        String status = body[0];
                        boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);

                        if (code == 0) {
                            if (isSuccess) {
                                packet = new loginPacket(code, status, "", true, callbackCode);
                            } else {
                                String errMsg = (body.length > 1) ? body[1] : "Unknown login error";
                                packet = new loginPacket(code, status, errMsg, false, callbackCode);
                            }
                        } else if (code == 1) {
                            if (isSuccess) {
                                if (body.length >= 2) {
                                    String authContext = body[1];
                                    packet = new loginPacket(code, authContext, "", true, callbackCode);
                                } else {
                                    packet = new loginPacket(code, "", "error: missing auth token", false, callbackCode);
                                }
                            } else {
                                String errMsg = (body.length > 1) ? body[1] : "Unknown auth error";
                                packet = new loginPacket(code, "", errMsg, false, callbackCode);
                            }
                        }
                        break;

                    case "REGISTER":
                        String[] regBody = bodyRaw.split(" ", 2);
                        String regStatus = regBody[0];
                        boolean isRegSuccess = "SUCCESS".equalsIgnoreCase(regStatus);
                        String regMsg = (regBody.length > 1) ? regBody[1] : "";
                        
                        packet = new registerPacket(code, isRegSuccess ? "SUCCESS" : regMsg, isRegSuccess, callbackCode);
                        break;
                        
                    default:
                        break;
                }

                if (packet != null) {
                    NetworkContext.packetQueueTCP.add(packet);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
            }
        }
    }
}