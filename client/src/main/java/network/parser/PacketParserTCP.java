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
                        {
                            
                            String[] body = bodyRaw.split(" ", 2);
                            String status = body[0];
                            boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);

                            if (isSuccess) {
                                if (body.length >= 2) {
                                    String[] loginData = body[1].split(" ", 2);
                                    String token = loginData[0];
                                    String pfpBase64 =
                                            loginData.length > 1
                                            ? loginData[1]
                                            : "";
                                    
                                    packet = new loginPacket(
                                            code,
                                            token,
                                            pfpBase64,
                                            "",
                                            true,
                                            callbackCode
                                    );

                                } else {

                                    packet = new loginPacket(
                                            code,
                                            "",
                                            "",
                                            "error: missing auth token",
                                            false,
                                            callbackCode
                                    );
                                }

                            } else {

                                String errMsg =
                                        (body.length > 1)
                                        ? body[1]
                                        : "Unknown login error";

                                packet = new loginPacket(
                                        code,
                                        "",
                                        "",
                                        errMsg,
                                        false,
                                        callbackCode
                                );
                            }
                        }
                        break;

                    case "REGISTER":
                        {
                            String regStatus = bodyRaw;
                            boolean isRegSuccess = "SUCCESS".equalsIgnoreCase(regStatus);
                            packet = new registerPacket(code, regStatus, isRegSuccess, callbackCode);
                        }
                        break;
                        
                    case "LOGOUT":
                        {
                            String regStatus = bodyRaw;
                            boolean isRegSuccess = "SUCCESS".equalsIgnoreCase(regStatus);
                            packet = new logoutPacket(regStatus, isRegSuccess, callbackCode);
                        }
                        break;

                   case "GETPROFILE":
                        {
                            if (code == 0) {
                                String limpo = bodyRaw.trim();
                                int quant = 0;
                                String users = "";
                                if (!limpo.equals("EMPTY")) {
                                    String[] parts = limpo.split(" ", 2);
                                    quant = Integer.parseInt(parts[0]);
                                    if (parts.length > 1) users = parts[1];
                                }
                                packet = new getUserPacket(code, users, quant, callbackCode, users);

                            } else if (code == 1) {
                                getUserPacket p = new getUserPacket(code, bodyRaw, 0, callbackCode, bodyRaw);

                                if (!bodyRaw.trim().equals("EMPTY")) {
                                    String[] f = bodyRaw.trim().split(" ", 9);
                                    if (f.length >= 8) {
                                        p.username     = f[0];
                                        p.pfpBase64    = f[1];
                                        p.wins         = Integer.parseInt(f[2]);
                                        p.losses       = Integer.parseInt(f[3]);
                                        p.totalMatches = Integer.parseInt(f[4]);
                                        p.totalPages   = Integer.parseInt(f[5]);
                                        p.currentPage  = Integer.parseInt(f[6]);
                                        p.matchCount   = Integer.parseInt(f[7]);
                                        p.matchesRaw   = f.length > 8 ? f[8] : "";
                                    }
                                }
                                packet = p;
                            }
                        }
                        break;

                    case "SETPFP":
                        {
                            String msg = bodyRaw;
                            Boolean success = false;
                            if(msg.equals("SUCCESS")){
                                success = true;
                            }
                            packet = new setPfpPacket(code, msg, callbackCode, success);
                        }
                        break;
                    case "DELETE_ACCOUNT":
                        {
                            boolean success = "SUCCESS".equalsIgnoreCase(bodyRaw);
                            String error = success ? "" : bodyRaw;
                            packet = new deleteAccountPacket(code, bodyRaw, callbackCode, success, error);
                        }
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