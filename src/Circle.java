import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Circle extends Shape{
    
    private double radius;
    
    public Circle(double centerX, double centerY, double radius) {
        super(centerX, centerY);
        this.radius = radius;
    }
    
    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }
    
    @Override
    public void draw(GraphicsContext gc) {
        // Set fill and stroke properties
        gc.setFill(fillColor);
        gc.setStroke(strokeColor);
        gc.setLineWidth(strokeWidth);
        
        // Draw the circle
        double diameter = radius * 2;
        gc.fillOval(x - radius, y - radius, diameter, diameter);
        gc.strokeOval(x - radius, y - radius, diameter, diameter);
        
        // Draw highlight indicator if highlighted
        if (highlighted) {
            gc.setStroke(Color.GREEN); // Couleur pour le chemin le plus court
            gc.setLineWidth(3);
            gc.strokeOval(x - radius - 2, y - radius - 2, diameter + 4, diameter + 4);
        }

        // Draw selection indicator if selected
        if (selected) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(1);
            gc.strokeRect(x - radius - 5, y - radius - 5, diameter + 10, diameter + 10);
        }
    }

    @Override
    public boolean contains(double pointX, double pointY) {
        double distance = Math.sqrt(Math.pow(pointX - x, 2) + Math.pow(pointY - y, 2));
        return distance <= radius;
    }
    
    @Override
    public Bounds getBounds() {
        return new BoundingBox(x - radius, y - radius, radius * 2, radius * 2);
    }
    
    
}
