package com.chancema.burghgen.generation.road;

import java.awt.Point;
import java.util.*;

import com.chancema.burghgen.model.MapSize;
import com.chancema.burghgen.model.RoadStyle;
import com.chancema.burghgen.model.TerrainTile;
import com.chancema.burghgen.model.TerrainType;
import com.chancema.burghgen.util.RoadTileLookup;

/**
 * Utility class for road node generation and pathfinding.
 */
public class RoadUtils {

    /**
     * Generates a list of major road nodes including the town center, gates, and district centers.
     *
     * @param townCenter Center of the town
     * @param mapWidth   Width of the map
     * @param mapHeight  Height of the map
     * @param rng        Seeded random instance
     * @return List of key road nodes
     */
    public static List<RoadNode> generateMajorRoadNodes(Point townCenter, int mapWidth, int mapHeight, Random rng, MapSize size) {
        List<RoadNode> nodes = new ArrayList<>();
        Set<Point> usedPoints = new HashSet<>();

        // Add town center
        nodes.add(new RoadNode(townCenter, RoadNode.NodeType.TOWN_CENTER));
        usedPoints.add(townCenter);

        // Generate 3–6 district centers in a rough radial layout
        int districtCount = switch (size) {
            case SMALL -> 2 + rng.nextInt(2);      // 2–3
            case MEDIUM -> 3 + rng.nextInt(3);     // 3–5
            case LARGE -> 5 + rng.nextInt(4);      // 5–8
        };
        for (int i = 0; i < districtCount; i++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            int distance = (int) (mapWidth * 0.15 + rng.nextDouble() * mapWidth * 0.2);

            int x = clamp((int) (townCenter.x + Math.cos(angle) * distance), 2, mapWidth - 3);
            int y = clamp((int) (townCenter.y + Math.sin(angle) * distance), 2, mapHeight - 3);
            Point p = new Point(x, y);

            if (usedPoints.add(p)) {
                nodes.add(new RoadNode(p, RoadNode.NodeType.DISTRICT_CENTER));
            }
        }

        int gateCount = switch (size) {
            case SMALL -> 2;
            case MEDIUM -> 3;
            case LARGE -> 4;
        };
        for (int i = 0; i < gateCount; i++) {
            int edge = rng.nextInt(4); // 0=top, 1=right, 2=bottom, 3=left
            int x = 0, y = 0;

            switch (edge) {
                case 0 -> { // Top
                    x = rng.nextInt(mapWidth);
                    y = 0;
                }
                case 1 -> { // Right
                    x = mapWidth - 1;
                    y = rng.nextInt(mapHeight);
                }
                case 2 -> { // Bottom
                    x = rng.nextInt(mapWidth);
                    y = mapHeight - 1;
                }
                case 3 -> { // Left
                    x = 0;
                    y = rng.nextInt(mapHeight);
                }
            }

            Point gate = new Point(clamp(x, 1, mapWidth - 2), clamp(y, 1, mapHeight - 2));
            if (usedPoints.add(gate)) {
                nodes.add(new RoadNode(gate, RoadNode.NodeType.GATE));
            }
        }

        return nodes;
    }

    /**
     * Clamps a value between a min and max.
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Connects major road nodes using a Delaunay-style MST approach.
     *
     * @param nodes List of road nodes to connect
     * @param rng   Random instance for edge selection
     * @return List of paths (each path is a pair of connected nodes)
     */
    public static List<List<RoadNode>> connectWithVoronoiGraph(List<RoadNode> nodes, Random rng) {
        List<List<RoadNode>> connections = new ArrayList<>();

        // Build MST using Prim’s Algorithm (greedy)
        Set<RoadNode> connected = new HashSet<>();
        PriorityQueue<RoadEdge> edgeQueue = new PriorityQueue<>(Comparator.comparingDouble(e -> e.cost));

        RoadNode start = nodes.get(0); // Usually the town center
        connected.add(start);

        // Add edges from start to all others
        for (RoadNode node : nodes) {
            if (!node.equals(start)) {
                edgeQueue.add(new RoadEdge(start, node));
            }
        }

        while (connected.size() < nodes.size() && !edgeQueue.isEmpty()) {
            RoadEdge edge = edgeQueue.poll();

            if (connected.contains(edge.to)) continue;

            // Connect edge
            connected.add(edge.to);
            connections.add(List.of(edge.from, edge.to));

            // Add edges from new node to unconnected nodes
            for (RoadNode node : nodes) {
                if (!connected.contains(node)) {
                    edgeQueue.add(new RoadEdge(edge.to, node));
                }
            }
        }

        // Add a few extra edges (random cross connections) to break gridlock
        int extraEdges = 1 + rng.nextInt(2);
        int attempts = 0;

        while (extraEdges > 0 && attempts < 20) {
            RoadNode a = nodes.get(rng.nextInt(nodes.size()));
            RoadNode b = nodes.get(rng.nextInt(nodes.size()));

            if (!a.equals(b) && !pathExists(connections, a, b)) {
                connections.add(List.of(a, b));
                extraEdges--;
            }
            attempts++;
        }

        return connections;
    }

    /**
     * Checks if a direct connection already exists between two nodes.
     */
    private static boolean pathExists(List<List<RoadNode>> paths, RoadNode a, RoadNode b) {
        return paths.stream().anyMatch(p ->
            (p.get(0).equals(a) && p.get(1).equals(b)) ||
            (p.get(0).equals(b) && p.get(1).equals(a))
        );
    }

    /**
     * Helper record for MST edge logic.
     */
    private static class RoadEdge {
        RoadNode from;
        RoadNode to;
        double cost;

        public RoadEdge(RoadNode from, RoadNode to) {
            this.from = from;
            this.to = to;
            this.cost = from.toPoint().distance(to.toPoint());
        }
    }

    /**
     * Carves a road between two connected RoadNodes directly into the terrain map.
     * Uses a simple line algorithm and expands width around the main path.
     *
     * @param map 2D terrain tile array to modify
     * @param path A list of two RoadNodes (source → destination)
     * @param rng Random instance
     * @param style The road style to apply
     */
    public static void carveRoadPath(
        TerrainTile[][] map,
        List<RoadNode> path,
        Random rng,
        RoadStyle style,
        MapSize size
    ) {
        if (path.size() != 2) return; // Only handles single-segment paths for now

        RoadNode a = path.get(0);
        RoadNode b = path.get(1);

        int x0 = a.getX();
        int y0 = a.getY();
        int x1 = b.getX();
        int y1 = b.getY();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        int roadWidth = switch (size) {
            case SMALL -> 2 + rng.nextInt(1);    // 2
            case MEDIUM -> 2 + rng.nextInt(2);   // 2–3
            case LARGE -> 3 + rng.nextInt(2);    // 3–4
        };


        while (true) {
            drawRoadAt(map, x, y, roadWidth, style);

            if (x == x1 && y == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    /**
     * Draws a road tile (or group of road tiles) at the given coordinate with a specified width.
     *
     * @param map The terrain map
     * @param x   X coordinate
     * @param y   Y coordinate
     * @param width How wide to make the road (square around center)
     * @param style The road style to apply
     */
    private static void drawRoadAt(
        TerrainTile[][] map,
        int x,
        int y,
        int width,
        RoadStyle style
    ) {
        int half = width / 2;

        for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && ny >= 0 && nx < map.length && ny < map[0].length) {
                    TerrainTile tile = map[nx][ny];
                    if (tile.getType() == TerrainType.WATER) continue;
                    tile.setType(TerrainType.DIRT);
                    tile.setHasRoad(true);
                    tile.setRoadStyle(style);
                    tile.setRoadTileId(RoadTileLookup.getTileIdForStyle(style));
                }
            }
        }
    }


    /**
     * Identifies the central lake region by collecting all WATER tiles
     * in the center 50% region of the map.
     *
     * @param terrainMap The terrain map
     * @return A set of Points representing the central lake body
     */
    public static Set<Point> getLakeCenterRegion(TerrainType[][] terrainMap) {
        Set<Point> lake = new HashSet<>();
        int width = terrainMap.length;
        int height = terrainMap[0].length;

        int startX = width / 4;
        int endX = width - startX;
        int startY = height / 4;
        int endY = height - startY;

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                if (terrainMap[x][y] == TerrainType.WATER) {
                    lake.add(new Point(x, y));
                }
            }
        }

        return lake;
    }

    /**
     * Selects a smart town center location that is not near the lake,
     * prefers GRASS or DIRT, and is not on WATER or SAND.
     *
     * @param terrainMap The terrain map
     * @param lake Set of lake points from getLakeCenterRegion()
     * @param rng Random instance
     * @return A Point representing the chosen town center
     */
    public static Point getSmartTownCenterLake(TerrainType[][] terrainMap, Set<Point> lake, Random rng) {
        int width = terrainMap.length;
        int height = terrainMap[0].length;
        List<Point> candidates = new ArrayList<>();

        // Define a buffer radius around the lake to avoid placing too close
        int buffer = 8;

        for (int x = buffer; x < width - buffer; x++) {
            for (int y = buffer; y < height - buffer; y++) {
                TerrainType type = terrainMap[x][y];
                if (type == TerrainType.GRASS || type == TerrainType.DIRT) {
                    Point p = new Point(x, y);
                    boolean nearLake = lake.stream().anyMatch(lakeTile -> lakeTile.distance(p) < buffer);
                    if (!nearLake) {
                        candidates.add(p);
                    }
                }
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(rng.nextInt(candidates.size()));
        }

        // Fallback: use center if no valid candidates
        return new Point(width / 2, height / 2);
    }


    /**
     * Selects a smart town center location for river maps.
     * Prefers GRASS or DIRT, avoids WATER and SAND, and places near the river but not too close.
     * Also biases location based on which edges the river touches.
     *
     * @param terrainMap The terrain map
     * @param rng Random instance
     * @return A Point representing the chosen town center
     */
    public static Point getSmartTownCenterRiver(TerrainType[][] terrainMap, Random rng) {
        int width = terrainMap.length;
        int height = terrainMap[0].length;
        List<Point> candidates = new ArrayList<>();
        List<Point> riverTiles = new ArrayList<>();

        // Scan terrain map for WATER tiles (river)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (terrainMap[x][y] == TerrainType.WATER) {
                    riverTiles.add(new Point(x, y));
                }
            }
        }

        // Early exit if no water detected
        if (riverTiles.isEmpty()) {
            return new Point(width / 2, height / 2);
        }

        // Minimum and maximum distance from river
        int minDist = 3;
        int maxDist = 10;

        // Detect river orientation and edge contact
        boolean touchesTop = riverTiles.stream().anyMatch(p -> p.y == 0);
        boolean touchesBottom = riverTiles.stream().anyMatch(p -> p.y == height - 1);
        boolean touchesLeft = riverTiles.stream().anyMatch(p -> p.x == 0);
        boolean touchesRight = riverTiles.stream().anyMatch(p -> p.x == width - 1);
        boolean verticalRiver = touchesTop && touchesBottom;

        // Gather candidate town center locations
        for (int x = 4; x < width - 4; x++) {
            for (int y = 4; y < height - 4; y++) {
                TerrainType type = terrainMap[x][y];
                if (type == TerrainType.GRASS || type == TerrainType.DIRT) {
                    Point p = new Point(x, y);
                    double dist = riverTiles.stream()
                                            .mapToDouble(rt -> rt.distance(p))
                                            .min()
                                            .orElse(Double.MAX_VALUE);
                    if (dist >= minDist && dist <= maxDist) {
                        candidates.add(p);
                    }
                }
            }
        }

        if (!candidates.isEmpty()) {
            // Bias toward centerline perpendicular to river flow, and opposite the main entry edge
            if (verticalRiver) {
                // River runs top→bottom
                if (touchesLeft && !touchesRight) {
                    candidates.sort(Comparator.comparingInt(p -> p.x)); // favor right side
                } else if (touchesRight && !touchesLeft) {
                    candidates.sort((p1, p2) -> Integer.compare(p2.x, p1.x)); // favor left side
                } else {
                    candidates.sort(Comparator.comparingInt(p -> Math.abs(p.x - width / 2)));
                }
            } else {
                // River runs left→right
                if (touchesTop && !touchesBottom) {
                    candidates.sort(Comparator.comparingInt(p -> p.y)); // favor bottom
                } else if (touchesBottom && !touchesTop) {
                    candidates.sort((p1, p2) -> Integer.compare(p2.y, p1.y)); // favor top
                } else {
                    candidates.sort(Comparator.comparingInt(p -> Math.abs(p.y - height / 2)));
                }
            }

            return candidates.get(rng.nextInt(Math.min(20, candidates.size())));
        }

        // Fallback: center of the map
        return new Point(width / 2, height / 2);
    }



    /**
     * Selects a smart town center location for seaside maps.
     * Prefers GRASS or DIRT, avoids WATER and SAND, and keeps away from the coastline.
     * Coastline is defined as any WATER tile directly adjacent to SAND.
     *
     * @param terrainMap The terrain map
     * @param rng Random instance
     * @return A Point representing the chosen town center
     */
    public static Point getSmartTownCenterSeaside(TerrainType[][] terrainMap, Random rng) {
        int width = terrainMap.length;
        int height = terrainMap[0].length;
        List<Point> candidates = new ArrayList<>();
        List<Point> coastline = new ArrayList<>();

        // Find all coastline water tiles (water touching sand)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (terrainMap[x][y] == TerrainType.WATER) {
                    boolean touchesSand = false;

                    // Check 4-direction neighbors
                    if (x > 0 && terrainMap[x - 1][y] == TerrainType.SAND) touchesSand = true;
                    if (x < width - 1 && terrainMap[x + 1][y] == TerrainType.SAND) touchesSand = true;
                    if (y > 0 && terrainMap[x][y - 1] == TerrainType.SAND) touchesSand = true;
                    if (y < height - 1 && terrainMap[x][y + 1] == TerrainType.SAND) touchesSand = true;

                    if (touchesSand) {
                        coastline.add(new Point(x, y));
                    }
                }
            }
        }

        // Early fallback if no coastline found
        if (coastline.isEmpty()) {
            return new Point(width / 2, height / 2);
        }

        // Define distance preferences from coastline
        int minDistFromCoast = 6;  // Keep off the shore
        int maxDistFromCoast = 20; // Still relatively close

        // Gather candidate inland locations
        for (int x = minDistFromCoast; x < width - minDistFromCoast; x++) {
            for (int y = minDistFromCoast; y < height - minDistFromCoast; y++) {
                TerrainType type = terrainMap[x][y];
                if (type == TerrainType.GRASS || type == TerrainType.DIRT) {
                    Point p = new Point(x, y);

                    double dist = coastline.stream()
                            .mapToDouble(ct -> ct.distance(p))
                            .min()
                            .orElse(Double.MAX_VALUE);

                    if (dist >= minDistFromCoast && dist <= maxDistFromCoast) {
                        candidates.add(p);
                    }
                }
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(rng.nextInt(candidates.size()));
        }

        // Fallback: center of the map
        return new Point(width / 2, height / 2);
    }




}
