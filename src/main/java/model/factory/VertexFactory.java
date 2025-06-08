package main.java.model.factory;

import main.java.model.shapes.Shape;
import main.java.model.shapes.CircleVertex;
import main.java.model.shapes.SquareVertex;
import main.java.model.shapes.TriangleVertex;

public class VertexFactory {
    public static Shape createVertex(String type, double x, double y, double sizeOrRadius) {
        return switch (type.toLowerCase()) {
            case "circle" -> new CircleVertex(x, y, sizeOrRadius);
            case "square" -> new SquareVertex(x, y, sizeOrRadius, sizeOrRadius); // Assuming square means width=height=sizeOrRadius
            case "triangle" -> new TriangleVertex(x, y, sizeOrRadius);
            default -> throw new IllegalArgumentException("Type de forme inconnu : " + type);
        };
    }
} 