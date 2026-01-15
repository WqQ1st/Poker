package data;

import java.util.ArrayList;


public class GameState {
    public String street;
    public int button;
    public int currentPlayerIndex;

    public int potThisHand;
    public int currentBet;
    public int actionsThisStreet;
    public int lastRaiseIncrement;

    public ArrayList<PlayerState> players = new ArrayList<>();
    public ArrayList<CardDTO> board = new ArrayList<>();

    // exact resume
    public ArrayList<CardDTO> deckOrder = new ArrayList<>();
    public int nextIndex;

    public static class PlayerState {
        public String name;
        public int stack;
        public int bet;
        public boolean inHand;
        public ArrayList<CardDTO> hand = new ArrayList<>();


    }
}
