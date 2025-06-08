package main.java.model.strategy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import main.java.model.database.DatabaseManager;

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