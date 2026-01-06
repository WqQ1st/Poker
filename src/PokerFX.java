import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

import java.util.List;

public class PokerFX extends Application {

    private Table table;

    private Label player1Label;
    private Label player2Label;
    private Label boardLabel;
    private Label potLabel;
    private Label statusLabel;
    private Label turnLabel;
    private TextField amountField;
    private int status = 0; //pre, flop, turn, river

    @Override
    public void start(Stage stage) {
        table = new Table();  // uses your existing engine
        table.startNewHand();
        table.setupBlindsForNewHand();

        // labels
        player1Label = new Label("Player 1: ");
        player2Label = new Label("Player 2: ");
        boardLabel   = new Label("Board: (no cards yet)");
        potLabel     = new Label("Pot: 0");
        statusLabel  = new Label("Current bet: ");
        turnLabel = new Label("To act: " + table.getCurrentPlayer().getName());

        amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setPrefWidth(80);

        updateBoard();
        updateStatus();


        // buttons


        Button checkBtn = new Button("Check");
        Button betBtn = new Button("Bet");
        Button foldBtn = new Button("Fold");
        Button raiseBtn = new Button("Raise");
        Button callBtn = new Button("Call");

        Button p1WinsBtn = new Button("P1 wins");
        Button p2WinsBtn = new Button("P2 wins");
        Button splitBtn  = new Button("Split pot");
        Button nextHandBtn = new Button("Next hand");

        // layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);

        HBox playersBox = new HBox(30, player1Label, player2Label);
        playersBox.setAlignment(Pos.CENTER);

        HBox boardBox = new HBox(boardLabel);
        boardBox.setAlignment(Pos.CENTER);


        HBox actionButtons = new HBox(10, checkBtn, callBtn, betBtn, raiseBtn, foldBtn, amountField);
        actionButtons.setAlignment(Pos.CENTER);


        HBox showdownBox = new HBox(10, p1WinsBtn, p2WinsBtn, splitBtn, nextHandBtn);
        showdownBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(
                playersBox,
                boardBox,
                potLabel,
                turnLabel,
                actionButtons,
                showdownBox,
                statusLabel
        );


        // for now, just placeholders so buttons don't do nothing
        checkBtn.setOnAction(e -> check());
        betBtn.setOnAction(e -> bet());
        raiseBtn.setOnAction(e -> raise());
        foldBtn.setOnAction(e -> fold());
        callBtn.setOnAction(e -> call());

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



        Scene scene = new Scene(root, 700, 300);
        stage.setTitle("Poker GUI");
        stage.setScene(scene);
        stage.show();
    }

    private void updatePlayers() {
        List<Player> ps = table.getPlayers();
        if (ps.size() >= 2) {
            player1Label.setText(ps.get(0).toString());
            player2Label.setText(ps.get(1).toString());
        }
        turnLabel.setText("To act: " + table.getCurrentPlayer().getName());

    }

    private void updateBoard() {
        boardLabel.setText("Board: " + table.getBoard().toString());
        potLabel.setText("Pot: " + table.getPotThisHand());
        updatePlayers();
    }


    private void updateStatus() {
        turnLabel.setText("To act: " + table.getCurrentPlayer().getName());
        int toCall = table.getToCallForCurrentPlayer();
        statusLabel.setText(
                "Street: " + table.getStreet()
                        + " | Current bet: " + table.getCurrentBet()
                        + " | To call: " + toCall
        );
    }

    private void refreshUI() {
        updateBoard();
        updateStatus();
    }


    public static void main(String[] args) {
        launch(args);
    }

    private void check() {
        table.applyActionFromUI(Action.CHECK, 0);
        refreshUI();
    }

    private void call() {
        table.applyActionFromUI(Action.CALL, 0);
        refreshUI();
    }

    private void bet() {
        int amt = parseAmount(amountField);
        if (amt > 0) {
            table.applyActionFromUI(Action.BET, amt);
            refreshUI();
        }
    }

    private void raise() {
        int amt = parseAmount(amountField);
        if (amt > 0) {
            table.applyActionFromUI(Action.RAISE, amt);
            refreshUI();
        }
    }

    private void fold() {
        table.applyActionFromUI(Action.FOLD, 0);
        refreshUI();
    }

    private int parseAmount(TextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception ex) {
            // can show an error popup later, for now just ignore bad input
            return 0;
        }
    }
}
