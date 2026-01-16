package poker.data;

import poker.Player;

import java.util.ArrayList;

public record PlayerDTO(String name, int stack, int bet, boolean inHand, ArrayList<CardDTO> hand) {
    public static ArrayList<PlayerDTO> fromPlayers(ArrayList<Player> players) {
        if (players == null) {
            throw new IllegalArgumentException("Players list is null");
        }
        ArrayList<PlayerDTO> result = new ArrayList<>();
        for (Player p : players) {
            result.add(new PlayerDTO(p.getName(), p.getStack(), p.getBet(), p.isIn(), CardDTO.fromCards(p.getHand().getHand())));
        }
        return result;
    }

    public static ArrayList<Player> toPlayers(ArrayList<PlayerDTO> players) {
        if (players == null) {
            throw new IllegalArgumentException("Players list is null");
        }
        ArrayList<Player> result = new ArrayList<>();
        for (PlayerDTO p : players) {
            result.add(new Player(p.name(), p.stack(), p.bet(), p.inHand(), CardDTO.toCards(p.hand())));
        }
        return result;
    }

    public static void applyToPlayers(ArrayList<PlayerDTO> players, ArrayList<Player> players1) {
        if (players.size() != players1.size()) {
            throw new IllegalArgumentException("Can't apply players; wrong size");
        }
        for (int i = 0; i < players.size(); i++) {
            Player p = players1.get(i);
            PlayerDTO pDTO = players.get(i);
            p.setName(pDTO.name());
            p.setStack(pDTO.stack());
            p.setBet(pDTO.bet());
            p.setIn(pDTO.inHand());
            p.setHand(pDTO.hand());
        }
    }
}
