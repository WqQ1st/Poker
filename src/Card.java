public class Card {
    private int suit; //clubs, diamonds, hearts, spades in order 1-4
    private int rank; //1-13: ace, 2, ..., king

    public Card(int s, int r) {
        suit = s;
        rank = r;
    }

    @Override
    public String toString() {
        String[] ranks = {"2","3","4","5","6","7","8","9","T","J","Q","K","A"};
        String[] suits = {"♣", "♦", "♥", "♠"};
        return ranks[rank - 1] + suits[suit - 1];
    }
}