public class Player {
    private final Hand hand;
    private String name;
    private Board board;
    private Action lastAction = Action.CHECK;

    public Player(String n, Board b) {
        hand = new Hand();
        name = n;
        board = b;
    }

    public void deal(Card c) {
        hand.add(c);
    }

    public String toString() {
        return name + " " + hand + ", action=" + lastAction + ")";
    }

    public Action getAction() {
        return lastAction;
    }

    public void act(Action a) {
        //TODO: do action

    }
}
