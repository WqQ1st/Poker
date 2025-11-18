public class Player {
    private final Hand hand;
    private String name;
    private int stack = 200;
    private int currentBet = 0;
    private boolean isIn = true;

    public Player(String n) {
        hand = new Hand();
        name = n;
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

    public void act(Action a) {
        //TODO: do action

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
}
