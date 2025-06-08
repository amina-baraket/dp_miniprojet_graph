package main.java.model.shapes;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SquareVertex extends Shape{

    private double width, height;

    public SquareVertex(double x, double y, double width, double height) {
        super(x, y);
        this.width = width;
        this.height = height;
    }

    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    @Override
    public void draw(GraphicsContext gc) {
        // Set fill and stroke properties
        gc.setFill(fillColor);
        gc.setStroke(strokeColor);
        gc.setLineWidth(strokeWidth);
        
        // Draw the rectangle
        gc.fillRect(x, y, width, height);
        gc.strokeRect(x, y, width, height);
        
        // Draw highlight indicator if highlighted
        if (highlighted) {
            gc.setStroke(Color.GREEN); // Couleur pour le chemin le plus court
            gc.setLineWidth(3);
            gc.strokeRect(x - 2, y - 2, width + 4, height + 4);
        }

        // Draw selection indicator if selected
        if (selected) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(1);
            gc.strokeRect(x - 5, y - 5, width + 10, height + 10);
        }
    }
    
    @Override
    public boolean contains(double pointX, double pointY) {
        return pointX >= x && pointX <= x + width && 
               pointY >= y && pointY <= y + height;
    }
    
    @Override
    public Bounds getBounds() {
        return new BoundingBox(x, y, width, height);
    }
    
}
