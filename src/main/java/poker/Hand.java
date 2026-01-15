package poker;

import java.util.ArrayList;

public class Hand {
    private ArrayList<Card> cards;

    public Hand() {
        cards = new ArrayList<>();
    }

    public void add(Card c) {
        cards.add(c);
    }

    public ArrayList<Card> getHand() {
        return cards;
    }

    public String toString() {
        return cards.toString();
    }
}