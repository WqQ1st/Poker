import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        play();
    }

    private static void play() {
        Board b = new Board(System.nanoTime()); //hardcode long seed for testing
        Table t = new Table(b);
        t.play();
    }
}