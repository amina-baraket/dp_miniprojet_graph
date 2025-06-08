package main.java.model.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import main.java.model.shapes.Shape;
import main.java.model.shapes.CircleVertex;
import main.java.model.shapes.SquareVertex;
import main.java.model.shapes.TriangleVertex;
import main.java.model.shapes.Line;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:drawingapp.db";

    public DatabaseManager() {
        try {
            // Force the SQLite JDBC driver to load
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite JDBC driver loaded.");
        } catch (ClassNotFoundException e) {
            System.err.println("Error loading SQLite JDBC driver: " + e.getMessage());
        }
        createNewDatabase();
        createTables();
    }

    private void createNewDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("A new database has been created or connected.");
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    private void createTables() {
        String createShapesTableSQL = "CREATE TABLE IF NOT EXISTS shapes (" +
                                      "id INTEGER PRIMARY KEY," +
                                      "type TEXT NOT NULL," +
                                      "x REAL NOT NULL," +
                                      "y REAL NOT NULL," +
                                      "fill_color TEXT," +
                                      "stroke_color TEXT," +
                                      "stroke_width REAL," +
                                      "prop1 REAL," + // For radius, width, endX, etc.
                                      "prop2 REAL," + // For height, endY, etc.
                                      "prop3 REAL" +  // For triangle size (if needed separately)
                                      ");";

        String createLogsTableSQL = "CREATE TABLE IF NOT EXISTS logs (" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "timestamp TEXT NOT NULL," +
                                    "message TEXT NOT NULL" +
                                    ");";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createShapesTableSQL);
            stmt.execute(createLogsTableSQL);
            System.out.println("Tables 'shapes' and 'logs' created or already exist.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public void saveShape(Shape shape) {
        String sql = "INSERT INTO shapes(id, type, x, y, fill_color, stroke_color, stroke_width, prop1, prop2, prop3) VALUES(?,?,?,?,?,?,?,?,?,?) " +
                     "ON CONFLICT(id) DO UPDATE SET type=excluded.type, x=excluded.x, y=excluded.y, fill_color=excluded.fill_color, " +
                     "stroke_color=excluded.stroke_color, stroke_width=excluded.stroke_width, prop1=excluded.prop1, prop2=excluded.prop2, prop3=excluded.prop3;";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, shape.getId());
            pstmt.setString(2, shape.getClass().getSimpleName());
            pstmt.setDouble(3, shape.getX());
            pstmt.setDouble(4, shape.getY());
            pstmt.setString(5, shape.getFillColor() != null ? shape.getFillColor().toString() : null);
            pstmt.setString(6, shape.getStrokeColor() != null ? shape.getStrokeColor().toString() : null);
            pstmt.setDouble(7, shape.getStrokeWidth());

            double prop1 = 0, prop2 = 0, prop3 = 0;
            if (shape instanceof CircleVertex) {
                CircleVertex circle = (CircleVertex) shape;
                prop1 = circle.getRadius();
            } else if (shape instanceof SquareVertex) {
                SquareVertex rect = (SquareVertex) shape;
                prop1 = rect.getWidth();
                prop2 = rect.getHeight();
            } else if (shape instanceof Line) {
                Line line = (Line) shape;
                prop1 = line.getEndX();
                prop2 = line.getEndY();
            } else if (shape instanceof TriangleVertex) {
                TriangleVertex triangle = (TriangleVertex) shape;
                prop1 = triangle.getSize();
            }
            pstmt.setDouble(8, prop1);
            pstmt.setDouble(9, prop2);
            pstmt.setDouble(10, prop3);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public List<Shape> loadShapes() {
        List<Shape> shapes = new ArrayList<>();
        String sql = "SELECT id, type, x, y, fill_color, stroke_color, stroke_width, prop1, prop2, prop3 FROM shapes";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                String fillColorStr = rs.getString("fill_color");
                String strokeColorStr = rs.getString("stroke_color");
                double strokeWidth = rs.getDouble("stroke_width");
                double prop1 = rs.getDouble("prop1");
                double prop2 = rs.getDouble("prop2");
                // double prop3 = rs.getDouble("prop3"); // Not used for now, but available

                Shape shape = null;
                switch (type) {
                    case "CircleVertex":
                        shape = new CircleVertex(x, y, prop1);
                        break;
                    case "SquareVertex":
                        shape = new SquareVertex(x, y, prop1, prop2);
                        break;
                    case "Line":
                        shape = new Line(x, y, prop1, prop2);
                        break;
                    case "TriangleVertex":
                        shape = new TriangleVertex(x, y, prop1);
                        break;
                    default:
                        System.err.println("Unknown shape type: " + type);
                }

                if (shape != null) {
                    shape.setId(id);
                    if (fillColorStr != null) shape.setFillColor(javafx.scene.paint.Color.valueOf(fillColorStr));
                    if (strokeColorStr != null) shape.setStrokeColor(javafx.scene.paint.Color.valueOf(strokeColorStr));
                    shape.setStrokeWidth(strokeWidth);
                    shapes.add(shape);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return shapes;
    }

    public void saveLog(String timestamp, String message) {
        String sql = "INSERT INTO logs(timestamp, message) VALUES(?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, timestamp);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public void clearShapesTable() {
        String sql = "DELETE FROM shapes";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Shapes table cleared.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
} 