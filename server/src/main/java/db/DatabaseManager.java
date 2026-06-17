package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import exceptions.DBException;
import io.github.cdimascio.dotenv.Dotenv;
import model.Match;

public class DatabaseManager {
    private static Connection connection;

    public static boolean connect() {
        Dotenv dotenv = Dotenv.configure()
                .directory("src/main/resources/config")
                .filename(".env")
                .load();

        String URL = dotenv.get("DB_HOST");
        String USER = dotenv.get("DB_USER");
        String PASSWORD = dotenv.get("DB_PASSWORD");

        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean userExists(String username) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] error");
        }

        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean createUser(String username, String password) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] error");
        }
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int validateCredentials(String username, String password) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] error");
        }
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return -1;
    }

    public static int getUserId(String username) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] error");
        }
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            throw new DBException("[DB] ID Error" + e.getMessage());
        }
        return -1;
    }

    public static int[] getUserStats(int userId) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] Sem conexão com o banco");
        }
        String sql = "SELECT matches_won, matches_lost FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new int[]{ rs.getInt("matches_won"), rs.getInt("matches_lost") };
                }
            }
        } catch (SQLException e) {
            throw new DBException("[DB] User error" + e.getMessage());
        }
        return null;
    }

    public static void saveMatchResult(int user1Id, int user2Id, int duration, int score1, int score2, int winnerId) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] error");
        }

        String insertMatchSql = "INSERT INTO match_history (user1_id, user2_id, duration_seconds, score_user1, score_user2, winner_id) VALUES (?, ?, ?, ?, ?, ?)";
        String updateWinnerSql = "UPDATE users SET matches_won = matches_won + 1 WHERE id = ?";
        String updateLoserSql = "UPDATE users SET matches_lost = matches_lost + 1 WHERE id = ?";

        int loserId = (user1Id == winnerId) ? user2Id : user1Id;

        synchronized (connection) {
            boolean originalAutoCommit = true;
            try {
                originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                try (PreparedStatement insertStmt = connection.prepareStatement(insertMatchSql);
                     PreparedStatement winStmt = connection.prepareStatement(updateWinnerSql);
                     PreparedStatement loseStmt = connection.prepareStatement(updateLoserSql)) {

                    insertStmt.setInt(1, user1Id);
                    insertStmt.setInt(2, user2Id);
                    insertStmt.setInt(3, duration);
                    insertStmt.setInt(4, score1);
                    insertStmt.setInt(5, score2);
                    insertStmt.setInt(6, winnerId);
                    insertStmt.executeUpdate();

                    winStmt.setInt(1, winnerId);
                    winStmt.executeUpdate();

                    loseStmt.setInt(1, loserId);
                    loseStmt.executeUpdate();

                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                throw new DBException("[DB] Error saving match" + e.getMessage());
            } finally {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<Match> getUserMatchHistory(int userId) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] Sem conexão com o banco");
        }

        List<Match> history = new ArrayList<>();
        
        String sql = "SELECT u1.username AS jogador1, u2.username AS jogador2, m.duration_seconds, " +
                    "m.score_user1, m.score_user2, (CASE WHEN m.winner_id = ? THEN 1 ELSE 0 END) AS won " +
                    "FROM match_history m " +
                    "JOIN users u1 ON m.user1_id = u1.id " +
                    "JOIN users u2 ON m.user2_id = u2.id " +
                    "WHERE m.user1_id = ? OR m.user2_id = ? " +
                    "ORDER BY m.match_date DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Match match = new Match(
                        rs.getString("jogador1"),
                        rs.getString("jogador2"),
                        rs.getInt("duration_seconds"),
                        rs.getInt("score_user1"),
                        rs.getInt("score_user2"),
                        rs.getInt("won") == 1
                    );
                    history.add(match);
                }
            }
        } catch (SQLException e) {
            throw new DBException("[DB] Match error" + e.getMessage());
        }

        return history;
    }

    public static List<String[]> searchUsernamesByPrefix(String prefix) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] Error");
        }

        List<String[]> usersList = new ArrayList<>();

        String sql = "SELECT username, pfp FROM users WHERE username LIKE ? LIMIT 10";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, prefix + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {

                    String username = rs.getString("username");

                    byte[] pfpBytes = rs.getBytes("pfp");

                    String pfpBase64 = "";

                    if (pfpBytes != null && pfpBytes.length > 0) {
                        pfpBase64 = Base64.getEncoder().encodeToString(pfpBytes);
                    }

                    usersList.add(new String[]{
                            username,
                            pfpBase64
                    });
                }
            }

        } catch (SQLException e) {
            throw new DBException("[DB] User search error: " + e.getMessage());
        }

        return usersList;
    }
    
    public static boolean updateUserPfp(int userId, byte[] imageBytes) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] Sem conexão com o banco");
        }

        String sql = "UPDATE users SET pfp = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBytes(1, imageBytes);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DBException("[DB] Erro ao atualizar pfp: " + e.getMessage());
        }
    }

    public static byte[] getUserPfp(int userId) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] Sem conexão com o banco");
        }

        String sql = "SELECT pfp FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("pfp");
                }
            }
        } catch (SQLException e) {
            throw new DBException("[DB] Erro ao buscar pfp: " + e.getMessage());
        }

        return null;
    }

    public static boolean deleteUser(int userId) throws DBException {
        if (connection == null || !connect()) {
            throw new DBException("[DB] error");
        }

        String sql = "DELETE FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DBException("[DB] Erro ao deletar usuário: " + e.getMessage());
        }
    }

    public static int getUserMatchHistoryCount(int userId) throws DBException {
        if (connection == null || !connect()) throw new DBException("[DB] error");
        String sql = "SELECT COUNT(*) FROM match_history WHERE user1_id = ? OR user2_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DBException("[DB] Count error: " + e.getMessage());
        }
        return 0;
    }

    public static List<Match> getUserMatchHistoryPaged(int userId, int page, int pageSize) throws DBException {
        if (connection == null || !connect()) throw new DBException("[DB] error");
        List<Match> history = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        String sql = "SELECT u1.username AS jogador1, u2.username AS jogador2, m.duration_seconds, " +
                    "m.score_user1, m.score_user2, (CASE WHEN m.winner_id = ? THEN 1 ELSE 0 END) AS won " +
                    "FROM match_history m " +
                    "JOIN users u1 ON m.user1_id = u1.id " +
                    "JOIN users u2 ON m.user2_id = u2.id " +
                    "WHERE m.user1_id = ? OR m.user2_id = ? " +
                    "ORDER BY m.match_date DESC LIMIT ? OFFSET ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, pageSize);
            stmt.setInt(5, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    history.add(new Match(
                        rs.getString("jogador1"),
                        rs.getString("jogador2"),
                        rs.getInt("duration_seconds"),
                        rs.getInt("score_user1"),
                        rs.getInt("score_user2"),
                        rs.getInt("won") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DBException("[DB] Paged match error: " + e.getMessage());
        }
        return history;
    }
}