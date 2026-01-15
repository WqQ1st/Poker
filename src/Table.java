import java.util.ArrayList;
import java.util.List;

public class Table {
    public enum Street {
        PREFLOP, FLOP, TURN, RIVER, SHOWDOWN
    }

    private int button;
    private Board board;
    private final int BB = 2; //big blind
    private final int SB = 1; //small blind
    private final ArrayList<Player> players;
    private Deck deck;
    private boolean roundOver = false;
    private int actionsThisStreet = 0;
    private int lastRaiseIncrement;


    private PokerFX ui;

    private Street street = Street.PREFLOP;


    private int currentBet = 0;     // highest committed amount this round
    private int potThisHand = 0;    // total pot for this hand
    private int currentPlayerIndex = 0;

    //private final HandEvaluator handEval = new HandEvaluator();

    public Table(PokerFX ui) {
        this.ui = ui;
        long seed = System.nanoTime();
        deck = new Deck(seed);
        board = new Board(deck); //hardcode seed for consistency
        button = 0;
        players = new ArrayList<>();
        players.add(new Player("p1"));
        players.add(new Player("p2"));
    }

    public int getMinRaise() {
        return lastRaiseIncrement;
    }

    public void startNewHand() {
        System.out.println("=== START NEW HAND === button=" + button
                + " stacks=" + players.get(0).getStack() + "," + players.get(1).getStack());
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
        setupBlindsForNewHand();
        lastRaiseIncrement = BB;
    }

    public ArrayList<Double> currEquity() {
        return board.equity(players.get(currentPlayerIndex).getHand().getHand(), players.get((currentPlayerIndex + 1) % 2).getHand().getHand());
    }

    public ArrayList<Double> otherEquity() {
        return board.equity(players.get((currentPlayerIndex + 1) % 2).getHand().getHand(), players.get(currentPlayerIndex).getHand().getHand());
    }

    private void resetRoundBets() {
        for (Player p : players) p.setBet(0);
        currentBet = 0;
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

    public Player getOtherPlayer() {
        return players.get((currentPlayerIndex + 1) % 2);
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

        System.out.println("BUTTON = " + button +
                " | SB = " + players.get(sbIndex).getName() +
                " | BB = " + players.get(bbIndex).getName());

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

        boolean performed = false; // only true if the action actually happens

        switch (type) {
            case FOLD -> {
                p.out();
                System.out.printf("%s folds.%n", p.getName());
                ui.setMsg(p.getName() + " folds.");
                int winner = findLastActivePlayerIndex();
                if (winner != -1) {
                    awardFold(winner);
                }
                return;
            }
            case CHECK -> {
                if (toCall > 0) {
                    System.out.println("Cannot CHECK — there is a bet to call.");
                    ui.setMsg("Cannot CHECK — there is a bet to call.");
                    return;
                }
                System.out.printf("%s checks.%n", p.getName());
                ui.setMsg(p.getName() + " checks.");
                performed = true;
            }
            case CALL -> {
                if (toCall == 0) {
                    System.out.println("Nothing to call; try CHECK or BET.");
                    ui.setMsg("Nothing to call; try CHECK or BET.");
                    return;
                }
                int pay = Math.min(toCall, p.getStack());
                p.setStack(p.getStack() - pay);
                p.setBet(p.getBet() + pay);
                potThisHand += pay;
                System.out.printf("%s calls %d. Pot=%d%n", p.getName(), pay, potThisHand);
                ui.setMsg(p.getName() + " calls " + pay + ". Pot=" + potThisHand);
                performed = true;
            }
            case BET -> {
                if (currentBet != 0) {
                    System.out.println("Cannot BET — already a bet, use RAISE.");
                    ui.setMsg("Cannot BET — already a bet, use RAISE.");
                    return;
                }
                if (amount < BB || amount > p.getStack() || amount > players.get((currentPlayerIndex + 1) % 2).getStack()) {
                    System.out.println("Invalid bet.");
                    ui.setMsg("Invalid bet.");
                    return;
                }
                p.setStack(p.getStack() - amount);
                p.setBet(p.getBet() + amount);
                currentBet = p.getBet();
                potThisHand += amount;
                System.out.printf("%s bets %d. Pot=%d%n", p.getName(), amount, potThisHand);
                ui.setMsg(p.getName() + " bets " + amount + ". Pot=" + potThisHand);
                lastRaiseIncrement = amount;
                performed = true;
            }
            case RAISE -> {
                if (currentBet == 0) {
                    System.out.println("Nothing to raise; use BET instead.");
                    ui.setMsg("Nothing to raise; use BET instead.");
                    return;
                }
                if (amount < lastRaiseIncrement) {
                    System.out.printf("Min raise is %d%n", lastRaiseIncrement);
                    ui.setMsg("Min raise is " + lastRaiseIncrement);
                    return;
                }
                int target = currentBet + amount;
                int need = target - p.getBet();
                if (need <= 0) {
                    System.out.println("You already matched that.");
                    ui.setMsg("You already matched that.");
                    return;
                }
                if (need > p.getStack()) {
                    System.out.println("Insufficient stack.");
                    ui.setMsg("Insufficient stack");
                    return;
                }
                p.setStack(p.getStack() - need);
                p.setBet(target);
                potThisHand += need;
                currentBet = target;
                System.out.printf("%s raises by %d to %d. Pot=%d%n", p.getName(), amount, currentBet, potThisHand);
                ui.setMsg(p.getName() + " raises by " + amount + " to " + currentBet + ". Pot=" + potThisHand);
                lastRaiseIncrement = amount;
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
        lastRaiseIncrement = BB;
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
                resolveShowdown();
                //startNewHand();
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
        if (winnerIndex < 0 || winnerIndex >= players.size()) {
            throw new IllegalArgumentException("Invalid player index");
        }

        Player winner = players.get(winnerIndex);
        winner.setStack(winner.getStack() + potThisHand);
        ArrayList<Card> seven = new ArrayList<>(winner.getHand().getHand());
        seven.addAll(board.getBoard());
        HandEvaluator.HandValue hv = HandEvaluator.eval5(HandEvaluator.bestHand(seven));
        System.out.printf("%s wins %d chips with %s.%n", winner.getName(), potThisHand, hv.toString());
        ui.setMsg(winner.getName() + " wins " + potThisHand + " chips with " + hv.toString());
        endHandAndPrepNext();
    }

    public void awardFold(int winnerIndex) {
        if (winnerIndex < 0 || winnerIndex >= players.size()) {
            throw new IllegalArgumentException("Invalid player index");
        }
        Player winner = players.get(winnerIndex);
        winner.setStack(winner.getStack() + potThisHand);
        System.out.printf("Opponent folds; %s wins %d chips.%n", winner.getName(), potThisHand);
        ui.setMsg("Opponent folds, " + winner.getName() + " wins " + potThisHand + " chips.");
        endHandAndPrepNext();
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
        ui.setMsg("Pot " + potThisHand + " split among " + activeCount + " players.");
        endHandAndPrepNext();
    }

    private void resolveShowdown() { //decides winner and awards
        ArrayList<Integer> activeIdx = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).isIn()) {
                activeIdx.add(i);
            }
        }

        if (activeIdx.isEmpty()) {
            return;
        } else if (activeIdx.size() > 2) {
            throw new IllegalArgumentException("More than 2 players detected");
        } else if (activeIdx.size() == 1) {
            awardPotToPlayer(activeIdx.get(0));
            return;
        }
        ArrayList<Card> h1 = players.get(activeIdx.get(0)).getHand().getHand();
        ArrayList<Card> h2 = players.get(activeIdx.get(1)).getHand().getHand();
        int diff = HandEvaluator.compareHands(board.getBoard(), h1, h2);
        if (diff == 0) {
            splitPotBetweenActivePlayers();
        } else if (diff > 0) {
            awardPotToPlayer(activeIdx.get(0));
        } else {
            awardPotToPlayer(activeIdx.get(1));
        }


    }

    private int findLastActivePlayerIndex() {
        int idx = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).isIn()) {
                if (idx != -1) return -1; // more than one still in
                idx = i;
            }
        }
        return idx;
    }

    private void advanceButton() {
        button = (button + 1) % players.size();
    }

    public Player getButtonP() {
        return players.get(button);
    }

    private void endHandAndPrepNext() {
        street = Street.SHOWDOWN;
        potThisHand = 0;
        resetRoundBets();
        advanceButton();
    }
}
