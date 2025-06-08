import javafx.application.Application;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import main.java.model.shapes.Shape;
import main.java.model.shapes.Line;
import main.java.model.shapes.CircleVertex;
import main.java.model.shapes.SquareVertex;
import main.java.model.shapes.TriangleVertex;
import main.java.model.factory.VertexFactory;
import main.java.model.strategy.LogStrategy;
import main.java.model.strategy.ConsoleLogStrategy;
import main.java.model.strategy.FileLogStrategy;
import main.java.model.strategy.DatabaseLogStrategy;
import main.java.model.database.DatabaseManager;
import main.java.model.observer.Observer;
import main.java.model.tools.ToolType;

public class DrawingApp extends Application implements Observer {

    private List<Shape> shapes = new ArrayList<>();
    private Shape selectedShape = null;
    private Shape firstShape = null;  // Première forme sélectionnée pour la connexion
    private double startX, startY;
    private TextArea logArea;
    private ColorPicker colorPicker;
    private Canvas canvas;
    private Shape shortestPathStartNode = null; // Nouveau : nœud de départ pour le plus court chemin
    private Shape shortestPathEndNode = null;   // Nouveau : nœud de fin pour le plus court chemin
    private List<Shape> currentShortestPath = new ArrayList<>(); // Nouveau : pour stocker le chemin trouvé
    private double currentShortestDistance = Double.POSITIVE_INFINITY; // Nouveau : pour stocker la distance

    // Classe interne pour aider à la reconnexion des lignes après le chargement des formes
    private static class LineDataForReconnect {
        Line line;
        int startShapeId;
        int endShapeId;

        LineDataForReconnect(Line line, int startShapeId, int endShapeId) {
            this.line = line;
            this.startShapeId = startShapeId;
            this.endShapeId = endShapeId;
        }
    }

    private ToolType currentTool = ToolType.NONE; // Initialisation du nouvel attribut

    private final TextField widthField = new TextField();
    private final TextField heightField = new TextField();
    private final TextField radiusField = new TextField();
    private final String logFilePath = "log" + File.separator + "log.txt";

    private DatabaseManager dbManager;
    private LogStrategy currentLogStrategy;

    private Graph graph; // Nouvelle instance de graphe

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialiser les composants UI qui sont des dépendances essentielles
        logArea = new TextArea(); 
        logArea.setPrefWidth(200);
        logArea.setEditable(false);

        BorderPane root = new BorderPane(); 
        canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // 2. Initialiser les gestionnaires (qui peuvent dépendre de l'UI déjà initialisée)
        dbManager = new DatabaseManager(); 
        currentLogStrategy = new ConsoleLogStrategy(logArea); // Default to console logging

        Button addCircleBtn = new Button("Add Circle");
        Button addRectBtn = new Button("Add Rectangle");
        Button addTriangleBtn = new Button("Add Triangle");
        Button saveLogBtn = new Button("Save Log");
        Button toggleDistanceBtn = new Button("Toggle Distances");
        Button showDistancesBtn = new Button("Show All Distances");
        Button saveToDbBtn = new Button("Save to DB"); // Nouveau bouton
        Button loadFromDbBtn = new Button("Load from DB"); // Nouveau bouton

        // Nouveaux boutons pour le plus court chemin
        Button selectStartNodeBtn = new Button("Select Start Node");
        Button selectEndNodeBtn = new Button("Select End Node");
        Button calculatePathBtn = new Button("Calculate Path");

        // Nouveaux boutons pour l'export/import texte
        Button exportGraphBtn = new Button("Export Graph");
        Button importGraphBtn = new Button("Import Graph");

        colorPicker = new ColorPicker(Color.LIGHTBLUE);

        // Log strategy selection
        ToggleGroup logStrategyGroup = new ToggleGroup();
        RadioButton consoleLogRadio = new RadioButton("Console Log");
        consoleLogRadio.setToggleGroup(logStrategyGroup);
        consoleLogRadio.setSelected(true); // Default
        RadioButton fileLogRadio = new RadioButton("File Log");
        fileLogRadio.setToggleGroup(logStrategyGroup);
        RadioButton dbLogRadio = new RadioButton("DB Log");
        dbLogRadio.setToggleGroup(logStrategyGroup);

        consoleLogRadio.setOnAction(e -> {
            currentLogStrategy = new ConsoleLogStrategy(logArea);
            log("Log strategy changed to Console.");
        });
        fileLogRadio.setOnAction(e -> {
            currentLogStrategy = new FileLogStrategy(logFilePath);
            log("Log strategy changed to File.");
        });
        dbLogRadio.setOnAction(e -> {
            currentLogStrategy = new DatabaseLogStrategy(dbManager);
            log("Log strategy changed to Database.");
        });

        HBox logStrategyControls = new HBox(10, new Label("Log Strategy:"), consoleLogRadio, fileLogRadio, dbLogRadio);
        logStrategyControls.setPadding(new Insets(5));

        // Size controls
        Label sizeLabel = new Label("Resize:");
        widthField.setPromptText("Width");
        heightField.setPromptText("Height");
        radiusField.setPromptText("Radius");

        VBox sizeControls = new VBox(5, sizeLabel, widthField, heightField, radiusField);
        sizeControls.setPadding(new Insets(5));

        widthField.setOnAction(e -> updateSize());
        heightField.setOnAction(e -> updateSize());
        radiusField.setOnAction(e -> updateSize());

        VBox controlPane = new VBox(10,
                new HBox(10, addCircleBtn, addRectBtn, addTriangleBtn),
                new HBox(10, saveLogBtn, toggleDistanceBtn, showDistancesBtn),
                new HBox(10, saveToDbBtn, loadFromDbBtn, new Label("Color:"), colorPicker),
                new HBox(10, exportGraphBtn, importGraphBtn), // Ajout des nouveaux boutons
                logStrategyControls,
                sizeControls,
                new HBox(10, selectStartNodeBtn, selectEndNodeBtn, calculatePathBtn),
                logArea);
        controlPane.setPadding(new Insets(10));

        root.setLeft(controlPane);
        root.setCenter(canvas);

        // Add triangle
        addTriangleBtn.setOnAction(e -> {
            currentTool = ToolType.TRIANGLE;
            log("Triangle tool selected. Click on canvas to add.");
        });

        // Add circle
        addCircleBtn.setOnAction(e -> {
            currentTool = ToolType.CIRCLE;
            log("Circle tool selected. Click on canvas to add.");
        });

        // Add rectangle
        addRectBtn.setOnAction(e -> {
            currentTool = ToolType.SQUARE;
            log("Rectangle tool selected. Click on canvas to add.");
        });

        // Save log (this will now use the currently selected log strategy)
        saveLogBtn.setOnAction(e -> {
            // If the user presses "Save Log", we want to explicitly save the current content of logArea to file
            // regardless of the current log strategy for regular messages.
            FileLogStrategy explicitFileLog = new FileLogStrategy(logFilePath);
            explicitFileLog.log(logArea.getText()); 
            log("Log area content saved to file."); // This message will go to the current strategy
        });

        // Save to DB
        saveToDbBtn.setOnAction(e -> saveDrawingToDatabase());

        // Load from DB
        loadFromDbBtn.setOnAction(e -> {
            loadDrawingFromDatabase(gc);
            rebuildGraph(); // Reconstruire le graphe après chargement
        });

        // Color picker
        colorPicker.setOnAction(e -> {
            if (selectedShape != null) {
                Color selectedColor = colorPicker.getValue();
                selectedShape.setFillColor(selectedColor);
                log("Changed shape color to: " + selectedColor.toString());
                redraw(gc);
            }
        });

        // Gestion du bouton de basculement des distances
        toggleDistanceBtn.setOnAction(e -> {
            for (Shape shape : shapes) {
                if (shape instanceof Line) {
                    ((Line) shape).toggleDistanceDisplay();
                }
            }
            redraw(gc);
            log("Toggled distance display");
        });

        // Gestion du bouton d'affichage des distances
        showDistancesBtn.setOnAction(e -> {
            calculateAndShowAllDistances();
        });

        // Export Graph to Text
        exportGraphBtn.setOnAction(e -> exportGraphToText());

        // Import Graph from Text
        importGraphBtn.setOnAction(e -> importGraphFromText());

        // Modifier le gestionnaire d'événements MOUSE_PRESSED
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            double clickX = e.getX();
            double clickY = e.getY();

            // 1. Gérer la création de formes si un outil est sélectionné
            if (currentTool == ToolType.CIRCLE || currentTool == ToolType.SQUARE || currentTool == ToolType.TRIANGLE) {
                Shape newShape = null;
                switch (currentTool) {
                    case CIRCLE:
                        newShape = VertexFactory.createVertex("circle", clickX, clickY, 40); // Rayon par défaut
                        break;
                    case SQUARE:
                        newShape = VertexFactory.createVertex("square", clickX, clickY, 80); // Taille par défaut
                        break;
                    case TRIANGLE:
                        newShape = VertexFactory.createVertex("triangle", clickX, clickY, 80); // Taille par défaut
                        break;
                    default:
                        break;
                }
                if (newShape != null) {
                    newShape.addObserver(this);
                    shapes.add(newShape);
                    log("Added " + currentTool.name().toLowerCase() + ": (" + String.format("%.1f, %.1f", clickX, clickY) + ")");
                    redraw(gc);
                    rebuildGraph();
                    currentTool = ToolType.NONE; // Réinitialiser l'outil après la création
                    return; // Consommer l'événement et sortir de la fonction
                }
            }

            // 2. Gérer la sélection et la connexion des formes (si aucun outil de création n'est actif)
            Shape clickedShape = findShapeAt(clickX, clickY);

            // Gérer la sélection des nœuds pour le plus court chemin avant la connexion générale
            if (shortestPathStartNode == null) {
                if (clickedShape != null && !(clickedShape instanceof Line)) {
                    shortestPathStartNode = clickedShape;
                    log("Nœud de départ sélectionné pour le plus court chemin: " + clickedShape.getClass().getSimpleName() + " (ID: " + clickedShape.getId() + ")");
                    shapes.forEach(s -> s.setSelected(false)); // Désélectionner toutes les autres formes pour la clarté
                    shortestPathStartNode.setSelected(true);
                    redraw(gc);
                    return; // Consommer l'événement
                }
            } else if (shortestPathEndNode == null) {
                if (clickedShape != null && !(clickedShape instanceof Line) && !clickedShape.equals(shortestPathStartNode)) {
                    shortestPathEndNode = clickedShape;
                    log("Nœud d'arrivée sélectionné pour le plus court chemin: " + clickedShape.getClass().getSimpleName() + " (ID: " + clickedShape.getId() + ")");
                    shortestPathEndNode.setSelected(true);
                    redraw(gc);
                    return; // Consommer l'événement
                }
            }
            
            // Logique de connexion des formes (si non en mode sélection de nœud de chemin)
            if (clickedShape != null && !(clickedShape instanceof Line)) { // Assurez-vous que ce n'est pas une ligne
                if (firstShape == null) {
                    // Première forme sélectionnée pour la connexion
                    firstShape = clickedShape;
                    shapes.forEach(s -> s.setSelected(false)); // Désélectionner toutes les autres formes
                    firstShape.setSelected(true);
                    selectedShape = clickedShape; // Mettre à jour selectedShape pour la manipulation
                    startX = clickX;
                    startY = clickY;
                    log("First shape selected for connection: " + clickedShape.getClass().getSimpleName() + " (ID: " + clickedShape.getId() + ")");
                } else if (firstShape != clickedShape) {
                    // Deuxième forme sélectionnée, créer une ligne
                    double[] tempPoints = getConnectingPoints(firstShape, clickedShape);
                    Line line = new Line(firstShape, clickedShape,
                                      tempPoints[0], tempPoints[1],
                                      tempPoints[2], tempPoints[3]);
                    shapes.add(line);
                    log("Connected " + firstShape.getClass().getSimpleName() + " (ID: " + firstShape.getId() +
                        ") to " + clickedShape.getClass().getSimpleName() + " (ID: " + clickedShape.getId() + ")");
                    redraw(gc);
                    rebuildGraph();
                    firstShape.setSelected(false); // Désélectionner la première forme
                    firstShape = null; // Réinitialiser pour la prochaine connexion
                    selectedShape = null; // Réinitialiser selectedShape
                    shapes.forEach(s -> s.setSelected(false)); // Désélectionner toutes les formes
                    log("Connection created. All shapes deselected.");
                } else { // Clic sur la même forme deux fois : désélectionner
                    shapes.forEach(s -> s.setSelected(false));
                    selectedShape = null;
                    firstShape = null;
                    log("Shape deselected.");
                }
                updateSizeFields();
                redraw(gc);
            } else if (clickedShape == null) {
                // Clic dans le vide, désélectionner tout et réinitialiser firstShape
                shapes.forEach(s -> s.setSelected(false));
                selectedShape = null;
                firstShape = null;
                log("Canvas clicked. All shapes deselected.");
                updateSizeFields();
                redraw(gc);
            }
        });

        // Mouse dragged
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (selectedShape != null) {
                double dx = e.getX() - startX;
                double dy = e.getY() - startY;
                selectedShape.move(dx, dy);
                startX = e.getX();
                startY = e.getY();

                // Mettre à jour toutes les lignes connectées si la forme déplacée n'est pas une ligne
                if (!(selectedShape instanceof Line)) {
                    for (Shape s : shapes) {
                        if (s instanceof Line line) {
                            if (line.getStartShape() != null && line.getStartShape().equals(selectedShape)) {
                                double[] newPoints = getConnectingPoints(line.getStartShape(), line.getEndShape());
                                line.setStartPoint(newPoints[0], newPoints[1]);
                            }
                            if (line.getEndShape() != null && line.getEndShape().equals(selectedShape)) {
                                double[] newPoints = getConnectingPoints(line.getStartShape(), line.getEndShape());
                                line.setEndPoint(newPoints[2], newPoints[3]);
                            }
                        }
                    }
                } else { // Si la forme déplacée est une ligne elle-même
                    Line movedLine = (Line) selectedShape;
                    // Si la ligne a des formes connectées, la maintenir connectée à celles-ci.
                    // Sinon, déplacer la ligne elle-même.
                    if (movedLine.getStartShape() != null && movedLine.getEndShape() != null) {
                        double[] newPoints = getConnectingPoints(movedLine.getStartShape(), movedLine.getEndShape());
                        movedLine.setStartPoint(newPoints[0], newPoints[1]);
                        movedLine.setEndPoint(newPoints[2], newPoints[3]);
                    } else {
                        // La ligne est flottante ou non connectée, la déplacer directement
                        movedLine.setStartPoint(movedLine.getX() + dx, movedLine.getY() + dy);
                        movedLine.setEndPoint(movedLine.getEndX() + dx, movedLine.getEndY() + dy);
                    }
                }
                redraw(gc);
            }
        });

        // Mouse released
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (selectedShape != null) {
                log("Moved shape to (" + selectedShape.getX() + ", " + selectedShape.getY() + ")");
            }
        });

        // Mettre à jour les gestionnaires de boutons Select Start/End Node et Calculate Path
        selectStartNodeBtn.setOnAction(e -> {
            currentTool = ToolType.NONE; // Pour ne pas créer de forme par inadvertance ou rester dans un mode spécifique
            shortestPathStartNode = null; // Réinitialiser pour une nouvelle sélection
            shortestPathEndNode = null;
            currentShortestPath.clear();
            currentShortestDistance = Double.POSITIVE_INFINITY;
            shapes.forEach(s -> s.setHighlight(false)); // Enlève le highlight du chemin précédent
            shapes.forEach(s -> s.setSelected(false)); // Désélectionner tout
            log("Mode sélection du nœud de départ activé. Veuillez cliquer sur une forme.");
            redraw(gc);
        });

        selectEndNodeBtn.setOnAction(e -> {
            if (shortestPathStartNode == null) {
                log("Veuillez d'abord sélectionner le nœud de départ.");
                return;
            }
            currentTool = ToolType.NONE; // Pour ne pas créer de forme par inadvertance ou rester dans un mode spécifique
            shortestPathEndNode = null; // Réinitialiser pour une nouvelle sélection
            currentShortestPath.clear();
            currentShortestDistance = Double.POSITIVE_INFINITY;
            shapes.forEach(s -> { s.setHighlight(false); if (!s.equals(shortestPathStartNode)) s.setSelected(false); }); // Désélectionner tout sauf le nœud de départ et enlève le highlight
            log("Mode sélection du nœud d'arrivée activé. Veuillez cliquer sur une forme.");
            redraw(gc);
        });

        calculatePathBtn.setOnAction(e -> {
            currentTool = ToolType.NONE; // Après le calcul, ne pas rester en mode sélection de nœud
            if (shortestPathStartNode == null || shortestPathEndNode == null) {
                log("Veuillez sélectionner à la fois un nœud de départ et un nœud d'arrivée.");
                return;
            }
            log("Calcul du plus court chemin...");
            Graph.PathResult result = graph.findShortestPath(shortestPathStartNode, shortestPathEndNode);

            if (!result.path.isEmpty()) {
                currentShortestPath = result.path;
                currentShortestDistance = result.distance;
                log(String.format("Plus court chemin trouvé: %s (Distance: %.1f)",
                                  pathToString(result.path), result.distance));

                // Mettre en évidence les formes et les lignes du chemin
                shapes.forEach(s -> s.setHighlight(false)); // Désélectionner toutes les formes pour le highlight
                currentShortestPath.forEach(s -> s.setHighlight(true)); // Mettre en évidence les nœuds du chemin

                for (int i = 0; i < currentShortestPath.size() - 1; i++) {
                    Shape node1 = currentShortestPath.get(i);
                    Shape node2 = currentShortestPath.get(i+1);
                    // Trouver la ligne qui connecte node1 et node2
                    for (Shape s : shapes) {
                        if (s instanceof Line line) {
                            if ((line.getStartShape() != null && line.getEndShape() != null) && // Ajouter des vérifications nulles
                                ((line.getStartShape().equals(node1) && line.getEndShape().equals(node2)) ||
                                (line.getStartShape().equals(node2) && line.getEndShape().equals(node1)))) {
                                line.setHighlight(true); // Mettre en évidence la ligne
                                break;
                            }
                        }
                    }
                }
                redraw(gc);
            } else {
                log("Aucun chemin trouvé entre les nœuds sélectionnés.");
                currentShortestPath.clear();
                currentShortestDistance = Double.POSITIVE_INFINITY;
                shapes.forEach(s -> s.setHighlight(false));
                redraw(gc);
            }
        });

        primaryStage.setTitle("Drawing App");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    private void updateSizeFields() {
        widthField.clear();
        heightField.clear();
        radiusField.clear();

        if (selectedShape instanceof SquareVertex rect) {
            widthField.setText(String.valueOf(rect.getWidth()));
            heightField.setText(String.valueOf(rect.getHeight()));
        } else if (selectedShape instanceof CircleVertex circle) {
            radiusField.setText(String.valueOf(circle.getRadius()));
        } else if (selectedShape instanceof TriangleVertex triangle) {
            radiusField.setText(String.valueOf(triangle.getSize()));
        }

    }

    private void updateSize() {
        if (selectedShape instanceof SquareVertex rect) {
            try {
                double newWidth = Double.parseDouble(widthField.getText());
                double newHeight = Double.parseDouble(heightField.getText());
                rect.setWidth(newWidth);
                rect.setHeight(newHeight);
                log("Resized rectangle to " + newWidth + "x" + newHeight);
                redrawShapes();
            } catch (NumberFormatException ignored) {
            }
        } else if (selectedShape instanceof TriangleVertex triangle) {
            try {
                double newSize = Double.parseDouble(radiusField.getText());
                triangle.setSize(newSize);
                log("Resized triangle to size " + newSize);
                redrawShapes();
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void redrawShapes() {
        redraw(canvas.getGraphicsContext2D());
    }

    private void redraw(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        for (Shape shape : shapes) {
            shape.draw(gc);
        }
        // Dessiner une ligne temporaire si une première forme est sélectionnée (firstShape != null)
        if (firstShape != null && currentTool == ToolType.NONE) { // Seulement si pas en mode création de forme
            gc.setStroke(Color.GRAY);
            gc.setLineDashes(5);

            Shape tempShape = VertexFactory.createVertex("circle", canvas.getGraphicsContext2D().getCanvas().getWidth() / 2, canvas.getGraphicsContext2D().getCanvas().getHeight() / 2, 0);
            double[] tempPoints = getConnectingPoints(firstShape, tempShape);
            gc.strokeLine(tempPoints[0], tempPoints[1],
                         tempPoints[2], tempPoints[3]);
            gc.setLineDashes(0);
        }
    }

    private double[] getConnectingPoints(Shape s1, Shape s2) {
        double x1 = s1.getX();
        double y1 = s1.getY();
        double x2 = s2.getX();
        double y2 = s2.getY();

        double startX = x1;
        double startY = y1;
        double endX = x2;
        double endY = y2;

        // Logique spécifique pour les cercles (connexion aux bords)
        if (s1 instanceof CircleVertex) {
            CircleVertex c1 = (CircleVertex) s1;
            double angle = Math.atan2(y2 - y1, x2 - x1);
            startX = x1 + c1.getRadius() * Math.cos(angle);
            startY = y1 + c1.getRadius() * Math.sin(angle);
        } else { // Pour Rectangle et Triangle, trouver le point d'intersection sur la boîte englobante
            Bounds bounds1 = s1.getBounds();
            if (bounds1 != null) {
                double[] intersection = getIntersectionPoint(x2, y2, x1, y1, bounds1.getMinX(), bounds1.getMinY(), bounds1.getWidth(), bounds1.getHeight());
                if (intersection != null) {
                    startX = intersection[0];
                    startY = intersection[1];
                }
            }
        }

        if (s2 instanceof CircleVertex) {
            CircleVertex c2 = (CircleVertex) s2;
            double angle = Math.atan2(y1 - y2, x1 - x2);
            endX = x2 + c2.getRadius() * Math.cos(angle);
            endY = y2 + c2.getRadius() * Math.sin(angle);
        } else { // Pour Rectangle et Triangle, trouver le point d'intersection sur la boîte englobante
            Bounds bounds2 = s2.getBounds();
            if (bounds2 != null) {
                double[] intersection = getIntersectionPoint(x1, y1, x2, y2, bounds2.getMinX(), bounds2.getMinY(), bounds2.getWidth(), bounds2.getHeight());
                if (intersection != null) {
                    endX = intersection[0];
                    endY = intersection[1];
                }
            }
        }

        return new double[]{startX, startY, endX, endY};
    }

    private void log(String msg) {
        // Use the current log strategy
        currentLogStrategy.log(msg);
    }

    private void saveDrawingToDatabase() {
        dbManager.clearShapesTable(); // Clear existing shapes before saving
        for (Shape shape : shapes) {
            dbManager.saveShape(shape);
        }
        log("Drawing saved to database.");
    }

    private void loadDrawingFromDatabase(GraphicsContext gc) {
        shapes.clear(); // Clear current shapes
        Shape.resetNextId(); // Reset ID counter to avoid conflicts
        shapes.addAll(dbManager.loadShapes());
        log("Drawing loaded from database.");
        redraw(gc);
    }

    private void calculateAndShowAllDistances() {
        log("\n=== Distances entre les formes connectées ===");
        int lineCount = 0;

        // Parcourir toutes les lignes
        for (Shape shape : shapes) {
            if (shape instanceof Line) {
                Line line = (Line) shape;
                // Trouver les formes connectées par cette ligne
                Shape startShape = findShapeAt(line.getX(), line.getY());
                Shape endShape = findShapeAt(line.getEndX(), line.getEndY());

                log("Checking start point for line: (" + line.getX() + ", " + line.getY() + ")");
                log("Checking end point for line: (" + line.getEndX() + ", " + line.getEndY() + ")");

                if (startShape != null && endShape != null) {
                    String startType = startShape.getClass().getSimpleName();
                    String endType = endShape.getClass().getSimpleName();

                    log(String.format("Distance entre %s et %s = %.1f pixels",
                        startType, endType, line.calculateDistance()));

                    lineCount++;
                } else {
                    // Ajouter un message de débogage plus détaillé
                    log("Ligne trouvée mais pas de forme aux extrémités : S(" + line.getX() + ", " + line.getY() + ") E(" + line.getEndX() + ", " + line.getEndY() + ")");
                    if (startShape == null) log("  Forme de début non trouvée pour le point (" + line.getX() + ", " + line.getY() + ").");
                    if (endShape == null) log("  Forme de fin non trouvée pour le point (" + line.getEndX() + ", " + line.getEndY() + ").");
                }
            }
        }

        if (lineCount == 0) {
            log("Aucune forme connectée trouvée");
        } else {
            log(String.format("Total: %d connexions", lineCount));
        }
        log("===============================\n");
    }

    private void rebuildGraph() {
        graph = new Graph(); // Réinitialiser le graphe
        for (Shape shape : shapes) {
            if (!(shape instanceof Line)) {
                graph.addNode(shape); // Ajouter toutes les formes non-lignes comme nœuds
            }
        }
        for (Shape shape : shapes) {
            if (shape instanceof Line line) {
                // S'assurer que les formes aux extrémités existent et ne sont pas nulles
                Shape startNode = line.getStartShape();
                Shape endNode = line.getEndShape();

                if (startNode != null && endNode != null) {
                    graph.addEdge(startNode, endNode, line);
                } else {
                    log("Avertissement: Une ligne est orpheline de forme de début ou de fin.");
                }
            }
        }
        log("Graphe reconstruit avec " + graph.getAdj().size() + " nœuds.");
    }

    private Shape findShapeAt(double x, double y) {
        double tolerance = 20.0; // Augmentation de la tolérance pour une meilleure détection
        log("findShapeAt called for point: (" + x + ", " + y + ")");
        for (Shape shape : shapes) {
            // On ignore les lignes elles-mêmes quand on cherche une forme connectée
            if (!(shape instanceof Line)) {
                // Créer une boîte englobante légèrement plus grande pour la tolérance
                Bounds bounds = shape.getBounds();
                if (bounds != null) {
                    BoundingBox tolerantBounds = new BoundingBox(
                        bounds.getMinX() - tolerance,
                        bounds.getMinY() - tolerance,
                        bounds.getWidth() + 2 * tolerance,
                        bounds.getHeight() + 2 * tolerance
                    );
                    log("  Checking shape: " + shape.getClass().getSimpleName() + " (ID: " + shape.getId() + ")");
                    log("    Shape bounds: MinX=" + bounds.getMinX() + ", MinY=" + bounds.getMinY() + ", Width=" + bounds.getWidth() + ", Height=" + bounds.getHeight());
                    log("    Tolerant bounds: MinX=" + tolerantBounds.getMinX() + ", MinY=" + tolerantBounds.getMinY() + ", Width=" + tolerantBounds.getWidth() + ", Height=" + tolerantBounds.getHeight());

                    // D'abord, une vérification rapide avec la boîte englobante tolérante
                    if (tolerantBounds.contains(x, y)) {
                        log("    Point (" + x + ", " + y + ") IS within tolerant bounds.");
                        // Si dans la boîte englobante, effectuer une vérification précise avec la méthode contains() de la forme
                        if (shape.contains(x, y)) {
                            log("    Point (" + x + ", " + y + ") IS within shape.contains(). Forme trouvée.");
                            return shape;
                        } else {
                            log("    Point (" + x + ", " + y + ") IS within tolerant bounds, but NOT within shape.contains().");
                        }
                    } else {
                        log("    Point (" + x + ", " + y + ") IS NOT within tolerant bounds.");
                    }
                }
            }
        }
        log("Aucune forme trouvée aux coordonnées (" + x + ", " + y + ")");
        return null;
    }

    // La méthode saveLogToFile() est maintenant gérée par FileLogStrategy
    private void saveLogToFile() {
        FileLogStrategy explicitFileLog = new FileLogStrategy(logFilePath);
        explicitFileLog.log(logArea.getText()); // Save the entire log area content
        log("Log area content saved to file."); // This message will go to the current strategy
    }

    // Méthode utilitaire pour convertir le chemin en chaîne de caractères pour le log
    private String pathToString(List<Shape> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            sb.append(path.get(i).getClass().getSimpleName()).append("(").append(path.get(i).getId()).append(")");
            if (i < path.size() - 1) {
                sb.append(" -> ");
            }
        }
        return sb.toString();
    }

    // Helper method to find intersection point of a line segment (x1, y1)-(x2, y2) with a rectangle (rx, ry, rw, rh)
    private double[] getIntersectionPoint(double x1, double y1, double x2, double y2, double rx, double ry, double rw, double rh) {
        double[] intersection = null;
        double minDistSq = Double.POSITIVE_INFINITY;

        // Test intersection with top side
        double[] p = lineLineIntersection(x1, y1, x2, y2, rx, ry, rx + rw, ry);
        if (p != null && p[0] >= rx && p[0] <= rx + rw) {
            double distSq = Math.pow(p[0] - x1, 2) + Math.pow(p[1] - y1, 2);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                intersection = p;
            }
        }
        // Test intersection with bottom side
        p = lineLineIntersection(x1, y1, x2, y2, rx, ry + rh, rx + rw, ry + rh);
        if (p != null && p[0] >= rx && p[0] <= rx + rw) {
            double distSq = Math.pow(p[0] - x1, 2) + Math.pow(p[1] - y1, 2);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                intersection = p;
            }
        }
        // Test intersection with left side
        p = lineLineIntersection(x1, y1, x2, y2, rx, ry, rx, ry + rh);
        if (p != null && p[1] >= ry && p[1] <= ry + rh) {
            double distSq = Math.pow(p[0] - x1, 2) + Math.pow(p[1] - y1, 2);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                intersection = p;
            }
        }
        // Test intersection with right side
        p = lineLineIntersection(x1, y1, x2, y2, rx + rw, ry, rx + rw, ry + rh);
        if (p != null && p[1] >= ry && p[1] <= ry + rh) {
            double distSq = Math.pow(p[0] - x1, 2) + Math.pow(p[1] - y1, 2);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                intersection = p;
            }
        }

        return intersection;
    }

    // Helper method for line-line intersection
    private double[] lineLineIntersection(double x1, double y1, double x2, double y2, 
                                          double x3, double y3, double x4, double y4) {
        double den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (den == 0) return null; // Lines are parallel or collinear

        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / den;

        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            double px = x1 + t * (x2 - x1);
            double py = y1 + t * (y2 - y1);
            return new double[]{px, py};
        }
        return null;
    }

    @Override
    public void update() {
        // Cette méthode est appelée quand une forme change d'état
        redraw(canvas.getGraphicsContext2D());
    }

    private void exportGraphToText() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Graph Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        fileChooser.setInitialFileName("graph_data.txt");

        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());

        if (file != null) {
            log("Exporting graph data to " + file.getAbsolutePath() + "...");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Shape shape : shapes) {
                    if (shape instanceof CircleVertex) {
                        CircleVertex c = (CircleVertex) shape;
                        writer.write(String.format("CIRCLE, %d, %.1f, %.1f, %.1f, %s, %s, %.1f\n",
                                                   c.getId(), c.getX(), c.getY(), c.getRadius(),
                                                   c.getFillColor().toString(), c.getStrokeColor().toString(), c.getStrokeWidth()));
                    } else if (shape instanceof SquareVertex) {
                        SquareVertex s = (SquareVertex) shape;
                        writer.write(String.format("SQUARE, %d, %.1f, %.1f, %.1f, %.1f, %s, %s, %.1f\n",
                                                   s.getId(), s.getX(), s.getY(), s.getWidth(), s.getHeight(),
                                                   s.getFillColor().toString(), s.getStrokeColor().toString(), s.getStrokeWidth()));
                    } else if (shape instanceof TriangleVertex) {
                        TriangleVertex t = (TriangleVertex) shape;
                        writer.write(String.format("TRIANGLE, %d, %.1f, %.1f, %.1f, %s, %s, %.1f\n",
                                                   t.getId(), t.getX(), t.getY(), t.getSize(),
                                                   t.getFillColor().toString(), t.getStrokeColor().toString(), t.getStrokeWidth()));
                    } else if (shape instanceof Line) {
                        Line l = (Line) shape;
                        // Sauvegarder les IDs des formes connectées pour la reconstruction
                        int startShapeId = (l.getStartShape() != null) ? l.getStartShape().getId() : -1; // -1 si non connecté
                        int endShapeId = (l.getEndShape() != null) ? l.getEndShape().getId() : -1;
                        writer.write(String.format("LINE, %d, %d, %d, %.1f, %.1f, %.1f, %.1f, %s, %s, %.1f\n",
                                                   l.getId(), startShapeId, endShapeId, l.getX(), l.getY(), l.getEndX(), l.getEndY(),
                                                   l.getFillColor().toString(), l.getStrokeColor().toString(), l.getStrokeWidth()));
                    }
                }
                log("Graph data exported successfully.");
            } catch (IOException e) {
                log("Error exporting graph data: " + e.getMessage());
                System.err.println("Error exporting graph data: " + e.getMessage());
            }
        } else {
            log("File export cancelled.");
        }
    }

    private void importGraphFromText() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Graph Data File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());

        if (file != null) {
            log("Importing graph data from " + file.getAbsolutePath() + "...");
            shapes.clear(); // Clear current shapes
            Shape.resetNextId(); // Reset ID counter
            List<LineDataForReconnect> linesToReconnectData = new ArrayList<>(); // Nouvelle liste
            Map<Integer, Shape> loadedShapesMap = new HashMap<>(); // Nouveau: pour stocker les formes par ID

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) { // Utilisation du fichier sélectionné
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(", ");
                    String type = parts[0];
                    int id = Integer.parseInt(parts[1]);
                    double x = Double.parseDouble(parts[2]);
                    double y = Double.parseDouble(parts[3]);

                    Shape newShape = null;
                    if (type.equals("CIRCLE")) {
                        double radius = Double.parseDouble(parts[4]);
                        newShape = new CircleVertex(x, y, radius);
                    } else if (type.equals("SQUARE")) {
                        double width = Double.parseDouble(parts[4]);
                        double height = Double.parseDouble(parts[5]);
                        newShape = new SquareVertex(x, y, width, height);
                    } else if (type.equals("TRIANGLE")) {
                        double size = Double.parseDouble(parts[4]);
                        newShape = new TriangleVertex(x, y, size);
                    } else if (type.equals("LINE")) {
                        int startShapeId = Integer.parseInt(parts[2]);
                        int endShapeId = Integer.parseInt(parts[3]);
                        double startX = Double.parseDouble(parts[4]);
                        double startY = Double.parseDouble(parts[5]);
                        double endX = Double.parseDouble(parts[6]);
                        double endY = Double.parseDouble(parts[7]);
                        
                        Line newLine = new Line(startX, startY, endX, endY);
                        newLine.setId(id);
                        linesToReconnectData.add(new LineDataForReconnect(newLine, startShapeId, endShapeId));
                        newShape = newLine;
                    }

                    if (newShape != null) {
                        newShape.setId(id);
                        if (!type.equals("LINE")) {
                             newShape.setFillColor(javafx.scene.paint.Color.valueOf(parts[parts.length - 3]));
                             newShape.setStrokeColor(javafx.scene.paint.Color.valueOf(parts[parts.length - 2]));
                             newShape.setStrokeWidth(Double.parseDouble(parts[parts.length - 1]));
                        } else {
                             newShape.setFillColor(javafx.scene.paint.Color.valueOf(parts[8]));
                             newShape.setStrokeColor(javafx.scene.paint.Color.valueOf(parts[9]));
                             newShape.setStrokeWidth(Double.parseDouble(parts[10]));
                        }
                        newShape.addObserver(this);
                        shapes.add(newShape);
                        loadedShapesMap.put(id, newShape);
                    }
                }

                for (LineDataForReconnect lineData : linesToReconnectData) {
                    Shape startShape = loadedShapesMap.get(lineData.startShapeId); 
                    Shape endShape = loadedShapesMap.get(lineData.endShapeId);   

                    lineData.line.setStartShape(startShape);
                    lineData.line.setEndShape(endShape);

                    if (startShape != null && endShape != null) {
                        double[] updatedPoints = getConnectingPoints(startShape, endShape);
                        lineData.line.setStartPoint(updatedPoints[0], updatedPoints[1]);
                        lineData.line.setEndPoint(updatedPoints[2], updatedPoints[3]);
                    }
                }

                log("Graph data imported successfully.");
                redraw(canvas.getGraphicsContext2D());
                rebuildGraph();
            } catch (IOException | NumberFormatException e) {
                log("Error importing graph data: " + e.getMessage());
                System.err.println("Error importing graph data: " + e.getMessage());
            }
        } else {
            log("File import cancelled.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
