import java.util.ArrayList;
import java.util.Random;

public class Board {
    private Card[] board;
    private Deck deck;
    private int nextIndex;

    public Board(long seed) {
        deck = new Deck(seed);
        board = new Card[5];
    }

    public void flop() {
        board[0] = deck.draw();
        board[1] = deck.draw();
        board[2] = deck.draw();
    }

    public void printDeck() {
        System.out.println(deck);
    }

    @Override
    public String toString() {
        String s = "";
        return s;
    }
}
