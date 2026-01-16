package poker.data;

import java.util.ArrayList;
import poker.Card;


public class GameState {
    public String street;
    public int button;
    public int currentPlayerIndex;

    public int potThisHand;
    public int currentBet;
    public int actionsThisStreet;
    public int lastRaiseIncrement;

    public ArrayList<PlayerDTO> players;

    // exact resume
    public CardDTO[] deck;
    public long seed;
    public int nextIndex;
    public ArrayList<CardDTO> board;

    public static CardDTO[] dtoDeck(Card[] e) {
        CardDTO[] d = new CardDTO[52];
        for (int i = 0; i < 52; i++) {
            if (e[i] != null) {
                d[i] = CardDTO.fromCard(e[i]);
            }
        }
        return d;
    }

    public static Card[] fromdtoDeck(CardDTO[] e) {
        Card[] d = new Card[52];
        for (int i = 0; i < 52; i++) {
            if (e[i] != null) {
                d[i] = CardDTO.toCard(e[i]);
            }
        }
        return d;
    }
}
