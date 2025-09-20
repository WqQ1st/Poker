import java.util.Random;

public class Deck {
    private Card[] deck;
    private int nextIndex;

    public Deck(long seed) {
        nextIndex = 0;
        deck = new Card[52];
        initDeck(deck);
        shuffle(seed);

    }

    public Card draw() {
        Card c = deck[nextIndex];
        nextIndex++;
        return c;
    }

    private void initDeck(Card[] d) {
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 13; j++) {
                d[(i - 1) * 13 + j - 1] = new Card(i, j);
            }
        }
    }

    private void shuffle(long seed) {
        Random rng = new Random(seed); // seeded RNG
        for (int i = deck.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1); // random index from 0..i
            Card temp = deck[i];
            deck[i] = deck[j];
            deck[j] = temp;
        }
    }

    @Override
    public String toString() {
        String s = "";
        for (int i = nextIndex; i < deck.length; i++) {
            s += deck[i].toString() + " ";
        }
        return s;
    }
}
