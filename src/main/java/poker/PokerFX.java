package poker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import poker.data.GameState;
import poker.data.LogEvent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import java.util.Comparator;
import java.util.Map;


public class PokerFX extends Application {

    private Table table;


    private ArrayList<ImageView> boardDisplay = new ArrayList<>();

    private ImageView card1Label;
    private ImageView card2Label;

    private ImageView p2Card1Label;
    private ImageView p2Card2Label;

    private Label msg = new Label("Started!");

    private Button potLabel; //pot size
    private Button statusLabel; //current bet, amt to call displayed
    private Button turnLabel; //next to act
    private Button buttonLabel; //who is on button
    private Button equityBtn; //check equity (cheating, uses other player's card info)
    private TextField amountField; //enter amt for bet/raise

    Button checkBtn;
    Button betBtn;
    Button foldBtn;
    Button raiseBtn;
    Button callBtn;


    private Button p1Stack;
    private Button p2Stack;

    private Button load;
    private Button save;

    private Button replay;
    private Button cancelReplay;

    private Image back;

    private boolean faceUp = false;
    private boolean otherFaceUp = false;

    private boolean pendingNextHand = false;

    private ArrayList<Double> equity;

    private boolean equityComputing = false;
    private javafx.concurrent.Task<ArrayList<Double>> equityTask;

    //for writing json
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    //for replay
    private Timeline replayTimeline;
    private List<LogEvent> replayEvents = List.of();
    private int replayIdx = 0;
    private GameState before;
    private boolean replayMode = false;
    private PauseTransition nextHandPause;
    private final int replaySpeedMs = 2000;


    @Override
    public void start(Stage stage) {
        table = new Table(this);  // uses existing engine
        table.startNewHand();

        //current player cards
        card1Label = new ImageView();
        card2Label = new ImageView();

        //other player cards
        p2Card1Label = new ImageView();
        p2Card2Label = new ImageView();

        //board cards
        ImageView flop1Label = new ImageView();
        ImageView flop2Label = new ImageView();
        ImageView flop3Label = new ImageView();
        ImageView turn1Label = new ImageView();
        ImageView river1Label = new ImageView();

        card1Label.setPreserveRatio(true);
        card2Label.setPreserveRatio(true);

        p2Card1Label.setPreserveRatio(true);
        p2Card2Label.setPreserveRatio(true);

        flop1Label.setPreserveRatio(true);
        flop2Label.setPreserveRatio(true);
        flop3Label.setPreserveRatio(true);
        turn1Label.setPreserveRatio(true);
        river1Label.setPreserveRatio(true);


        boardDisplay.add(flop1Label);
        boardDisplay.add(flop2Label);
        boardDisplay.add(flop3Label);
        boardDisplay.add(turn1Label);
        boardDisplay.add(river1Label);

        potLabel     = new Button("Pot: 0");
        statusLabel  = new Button("Current bet: ");

        //2nd row
        turnLabel = new Button("To act: " + table.getCurrentPlayer().getName());
        buttonLabel = new Button("Button: " + table.getButtonP().getName());
        equityBtn = new Button("Equity");

        p1Stack = new Button("P1 Stack: ");
        p2Stack = new Button("P2 Stack: ");

        amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setPrefWidth(80);


        //images
        back = new Image(getClass().getResource("/cards/bicycle_blue@1x.png").toExternalForm());

        //configure board and status buttons
        updateBoard();
        updateStatus();

        // buttons
        checkBtn = new Button("Check");
        betBtn = new Button("Bet");
        foldBtn = new Button("Fold");
        raiseBtn = new Button("Raise By");
        callBtn = new Button("Call");

        Button flipBtn = new Button("Flip");

        Button flipOtherBtn = new Button("Flip Other");

        /*
        Button p1WinsBtn = new Button("P1 wins");
        Button p2WinsBtn = new Button("P2 wins");
        Button splitBtn  = new Button("Split pot");
        Button nextHandBtn = new Button("Next hand");

         */


        // layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);

        HBox otherCards = new HBox(15, p2Card1Label, p2Card2Label);

        HBox cardBox = new HBox(30, card1Label, card2Label, otherCards);
        cardBox.setAlignment(Pos.CENTER);

        HBox boardBox = new HBox(5, flop1Label, flop2Label, flop3Label, turn1Label, river1Label); //boardLabel or card png's
        boardBox.setAlignment(Pos.CENTER);


        HBox actionButtons = new HBox(10, checkBtn, callBtn, betBtn, foldBtn, flipBtn, flipOtherBtn, raiseBtn, amountField);
        actionButtons.setAlignment(Pos.CENTER);


        //HBox showdownBox = new HBox(10, p1WinsBtn, p2WinsBtn, splitBtn, nextHandBtn);
        //showdownBox.setAlignment(Pos.CENTER);

        HBox stacks = new HBox(10, p1Stack, potLabel, p2Stack);
        stacks.setAlignment(Pos.CENTER);

        msg.setFont(Font.font(18)); // bigger
        msg.setStyle(
                "-fx-background-color: rgba(255,255,255,0.85);" +
                        "-fx-padding: 10;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: rgba(0,0,0,0.2);"
        );



        HBox turns = new HBox(10, turnLabel, buttonLabel, equityBtn);
        turns.setAlignment(Pos.CENTER);


        //save, load, replay
        save = new Button("Save");
        load = new Button("Load");
        replay = new Button("Replay");
        cancelReplay = new Button("Cancel Replay");
        HBox sl = new HBox(10, save, load, replay, cancelReplay);


        root.getChildren().addAll(
                boardBox,
                stacks,
                turns,
                actionButtons,
                statusLabel,
                cardBox,
                msg,
                sl
        );

        checkBtn.setOnAction(e -> check());
        betBtn.setOnAction(e -> bet());
        raiseBtn.setOnAction(e -> raise());
        foldBtn.setOnAction(e -> fold());
        callBtn.setOnAction(e -> call());
        flipBtn.setOnAction(e -> onFlip());
        flipOtherBtn.setOnAction(e -> onOtherFlip());

        //save, load, replay button actions
        save.setOnAction(e -> save());
        load.setOnAction(e -> load());
        replay.setOnAction(e -> replay());
        cancelReplay.setOnAction(e -> cancelReplay());
/*
        p1WinsBtn.setOnAction(e -> {
            table.awardPotToPlayer(0);
            refreshUI();
        });

        p2WinsBtn.setOnAction(e -> {
            table.awardPotToPlayer(1);
            refreshUI();
        });

        splitBtn.setOnAction(e -> {
            table.splitPotBetweenActivePlayers();
            refreshUI();
        });



        nextHandBtn.setOnAction(e -> {
            table.startNewHand();
            refreshUI();
        });

 */

        equityBtn.setOnAction(e -> onEquity());


        root.setStyle("-fx-background-color: darkgreen;");
        Scene scene = new Scene(root, 600, 450);

        //makes the layout stretch
        root.setFillWidth(true);
        boardBox.setMaxWidth(Double.MAX_VALUE);
        cardBox.setMaxWidth(Double.MAX_VALUE);
        actionButtons.setMaxWidth(Double.MAX_VALUE);
        stacks.setMaxWidth(Double.MAX_VALUE);
        turns.setMaxWidth(Double.MAX_VALUE);

        //scales cards with window size
        var boardCardH = scene.widthProperty().divide(7.5);   // 600/7.5 = 80px at start
        var handCardH  = scene.widthProperty().divide(6.0);   // 600/6 = 100px at start
        var otherHandCardH = scene.widthProperty().divide(12.0);

        for (ImageView iv : boardDisplay) {
            iv.fitHeightProperty().bind(boardCardH);
        }

        card1Label.fitHeightProperty().bind(handCardH);
        card2Label.fitHeightProperty().bind(handCardH);

        p2Card1Label.fitHeightProperty().bind(otherHandCardH);
        p2Card2Label.fitHeightProperty().bind(otherHandCardH);


        stage.setTitle("Poker GUI");
        stage.setScene(scene);
        stage.show();

    }

    @Override
    public void stop() {
        if (table != null) {
            table.shutdown();
        }
    }

    public void save() {
        cancelEquityIfRunning();

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Poker Game");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Poker Save (JSON)", "*.json"));
        fc.setInitialFileName("poker_save_" + Instant.now().toString() + ".json");

        File f = fc.showSaveDialog(msg.getScene().getWindow());
        if (f == null) return;

        try {
            GameState state = table.toState();

            mapper.writeValue(f, state);
            setMsg("Saved: " + f.getName());
        } catch (Exception ex) {
            setMsg("Save failed: " + ex.getMessage());
        }
    }

    public void load(){
        cancelEquityIfRunning();

        FileChooser fc = new FileChooser();
        fc.setTitle("Load Poker Game");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Poker Save (JSON)", "*.json"));

        File f = fc.showOpenDialog(msg.getScene().getWindow());
        if (f == null) return;

        try {
            GameState state = mapper.readValue(f, GameState.class);

            // reset UI only flags so loaded state displays predictably
            faceUp = false;
            otherFaceUp = false;
            pendingNextHand = false;

            table.loadState(state);

            refreshUI();
            setMsg("Loaded: " + f.getName());
        } catch (Exception ex) {
            setMsg("Load failed: " + ex.getMessage());
        }
    }
    private void replay() {
        if (nextHandPause != null) nextHandPause.stop();
        pendingNextHand = false;

        cancelEquityIfRunning();

        // if already replaying, ignore
        if (replayTimeline != null) replayTimeline.stop();

        before = table.toState();

        FileChooser fc = new FileChooser();
        fc.setTitle("Replay Poker Session");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Poker Log (JSONL)", "*.jsonl")
        );

        File f = fc.showOpenDialog(msg.getScene().getWindow());
        if (f == null) return;

        try {
            // parse log events
            replayEvents = readJsonlEvents(f.toPath());
            replayEvents.sort(Comparator
                    .comparingInt(LogEvent::handId)
                    .thenComparingInt(e -> e.seq() == null ? -1 : e.seq())
                    .thenComparing(LogEvent::ts));

            if (replayEvents.isEmpty()) {
                setMsg("Replay file had no events.");
                return;
            }

            // reset UI flags so state displays predictably
            faceUp = false;
            otherFaceUp = false;
            pendingNextHand = false;

            replayIdx = 0;
            replayMode = true;
            setReplayMode(true);

            startReplayTimeline(replaySpeedMs); // ms per event, tune later
            setMsg("Replaying: " + f.getName());

        } catch (Exception ex) {
            replayMode = false;
            setReplayMode(false);
            setMsg("Replay failed: " + ex.getMessage());
        }
    }


    private void cancelReplay() {
        if (!replayMode) {
            return;
        }

        if (replayTimeline != null) {
            replayTimeline.stop();
            replayTimeline = null;
        }

        replayMode = false;
        setReplayMode(false);

        if (before != null) {
            table.loadState(before);
            faceUp = false;
            otherFaceUp = false;
            pendingNextHand = false;
            refreshUI();
        }

        setMsg("Replay cancelled");
    }

    private void finishReplay() {
        if (replayTimeline != null) {
            replayTimeline.stop();
            replayTimeline = null;
        }
        replayMode = false;
        setReplayMode(false);

        // restore the state from before replay
        /*
        if (before != null) {
            table.loadState(before);
            refreshUI();
        }
         */

        setMsg("Replay finished");
    }



    private void setReplayMode(boolean on) {
        foldBtn.setDisable(on);
        checkBtn.setDisable(on);
        callBtn.setDisable(on);
        betBtn.setDisable(on);
        raiseBtn.setDisable(on);
        save.setDisable(on);
        load.setDisable(on);
        amountField.setDisable(on);

        replay.setDisable(on);   // prevent starting a second replay
        cancelReplay.setDisable(!on); // only enabled during replay
    }

    private void onEquity() {
        if (equityComputing) return;          // prevents spamming
        equityComputing = true;

        equityBtn.setDisable(true);
        setMsg("Computing equity...");

        equityTask = new javafx.concurrent.Task<>() {
            @Override
            protected ArrayList<Double> call() {
                // heavy work happens here, NOT on the UI thread
                return table.currEquity();
            }
        };

        equityTask.setOnSucceeded(ev -> {
            equityComputing = false;
            equityBtn.setDisable(false);

            equity = equityTask.getValue();
            equityTask = null;
            setMsg(equityMsg(equity));
        });

        equityTask.setOnFailed(ev -> {
            equityComputing = false;
            equityBtn.setDisable(false);

            Throwable ex = equityTask.getException();
            equityTask = null;
            setMsg("Equity failed: " + (ex == null ? "unknown error" : ex.getMessage()));
        });

        equityTask.setOnCancelled(ev -> {
            equityComputing = false;
            equityBtn.setDisable(false);
            equityTask = null;
            setMsg("Equity cancelled");
        });

        Thread t = new Thread(equityTask);
        t.setDaemon(true);   // so it won’t keep app alive on exit
        t.start();
    }

    private void updatePlayers() {
        List<Player> ps = table.getPlayers();
        if (ps.size() < 2) {
            return;
        }

        if (!otherFaceUp) {
            p2Card1Label.setImage(back);
            p2Card2Label.setImage(back);
        } else {
            ArrayList<Card> pCards = table.getOtherPlayer().getHand().getHand();
            Image img1 = new Image(getClass().getResource("/cards/" + (pCards.get(0).getRank() + 1) + pCards.get(0).getSuitLetter() + "@1x.png").toExternalForm());
            Image img2 = new Image(getClass().getResource("/cards/" + (pCards.get(1).getRank() + 1) + pCards.get(1).getSuitLetter()  + "@1x.png").toExternalForm());
            p2Card1Label.setImage(img1);
            p2Card2Label.setImage(img2);
        }

        if (!faceUp) {
            card1Label.setImage(back);
            card2Label.setImage(back);
        } else {
            ArrayList<Card> pCards = table.getCurrentPlayer().getHand().getHand();
            Image img1 = new Image(getClass().getResource("/cards/" + (pCards.get(0).getRank() + 1) + pCards.get(0).getSuitLetter() + "@1x.png").toExternalForm());
            Image img2 = new Image(getClass().getResource("/cards/" + (pCards.get(1).getRank() + 1) + pCards.get(1).getSuitLetter()  + "@1x.png").toExternalForm());
            card1Label.setImage(img1);
            card2Label.setImage(img2);
        }

        String prefix = replayMode ? "Next: " : "To act: ";
        turnLabel.setText(prefix + table.getCurrentPlayer().getName());

        turnLabel.setText(prefix + table.getCurrentPlayer().getName());

    }

    private void updateBoard() {
        p1Stack.setText(table.getPlayers().get(0).getName() + " Stack: " + table.getPlayers().get(0).getStack());
        p2Stack.setText(table.getPlayers().get(1).getName() + " Stack: " + table.getPlayers().get(1).getStack());
        int i = 0;
        for (Card c : table.getBoard().getBoard()) {
            boardDisplay.get(i).setImage(new Image(getClass().getResource("/cards/" + (c.getRank() + 1) + c.getSuitLetter() + "@1x.png").toExternalForm()));
            i++;
        }
        for (int j = i; j < 5; j++) {
            boardDisplay.get(j).setImage(back);
        }

        potLabel.setText("Pot: " + table.getPotThisHand());
        updatePlayers();
    }


    private void updateStatus() {
        String prefix = replayMode ? "Next: " : "To act: ";
        turnLabel.setText(prefix + table.getCurrentPlayer().getName());

        turnLabel.setText(prefix + table.getCurrentPlayer().getName());
        buttonLabel.setText("Button: " + table.getButtonP().getName());
        int toCall = table.getToCallForCurrentPlayer();
        statusLabel.setText(
                "Street: " + table.getStreet()
                        + " | Current bet: " + table.getCurrentBet()
                        + " | To call: " + toCall
        );
    }

    private String equityMsg(ArrayList<Double> eq) {
        double win = eq.get(0) * 100.0;
        double draw = eq.get(1) * 100.0;
        double total = win + draw / 2.0;
        return String.format(
                "Win equity: %.2f%%, Draw equity: %.2f%%\nTotal equity: %.2f%%", win, draw, total);
    }

    private void refreshUI() {
        updateBoard();
        updateStatus();
    }


    public static void main(String[] args) {
        launch(args);
    }

    private void check() {
        cancelEquityIfRunning();
        table.applyActionFromUI(Action.CHECK, 0);
        refreshUI();
        autoNextHandIfOver();
    }

    private void call() {
        cancelEquityIfRunning();
        table.applyActionFromUI(Action.CALL, 0);
        refreshUI();
        autoNextHandIfOver();
    }

    private void bet() {
        int amt = parseAmount(amountField);
        if (amt > 0) {
            cancelEquityIfRunning();
            table.applyActionFromUI(Action.BET, amt);
            refreshUI();
        }
        autoNextHandIfOver();
    }

    private void raise() {
        int amt = parseAmount(amountField);
        if (amt > 0) {
            cancelEquityIfRunning();
            table.applyActionFromUI(Action.RAISE, amt);
            refreshUI();
        }
        autoNextHandIfOver();
    }

    private void fold() {
        cancelEquityIfRunning();
        table.applyActionFromUI(Action.FOLD, 0);
        refreshUI();
        autoNextHandIfOver();
    }

    private void onFlip() {
        faceUp = !faceUp;
        refreshUI();
    }

    private void onOtherFlip() {
        otherFaceUp = !otherFaceUp;
        refreshUI();
    }

    private int parseAmount(TextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    public void setMsg(String s) {
        msg.setText(s);
    }

    private void autoNextHandIfOver() { //sets on a timer so ui refreshes smoothly
        if (table.getStreet() != Table.Street.SHOWDOWN || pendingNextHand || replayMode) {
            return;
        }
        pendingNextHand = true;

        if (nextHandPause != null) nextHandPause.stop();

        nextHandPause = new PauseTransition(Duration.millis(6000));
        nextHandPause.setOnFinished(e -> {
            if (replayMode) return;
            cancelEquityIfRunning();
            pendingNextHand = false;
            setMsg("New hand!");
            table.startNewHand();
            refreshUI();
        });
        nextHandPause.play();
    }

    private void cancelEquityIfRunning() {
        if (equityTask != null && equityTask.isRunning()) {
            equityTask.cancel();
        }
    }

    public boolean isReplayMode() {
        return replayMode;
    }

    private List<LogEvent> readJsonlEvents(Path p) throws Exception {
        List<LogEvent> out = new ArrayList<>();
        try (var lines = Files.lines(p)) {
            lines.forEach(line -> {
                if (line == null || line.isBlank()) return;
                try {
                    out.add(mapper.readValue(line, LogEvent.class));
                } catch (Exception e) {
                    throw new RuntimeException("Bad log line: " + line, e);
                }
            });
        }
        return out;
    }

    private void startReplayTimeline(int msPerEvent) {
        if (replayTimeline != null) replayTimeline.stop();

        replayTimeline = new Timeline(new KeyFrame(Duration.millis(msPerEvent), e -> stepReplay()));
        replayTimeline.setCycleCount(Timeline.INDEFINITE);
        replayTimeline.playFromStart();
    }

    private void stepReplay() {
        if (replayIdx >= replayEvents.size()) {
            finishReplay();
            return;
        }

        LogEvent ev = replayEvents.get(replayIdx++);
        applyReplaySnapshot(ev);
        refreshUI();

        setMsg(formatReplayMsg(ev));
    }

    private String formatReplayMsg(LogEvent ev) {
        String streetTxt = ev.street() == null ? "" : (" " + ev.street());

        if ("ACTION".equals(ev.eventType())) {
            String who = ev.playerName() == null ? "?" : ev.playerName();
            String act = ev.action() == null ? "?" : ev.action();

            if ("BET".equals(act) || "RAISE".equals(act) || "CALL".equals(act)) {
                int amt = ev.amountPaid() == null ? 0 : ev.amountPaid();
                return who + " " + act + " " + amt + " " + streetTxt;
            }

            return who + " " + act + " " + streetTxt;
        }

        // system events like STREET_ADVANCE, HAND_START, HAND_END
        String act = ev.action() == null ? ev.eventType() : ev.action();
        return act + streetTxt;
    }


    private void applyReplaySnapshot(LogEvent ev) {
        // prefer AFTER snapshots, fall back to BEFORE if after is null
        var stacks = ev.stacksAfter() != null ? ev.stacksAfter() : ev.stacksBefore();
        var bets   = ev.betsAfter()   != null ? ev.betsAfter()   : ev.betsBefore();
        var board  = ev.board(); // your JSON uses "board": [...]

        table.applyReplayFrame(
                ev.street(),
                ev.potAfter() != null ? ev.potAfter() : ev.potBefore(),
                ev.currentBetAfter() != null ? ev.currentBetAfter() : ev.currentBetBefore(),
                stacks,
                bets,
                board,
                ev.holeCards(),
                ev.buttonIdx(),
                ev.toActIdx()
        );

        faceUp = true;
        otherFaceUp = true;
    }


}
