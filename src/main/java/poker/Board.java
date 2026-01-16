package poker;

import java.util.ArrayList;

public class Board {
    private final Card[] board;
    private final Deck deck;
    private int numCards = 0;

    public Board(Deck d) {
        deck = d;
        board = new Card[5];
    }

    public Deck getDeck() {
        return deck;
    }

    public ArrayList<Card> getBoard() {
        ArrayList<Card> cards = new ArrayList<>();
        for (Card c : board) {
            if (c == null) {
                break;
            }
            cards.add(c);
        }
        return cards;
    }

    public void flop() {
        deck.burn(); //burn
        board[0] = deck.draw();
        board[1] = deck.draw();
        board[2] = deck.draw();
        numCards += 3;
    }

    public void turn() {
        deck.burn(); //burn
        board[3] = deck.draw();
        numCards++;
    }

    public void river() {
        deck.burn(); //burn
        board[4] = deck.draw();
        numCards++;
    }

    public void printDeck() {
        System.out.println(deck);
    }

    public ArrayList<Double> equity(ArrayList<Card> p1, ArrayList<Card> p2) {
        long numTotal = 0;
        long p1Win = 0;
        long numDraw = 0;
        ArrayList<Double> result = new ArrayList<>(); //winProb, then drawProb


        ArrayList<Card> tempBoard = new ArrayList<>();
        for (int i = 0; i < numCards; i++) {
            tempBoard.add(board[i]);
        }
        if (numCards == 5) { //post-river
            int comp = HandEvaluator.compareHands(tempBoard, p1, p2);
            numTotal++;
            if (comp > 0) {
                p1Win++;
            } else if (comp == 0) {
                numDraw++;
            }
        } else if (numCards == 4) { //post-turn
            for (int i = 0; i < 52; i++) {
                if (Thread.currentThread().isInterrupted()) break;
                Card c = deck.getCard(i);
                if (c != null) {
                    numTotal++;
                    tempBoard.add(c);
                    int comp = HandEvaluator.compareHands(tempBoard, p1, p2);
                    if (comp > 0) {
                        p1Win++;
                    } else if (comp == 0) {
                        numDraw++;
                    }
                    tempBoard.remove(tempBoard.size() - 1);
                }
            }
        } else if (numCards == 3) { //post-flop
            for (int i = 0; i < 52; i++) {
                if (Thread.currentThread().isInterrupted()) break;
                Card c1 = deck.getCard(i);
                if (c1 == null) {
                    continue;
                }
                tempBoard.add(c1);
                for (int j = i + 1; j < 52; j++) {
                    Card c2 = deck.getCard(j);
                    if (c2 == null) {
                        continue;
                    }
                    numTotal++;
                    tempBoard.add(c2);
                    int comp = HandEvaluator.compareHands(tempBoard, p1, p2);
                    if (comp > 0) {
                        p1Win++;
                    } else if (comp == 0) {
                        numDraw++;
                    }
                    tempBoard.remove(tempBoard.size() - 1); //removes c2
                }
                tempBoard.remove(tempBoard.size() - 1); //removes c1
            }
        } else if (numCards == 0) { //pre-flop
            for (int a = 0; a < 52; a++) {
                if (Thread.currentThread().isInterrupted()) break;
                Card c1 = deck.getCard(a);
                if (c1 == null) continue;
                tempBoard.add(c1);

                for (int b = a + 1; b < 52; b++) {
                    Card c2 = deck.getCard(b);
                    if (c2 == null) continue;
                    tempBoard.add(c2);

                    for (int c = b + 1; c < 52; c++) {
                        Card c3 = deck.getCard(c);
                        if (c3 == null) continue;
                        tempBoard.add(c3);

                        for (int d = c + 1; d < 52; d++) {
                            Card c4 = deck.getCard(d);
                            if (c4 == null) continue;
                            tempBoard.add(c4);

                            for (int e = d + 1; e < 52; e++) {
                                Card c5 = deck.getCard(e);
                                if (c5 == null) continue;
                                tempBoard.add(c5);

                                numTotal++;
                                int comp = HandEvaluator.compareHands(tempBoard, p1, p2);
                                if (comp > 0) p1Win++;
                                else if (comp == 0) numDraw++;

                                tempBoard.remove(tempBoard.size() - 1);
                            }
                            tempBoard.remove(tempBoard.size() - 1);
                        }
                        tempBoard.remove(tempBoard.size() - 1);
                    }
                    tempBoard.remove(tempBoard.size() - 1);
                }
                tempBoard.remove(tempBoard.size() - 1);
            }
        } else {
            throw new IllegalArgumentException("Board must have 0, 3, 4, or 5 cards");
        }
        result.add((double) p1Win / numTotal);
        result.add((double) numDraw / numTotal);
        return result;
    }

    public void clear() {
        for (int i = 0; i < 5; i++) {
            board[i] = null;
        }
        numCards = 0;
    }

    public void addCard(Card c) {
        if (numCards == 5) {
            throw new IllegalArgumentException("Board already has 5 cards");
        }
        board[numCards] = c;
        numCards++;
    }

    public void setDeck(Card[] d, long s) {
        deck.setDeck(d);
        deck.setSeed(s);
    }



    @Override
    public String toString() {
        String s = "";
        for (Card c : board) {
            if (c != null) {
                s += c + " ";
            }
        }
        return s;
    }
}
