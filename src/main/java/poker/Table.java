package poker;

import poker.data.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

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
    private int actionsThisStreet = 0;
    private int lastRaiseIncrement;
    private long seed;

    private int handId = -1; //or 0 for 1-indexing; starting a new hand increments by 1
    private String sessionId;
    private LogWriter logger;


    private PokerFX ui;

    private Street street = Street.PREFLOP;


    private int currentBet = 0;     // highest committed amount this round
    private int potThisHand = 0;    // total pot for this hand
    private int currentPlayerIndex = 0;

    private Integer seq = 0;




    //private final HandEvaluator handEval = new HandEvaluator();

    public Table(PokerFX ui) {
        this.ui = ui;
        long seed = System.nanoTime();
        this.seed = seed;
        deck = new Deck(seed);
        board = new Board(deck); //hardcode seed for consistency
        button = 0;
        players = new ArrayList<>();
        players.add(new Player("p1"));
        players.add(new Player("p2"));
        sessionId = UUID.randomUUID().toString(); //assign unique sessionId
        Path logPath = Paths.get("logs", "pokerfx_" + sessionId + ".jsonl");
        try {
            this.logger = new LogWriter(logPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create logger", e);
        }
    }

    public int getMinRaise() {
        return lastRaiseIncrement;
    }

    public void startNewHand() {
        System.out.println("=== START NEW HAND === button=" + button
                + " stacks=" + players.get(0).getStack() + "," + players.get(1).getStack());
        // New deck and board for each hand
        handId++;
        seq = 0;

        seed = System.nanoTime();
        deck = new Deck(seed);
        board = new Board(deck);

        potThisHand = 0;
        currentBet = 0;
        //roundOver = false;
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

        LogEvent e = new LogEvent(
                Instant.now().toString(),
                sessionId,
                handId,
                seq,
                "HAND_START", //eventType
                null, //street
                button, //buttonidx
                currentPlayerIndex, //toActIdx
                null, //playerName
                null, //playerIndex
                null, //action
                null, //amount paid
                null, //raiseIncrement
                null, //toCallBefore
                null, //lastRaiseBefore
                potThisHand, //potBefore
                potThisHand, //potAfter
                0, //currentBetBefore
                currentBet,
                snapshotStacks(),
                snapshotBets(), //betsBefore
                null, //stacksAfter
                null, //betsAfter
                snapshotBoard(),
                snapshotHoleCards()
        );
        logger.append(e);
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
        if (handId < 0 || potThisHand == 0 || street == Street.SHOWDOWN) {
            return;
        }
        Player p = getCurrentPlayer();
        int toCall = getToCallForCurrentPlayer();

        boolean performed = false; // only true if the action actually happens

        //all prev fields, kept for logging
        int potBefore = potThisHand;
        int currentBetBefore = currentBet;
        int actionsBefore = actionsThisStreet;
        int lastRaiseBefore = lastRaiseIncrement;
        Street streetBefore = street;
        int playerBetBefore = p.getBet();
        int playerStackBefore = p.getStack();
        int toCallBefore = toCall;
        int amountPaid = 0;
        int raiseIncrement = 0;
        int playerIndex = currentPlayerIndex;

        var stacksBefore = snapshotStacks();
        var betsBefore = snapshotBets();



        switch (type) {
            case FOLD -> {
                p.out();
                System.out.printf("%s folds.%n", p.getName());
                ui.setMsg(p.getName() + " folds.");
                performed = true;
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
                amountPaid = pay;
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
                amountPaid = amount;
                raiseIncrement = amount;
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
                amountPaid = need;
                raiseIncrement = amount;
            }
        }

        if (!performed) {
            // invalid action, do not move turn or change street

            return;
        }

        if (type == Action.FOLD) {
            seq++;
            LogEvent e = new LogEvent(
                    Instant.now().toString(),
                    sessionId,
                    handId,
                    seq,
                    "ACTION",
                    street.name(),
                    button,
                    null,
                    p.getName(),
                    playerIndex,
                    type.name(),
                    amountPaid,
                    raiseIncrement,
                    toCallBefore,
                    lastRaiseBefore,
                    potBefore,
                    potThisHand,
                    currentBetBefore,
                    currentBet,
                    stacksBefore,
                    betsBefore,
                    snapshotStacks(),
                    snapshotBets(),
                    snapshotBoard(),
                    snapshotHoleCards()
            );
            logger.append(e);

            int winner = findLastActivePlayerIndex();
            if (winner != -1) awardFold(winner);
            return;
        }

        actionsThisStreet++;

        // move to next player if hand not over yet
        if (!isHandOver() && street != Street.SHOWDOWN) {
            advanceToNextPlayer();
        }

        //log
        seq++;
        LogEvent e = new LogEvent(
                Instant.now().toString(),
                sessionId,
                handId,
                seq,
                "ACTION",
                streetBefore.name(),
                button,
                currentPlayerIndex,
                p.getName(),
                playerIndex,
                type.name(),
                amountPaid,
                raiseIncrement,
                toCallBefore,
                lastRaiseBefore,
                potBefore,
                potThisHand,
                currentBetBefore,
                currentBet,
                stacksBefore,
                betsBefore,
                snapshotStacks(),
                snapshotBets(),
                snapshotBoard(),
                snapshotHoleCards()
        );


        try {
            logger.append(e);
        } catch (Exception ex) {
            System.out.println("LOGGING FAILED: " + ex.getMessage());
        }

        // possibly end betting round or hand
        maybeAdvanceAfterAction();

    }

    private Map<String, Integer> snapshotStacks() {
        Map<String, Integer> stacks = new HashMap<>();
        for (Player p : players) {
            stacks.put(p.getName(), p.getStack());
        }
        return stacks;
    }

    private Map<String, Integer> snapshotBets() {
        Map<String, Integer> bets = new HashMap<>();
        for (Player p : players) {
            bets.put(p.getName(), p.getBet());
        }
        return bets;
    }

    private List<String> snapshotBoard() {
        List<String> board = new ArrayList<>();
        for (Card c : this.board.getBoard()) {
            board.add(c.toString());
        }
        return board;
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
        //before fields
        Street beforeStreet = street;
        int potBefore = potThisHand;
        int currentBetBefore = currentBet;
        var stacksBefore = snapshotStacks();
        var betsBefore = snapshotBets();
        var boardBefore = snapshotBoard();
        int minRaiseBefore = lastRaiseIncrement;


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
            }
            default -> {
                return;
            }
        }

        if (street != Street.SHOWDOWN) {
            resetBetsForNewStreet();
        }

        if (street == Street.FLOP || street == Street.TURN || street == Street.RIVER) {
            currentPlayerIndex = button; // button acts first postflop, heads up
        }

        logSystemEvent(
                "STREET_ADVANCE",
                beforeStreet.name() + "_TO_" + street.name(),
                beforeStreet,
                minRaiseBefore,
                potBefore,
                currentBetBefore,
                stacksBefore,
                betsBefore,
                boardBefore
        );

        if (street == Street.SHOWDOWN) {
            resolveShowdown();
        }
    }

    private void logSystemEvent(String eventType, String action, Street streetBefore, int minRaiseBefore,
                                int potBefore, int currentBetBefore,
                                Map<String, Integer> stacksBefore, Map<String, Integer> betsBefore,
                                List<String> boardBefore) {
        seq++;
        LogEvent e = new LogEvent(
                Instant.now().toString(),
                sessionId,
                handId,
                seq,
                eventType,
                street.name(),
                button,
                currentPlayerIndex, //toActIdx
                "SYSTEM",
                null,
                action,
                0,
                0,
                0,
                minRaiseBefore,
                potBefore,
                potThisHand,
                currentBetBefore,
                currentBet,
                stacksBefore,
                betsBefore,
                snapshotStacks(),
                snapshotBets(),
                snapshotBoard(),
                snapshotHoleCards()
        );

        try {
            logger.append(e);
        } catch (Exception ex) {
            System.out.println("LOGGING FAILED: " + ex.getMessage());
        }
    }


    private void maybeAdvanceAfterAction() {
        if (isBettingRoundComplete()) {
            advanceStreet();
        }
    }

    public void awardPotToPlayer(int winnerIndex) {
        if (winnerIndex < 0 || winnerIndex >= players.size()) {
            throw new IllegalArgumentException("Invalid player index");
        }

        Player winner = players.get(winnerIndex);
        Map<String, Integer> stacksBefore = snapshotStacks();
        winner.setStack(winner.getStack() + potThisHand);
        ArrayList<Card> seven = new ArrayList<>(winner.getHand().getHand());
        seven.addAll(board.getBoard());
        HandEvaluator.HandValue hv = HandEvaluator.eval5(HandEvaluator.bestHand(seven));
        System.out.printf("%s wins %d chips with %s.%n", winner.getName(), potThisHand, hv.toString());
        ui.setMsg(winner.getName() + " wins " + potThisHand + " chips with " + hv.toString());
        endHandAndPrepNext(stacksBefore);
    }

    public void awardFold(int winnerIndex) {
        if (winnerIndex < 0 || winnerIndex >= players.size()) {
            throw new IllegalArgumentException("Invalid player index");
        }
        Player winner = players.get(winnerIndex);
        Map<String, Integer> stacksBefore = snapshotStacks();
        winner.setStack(winner.getStack() + potThisHand);
        System.out.printf("Opponent folds; %s wins %d chips.%n", winner.getName(), potThisHand);
        ui.setMsg("Opponent folds, " + winner.getName() + " wins " + potThisHand + " chips.");
        endHandAndPrepNext(stacksBefore);
    }

    public void splitPotBetweenActivePlayers() {
        // simple 2-player split for now
        Map<String, Integer> stacksBefore = snapshotStacks();
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
        endHandAndPrepNext(stacksBefore);
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

    private void endHandAndPrepNext(Map<String, Integer> stacksBefore) {
        seq++;
        LogEvent e = new LogEvent(
                Instant.now().toString(),
                sessionId,
                handId,
                seq,
                "HAND_END", //eventType
                null, //street
                button,
                null, //toActIdx
                null, //playerName
                null, //playerIndex
                null, //action
                null, //amount paid
                null, //raiseIncrement
                null, //toCallBefore
                null, //lastRaiseBefore
                potThisHand, //potBefore
                0, //potAfter
                0, //currentBetBefore
                0, //currentBetAfter
                stacksBefore, //stacksBefore
                snapshotBets(), //betsBefore
                snapshotStacks(), //stacksAfter
                null, //betsAfter
                snapshotBoard(),
                snapshotHoleCards()
        );
        logger.append(e);
        street = Street.SHOWDOWN;
        potThisHand = 0;
        resetRoundBets();
        advanceButton();
    }

    public GameState toState() {
        GameState gs = new GameState();

        //table related
        gs.button = this.button;
        gs.currentPlayerIndex = this.currentPlayerIndex;
        gs.lastRaiseIncrement = this.lastRaiseIncrement;
        gs.actionsThisStreet = this.actionsThisStreet;
        gs.street = this.street.name();
        gs.potThisHand = this.potThisHand;
        gs.currentBet = this.currentBet;

        //player, board, deck
        gs.players = PlayerDTO.fromPlayers(players);
        gs.nextIndex = board.getDeck().getNextIndex();
        gs.deck = GameState.dtoDeck(board.getDeck().getDeck());
        gs.board = CardDTO.fromCards(getBoard().getBoard());
        gs.seed = seed;

        return gs;
    }

    public void loadState(GameState gs) {
        //load players
        PlayerDTO.applyToPlayers(gs.players, players);

        //load board, deck, seed
        board.clear();
        for (Card c : CardDTO.toCards(gs.board)) {
            board.addCard(c);
        }
        seed = gs.seed;
        board.setDeck(GameState.fromdtoDeck(gs.deck), seed);
        board.getDeck().setNextIndex(gs.nextIndex);


        //load table
        button = gs.button;
        currentPlayerIndex = gs.currentPlayerIndex;
        lastRaiseIncrement = gs.lastRaiseIncrement;
        actionsThisStreet = gs.actionsThisStreet;
        potThisHand = gs.potThisHand;
        currentBet = gs.currentBet;
        street = Street.valueOf(gs.street);

    }

    public void shutdown() {
        try {
            if (logger != null) {
                logger.close();
            }
        } catch (Exception e) {
            System.out.println("Could not close properly");
            //e.printStackTrace();
        }
    }


    public void applyReplayFrame(String streetName,
                                 Integer potThisHand,
                                 Integer currentBet,
                                 Map<String,Integer> stacks,
                                 Map<String,Integer> bets,
                                 List<String> boardStrs,
                                 Map<String, List<String>> holeCards,
                                 Integer buttonIdx, Integer toActIdx) {
        if (buttonIdx != null) this.button = buttonIdx;
        if (toActIdx != null) this.currentPlayerIndex = toActIdx;

        if (streetName != null) this.street = Street.valueOf(streetName);
        if (potThisHand != null) this.potThisHand = potThisHand;
        if (currentBet != null) this.currentBet = currentBet;

        if (stacks != null || bets != null) {
            for (Player p : players) {
                if (stacks != null && stacks.containsKey(p.getName())) p.setStack(stacks.get(p.getName()));
                if (bets != null && bets.containsKey(p.getName())) p.setBet(bets.get(p.getName()));
            }
        }

        if (boardStrs != null) {
            board.clear();
            for (String cs : boardStrs) {
                board.addCard(Card.fromString(cs));
            }
        }

        if (holeCards != null) {
            for (Player p : players) {
                List<String> cs = holeCards.get(p.getName());
                if (cs == null) continue;

                p.clearHand();
                for (String s : cs) {
                    p.deal(Card.fromString(s));
                }
            }
        }

    }

    private Map<String, List<String>> snapshotHoleCards() {
        Map<String, List<String>> out = new HashMap<>();
        for (Player p : players) {
            List<String> cards = new ArrayList<>();
            for (Card c : p.getHand().getHand()) {
                cards.add(c.toString());
            }
            out.put(p.getName(), cards);
        }
        return out;
    }

}
