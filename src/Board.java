import java.util.ArrayList;
import java.util.Random;

public class Board {
    private final Card[] board;
    private final Deck deck;
    private int nextIndex;
    private final ArrayList<Player> players;

    public Board(long seed) {
        deck = new Deck(seed);
        board = new Card[5];
        players = new ArrayList<>();
    }

    public void addPlayer(Player p) {
        players.add(p);
    }

    public void deal() {
        for (int i = 0; i < 2; i++) {
            for (Player p : players) {
                p.deal(deck.draw());

            }
        }
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

        s += "\nplayers: " + players;
        return s;
    }
}
