package main.java.model.shapes;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import main.java.model.shapes.Drawable;
import main.java.model.observer.Observable;
import main.java.model.observer.Observer;
import java.util.ArrayList;
import java.util.List;

public abstract class Shape implements Drawable, Observable {

    protected double x, y;
    protected double rotation = 0;
    protected Color fillColor;
    protected Color strokeColor;
    protected double strokeWidth;
    protected boolean selected;
    protected boolean highlighted;
    protected int id;
    private static int nextId = 1;
    private List<Observer> observers = new ArrayList<>();

    public Shape(double x, double y) {
        this.x = x;
        this.y = y;
        this.fillColor = Color.LIGHTBLUE;
        this.strokeColor = Color.BLACK;
        this.strokeWidth = 2.0;
        this.selected = false;
        this.highlighted = false;
        this.id = nextId++;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    
    public Color getFillColor() { return fillColor; }
    public void setFillColor(Color fillColor) { this.fillColor = fillColor; }
    
    public Color getStrokeColor() { return strokeColor; }
    public void setStrokeColor(Color strokeColor) { this.strokeColor = strokeColor; }
    
    public double getStrokeWidth() { return strokeWidth; }
    
    public abstract void draw(GraphicsContext gc);
    public abstract boolean contains(double x, double y);
    public abstract Bounds getBounds();

    public void setRotation(double angle) { this.rotation = angle; }
    public double getRotation() { return rotation; }


    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        notifyObservers();
    }

    @Override
    public boolean isSelected() {
        return selected;
    }
    
    public void setStrokeWidth(double strokeWidth) { this.strokeWidth = strokeWidth; }

    public boolean isHighlight() {
        return highlighted;
    }

    @Override
    public void setHighlight(boolean highlighted) {
        this.highlighted = highlighted;
        notifyObservers();
    }

    @Override
    public void move(double deltaX, double deltaY) {
        this.x += deltaX;
        this.y += deltaY;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static void resetNextId() {
        nextId = 1;
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update();
        }
    }
}