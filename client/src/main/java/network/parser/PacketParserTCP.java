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

                    case "MATCH":
                        {
                            String[] body = bodyRaw.split(" ", 3);
                            String status = body.length > 0 ? body[0] : "";
                            boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);

                            if (code == 5) {
                                String payload = isSuccess && bodyRaw.length() > status.length() + 1
                                        ? bodyRaw.substring(status.length() + 1).trim()
                                        : "";
                                packet = new matchListPacket(code, payload, callbackCode, isSuccess, payload);
                            } else if (code == 7) {
                                String payload = body.length > 1 ? body[1] : "";
                                packet = new matchListPacket(code, payload, callbackCode, isSuccess, payload);
                            } else if (isSuccess) {
                                String action = body.length > 1 ? body[1] : "";
                                String matchCode = body.length > 2 ? body[2].trim() : "";
                                packet = new matchPacket(code, bodyRaw, callbackCode, true, action, matchCode);
                            } else {
                                String reason = body.length > 1 ? body[1] : "Unknown_error";
                                packet = new matchPacket(code, reason, callbackCode, false, "FAIL", "");
                            }
                        }
                        break;

                    case "SPECTATE":
                        {
                            String[] body = bodyRaw.split(" ", 3);
                            String status = body.length > 0 ? body[0] : "";
                            boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);
                            if (isSuccess) {
                                String action = body.length > 1 ? body[1] : "";
                                String matchCodeVal = body.length > 2 ? body[2].trim() : "";
                                packet = new matchPacket(code, bodyRaw, callbackCode, true, action, matchCodeVal);
                            } else {
                                String reason = body.length > 1 ? body[1] : "Unknown_error";
                                packet = new matchPacket(code, reason, callbackCode, false, "FAIL", "");
                            }
                        }
                        break;

                    case "MATCH_COUNTDOWN":
                        {
                            
                            String[] body = bodyRaw.split(" ", 2);
                            if (body.length >= 2) {
                                try {
                                    String matchCode = body[0];
                                    int secondsLeft = Integer.parseInt(body[1].trim());
                                    packet = new matchCountdownPacket(matchCode, secondsLeft);
                                } catch (NumberFormatException e) {
            
                                }
                            }
                        }
                        break;

                    case "MATCH_ABORT":
                        {
                            String[] body = bodyRaw.split(" ", 2);
                            if (body.length >= 2) {
                                String matchCode = body[0];
                                String reason = body[1].trim();
                                packet = new matchAbortPacket(matchCode, reason);
                            }
                        }
                        break;

                    case "MATCH_STATE":
                        {
                            String[] body = bodyRaw.split(" ", 3);
                            if (body.length >= 3) {
                                String matchCode = body[0];
                                String state = body[1];
                                String payload = body[2];
                                packet = new matchStatePacket(matchCode, state, payload);
                            }
                        }
                        break;

                    case "MATCHRESULT":
                        {
                            String[] body = bodyRaw.split(" ", 2);
                            String status = body.length > 0 ? body[0] : "";
                            boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);
                            String payload = body.length > 1 ? body[1] : "";
                            if (isSuccess && !payload.isBlank()) {
                                String[] fields = payload.split("\\|", 5);
                                if (fields.length >= 3) {
                                    long startMs = fields.length >= 4 ? parseLongSafe(fields[3]) : 0L;
                                    long endMs   = fields.length >= 5 ? parseLongSafe(fields[4]) : 0L;
                                    packet = new matchResultPacket(fields[0], fields[1], fields[2], startMs, endMs);
                                }
                            }
                        }
                        break;

                    case "CHAT":
                        {
                            String[] body = bodyRaw.split(" ", 4);
                            if (body.length >= 4) {
                                String matchCode = body[0];
                                try {
                                    int senderId = Integer.parseInt(body[1]);
                                    String senderName = body[2];
                                    String message = body[3];
                                    packet = new chatPacket(matchCode, senderId, senderName, message);
                                } catch (NumberFormatException e) {
                                }
                            }
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

    private static long parseLongSafe(String s) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; }
    }
}