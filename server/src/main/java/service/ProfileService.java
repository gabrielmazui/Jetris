package service;

import db.DatabaseManager;
import exceptions.DBException;
import model.Match;
import model.User;
import network.connection.TCPConnectionManager;
import network.packets.UserPacket;

import java.util.Base64;
import java.util.List;

public class ProfileService {

    public static void getUserProfileHandle(UserPacket user, String clientIp) throws DBException {
        String username = user.getUsername();
        int page = user.getPage();
        int pageSize = 10;

        int id = DatabaseManager.getUserId(username);

        if (id == -1) {
            TCPConnectionManager.send(clientIp, "GETPROFILE 1 " + user.getCallbackCode() + " EMPTY");
            return;
        }

        int[] stats = DatabaseManager.getUserStats(id);
        int wins   = (stats != null) ? stats[0] : 0;
        int losses = (stats != null) ? stats[1] : 0;

        byte[] pfpBytes = DatabaseManager.getUserPfp(id);
        String pfpBase64 = (pfpBytes != null && pfpBytes.length > 0)
            ? Base64.getEncoder().encodeToString(pfpBytes)
            : "NULL";

        int totalMatches = DatabaseManager.getUserMatchHistoryCount(id);
        int totalPages   = Math.max(1, (int) Math.ceil((double) totalMatches / pageSize));

        List<Match> pageMatches = DatabaseManager.getUserMatchHistoryPaged(id, page, pageSize);

        User usr = new User(id, username, wins, losses, pageMatches);

        StringBuilder sb = new StringBuilder("GETPROFILE 1 ")
            .append(user.getCallbackCode()).append(" ")
            .append(usr.getUsername()).append(" ")
            .append(pfpBase64).append(" ")
            .append(usr.getMatchesWon()).append(" ")
            .append(usr.getMatchesLost()).append(" ")
            .append(totalMatches).append(" ")
            .append(totalPages).append(" ")
            .append(page).append(" ")
            .append(pageMatches.size());

        for (Match m : usr.getMatchHistory()) {
            sb.append(" ").append(m.getJogador1())
            .append(" ").append(m.getJogador2())
            .append(" ").append(m.getDurationSeconds())
            .append(" ").append(m.getScoreUser1())
            .append(" ").append(m.getScoreUser2())
            .append(" ").append(m.isWon())
            .append(" ").append(m.getMatchDateMillis());
        }

        TCPConnectionManager.send(clientIp, sb.toString());
    }

    public static void searchUsernamesHandle(UserPacket user, String clientIp) throws DBException {
        String query = user.getUsername();
        String toSend = "GETPROFILE 0 " + user.getCallbackCode() + " ";

        if (query == null || query.trim().isEmpty()) {
            toSend += "EMPTY";
        } else {
            List<String[]> listUsers = db.DatabaseManager.searchUsernamesByPrefix(query.trim());
            int quantUsers = listUsers.size();
            toSend += quantUsers + " ";
            
            for(int i = 0; i < quantUsers; i++){
                String foundUsername = listUsers.get(i)[0];
                String foundPfp = listUsers.get(i)[1];

                if (foundPfp == null || foundPfp.isBlank()) {
                    foundPfp = "NULL";
                }

                toSend += foundUsername + " " + foundPfp + " ";
            }
        }

        TCPConnectionManager.send(clientIp, toSend.trim());
    }

    public static void updatePfpHandle(String username, String base64Image, int callbackCode, String clientIp) {
        String response = "SETPFP 0 " + callbackCode + " ";

        try {
            int userId = DatabaseManager.getUserId(username);

            if (userId == -1) {
                TCPConnectionManager.send(clientIp, response + "USER_NOT_FOUND");
                return;
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Image.trim());

            if (imageBytes.length > 5 * 1024 * 1024) {
                TCPConnectionManager.send(clientIp, response + "FILE_TOO_LARGE");
                return;
            }

            if (imageBytes.length < 8 ||
                imageBytes[0] != (byte) 0x89 ||
                imageBytes[1] != (byte) 0x50 ||
                imageBytes[2] != (byte) 0x4E ||
                imageBytes[3] != (byte) 0x47) {

                TCPConnectionManager.send(clientIp, response + "INVALID_FORMAT");
                return;
            }

            boolean updated = DatabaseManager.updateUserPfp(userId, imageBytes);

            if (!updated) {
                TCPConnectionManager.send(clientIp, response + "DB_ERROR");
                return;
            }

            TCPConnectionManager.send(clientIp, response + "SUCCESS");

        } catch (Exception e) {
            System.err.println("[ProfileService] Erro ao processar upload de foto: " + e.getMessage());

            TCPConnectionManager.send(clientIp, response + "ERROR");
        }
    }
}