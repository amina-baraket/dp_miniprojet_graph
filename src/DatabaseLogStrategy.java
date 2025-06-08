import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseLogStrategy implements LogStrategy {
    private DatabaseManager dbManager;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DatabaseLogStrategy(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        dbManager.saveLog(timestamp, message);
    }
} 