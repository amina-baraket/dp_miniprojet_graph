import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Line extends Shape {
    private double endX, endY;
    private boolean showDistance = true;  // Pour afficher ou non la distance
    private Shape startShape; // Nouvelle propriété pour la forme de début
    private Shape endShape;   // Nouvelle propriété pour la forme de fin

    public Line(Shape startShape, Shape endShape, double startX, double startY, double endX, double endY) {
        super(startX, startY);
        this.startShape = startShape;
        this.endShape = endShape;
        this.endX = endX;
        this.endY = endY;
        this.strokeColor = Color.BLACK;
        this.strokeWidth = 2.0;
    }

    // Constructeur existant pour la compatibilité si des lignes sont créées sans formes spécifiques
    public Line(double startX, double startY, double endX, double endY) {
        this(null, null, startX, startY, endX, endY);
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Dessiner la ligne de base
        gc.setStroke(strokeColor);
        gc.setLineWidth(strokeWidth);
        gc.strokeLine(x, y, endX, endY);

        // Draw highlight indicator if highlighted
        if (highlighted) {
            gc.setStroke(Color.GREEN); // Couleur pour le chemin le plus court
            gc.setLineWidth(4); // Ligne plus épaisse pour le highlight
            gc.strokeLine(x, y, endX, endY);
        }
        
        if (showDistance) {
            // Calculer et afficher la distance
            double distance = calculateDistance();
            String distanceText = String.format("%.1f", distance);
            
            // Position du texte (milieu de la ligne)
            double textX = (x + endX) / 2;
            double textY = (y + endY) / 2;
            
            // Dessiner un fond blanc pour le texte
            gc.setFill(Color.WHITE);
            gc.fillRect(textX - 20, textY - 10, 40, 20);
            
            // Dessiner le texte
            gc.setFill(Color.BLACK);
            gc.fillText(distanceText, textX - 15, textY + 5);
        }
        
        if (selected) {
            // Dessiner des points aux extrémités pour indiquer la sélection
            gc.setFill(Color.BLUE);
            gc.fillOval(x - 4, y - 4, 8, 8);
            gc.fillOval(endX - 4, endY - 4, 8, 8);
        }
    }

    public double calculateDistance() {
        return Math.sqrt(Math.pow(endX - x, 2) + Math.pow(endY - y, 2));
    }

    public void toggleDistanceDisplay() {
        showDistance = !showDistance;
    }

    @Override
    public boolean contains(double x, double y) {
        // Vérifier si le point est proche de la ligne
        double distance = distanceToLine(x, y);
        return distance < 5.0; // 5 pixels de tolérance
    }

    private double distanceToLine(double px, double py) {
        double lineLength = Math.sqrt(Math.pow(endX - x, 2) + Math.pow(endY - y, 2));
        if (lineLength == 0) return Math.sqrt(Math.pow(px - x, 2) + Math.pow(py - y, 2));
        
        double t = ((px - x) * (endX - x) + (py - y) * (endY - y)) / (lineLength * lineLength);
        t = Math.max(0, Math.min(1, t));
        
        double projX = x + t * (endX - x);
        double projY = y + t * (endY - y);
        
        return Math.sqrt(Math.pow(px - projX, 2) + Math.pow(py - projY, 2));
    }

    @Override
    public Bounds getBounds() {
        return new javafx.geometry.BoundingBox(
            Math.min(x, endX),
            Math.min(y, endY),
            Math.abs(endX - x),
            Math.abs(endY - y)
        );
    }

    public double getEndX() { return endX; }
    public double getEndY() { return endY; }

    // Nouvelles méthodes pour définir les points de début et de fin
    public void setStartPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setEndPoint(double x, double y) {
        this.endX = x;
        this.endY = y;
    }

    // Nouveaux getters pour les formes connectées
    public Shape getStartShape() {
        return startShape;
    }

    public Shape getEndShape() {
        return endShape;
    }

    // Nouveaux setters pour les formes connectées (optionnel, mais utile si la connexion change)
    public void setStartShape(Shape startShape) {
        this.startShape = startShape;
    }

    public void setEndShape(Shape endShape) {
        this.endShape = endShape;
    }
} 