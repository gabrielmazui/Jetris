package model;

import java.util.List;

public class User {
    private int id;
    private String username;
    private int matchesWon;
    private int matchesLost;
    private List<Match> matchHistory;

    public User(int id, String username, int matchesWon, int matchesLost, List<Match> matchHistory) {
        this.id = id;
        this.username = username;
        this.matchesWon = matchesWon;
        this.matchesLost = matchesLost;
        this.matchHistory = matchHistory;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public int getMatchesWon() { return matchesWon; }
    public int getMatchesLost() { return matchesLost; }
    public List<Match> getMatchHistory() { return matchHistory; }
}