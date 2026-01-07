import java.util.ArrayList;
import java.util.Random;

public class Board {
    private final Card[] board;
    private final Deck deck;
    private int nextIndex;

    public Board(Deck d) {
        deck = d;
        board = new Card[5];
    }

    public ArrayList<Card> getBoard() {
        ArrayList<Card> cards = new ArrayList<>();
        for (Card c : board) {
            if (c == null) {
                break;
            }
            cards.add(c);
        }
        return cards;
    }

    public void flop() {
        deck.draw(); //burn
        board[0] = deck.draw();
        board[1] = deck.draw();
        board[2] = deck.draw();
    }

    public void turn() {
        deck.draw(); //burn
        board[3] = deck.draw();
    }

    public void river() {
        deck.draw(); //burn
        board[4] = deck.draw();
    }

    public void printDeck() {
        System.out.println(deck);
    }

    @Override
    public String toString() {
        String s = "";
        for (Card c : board) {
            if (c != null) {
                s += c + " ";
            }
        }
        return s;
    }
}
