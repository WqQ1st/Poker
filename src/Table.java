import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Table {
    public enum Street {
        PREFLOP, FLOP, TURN, RIVER, SHOWDOWN
    }

    private int button = 0;
    private Board board;
    private final int BB = 2; //big blind
    private final int SB = 1; //small blind
    private int pot = 0;
    private final ArrayList<Player> players;
    private Deck deck;
    private boolean roundOver = false;
    private int actionsThisStreet = 0;

    private Street street = Street.PREFLOP;


    private int currentBet = 0;     // highest committed amount this round
    private int potThisHand = 0;    // total pot for this hand
    private Scanner in = new Scanner(System.in);
    private int currentPlayerIndex = 0;

    public Table() {
        long seed = System.nanoTime();
        deck = new Deck(seed);
        board = new Board(deck); //hardcode seed for consistency
        button = 0;
        players = new ArrayList<>();
        players.add(new Player("joe"));
        players.add(new Player("jack"));
    }

    public void playLoop() {
        while (true) {
            roundOver = false;
            play();
        }
    }

    public void startNewHand() {
        // New deck and board for each hand
        long seed = System.nanoTime();
        deck = new Deck(seed);
        board = new Board(deck);

        potThisHand = 0;
        currentBet = 0;
        roundOver = false;
        street = Street.PREFLOP;
        actionsThisStreet = 0;

        // Everyone is back "in", no committed chips for this round
        for (Player p : players) {
            p.clearHand();        // remove previous cards
            p.setBet(0);
            p.in();               // mark as active in the hand
        }

        deal(2);
    }


    public void play() {
        startNewHand();

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

    public void dealFlop() {
        board.flop();
    }

    public void dealTurn() {
        board.turn();
    }

    public void dealRiver() {
        board.river();
    }

    public Board getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getPotThisHand() {
        return potThisHand;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public int getToCallForCurrentPlayer() {
        Player p = getCurrentPlayer();
        return Math.max(0, currentBet - p.getBet());
    }

    public void setupBlindsForNewHand() {
        int sbIndex = button % players.size();
        int bbIndex = (button + 1) % players.size();

        Player sb = players.get(sbIndex);
        Player bb = players.get(bbIndex);

        sb.setBet(SB);
        sb.setStack(sb.getStack() - SB);

        bb.setBet(BB);
        bb.setStack(bb.getStack() - BB);

        potThisHand = SB + BB;
        currentBet = BB;

        // First to act preflop is player after BB in heads-up
        currentPlayerIndex = (bbIndex + 1) % players.size();
        actionsThisStreet = 0;
    }

    private void advanceToNextPlayer() {
        int guard = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            guard++;
        } while (guard < players.size() &&
                (!players.get(currentPlayerIndex).isIn()
                        || players.get(currentPlayerIndex).getStack() <= 0));
    }

    public void applyActionFromUI(Action type, int amount) {
        Player p = getCurrentPlayer();
        int toCall = getToCallForCurrentPlayer();
        int minRaise = BB; // simple for now

        boolean performed = false; // only true if the action actually happens

        switch (type) {
            case FOLD -> {
                p.out();
                System.out.printf("%s folds.%n", p.getName());
                performed = true;
            }
            case CHECK -> {
                if (toCall > 0) {
                    System.out.println("Cannot CHECK — there is a bet to call.");
                    return;
                }
                System.out.printf("%s checks.%n", p.getName());
                performed = true;
            }
            case CALL -> {
                if (toCall == 0) {
                    System.out.println("Nothing to call; try CHECK or BET.");
                    return;
                }
                int pay = Math.min(toCall, p.getStack());
                p.setStack(p.getStack() - pay);
                p.setBet(p.getBet() + pay);
                potThisHand += pay;
                System.out.printf("%s calls %d. Pot=%d%n", p.getName(), pay, potThisHand);
                performed = true;
            }
            case BET -> {
                if (currentBet != 0) {
                    System.out.println("Cannot BET — already a bet, use RAISE.");
                    return;
                }
                if (amount <= 0 || amount > p.getStack()) {
                    System.out.println("Invalid bet.");
                    return;
                }
                p.setStack(p.getStack() - amount);
                p.setBet(p.getBet() + amount);
                currentBet = p.getBet();
                potThisHand += amount;
                System.out.printf("%s bets %d. Pot=%d%n", p.getName(), amount, potThisHand);
                performed = true;
            }
            case RAISE -> {
                if (currentBet == 0) {
                    System.out.println("Nothing to raise; use BET instead.");
                    return;
                }
                if (amount < minRaise) {
                    System.out.printf("Min raise is %d%n", minRaise);
                    return;
                }
                int target = currentBet + amount;
                int need = target - p.getBet();
                if (need <= 0) {
                    System.out.println("You already matched that.");
                    return;
                }
                if (need > p.getStack()) {
                    System.out.println("Insufficient stack.");
                    return;
                }
                p.setStack(p.getStack() - need);
                p.setBet(target);
                potThisHand += need;
                currentBet = target;
                System.out.printf("%s raises by %d to %d. Pot=%d%n",
                        p.getName(), amount, currentBet, potThisHand);
                performed = true;
            }
        }

        if (!performed) {
            // invalid action, do not move turn or change street
            return;
        }

        actionsThisStreet++;

        // move to next player if hand not over yet
        if (!isHandOver() && street != Street.SHOWDOWN) {
            advanceToNextPlayer();
        }

        // possibly end betting round or hand
        maybeAdvanceAfterAction();
    }

    public Street getStreet() {
        return street;
    }

    private boolean isHandOver() {
        int active = 0;
        for (Player p : players) {
            if (p.isIn()) active++;
        }
        return active <= 1;
    }
    private boolean isBettingRoundComplete() {
        // do not end the round before at least 2 actions
        if (actionsThisStreet < players.size()) return false;

        int active = 0;
        int matched = 0;

        for (Player p : players) {
            if (!p.isIn()) continue;
            active++;
            // either they matched current bet or are all in
            if (p.getStack() == 0 || p.getBet() == currentBet) {
                matched++;
            }
        }

        // everyone still in has matched the bet size
        return active > 0 && matched == active;
    }

    private void resetBetsForNewStreet() {
        currentBet = 0;
        for (Player p : players) {
            p.setBet(0);
        }
        actionsThisStreet = 0;
    }

    private void advanceStreet() {
        resetBetsForNewStreet();

        switch (street) {
            case PREFLOP -> {
                street = Street.FLOP;
                board.flop();
            }
            case FLOP -> {
                street = Street.TURN;
                board.turn();
            }
            case TURN -> {
                street = Street.RIVER;
                board.river();
            }
            case RIVER -> {
                street = Street.SHOWDOWN;
                // TODO: showdown logic, hand evaluation, awarding pot
                System.out.println("Showdown (hand evaluation not implemented yet)");
            }
            default -> {
            }
        }
    }

    private void maybeAdvanceAfterAction() {
        if (isHandOver()) {
            street = Street.SHOWDOWN;
            //System.out.println("Hand over by fold.");
            // TODO: award pot to remaining player
            return;
        }

        if (isBettingRoundComplete()) {
            advanceStreet();
        }
    }

    public void awardPotToPlayer(int winnerIndex) {
        if (winnerIndex < 0 || winnerIndex >= players.size()) return;

        Player winner = players.get(winnerIndex);
        winner.setStack(winner.getStack() + potThisHand);
        System.out.printf("%s wins %d chips.%n", winner.getName(), potThisHand);

        potThisHand = 0;
        resetRoundBets();
        street = Street.SHOWDOWN;
    }

    public void splitPotBetweenActivePlayers() {
        // simple 2-player split for now
        int activeCount = 0;
        for (Player p : players) {
            if (p.isIn()) activeCount++;
        }
        if (activeCount == 0) return;

        int share = potThisHand / activeCount;
        int remainder = potThisHand % activeCount;

        for (Player p : players) {
            if (p.isIn()) {
                p.setStack(p.getStack() + share);
            }
        }
        // if odd chip, just give to button or first active player
        if (remainder > 0) {
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).isIn()) {
                    players.get(i).setStack(players.get(i).getStack() + remainder);
                    break;
                }
            }
        }

        System.out.printf("Pot %d split among %d players.%n", potThisHand, activeCount);
        potThisHand = 0;
        resetRoundBets();
        street = Street.SHOWDOWN;
    }

}
