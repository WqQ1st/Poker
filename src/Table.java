public class Table {
    private int button;
    private Board board;
    private final int BB = 20; //big blind
    private final int SB = 20; //small blind
    private int pot = 0;

    public Table(Board b) {
        board = b;
        button = 0;

    }

    public void play() {
        board.printDeck();
        board.addPlayer(new Player("joe", board));
        board.addPlayer(new Player("jack", board));
        board.deal();
        board.flop();
        System.out.println(board);
        board.turn();
        System.out.println(board);
        board.river();
        System.out.println(board);
    }

    public void action() {

    }



}
