package poker.data;

import java.util.List;
import java.util.Map;

public record LogEvent(String ts, String sessionId, Integer handId, Integer seq, String eventType, String street,
                       Integer buttonIdx, Integer toActIdx, String playerName, Integer playerIndex, String action,
                       Integer amountPaid, Integer raiseIncrement, Integer toCallBefore, Integer minRaiseBefore, Integer potBefore,
                       Integer potAfter, Integer currentBetBefore, Integer currentBetAfter,
                       Map<String, Integer> stacksBefore, Map<String, Integer> betsBefore,
                       Map<String, Integer> stacksAfter, Map<String, Integer> betsAfter, List<String> board,
                       Map<String, List<String>> holeCards) {

}
