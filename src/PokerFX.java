import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PokerFX extends Application {

    private Table table;

    private ArrayList<ImageView> boardDisplay = new ArrayList<>();

    private ImageView card1Label;
    private ImageView card2Label;

    /*
    private ImageView flop1Label;
    private ImageView flop2Label;
    private ImageView flop3Label;
    private ImageView turn1Label;
    private ImageView river1Label;
     */

    private TextArea msg = new TextArea();



    private Label boardLabel;
    private Button potLabel;
    private Button statusLabel;
    private Button turnLabel;
    private TextField amountField;
    private int status = 0; //pre, flop, turn, river

    private Button p1Stack;
    private Button p2Stack;

    private Image back;

    private boolean faceUp = false;

    @Override
    public void start(Stage stage) {
        table = new Table(this);  // uses existing engine
        table.startNewHand();
        table.setupBlindsForNewHand();

        //player cards
        card1Label = new ImageView();
        card1Label.setFitHeight(100);
        card1Label.setFitWidth(70);
        card2Label = new ImageView();
        card2Label.setFitHeight(100);
        card2Label.setFitWidth(70);

        //board cards
        ImageView flop1Label = new ImageView();
        ImageView flop2Label = new ImageView();
        ImageView flop3Label = new ImageView();
        ImageView turn1Label = new ImageView();
        ImageView river1Label = new ImageView();

        flop1Label.setFitHeight(100);
        flop1Label.setFitWidth(70);
        flop2Label.setFitHeight(100);
        flop2Label.setFitWidth(70);
        flop2Label.setFitHeight(100);
        flop2Label.setFitWidth(70);
        flop3Label.setFitHeight(100);
        flop3Label.setFitWidth(70);
        turn1Label.setFitHeight(100);
        turn1Label.setFitWidth(70);
        river1Label.setFitHeight(100);
        river1Label.setFitWidth(70);

        boardDisplay.add(flop1Label);
        boardDisplay.add(flop2Label);
        boardDisplay.add(flop3Label);
        boardDisplay.add(turn1Label);
        boardDisplay.add(river1Label);

        boardLabel   = new Label("Board: (no cards yet)");
        potLabel     = new Button("Pot: 0");
        statusLabel  = new Button("Current bet: ");
        turnLabel = new Button("To act: " + table.getCurrentPlayer().getName());

        p1Stack = new Button("P1 Stack: ");
        p2Stack = new Button("P2 Stack: ");

        amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setPrefWidth(80);

        updateBoard();
        updateStatus();

        //images
        back = new Image(getClass().getResource("/cards/bicycle_blue@1x.png").toExternalForm());


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

        HBox boardBox = new HBox(flop1Label, flop2Label, flop3Label, turn1Label, river1Label); //boardLabel or card png's
        boardBox.setAlignment(Pos.CENTER);


        HBox actionButtons = new HBox(10, checkBtn, callBtn, betBtn, foldBtn, flipBtn, raiseBtn, amountField);
        actionButtons.setAlignment(Pos.CENTER);


        HBox showdownBox = new HBox(10, p1WinsBtn, p2WinsBtn, splitBtn, nextHandBtn);
        showdownBox.setAlignment(Pos.CENTER);

        HBox stacks = new HBox(10, p1Stack, potLabel, p2Stack);
        stacks.setAlignment(Pos.CENTER);

        msg.setStyle("-fx-font-size: 15px;" + "-fx-text-alignment: center;");

        root.getChildren().addAll(
                boardBox,
                stacks,
                turnLabel,
                actionButtons,
                statusLabel,
                cardBox,
                msg
        ); //removed showdownBox


        // for now, just placeholders so buttons don't do nothing
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
        Scene scene = new Scene(root, 700, 400);

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
        boardLabel.setText("Board: " + table.getBoard().toString());
        p1Stack.setText(table.getPlayers().get(0).getName() + "Stack: " + table.getPlayers().get(0).getStack());
        p2Stack.setText(table.getPlayers().get(1).getName() + "Stack: " + table.getPlayers().get(1).getStack());
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

    public void setMsg(String s) {
        msg.setText(s);
    }
}
