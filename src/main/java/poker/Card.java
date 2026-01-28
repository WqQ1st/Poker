package poker;

public class Card {
    private final int suit; //clubs, diamonds, hearts, spades in order 1-4
    private final int rank; //1-13: 2, ..., king, ace

    public Card(int s, int r) {
        suit = s;
        rank = r;
    }

    public static Card fromString(String cs) {
        int r = stringToRank(cs.substring(0, 1));
        int s = stringToSuit(cs.substring(1, 2));
        if (s == -1 || r == -1) {
            throw new IllegalArgumentException("Bad card string: " + cs + " parsed s=" + s + " r=" + r);
        }

        return new Card(s, r);
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
//        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"};
//        String[] suits = {"♣", "♦", "♥", "♠"};
//        return ranks[rank - 1] + suits[suit - 1];
        return rankToString(rank) + suitToString(suit);
    }

    public static String rankToString(int r) {
        return switch (r) {
            case 1 -> "2";
            case 2 -> "3";
            case 3 -> "4";
            case 4 -> "5";
            case 5 -> "6";
            case 6 -> "7";
            case 7 -> "8";
            case 8 -> "9";
            case 9 -> "T";
            case 10 -> "J";
            case 11 -> "Q";
            case 12 -> "K";
            case 13 -> "A";
            default -> "?";
        };
    }

    public static String suitToString(int s) {
        return switch (s) {
            case 1 -> "c";
            case 2 -> "d";
            case 3 -> "h";
            case 4 -> "s";
            default -> "?";
        };
    }

    public static int stringToSuit(String s) {
        return switch (s) {
            case "c" -> 1;
            case "d" -> 2;
            case "h" -> 3;
            case "s" -> 4;
            default -> -1;
        };
    }

    public static int stringToRank(String r) {
        return switch (r) {
            case "2" -> 1;
            case "3" -> 2;
            case "4" -> 3;
            case "5" -> 4;
            case "6" -> 5;
            case "7" -> 6;
            case "8" -> 7;
            case "9" -> 8;
            case "T"  -> 9;
            case "J" -> 10;
            case "Q" -> 11;
            case "K" -> 12;
            case "A" -> 13;
            default -> -1;
        };
    }
}