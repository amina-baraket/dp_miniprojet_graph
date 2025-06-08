import java.util.*;
import main.java.model.shapes.Line;
import main.java.model.shapes.Shape;

public class Graph {
    private Map<Shape, List<Line>> adj;

    public Graph() {
        this.adj = new HashMap<>();
    }

    public void addNode(Shape node) {
        adj.putIfAbsent(node, new ArrayList<>());
    }

    public void addEdge(Shape source, Shape destination, Line line) {
        addNode(source);
        addNode(destination);
        // We only add the line if both ends are actually the shapes, to avoid partial lines.
        // The line itself carries the distance.
        adj.get(source).add(line);
        // For undirected graph, add edge in both directions. Assuming lines are undirected.
        adj.get(destination).add(line);
    }

    public Map<Shape, List<Line>> getAdj() {
        return adj;
    }

    // Method to clear the graph, useful for rebuilding it
    public void clear() {
        adj.clear();
    }

    public static class PathResult {
        public List<Shape> path;
        public double distance;

        public PathResult(List<Shape> path, double distance) {
            this.path = path;
            this.distance = distance;
        }
    }

    public PathResult findShortestPath(Shape startNode, Shape endNode) {
        Map<Shape, Double> distances = new HashMap<>();
        Map<Shape, Shape> predecessors = new HashMap<>();
        PriorityQueue<Shape> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (Shape node : adj.keySet()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(startNode, 0.0);
        pq.add(startNode);

        while (!pq.isEmpty()) {
            Shape current = pq.poll();

            if (current.equals(endNode)) {
                break; // Found the shortest path to endNode
            }

            for (Line line : adj.getOrDefault(current, Collections.emptyList())) {
                Shape neighbor = null;
                if (line.getStartShape().equals(current)) {
                    neighbor = line.getEndShape();
                } else if (line.getEndShape().equals(current)) {
                    neighbor = line.getStartShape();
                }

                if (neighbor == null) continue; // Should not happen with correctly formed lines

                double newDist = distances.get(current) + line.calculateDistance();

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    predecessors.put(neighbor, current);
                    pq.add(neighbor);
                }
            }
        }

        // Reconstruct path
        List<Shape> path = new ArrayList<>();
        double totalDistance = distances.getOrDefault(endNode, Double.POSITIVE_INFINITY);

        if (totalDistance == Double.POSITIVE_INFINITY) {
            return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY); // No path found
        }

        Shape step = endNode;
        while (step != null && !step.equals(startNode)) {
            path.add(0, step); // Add to the beginning to reverse order
            step = predecessors.get(step);
        }
        if (step != null && step.equals(startNode)) {
            path.add(0, startNode);
        } else {
            return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY); // Path reconstruction failed
        }

        return new PathResult(path, totalDistance);
    }
} 