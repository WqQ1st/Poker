import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        play();
    }

    private static void play() {
        PokerFX ui = new PokerFX();
        Table t = new Table(ui);
        t.playLoop();
    }
}