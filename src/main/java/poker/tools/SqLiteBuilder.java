package poker.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public class SqLiteBuilder {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        boolean RESET_DB = true;

        String sessionId = "39ca075d-a35f-49da-8e1f-564545250583";
        Path logFile = Path.of("logs/pokerfx_" + sessionId + ".jsonl");
        Path dbFile  = Path.of("data/poker_" + sessionId + ".db");

        if (!Files.exists(logFile)) {
            throw new IllegalArgumentException("Log file not found: " + logFile.toAbsolutePath());
        }

        Files.createDirectories(dbFile.getParent());
        String url = "jdbc:sqlite:" + dbFile;

        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);

            if (RESET_DB) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("DROP TABLE IF EXISTS events");
                }
            }

            createSchema(conn);

            try {
                int inserted = importFile(conn, logFile);
                conn.commit();
                System.out.println("Imported " + inserted + " events");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

            printLast10(conn);
            runSanityChecks(conn);
        }
    }

    private static void createSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS events (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,

                  session_id TEXT NOT NULL,
                  hand_id INTEGER NOT NULL,
                  seq INTEGER NOT NULL,

                  ts TEXT NOT NULL,
                  event_type TEXT NOT NULL,
                  street TEXT,

                  button_idx INTEGER,
                  to_act_idx INTEGER,

                  player_index INTEGER,
                  player_name TEXT,
                  action TEXT,

                  amount_paid INTEGER,
                  raise_increment INTEGER,
                  to_call_before INTEGER,
                  min_raise_before INTEGER,

                  pot_before INTEGER NOT NULL,
                  pot_after INTEGER NOT NULL,
                  current_bet_before INTEGER NOT NULL,
                  current_bet_after INTEGER NOT NULL,

                  payload_json TEXT NOT NULL,

                  UNIQUE(session_id, hand_id, seq)
                )
                """);

            st.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_events_session_hand_seq
                ON events(session_id, hand_id, seq)
                """);

            st.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_events_type
                ON events(event_type)
                """);
        }
    }

    private static int importFile(Connection conn, Path logFile) throws Exception {
        String insertSql = """
            INSERT INTO events(
              session_id, hand_id, seq,
              ts, event_type, street,
              button_idx, to_act_idx,
              player_index, player_name, action,
              amount_paid, raise_increment, to_call_before, min_raise_before,
              pot_before, pot_after,
              current_bet_before, current_bet_after,
              payload_json
            ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        int[] count = {0};

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            try (var lines = Files.lines(logFile)) {
                lines.forEach(line -> {
                    if (line == null || line.isBlank()) return;

                    try {
                        JsonNode n = mapper.readTree(line);

                        // required
                        ps.setString(1, n.path("sessionId").asText());
                        ps.setInt(2, n.path("handId").asInt());
                        ps.setInt(3, n.path("seq").asInt());

                        ps.setString(4, n.path("ts").asText());
                        ps.setString(5, n.path("eventType").asText());
                        ps.setString(6, textOrNull(n, "street"));

                        // nullable ints
                        setIntOrNull(ps, 7, n.get("buttonIdx"));
                        setIntOrNull(ps, 8, n.get("toActIdx"));

                        setIntOrNull(ps, 9, n.get("playerIndex"));
                        ps.setString(10, textOrNull(n, "playerName"));
                        ps.setString(11, textOrNull(n, "action"));

                        setIntOrNull(ps, 12, n.get("amountPaid"));
                        setIntOrNull(ps, 13, n.get("raiseIncrement"));
                        setIntOrNull(ps, 14, n.get("toCallBefore"));
                        setIntOrNull(ps, 15, n.get("minRaiseBefore"));

                        // pot and current bet should always exist in your logs
                        ps.setInt(16, n.path("potBefore").asInt());
                        ps.setInt(17, n.path("potAfter").asInt());
                        ps.setInt(18, n.path("currentBetBefore").asInt());
                        ps.setInt(19, n.path("currentBetAfter").asInt());

                        // keep entire line for future parsing
                        ps.setString(20, line);

                        ps.addBatch();
                        count[0]++;

                        if (count[0] % 500 == 0) ps.executeBatch();
                    } catch (Exception e) {
                        throw new RuntimeException("Bad log line: " + line, e);
                    }
                });
            }

            ps.executeBatch();
        }

        return count[0];
    }

    private static void setIntOrNull(PreparedStatement ps, int idx, JsonNode node) throws SQLException {
        if (node == null || node.isNull() || node.isMissingNode()) {
            ps.setNull(idx, Types.INTEGER);
            return;
        }
        // also treat empty string as null just in case
        if (node.isTextual() && node.asText().isBlank()) {
            ps.setNull(idx, Types.INTEGER);
            return;
        }
        ps.setInt(idx, node.asInt());
    }

    private static String textOrNull(JsonNode n, String field) {
        JsonNode x = n.get(field);
        if (x == null || x.isNull() || x.isMissingNode()) return null;
        String s = x.asText();
        return s == null || s.isBlank() ? null : s;
    }

    private static void printLast10(Connection conn) throws SQLException {
        try (PreparedStatement q = conn.prepareStatement("""
            SELECT id, hand_id, seq, ts, event_type, street, player_name, action, amount_paid, pot_after
            FROM events
            ORDER BY hand_id DESC, seq DESC
            LIMIT 10
            """)) {
            try (ResultSet rs = q.executeQuery()) {
                System.out.println("Last 10 events:");
                while (rs.next()) {
                    System.out.printf(
                            "%d | hand=%d seq=%d | %s | %s | %s | %s | %s | paid=%s | pot=%d%n",
                            rs.getLong("id"),
                            rs.getInt("hand_id"),
                            rs.getInt("seq"),
                            rs.getString("ts"),
                            rs.getString("event_type"),
                            rs.getString("street"),
                            rs.getString("player_name"),
                            rs.getString("action"),
                            rs.getObject("amount_paid"),
                            rs.getInt("pot_after")
                    );
                }
            }
        }
    }

    private static void runSanityChecks(Connection conn) throws SQLException {
        // 1) HAND_END should be last seq in each hand
        try (PreparedStatement q = conn.prepareStatement("""
            SELECT session_id, hand_id,
                   MAX(seq) AS max_seq,
                   MAX(CASE WHEN event_type='HAND_END' THEN seq ELSE NULL END) AS hand_end_seq
            FROM events
            GROUP BY session_id, hand_id
            HAVING hand_end_seq IS NOT NULL AND hand_end_seq != max_seq
            """)) {
            try (ResultSet rs = q.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf(
                            "SANITY FAIL: HAND_END not last for session=%s hand=%d max_seq=%d hand_end_seq=%s%n",
                            rs.getString("session_id"),
                            rs.getInt("hand_id"),
                            rs.getInt("max_seq"),
                            rs.getObject("hand_end_seq")
                    );
                }
                if (!any) System.out.println("Sanity OK: HAND_END is last in each hand (where present).");
            }
        }

        // 2) duplicate seq would violate UNIQUE, but this catches issues if you remove it later
        try (PreparedStatement q = conn.prepareStatement("""
            SELECT session_id, hand_id, seq, COUNT(*) AS c
            FROM events
            GROUP BY session_id, hand_id, seq
            HAVING c > 1
            """)) {
            try (ResultSet rs = q.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf(
                            "SANITY FAIL: Duplicate seq session=%s hand=%d seq=%d count=%d%n",
                            rs.getString("session_id"),
                            rs.getInt("hand_id"),
                            rs.getInt("seq"),
                            rs.getInt("c")
                    );
                }
                if (!any) System.out.println("Sanity OK: No duplicate seq.");
            }
        }
    }
}
