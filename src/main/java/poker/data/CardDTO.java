package poker.data;

import poker.Card;

import java.util.ArrayList;

public record CardDTO(int suit, int rank) {
    public static ArrayList<CardDTO> fromCards(ArrayList<Card> hand) {
        ArrayList<CardDTO> result = new ArrayList<>();
        for (Card c : hand) {
             result.add(new CardDTO(c.getSuit(), c.getRank()));
        }
        return result;
    }
}
