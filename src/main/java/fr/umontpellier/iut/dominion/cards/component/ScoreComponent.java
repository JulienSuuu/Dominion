package fr.umontpellier.iut.dominion.cards.component;

public class ScoreComponent implements CardComponent {
    int score;
    public ScoreComponent(int score) {
        this.score = score;
    }
    public int getScore() {
        return score;
    }
}
