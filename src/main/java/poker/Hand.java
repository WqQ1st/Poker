package poker;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Hand {
    private ArrayList<Card> cards;

    public Hand() {
        cards = new ArrayList<>();
    }

    public Hand(ArrayList<Card> h) {
        if (h.size() != 2 || h.get(0) == null || h.get(1) == null) {
            throw new IllegalArgumentException("Not a valid hand");
        }
        cards = h;
    }

    public void add(Card c) {
        cards.add(c);
    }

    public ArrayList<Card> getHand() {
        return cards;
    }

    public void setHand(ArrayList<Card> h) {
        cards = h;
    }

    public String toString() {
        return cards.toString();
    }
}