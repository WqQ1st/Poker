package poker;

public class Card {
    private final int suit; //clubs, diamonds, hearts, spades in order 1-4
    private final int rank; //1-13: 2, ..., king, ace

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

    static String rankToString(int r) {
        return switch (r) {
            case 1 -> "2";
            case 2 -> "3";
            case 3 -> "4";
            case 4 -> "5";
            case 5 -> "6";
            case 6 -> "7";
            case 7 -> "8";
            case 8 -> "9";
            case 9 -> "10";
            case 10 -> "J";
            case 11 -> "Q";
            case 12 -> "K";
            case 13 -> "A";
            default -> "?";
        };
    }
}