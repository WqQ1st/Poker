import java.util.ArrayList;
import java.util.Scanner;

public class Table {
    private int button = 0;
    private final Board board;
    private final int BB = 2; //big blind
    private final int SB = 1; //small blind
    private int pot = 0;
    private final ArrayList<Player> players;
    private final Deck deck;
    private boolean roundOver = false;


    private int currentBet = 0;     // highest committed amount this round
    private int potThisHand = 0;    // total pot for this hand
    private Scanner in = new Scanner(System.in);

    public Table() {
        long seed = System.nanoTime();
        deck = new Deck(seed);
        board = new Board(deck); //hardcode seed for consistency
        button = 0;
        players = new ArrayList<>();
    }

    public void playLoop() {
        while (true) {
            roundOver = false;
            play();
        }
    }

    public void play() {
        players.add(new Player("joe"));
        players.add(new Player("jack"));

        deal(2); //2 cards per person

        players.get((button + 1) % players.size()).setBet(BB);
        players.get((button + 1) % players.size()).setStack(players.get((button + 1) % players.size()).getStack() - BB);

        players.get((button) % players.size()).setBet(SB);
        players.get((button) % players.size()).setStack(players.get((button) % players.size()).getStack() - SB);
        potThisHand = SB + BB;

        currentBet = BB;

        printPlayers();

        runBettingRound((button) % players.size(), button % players.size());
        resetRoundBets();
        if (roundOver) {
            button += 1;
            return;
        }

        board.flop();
        System.out.println(board);
        printPlayers();

        runBettingRound((button + 1) % players.size(), -1);
        resetRoundBets();
        if (roundOver) {
            button += 1;
            return;
        }

        board.turn();
        System.out.println(board);
        printPlayers();

        runBettingRound((button + 1) % players.size(), -1);
        resetRoundBets();
        if (roundOver) {
            button += 1;
            return;
        }

        board.river();
        System.out.println(board);
        printPlayers();

        runBettingRound((button + 1) % players.size(), -1);
        resetRoundBets();

        button += 1;

    }

    public final class ChosenAction {
        public final Action type;
        public final int amount; // used for BET/RAISE; 0 otherwise
        public ChosenAction(Action type, int amount) {
            this.type = type;
            this.amount = amount;
        }
    }

    private void resetRoundBets() {
        for (Player p : players) p.setBet(0);
        currentBet = 0;
    }

    private void runBettingRound(int firstToActIndex, int initialLastAggressorIndex) {
        // Count players still in hand
        int active = 0;
        for (Player p : players) {
            if (p.isIn()) {
                active++;
            }
        }
        if (active <= 1) return;

        // Track who last raised to know when the loop can end
        int lastRaiser = initialLastAggressorIndex;
        int i = firstToActIndex;

        int pending = (currentBet == 0)
                ? active
                : (lastRaiser != -1 ? active : active - 1);

        while (true) {

            int guard = 0;
            while ((!players.get(i).isIn() || players.get(i).getStack() <= 0) && guard++ < players.size()) {
                i = (i + 1) % players.size();
            }

            Player p = players.get(i);
            if (p.isIn() && p.getStack() > 0) {
                int toCall = Math.max(0, currentBet - p.getBet());
                int minRaise = BB; // keep it simple for now

                ChosenAction ca = action(p, toCall, minRaise);

                switch (ca.type) {
                    case FOLD -> {
                        p.out();
                        active--;
                        System.out.printf("%s folds.%n", p.getName());
                        if (active == 1) {
                            roundOver = true;
                            return; // round over, hand effectively ends
                        }
                        if (pending > 0) pending--;
                    }
                    case CHECK -> {
                        System.out.printf("%s checks.%n", p.getName());
                        if (pending > 0) pending--;
                    }
                    case CALL -> {
                        int pay = ca.amount;
                        p.setStack(p.getStack() - pay);
                        p.setBet(p.getBet() + pay);
                        potThisHand += pay;
                        System.out.printf("%s calls %d. Pot=%d%n", p.getName(), pay, potThisHand);
                        if (pending > 0) pending--;
                    }
                    case BET -> {
                        int bet = ca.amount;
                        p.setStack(p.getStack() - bet);
                        p.setBet(p.getBet() + bet);
                        currentBet = p.getBet();
                        potThisHand += bet;
                        lastRaiser = i;
                        System.out.printf("%s bets %d. Pot=%d%n", p.getName(), bet, potThisHand);
                        pending = active - 1;
                    }

                    case RAISE -> {
                        int target = currentBet + ca.amount; // raise by amount
                        int need = target - p.getBet();
                        p.setStack(p.getStack() - need);
                        p.setBet(target);
                        potThisHand += need;
                        currentBet = target;
                        lastRaiser = i;
                        System.out.printf("%s raises by %d to %d. Pot=%d%n",
                                p.getName(), ca.amount, currentBet, potThisHand);
                        pending = active - 1;
                    }
                }
            }

            if (pending <= 0) return;

            // advance to next player
            i = (i + 1) % players.size();
            // skip players not in hand
            int skipGuard = 0;
            while (!players.get(i).isIn() && skipGuard++ < players.size()) {
                i = (i + 1) % players.size();
            }
        }
    }

    public ChosenAction action(Player p, int minToCall, int minRaise) {
        while (true) {
            System.out.printf("%n%s to act. Stack=%d, ToCall=%d, CurrentBet=%d, Pot=%d%n",
                    p.getName(), p.getStack(), minToCall, currentBet, potThisHand);
            System.out.print("Enter action [check/call/bet x/raise x/fold]: ");

            String line = in.nextLine().trim().toLowerCase();
            if (line.startsWith("fold")) return new ChosenAction(Action.FOLD, 0);
            if (line.startsWith("check")) {
                if (minToCall == 0) return new ChosenAction(Action.CHECK, 0);
                System.out.println("Cannot CHECK — there is a bet to call.");
                continue;
            }
            if (line.startsWith("call")) {
                if (minToCall == 0) {
                    System.out.println("Nothing to call; try CHECK or BET.");
                    continue;
                }
                int callAmt = Math.min(minToCall, p.getStack()); // allow all-in call
                return new ChosenAction(Action.CALL, callAmt);
            }
            if (line.startsWith("bet")) {
                String[] sp = line.split("\\s+");
                if (currentBet != 0) {
                    System.out.println("Cannot BET — there is already a bet; try RAISE.");
                    continue;
                }
                if (sp.length < 2) { System.out.println("Usage: bet <amount>"); continue; }
                int amt = Integer.parseInt(sp[1]);
                if (amt <= 0 || amt > p.getStack()) { System.out.println("Invalid bet."); continue; }
                return new ChosenAction(Action.BET, amt);
            }
            if (line.startsWith("raise")) {
                String[] sp = line.split("\\s+");
                if (currentBet == 0) {
                    System.out.println("Nothing to raise; try BET.");
                    continue;
                }
                if (sp.length < 2) { System.out.println("Usage: raise <amount> (raise size)"); continue; }
                int raiseSize = Integer.parseInt(sp[1]);
                if (raiseSize < minRaise) {
                    System.out.printf("Min raise is %d%n", minRaise);
                    continue;
                }
                int target = currentBet + raiseSize;
                int need = target - p.getBet();
                if (need <= 0) { System.out.println("You already matched that."); continue; }
                if (need > p.getStack()) { System.out.println("Insufficient stack."); continue; }
                return new ChosenAction(Action.RAISE, raiseSize);
            }
            System.out.println("Unrecognized input.");
        }
    }

    public void deal(int n) {
        for (int i = 0; i < n; i++) {
            for (Player p : players) {
                p.deal(deck.draw());

            }
        }
    }

    public void printPlayers() {
        for (Player p : players) {
            System.out.println(p);
        }
    }

}
