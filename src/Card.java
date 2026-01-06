public class Card {
    private final int suit; //clubs, diamonds, hearts, spades in order 1-4
    private final int rank; //1-13: ace, 2, ..., king

    public Card(int s, int r) {
        suit = s;
        rank = r;
    }

    public int getSuit() {
        return suit;
    }
    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        String[] ranks = {"2","3","4","5","6","7","8","9","T","J","Q","K","A"};
        String[] suits = {"♣", "♦", "♥", "♠"};
        return ranks[rank - 1] + suits[suit - 1];
    }
}