package model;

public class Match {
    private String jogador1;
    private String jogador2;
    private int durationSeconds;
    private int scoreUser1;
    private int scoreUser2;
    private boolean won;
    private long matchDateMillis;

    public Match(String jogador1, String jogador2, int durationSeconds, int scoreUser1, int scoreUser2, boolean won, long matchDateMillis) {
        this.jogador1 = jogador1;
        this.jogador2 = jogador2;
        this.durationSeconds = durationSeconds;
        this.scoreUser1 = scoreUser1;
        this.scoreUser2 = scoreUser2;
        this.won = won;
        this.matchDateMillis = matchDateMillis;
    }

    public String getJogador1() { return jogador1; }
    public String getJogador2() { return jogador2; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getScoreUser1() { return scoreUser1; }
    public int getScoreUser2() { return scoreUser2; }
    public boolean isWon() { return won; }
    public long getMatchDateMillis() { return matchDateMillis; }
}
