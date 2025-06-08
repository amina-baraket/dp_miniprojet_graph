public interface Drawable {
    void draw(javafx.scene.canvas.GraphicsContext gc);
    boolean contains(double x, double y);
    void setSelected(boolean selected);
    boolean isSelected();
    void move(double deltaX, double deltaY);
    javafx.geometry.Bounds getBounds();
}