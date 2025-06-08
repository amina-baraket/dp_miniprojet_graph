package main.java.model.strategy;

import javafx.scene.control.TextArea;

public class ConsoleLogStrategy implements LogStrategy {
    private TextArea logArea;

    public ConsoleLogStrategy(TextArea logArea) {
        this.logArea = logArea;
    }

    @Override
    public void log(String message) {
        logArea.appendText(message + "\n");
    }
} 