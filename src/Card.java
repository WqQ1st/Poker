public class Card {
    private final int suit; //clubs, diamonds, hearts, spades in order 1-4
    private final int rank; //0-13: 2, ..., king, ace

    public Card(int s, int r) {
        suit = s;
        rank = r;
    }

    public int getSuit() {
        return suit;
    }

    public char getSuitLetter() {
        if (suit == 1) {
            return 'C';
        } else if (suit == 2) {
            return 'D';
        } else if (suit == 3) {
            return 'H';
        } else if (suit == 4) {
            return 'S';
        } else {
            throw new IllegalArgumentException("Suit is not valid: 1-4");
        }
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