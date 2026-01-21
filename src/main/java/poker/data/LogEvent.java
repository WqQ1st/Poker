package poker.data;

import java.util.List;
import java.util.Map;

public record LogEvent(String ts, String sessionId, int handId, String eventType, String street,
                       String playerName, int playerIndex, String action, int amountPaid,
                       int raiseIncrement, int toCallBefore, int minRaiseBefore, int potBefore,
                       int potAfter, int currentBetBefore, int currentBetAfter,
                       Map<String, Integer> stacksBefore, Map<String, Integer> betsBefore,
                       Map<String, Integer> stacksAfter, Map<String, Integer> betsAfter, List<String> board) {

}
