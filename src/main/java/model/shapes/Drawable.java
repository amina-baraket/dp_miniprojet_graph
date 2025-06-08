package main.java.model.shapes;

import javafx.scene.canvas.GraphicsContext;

public interface Drawable {
    void draw(GraphicsContext gc);
    boolean contains(double x, double y);
    void setSelected(boolean selected);
    boolean isSelected();
    void move(double deltaX, double deltaY);
    javafx.geometry.Bounds getBounds();
    void setHighlight(boolean highlighted);
    boolean isHighlight();
}