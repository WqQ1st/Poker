package poker;

import java.util.ArrayList;
import java.util.Arrays;


enum Category {
    HIGH_CARD,
    ONE_PAIR,
    TWO_PAIR,
    THREE_OF_A_KIND,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    STRAIGHT_FLUSH
}

public class HandEvaluator {

    static final class HandValue implements Comparable<HandValue> {
        final Category cat;
        final int[] tiebreak; // highest first, compare lexicographically

        HandValue(Category cat, int... tiebreak) {
            this.cat = cat;
            this.tiebreak = tiebreak;
        }

        @Override
        public String toString() {
            String s = "";
            switch (this.cat) {
                case HIGH_CARD:
                    s += Card.rankToString(tiebreak[0]) + "high";
                    break;
                case ONE_PAIR:
                    s += "a pair of " + Card.rankToString(tiebreak[0]) + "s";
                    break;
                case TWO_PAIR:
                    s += "a " + Card.rankToString(tiebreak[0]) + " and " + Card.rankToString(tiebreak[1]) + " Two Pair";
                    break;
                case THREE_OF_A_KIND:
                    s += "three of a kind of " + Card.rankToString(tiebreak[0]) + "s";
                    break;
                case STRAIGHT:
                    s += "a " + Card.rankToString(tiebreak[0]) + " high Straight";
                    break;
                case FLUSH:
                    s += "a " + Card.rankToString(tiebreak[0]) + " high Flush";
                    break;
                case FULL_HOUSE:
                    s += Card.rankToString(tiebreak[0]) + "s full of " + Card.rankToString(tiebreak[1])+ "s";
                    break;
                case FOUR_OF_A_KIND:
                    s += "quad " + Card.rankToString(tiebreak[0]) + "s";
                    break;
                case STRAIGHT_FLUSH:
                    s += "a " + Card.rankToString(tiebreak[0]) + " high straight flush";
                    break;
                default:
                    throw new IllegalArgumentException("No value assigned");
            }
            return s;
        }

        @Override
        public int compareTo(HandValue o) {
            int c = Integer.compare(this.cat.ordinal(), o.cat.ordinal());
            if (c != 0) return c;

            int n = Math.min(this.tiebreak.length, o.tiebreak.length);
            for (int i = 0; i < n; i++) {
                int d = Integer.compare(this.tiebreak[i], o.tiebreak[i]);
                if (d != 0) return d;
            }
            return Integer.compare(this.tiebreak.length, o.tiebreak.length);
        }
    }

    public static ArrayList<Card> bestHand(ArrayList<Card> hand) { //best 5 of 7
        if (hand.size() != 7) {
            throw new IllegalArgumentException("need 7 cards");
        }
        ArrayList<Card> best = null;
        for (int a = 0; a < 3; a++) {
            for (int b = a + 1; b < 4; b++) {
                for (int c = b + 1; c < 5; c++) {
                    for (int d = c + 1; d < 6; d++) {
                        for (int e = d + 1; e < 7; e++) {
                            ArrayList<Card> five = new ArrayList<>(Arrays.asList(hand.get(a), hand.get(b), hand.get(c), hand.get(d), hand.get(e)));
                            if (best == null || compare(five, best) > 0) {
                                best = five;
                            }
                        }
                    }
                }
            }
        }

        return best;
    }

    public static HandValue eval5(ArrayList<Card> hand) {
        if (hand.size() != 5) {
            throw new IllegalArgumentException("need 5 cards");
        }
        int[] ranks = new int[5];
        int[] suits = new int[5];
        for (int i = 0; i < 5; i++) {
            ranks[i] = hand.get(i).getRank();
            suits[i] = hand.get(i).getSuit();
        }
        Arrays.sort(ranks); // ascending

        boolean flush = isFlush(suits);
        StraightInfo st = straightInfo(ranks);
        boolean straight = st.isStraight;

        int[] count = new int[14]; //frequency of each rank
        for (int r : ranks) {
            count[r]++;
        }

        ArrayList<Integer> ranksPresent = new ArrayList<>();
        for (int i = 13; i > 0; i--) { //desc
            if (count[i] > 0) {
                ranksPresent.add(i);
            }
        }

        ArrayList<int[]> groups = new ArrayList<>(); //stored as [rank, frequency]
        for (int i = 13; i > 0; i--) {
            if (count[i] > 0) {
                groups.add(new int[]{i, count[i]});
            }
        }

        groups.sort((a, b) -> {
            if (a[1] != b[1]) {
                return Integer.compare(b[1], a[1]);
            }
            return Integer.compare(b[0], a[0]);
        });

        int[] ranksDesc = desc(ranks);


        if (straight && flush) {
            return new HandValue(Category.STRAIGHT_FLUSH, st.high);
        }

        if (groups.get(0)[1] == 4) {
            int four = groups.get(0)[0];
            int kicker = groups.get(1)[0];
            return new HandValue(Category.FOUR_OF_A_KIND, four, kicker); //num, kicker
        }

        if (groups.get(0)[1] == 3 && groups.get(1)[1] == 2) {
            int three = groups.get(0)[0];
            int two = groups.get(1)[0];
            return new HandValue(Category.FULL_HOUSE, three, two); //three, two
        }

        if (flush) {
            return new HandValue(Category.FLUSH, ranksDesc);
        }

        if (straight) {
            return new HandValue(Category.STRAIGHT, st.high);
        }

        if (groups.get(0)[1] == 3) {
            int three = groups.get(0)[0];
            int[] kick = excludeAll(ranksDesc, three); //kickers in desc order
            return new HandValue(Category.THREE_OF_A_KIND, concat(new int[]{three}, kick)); //allows comparison
        }

        if (groups.get(0)[1] == 2 && groups.get(1)[1] == 2) {
            int p1 = groups.get(0)[0]; //pair 1
            int p2 = groups.get(1)[0]; //pair 2
            int hi = Math.max(p1, p2);
            int lo = Math.min(p1, p2);
            int kicker = groups.get(2)[0];
            return new HandValue(Category.TWO_PAIR, hi, lo, kicker);
        }

        if (groups.get(0)[1] == 2) {
            int pair = groups.get(0)[0];
            int[] kick = excludeAll(ranksDesc, pair);
            return new HandValue(Category.ONE_PAIR, concat(new int[]{pair}, kick));
        }

        return new HandValue(Category.HIGH_CARD, ranksDesc);
    }

    public static int compare(ArrayList<Card> h1, ArrayList<Card> h2) {
        if (h1.size() != 5 || h2.size() != 5) {
            throw new IllegalArgumentException("need 5 cards");
        }
        HandValue v1 = eval5(h1);
        HandValue v2 = eval5(h2);
        return v1.compareTo(v2);
    }

    public static int compare7(ArrayList<Card> h1, ArrayList<Card> h2) { //pick 5 best and compare
        if (h1.size() != 7 || h2.size() != 7) {
            throw new IllegalArgumentException("need 7 cards");
        }
        ArrayList<Card> best1 = bestHand(h1);
        ArrayList<Card> best2 = bestHand(h2);
        return compare(best1, best2);
    }

    public static int compareHands(ArrayList<Card> board, ArrayList<Card> h1, ArrayList<Card> h2) {
        if (h1.size() != 2 || h2.size() != 2 || board.size() != 5) {
            throw new IllegalArgumentException("size mismatch");
        }
        ArrayList<Card> seven1 = new ArrayList<>(board);
        ArrayList<Card> seven2 = new ArrayList<>(board);
        seven1.addAll(h1);
        seven2.addAll(h2);
        return compare7(seven1, seven2);
    }

    private static boolean isFlush(int[] suits) {
        int s = suits[0];
        for (int i = 1; i < suits.length; i++) if (suits[i] != s) return false;
        return true;
    }

    private static int[] desc(int[] asc) {
        int[] out = new int[asc.length];
        for (int i = 0; i < asc.length; i++) out[i] = asc[asc.length - 1 - i];
        return out;
    }

    private static int[] excludeAll(int[] ranksDesc, int ex) {
        int[] tmp = new int[ranksDesc.length];
        int k = 0;
        for (int r : ranksDesc) if (r != ex) tmp[k++] = r;
        return Arrays.copyOf(tmp, k);
    }

    private static int[] concat(int[] a, int[] b) {
        int[] out = new int[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    // straight helper
    private static final class StraightInfo {
        final boolean isStraight;
        final int high; // high card (0-indexing, add 1)
        StraightInfo(boolean s, int h) { isStraight = s; high = h; }
    }

    // ranksAsc length 5, ascending
    private static StraightInfo straightInfo(int[] ranksAsc) {
        // distinct
        int distinctCount = 1;
        for (int i = 1; i < 5; i++) if (ranksAsc[i] != ranksAsc[i - 1]) distinctCount++;
        if (distinctCount != 5) return new StraightInfo(false, -1);

        // normal straight
        boolean normal = true;
        for (int i = 1; i < 5; i++) {
            if (ranksAsc[i] != ranksAsc[i - 1] + 1) {
                normal = false;
                break;
            }
        }
        if (normal) {
            return new StraightInfo(true, ranksAsc[4]);
        }

        // wheel: [1,2,3,4,13] (2,3,4,5,A)
        boolean wheel = ranksAsc[0] == 1 && ranksAsc[1] == 2 && ranksAsc[2] == 3 && ranksAsc[3] == 4 && ranksAsc[4] == 13;
        if (wheel) {
            return new StraightInfo(true, 4); // 5-high straight
        }

        return new StraightInfo(false, -1);
    }


}
