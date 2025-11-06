import java.util.ArrayList;

public class Hand {
    private ArrayList<Card> cards;

    public Hand() {
        cards = new ArrayList<>();
    }

    public void add(Card c) {
        cards.add(c);
    }

    public String toString() {
        return cards.toString();
    }
}