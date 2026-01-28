package poker.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

public class SqLiteBuilder {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        boolean RESET_DB = true; // set false when data needs to be kept

        String sessionId = "39ca075d-a35f-49da-8e1f-564545250583";
        Path logFile = Path.of("logs/pokerfx_" + sessionId + ".jsonl");
        Path dbFile  = Path.of("data/poker_" + sessionId + ".db");

        if (!Files.exists(logFile)) {
            throw new IllegalArgumentException("Log file not found: " + logFile.toAbsolutePath());
        }

        Files.createDirectories(dbFile.getParent());

        String url = "jdbc:sqlite:" + dbFile.toString();

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
                System.out.println("Imported " + inserted + " events ...");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }


            // quick sanity query
            printLast10(conn);
        }
    }

    private static void createSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS events (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  session_id TEXT NOT NULL,
                  hand_id INTEGER NOT NULL,
                  ts TEXT NOT NULL,
                  event_type TEXT NOT NULL,
                  street TEXT,
                  player_index INTEGER,
                  player_name TEXT,
                  action TEXT,
                  amount_paid INTEGER,
                  raise_increment INTEGER,
                  pot_before INTEGER,
                  pot_after INTEGER,
                  current_bet_before INTEGER,
                  current_bet_after INTEGER,
                  payload_json TEXT NOT NULL
                )
                """);

            st.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_events_session_hand_ts
                ON events(session_id, hand_id, ts)
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
              session_id, hand_id, ts, event_type, street,
              player_index, player_name, action,
              amount_paid, raise_increment,
              pot_before, pot_after,
              current_bet_before, current_bet_after,
              payload_json
            ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        int[] count = {0};

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            try (var lines = Files.lines(logFile)) {
                lines.forEach(line -> {
                    if (line.isBlank()) return;  // optional but recommended
                    try {
                        JsonNode n = mapper.readTree(line);

                        ps.setString(1, n.path("sessionId").asText());
                        ps.setInt(2, n.path("handId").asInt());
                        ps.setString(3, n.path("ts").asText());           // keep as ISO string
                        ps.setString(4, n.path("eventType").asText());
                        ps.setString(5, textOrNull(n, "street"));

                        ps.setInt(6, n.path("playerIndex").asInt(-1));
                        ps.setString(7, textOrNull(n, "playerName"));
                        ps.setString(8, textOrNull(n, "action"));

                        ps.setInt(9, n.path("amountPaid").asInt(0));
                        ps.setInt(10, n.path("raiseIncrement").asInt(0));

                        ps.setInt(11, n.path("potBefore").asInt(0));
                        ps.setInt(12, n.path("potAfter").asInt(0));

                        ps.setInt(13, n.path("currentBetBefore").asInt(0));
                        ps.setInt(14, n.path("currentBetAfter").asInt(0));

                        ps.setString(15, line);

                        ps.addBatch();
                        count[0]++;

                        // Flush every 500 rows for speed
                        if (count[0] % 500 == 0) {
                            ps.executeBatch();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Bad log line: " + line, e);
                    }
                });
            }

            ps.executeBatch();
        }

        return count[0];
    }

    private static String textOrNull(JsonNode n, String field) {
        JsonNode x = n.get(field);
        if (x == null || x.isNull() || x.isMissingNode()) return null;
        String s = x.asText();
        return s.isBlank() ? null : s;
    }

    private static void printLast10(Connection conn) throws SQLException {
        try (PreparedStatement q = conn.prepareStatement("""
            SELECT id, ts, event_type, street, player_name, action, amount_paid, pot_after
            FROM events
            ORDER BY id DESC
            LIMIT 10
            """)) {
            try (ResultSet rs = q.executeQuery()) {
                System.out.println("Last 10 events:");
                while (rs.next()) {
                    System.out.printf(
                            "%d | %s | %s | %s | %s | %s | paid=%d | pot=%d%n",
                            rs.getLong("id"),
                            rs.getString("ts"),
                            rs.getString("event_type"),
                            rs.getString("street"),
                            rs.getString("player_name"),
                            rs.getString("action"),
                            rs.getInt("amount_paid"),
                            rs.getInt("pot_after")
                    );
                }
            }
        }
    }
}
