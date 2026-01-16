package poker;

import poker.data.CardDTO;

import java.util.ArrayList;

public class Player {
    private Hand hand;
    private String name;
    private int stack = 200;
    private int currentBet = 0;
    private boolean isIn = true;

    public Player(String n) {
        hand = new Hand();
        name = n;
    }

    public Player(String name, int stack, int bet, boolean inHand, ArrayList<Card> hand) {
        this.name = name;
        this.stack = stack;
        this.currentBet = bet;
        isIn = inHand;
        this.hand = new Hand(hand);
    }

    public Hand getHand() {
        return hand;
    }

    public void deal(Card c) {
        hand.add(c);
    }

    public String toString() {
        return name + ", stack: " + Integer.toString(stack) + ", " + hand;
    }

    public void setBet(int bet) {
        currentBet = bet;
    }

    public int getBet() {
        return currentBet;
    }

    public int getStack() {
        return stack;
    }

    public void setStack(int s) {
        stack = s;
    }

    public String getName() {
        return name;
    }

    public boolean isIn() {
        return isIn;
    }

    public void out() {
        isIn = false;
    }

    public void in() {
        isIn = true;
    }

    public void clearHand() {
        hand = new Hand();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIn(boolean b) {
        isIn = b;
    }

    public void setHand(ArrayList<CardDTO> hand) {
        this.hand.setHand(CardDTO.toCards(hand));
    }
}
