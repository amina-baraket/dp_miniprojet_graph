package main.java.model.strategy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogStrategy implements LogStrategy {
    private final String logFilePath;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FileLogStrategy(String logFilePath) {
        this.logFilePath = logFilePath;
        // Ensure the log directory exists
        File logFile = new File(logFilePath);
        File logDir = logFile.getParentFile();
        if (logDir != null && !logDir.exists()) {
            System.out.println("Attempting to create log directory: " + logDir.getAbsolutePath());
            boolean created = logDir.mkdirs();
            if (created) {
                System.out.println("Log directory created successfully.");
            } else {
                System.err.println("Failed to create log directory: " + logDir.getAbsolutePath());
            }
        }
    }

    @Override
    public void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] %s", timestamp, message);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(logEntry + "\n");
            System.out.println("Logged to file: " + logEntry);
        } catch (IOException e) {
            System.err.println("Error writing to log file '" + logFilePath + "': " + e.getMessage());
            e.printStackTrace(); // Print stack trace for more details
        }
    }
} 