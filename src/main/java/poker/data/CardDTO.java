package poker.data;

import poker.Card;

import java.util.ArrayList;

public record CardDTO(int suit, int rank) {
    public static ArrayList<CardDTO> fromCards(ArrayList<Card> hand) {
        if (hand == null) {
            throw new IllegalArgumentException("inputted hand is null");
        }
        ArrayList<CardDTO> result = new ArrayList<>();
        for (Card c : hand) {
             result.add(new CardDTO(c.getSuit(), c.getRank()));
        }
        return result;
    }

    public static ArrayList<Card> toCards(ArrayList<CardDTO> hand) {
        if (hand == null) {
            throw new IllegalArgumentException("inputted hand is null");
        }
        ArrayList<Card> result = new ArrayList<>();
        for (CardDTO dto : hand) {
            result.add(new Card(dto.suit(), dto.rank()));
        }
        return result;
    }

    public static CardDTO fromCard(Card c) {
        return new CardDTO(c.getSuit(), c.getRank());
    }

    public static Card toCard(CardDTO c) {
        return new Card(c.suit(), c.rank());
    }
}
