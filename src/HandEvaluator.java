import java.util.ArrayList;

public class HandEvaluator {
    public static ArrayList<Player> winners(Board b, ArrayList<Player> players) {
        ArrayList<Card> strongest = null;
        int index = 0;
        ArrayList<Integer> strongestIndices = new ArrayList<>();
        for (Player p : players) {
            ArrayList<Card> cards = new ArrayList<>(p.getHand().getHand());
            cards.addAll(b.getBoard());
            if (compare(cards, strongest) >= 0) {
                strongest = cards;
                if (compare(cards, strongest) > 0) {
                    strongestIndices = new ArrayList<>();
                }
                strongestIndices.add(index);
            }
            index++;
        }

        ArrayList<Player> result = new ArrayList<>();
        for (int i : strongestIndices) {
            result.add(players.get(i));
        }
        return result;
    }

    public static int compare(ArrayList<Card> h1, ArrayList<Card> h2) { //pick 5 best and compare
        return 0;
    }
}
