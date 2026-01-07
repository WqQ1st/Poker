import javafx.animation.PauseTransition;
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
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PokerFX extends Application {

    private Table table;

    private ArrayList<ImageView> boardDisplay = new ArrayList<>();

    private ImageView card1Label;
    private ImageView card2Label;

    private Label msg = new Label("Started!");

    private Button potLabel; //pot size
    private Button statusLabel; //current bet, amt to call displayed
    private Button turnLabel; //next to act
    private Button buttonLabel; //who is on button
    private TextField amountField; //enter amt for bet/raise

    private Button p1Stack;
    private Button p2Stack;

    private Image back;

    private boolean faceUp = false;

    private boolean pendingNextHand = false;

    @Override
    public void start(Stage stage) {
        table = new Table(this);  // uses existing engine
        table.startNewHand();

        //player cards
        card1Label = new ImageView();
        card2Label = new ImageView();

        //board cards
        ImageView flop1Label = new ImageView();
        ImageView flop2Label = new ImageView();
        ImageView flop3Label = new ImageView();
        ImageView turn1Label = new ImageView();
        ImageView river1Label = new ImageView();

        card1Label.setPreserveRatio(true);
        card2Label.setPreserveRatio(true);

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
        turnLabel = new Button("To act: " + table.getCurrentPlayer().getName());
        buttonLabel = new Button("Button: " + table.getButtonP().getName());

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
        Button checkBtn = new Button("Check");
        Button betBtn = new Button("Bet");
        Button foldBtn = new Button("Fold");
        Button raiseBtn = new Button("Raise By");
        Button callBtn = new Button("Call");

        Button flipBtn = new Button("Flip");

        Button p1WinsBtn = new Button("P1 wins");
        Button p2WinsBtn = new Button("P2 wins");
        Button splitBtn  = new Button("Split pot");
        Button nextHandBtn = new Button("Next hand");

        // layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);

        HBox cardBox = new HBox(30, card1Label, card2Label);
        cardBox.setAlignment(Pos.CENTER);

        HBox boardBox = new HBox(5, flop1Label, flop2Label, flop3Label, turn1Label, river1Label); //boardLabel or card png's
        boardBox.setAlignment(Pos.CENTER);


        HBox actionButtons = new HBox(10, checkBtn, callBtn, betBtn, foldBtn, flipBtn, raiseBtn, amountField);
        actionButtons.setAlignment(Pos.CENTER);


        HBox showdownBox = new HBox(10, p1WinsBtn, p2WinsBtn, splitBtn, nextHandBtn);
        showdownBox.setAlignment(Pos.CENTER);

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

        HBox turns = new HBox(10, turnLabel, buttonLabel);
        turns.setAlignment(Pos.CENTER);

        root.getChildren().addAll(
                boardBox,
                stacks,
                turns,
                actionButtons,
                statusLabel,
                cardBox,
                msg
        );

        checkBtn.setOnAction(e -> check());
        betBtn.setOnAction(e -> bet());
        raiseBtn.setOnAction(e -> raise());
        foldBtn.setOnAction(e -> fold());
        callBtn.setOnAction(e -> call());
        flipBtn.setOnAction(e -> {
            flip();
            refreshUI();
        });

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

        for (ImageView iv : boardDisplay) {
            iv.fitHeightProperty().bind(boardCardH);
        }

        card1Label.fitHeightProperty().bind(handCardH);
        card2Label.fitHeightProperty().bind(handCardH);


        stage.setTitle("Poker GUI");
        stage.setScene(scene);
        stage.show();

    }

    private void updatePlayers() {
        List<Player> ps = table.getPlayers();
        if (ps.size() < 2) {
            return;
        }
        if (!faceUp) {
            card1Label.setImage(back);
            card2Label.setImage(back);
        } else {
            ArrayList<Card> pCards = table.getCurrentPlayer().getHand().getHand();
            //System.out.print(pCards);
            Image img1 = new Image(getClass().getResource("/cards/" + (pCards.get(0).getRank() + 1) + pCards.get(0).getSuitLetter() + "@1x.png").toExternalForm());
            Image img2 = new Image(getClass().getResource("/cards/" + (pCards.get(1).getRank() + 1) + pCards.get(1).getSuitLetter()  + "@1x.png").toExternalForm());
            card1Label.setImage(img1);
            card2Label.setImage(img2);
        }


        turnLabel.setText("To act: " + table.getCurrentPlayer().getName());

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

    public void flip() {
        faceUp = !faceUp;
    }


    private void updateStatus() {
        turnLabel.setText("To act: " + table.getCurrentPlayer().getName());
        buttonLabel.setText("Button: " + table.getButtonP().getName());
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
        autoNextHandIfOver();
    }

    private void call() {
        table.applyActionFromUI(Action.CALL, 0);
        refreshUI();
        autoNextHandIfOver();
    }

    private void bet() {
        int amt = parseAmount(amountField);
        if (amt > 0) {
            table.applyActionFromUI(Action.BET, amt);
            refreshUI();
        }
        autoNextHandIfOver();
    }

    private void raise() {
        int amt = parseAmount(amountField);
        if (amt > 0) {
            table.applyActionFromUI(Action.RAISE, amt);
            refreshUI();
        }
        autoNextHandIfOver();
    }

    private void fold() {
        table.applyActionFromUI(Action.FOLD, 0);
        refreshUI();
        autoNextHandIfOver();
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
        if (table.getStreet() != Table.Street.SHOWDOWN || pendingNextHand) {
            return;
        }
        pendingNextHand = true;

        PauseTransition pause = new PauseTransition(Duration.millis(6000));
        pause.setOnFinished(e -> {
            pendingNextHand = false;
            setMsg("New hand!");
            table.startNewHand();
            refreshUI();
        });
        pause.play();
    }
}
