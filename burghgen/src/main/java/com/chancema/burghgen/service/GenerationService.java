package com.chancema.burghgen.service;

import com.chancema.burghgen.generation.road.RoadGenerator;
import com.chancema.burghgen.model.MapSize;
import com.chancema.burghgen.model.TerrainTile;
import com.chancema.burghgen.model.TerrainType;
import com.chancema.burghgen.util.SimplexNoise;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * TerrainGenerationService handles the procedural generation of different types
 * of terrain maps for BurghGen. It supports rivers, lakes, and seaside landscapes,
 * and applies various land features using randomized algorithms with seed control.
 */
@Service
public class GenerationService {

    // List of coordinates representing water tiles that are adjacent to land.
    private final List<int[]> coastalWaterTiles = new ArrayList<>();

    // List of coordinates representing sand tiles that are adjacent to water.
    private final List<int[]> coastalSandTiles = new ArrayList<>();


    // ============================================
    // ========== Main Generative Method ==========
    // ============================================

    /**
     * Generates a 2D terrain grid of a specified type using procedural generation logic.
     * Supports 'river', 'lake', and 'seaside' terrain types.
     *
     * @param type   The terrain generation type ("river", "lake", or "seaside").
     * @param seed   The random seed used to ensure reproducible generation.
     * @param width  Width of the terrain grid.
     * @param height Height of the terrain grid.
     * @return A list of TerrainTile objects representing the final generated terrain.
     */
    public List<TerrainTile> generateGrid(String type, long seed, int width, int height) {
        TerrainType[][] map = new TerrainType[height][width];

        // Choose terrain generation algorithm based on the type
        if (type.equalsIgnoreCase("river")) {
            generateRiver(map, seed);
        } else if (type.equalsIgnoreCase("lake")) {
            generateLake(map, seed);
        } else if (type.equalsIgnoreCase("seaside")) {
            generateSeaside(map, seed);
        }

        SimplexNoise noiseGen = new SimplexNoise(seed);
        double scale = 0.05; // Lower = larger patches

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (map[y][x] == null) {
                    int dist = nearestCoastalSandDistance(x, y, coastalSandTiles);
                    double coastalBias = Math.max(0, (6 - dist) / 6.0); // 1.0 near coast, 0.0 far away

                    double noise = noiseGen.noise(x * scale, y * scale);

                    // If very close to sea, increase chance of sand
                    double sandThreshold = -0.5 + (coastalBias * 0.4); // raises threshold from -0.5 up to -0.1

                    if (coastalBias > 0.0 && noise < sandThreshold) {
                        map[y][x] = TerrainType.SAND;
                    } else if (noise < -0.2) {
                        map[y][x] = TerrainType.DIRT;
                    } else {
                        map[y][x] = TerrainType.GRASS;
                    }
                }
            }
        }

        TerrainTile[][] tileMap = new TerrainTile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tileMap[x][y] = new TerrainTile(x, y, map[x][y]);
            }
        }


        RoadGenerator.generateRoadNetwork(tileMap, width, height, seed, MapSize.fromDimensions(width, height), map, type);



        // Convert the raw map data to a list of TerrainTile objects for rendering
        List<TerrainTile> tiles = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles.add(tileMap[y][x]);
            }
        }


        return tiles;
    }


    // ============================================
    // ============== Utility Methods =============
    // ============================================

    /**
     * Checks if a given (x, y) coordinate is within the bounds of the map.
     *
     * @param map The terrain map grid.
     * @param x   The x-coordinate to check.
     * @param y   The y-coordinate to check.
     * @return True if the coordinate is within bounds, false otherwise.
     */
    private boolean inBounds(TerrainType[][] map, int x, int y) {
        return x >= 0 && x < map[0].length && y >= 0 && y < map.length;
    }

    /**
     * Checks if the given tile or any of its neighbors touches a specific terrain type.
     *
     * @param map  The terrain map grid.
     * @param x    X-coordinate of the target tile.
     * @param y    Y-coordinate of the target tile.
     * @param type The TerrainType to check for adjacency.
     * @return True if the tile touches the specified type, false otherwise.
     */
    private boolean touchesType(TerrainType[][] map, int x, int y, TerrainType type) {
        int width = map[0].length;
        int height = map.length;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                // Check all adjacent and diagonal neighbors
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && map[ny][nx] == type) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the tile or its neighbors touch water.
     */
    private boolean touchesWater(TerrainType[][] map, int x, int y) {
        return touchesType(map, x, y, TerrainType.WATER);
    }

    /**
     * Checks if the tile or its neighbors touch sand.
     */
    private boolean touchesSand(TerrainType[][] map, int x, int y) {
        return touchesType(map, x, y, TerrainType.SAND);
    }

    /**
     * Determines if the tile is touching land (i.e., any tile that is not water and not null).
     *
     * @param map The terrain map grid.
     * @param x   X-coordinate of the tile.
     * @param y   Y-coordinate of the tile.
     * @return True if touching land, false if fully surrounded by water or null.
     */
    private boolean touchesLand(TerrainType[][] map, int x, int y) {
        int width = map[0].length;
        int height = map.length;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                // Check for any adjacent tile that is not water and not null
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    TerrainType t = map[ny][nx];
                    if (t != TerrainType.WATER && t != null) return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a random coordinate on one of the edges of the map.
     *
     * @param width  Map width.
     * @param height Map height.
     * @param rng    The random number generator to use.
     * @return A coordinate array [x, y] representing the edge point.
     */
    private int[] randomEdgePoint(int width, int height, Random rng) {
        int side = rng.nextInt(4); // 0=top, 1=right, 2=bottom, 3=left
        return switch (side) {
            case 0 -> new int[]{rng.nextInt(width), 0};               // Top edge
            case 1 -> new int[]{width - 1, rng.nextInt(height)};      // Right edge
            case 2 -> new int[]{rng.nextInt(width), height - 1};      // Bottom edge
            default -> new int[]{0, rng.nextInt(height)};             // Left edge
        };
    }

    /**
     * Checks whether two edge points are on the same edge of the map.
     *
     * @param a      First coordinate.
     * @param b      Second coordinate.
     * @param width  Map width.
     * @param height Map height.
     * @return True if both points are on the same edge; false otherwise.
     */
    private boolean sameEdge(int[] a, int[] b, int width, int height) {
        return (a[0] == 0 && b[0] == 0) ||                          // Left edge
               (a[0] == width - 1 && b[0] == width - 1) ||          // Right edge
               (a[1] == 0 && b[1] == 0) ||                          // Top edge
               (a[1] == height - 1 && b[1] == height - 1);          // Bottom edge
    }

    /**
     * Ensures that two points are far enough apart for a meaningful river/lake path.
     *
     * @param a      First coordinate.
     * @param b      Second coordinate.
     * @param width  Map width.
     * @param height Map height.
     * @return True if the Manhattan distance between a and b is sufficiently large.
     */
    private boolean isFarEnough(int[] a, int[] b, int width, int height) {
        int dx = Math.abs(a[0] - b[0]);
        int dy = Math.abs(a[1] - b[1]);
        return (dx + dy) >= (width + height) / 2;
    }

    /**
     * Finds the nearest coastal sand tile distance from a given coordinate.
     *
     * @param x      X-coordinate to check.
     * @param y      Y-coordinate to check.
     * @param coastalSandTiles List of coordinates representing coastal sand tiles.
     * @return The distance to the nearest coastal sand tile.
     */
    private int nearestCoastalSandDistance(int x, int y, List<int[]> coastalSandTiles) {
        int minDist = Integer.MAX_VALUE;
        for (int[] tile : coastalSandTiles) {
            int dx = x - tile[0];
            int dy = y - tile[1];
            int distSq = dx * dx + dy * dy;
            if (distSq < minDist) {
                minDist = distSq;
            }
        }
        return (int) Math.sqrt(minDist);
    }


    // ============================================
    // ============ River Generation ==============
    // ============================================

    /**
     * Generates a primary river with optional branches and loops.
     *
     * @param map 2D grid of TerrainTypes to write into.
     * @param seed RNG seed for deterministic generation.
     */
    private void generateRiver(TerrainType[][] map, long seed) {
        Random rng = new Random(seed);
        int height = map.length;
        int width = map[0].length;

        // River thickness scales with map size
        int riverThickness = Math.max(1, Math.min(3, Math.min(width, height) / 32));

        // Start and end points must be on different edges and far apart
        int[] start = randomEdgePoint(width, height, rng);
        int[] end = randomEdgePoint(width, height, rng);
        while (sameEdge(start, end, width, height) || !isFarEnough(start, end, width, height)) {
            end = randomEdgePoint(width, height, rng);
        }

        // Begin carving main river path
        List<int[]> mainRiverPath = new ArrayList<>();
        int x = start[0];
        int y = start[1];

        // Carve a meandering river from start to end
        while (x != end[0] || y != end[1]) {
            carveCircle(map, x, y, riverThickness);  // Carve a river segment
            mainRiverPath.add(new int[]{x, y});      // Track the path

            // Bias movement toward end point
            if (rng.nextDouble() < 0.6) {
                if (x < end[0]) x++;
                else if (x > end[0]) x--;
            } else {
                if (y < end[1]) y++;
                else if (y > end[1]) y--;
            }

            // Occasional wobble for natural shape
            if (rng.nextDouble() < 0.2) {
                x += rng.nextInt(3) - 1;
                y += rng.nextInt(3) - 1;
            }

            // Clamp to map bounds
            x = Math.max(0, Math.min(width - 1, x));
            y = Math.max(0, Math.min(height - 1, y));
        }

        // Optional rejoining branch (loop)
        if (rng.nextDouble() < 0.5 && mainRiverPath.size() > 20) {
            int maxTries = 10;
            for (int attempt = 0; attempt < maxTries; attempt++) {
                int i1 = rng.nextInt(mainRiverPath.size() / 2);
                int i2 = mainRiverPath.size() / 2 + rng.nextInt(mainRiverPath.size() / 2);

                // Ensure points aren't too close together
                if (Math.abs(i1 - i2) < mainRiverPath.size() / 4) continue;

                int[] loopStart = mainRiverPath.get(i1);
                int[] loopEnd = mainRiverPath.get(i2);

                // Carve a small offshoot path that rejoins
                carveBranch(map, loopStart[0], loopStart[1], loopEnd[0], loopEnd[1], rng, riverThickness - 1);
                break;
            }
        }

        // Optional diverging branch that exits to a different edge
        if (rng.nextDouble() < 0.5 && mainRiverPath.size() > 20) {
            int[] branchSource = mainRiverPath.get(mainRiverPath.size() / 2 + rng.nextInt(mainRiverPath.size() / 2));
            int[] exit = randomEdgePoint(width, height, rng);

            // Avoid same-edge exits
            while (sameEdge(branchSource, exit, width, height)) {
                exit = randomEdgePoint(width, height, rng);
            }

            // Carve a diverging branch toward the new edge
            carveBranch(map, branchSource[0], branchSource[1], exit[0], exit[1], rng, riverThickness - 1);
        }
    }

    /**
     * Carves a river path from a starting point to an endpoint using randomized directional movement.
     * Each step adds a circular water feature and tracks the tile positions for later use (e.g., delta generation).
     *
     * @param map       The terrain map to modify.
     * @param startX    Starting X coordinate.
     * @param startY    Starting Y coordinate.
     * @param endX      Ending X coordinate.
     * @param endY      Ending Y coordinate.
     * @param rng       Random generator with consistent seed.
     * @param thickness Radius of the circular water path to carve.
     * @return          A list of all tile positions that were carved (in order).
     */
    private List<int[]> carveAndTrackRiverPath(TerrainType[][] map, int startX, int startY, int endX, int endY, Random rng, int thickness) {
        List<int[]> path = new ArrayList<>();
        int x = startX;
        int y = startY;

        int width = map[0].length;
        int height = map.length;

        // Loop until endpoint is reached
        while (x != endX || y != endY) {
            carveCircle(map, x, y, thickness);  // Carve water at current position
            path.add(new int[]{x, y});          // Track the tile for return

            // Bias movement toward end point
            if (rng.nextDouble() < 0.6) {
                x += Integer.compare(endX, x);  // Move in X direction
            } else {
                y += Integer.compare(endY, y);  // Move in Y direction
            }

            // Add some jitter to simulate natural river bends
            if (rng.nextDouble() < 0.2) {
                x += rng.nextInt(3) - 1;
                y += rng.nextInt(3) - 1;
            }

            // Clamp coordinates to map boundaries
            x = Math.max(0, Math.min(width - 1, x));
            y = Math.max(0, Math.min(height - 1, y));
        }

        return path;
    }


    /**
     * Carves a river from a start to an end point without returning the carved path.
     * This is a convenience wrapper around carveAndTrackRiverPath().
     *
     * @param map       The terrain map to modify.
     * @param startX    Starting X coordinate.
     * @param startY    Starting Y coordinate.
     * @param endX      Ending X coordinate.
     * @param endY      Ending Y coordinate.
     * @param rng       Random generator with consistent seed.
     * @param thickness Radius of the river at each step.
     */
    private void carveRiverPath(TerrainType[][] map, int startX, int startY, int endX, int endY, Random rng, int thickness) {
        // Simply call the path-tracking variant but ignore the return value
        carveAndTrackRiverPath(map, startX, startY, endX, endY, rng, thickness);
    }

    // ============================================
    // ============= Lake Generation ==============
    // ============================================

    /**
     * Generates a central lake with natural edges, optional offshoot ponds,
     * small grass islands, and a possible outflow river.
     *
     * @param map 2D grid of TerrainTypes to modify.
     * @param seed RNG seed for deterministic results.
     */
    private void generateLake(TerrainType[][] map, long seed) {
        Random rng = new Random(seed);
        int height = map.length;
        int width = map[0].length;

        // Lake starts near the center of the map
        int cx = width / 2;
        int cy = height / 2;

        // Lake target size: 5%–15% of total map area
        int targetSize = (width * height) / (10 + rng.nextInt(10));
        int added = 0;

        Set<String> visited = new HashSet<>();
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{cx, cy});
        visited.add(cx + "," + cy);

        // Perform a randomized flood fill outward from the center
        while (!queue.isEmpty() && added < targetSize) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];

            if (x < 0 || x >= width || y < 0 || y >= height) continue;
            if (map[y][x] == TerrainType.WATER) continue;

            map[y][x] = TerrainType.WATER;
            added++;

            // Expand to neighboring cells based on a noise-like threshold
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = x + dx;
                    int ny = y + dy;
                    String key = nx + "," + ny;

                    if (!visited.contains(key) && rng.nextDouble() < 0.65 + rng.nextDouble() * 0.25) {
                        queue.add(new int[]{nx, ny});
                        visited.add(key);
                    }
                }
            }

            // Occasionally jump to a nearby area to vary shape
            if (rng.nextDouble() < 0.1) {
                int ox = x + rng.nextInt(7) - 3;
                int oy = y + rng.nextInt(7) - 3;
                String key = ox + "," + oy;
                if (!visited.contains(key)) {
                    queue.add(new int[]{ox, oy});
                    visited.add(key);
                }
            }
        }

        // Add 0–2 offshoot ponds near the main lake
        int offshoots = rng.nextInt(3);
        for (int i = 0; i < offshoots; i++) {
            int px = cx + rng.nextInt(width / 4) - width / 8;
            int py = cy + rng.nextInt(height / 4) - height / 8;
            int r = 1 + rng.nextInt(2);

            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int nx = px + dx;
                    int ny = py + dy;
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height && dx * dx + dy * dy <= r * r) {
                        map[ny][nx] = TerrainType.WATER;
                    }
                }
            }
        }

        // Add up to 2 small grassy islands inside the lake
        for (int i = 0; i < 2; i++) {
            for (int tries = 0; tries < 20; tries++) {
                int ix = rng.nextInt(width);
                int iy = rng.nextInt(height);
                if (isSurroundedByWater(map, ix, iy)) {
                    map[iy][ix] = TerrainType.GRASS;
                    break;
                }
            }
        }

        // Optionally generate an outflow river from the lake to map edge
        if (rng.nextDouble() < 0.5) {
            List<int[]> lakeEdge = getLakeEdge(map);
            if (!lakeEdge.isEmpty()) {
                int[] start = lakeEdge.get(rng.nextInt(lakeEdge.size()));
                int[] end = randomEdgePoint(width, height, rng);
                if (isFarEnough(start, end, width, height)) {
                    carveRiverPath(map, start[0], start[1], end[0], end[1], rng, 1);
                }
            }
        }
    }

    /**
     * Finds water tiles that touch non-water terrain, identifying lake edge.
     *
     * @param map The terrain map to scan.
     * @return A list of tile coordinates on the lake edge.
     */
    private List<int[]> getLakeEdge(TerrainType[][] map) {
        List<int[]> edge = new ArrayList<>();
        int width = map[0].length;
        int height = map.length;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (map[y][x] == TerrainType.WATER) {
                    if (map[y - 1][x] != TerrainType.WATER ||
                        map[y + 1][x] != TerrainType.WATER ||
                        map[y][x - 1] != TerrainType.WATER ||
                        map[y][x + 1] != TerrainType.WATER) {
                        edge.add(new int[]{x, y});
                    }
                }
            }
        }

        return edge;
    }

    /**
     * Determines if a tile is fully surrounded by water on all 8 sides.
     *
     * @param map Terrain grid to inspect.
     * @param x Tile x-coordinate.
     * @param y Tile y-coordinate.
     * @return True if all neighbors are WATER, false otherwise.
     */
    private boolean isSurroundedByWater(TerrainType[][] map, int x, int y) {
        int width = map[0].length;
        int height = map.length;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) return false;
                if (map[ny][nx] != TerrainType.WATER) return false;
            }
        }
        return true;
    }

    // ============================================
    // =========== Seaside Generation =============
    // ============================================

    /**
     * Generates a seaside landscape with coastal water, sand, and optional features like capes and inlets.
     * It carves a sinusoidal coastline and can create harbors based on random conditions.
     *
     * @param map 2D grid of TerrainTypes to modify.
     * @param seed RNG seed for deterministic generation.
     */
    @SuppressWarnings({ "unused" })
    private void generateSeaside(TerrainType[][] map, long seed) {
        Random rng = new Random(seed);
        coastalWaterTiles.clear();
        coastalSandTiles.clear();
        HarborTarget harborTarget = new HarborTarget();
        boolean[] harborCreated = new boolean[] { false };


        int height = map.length;
        int width = map[0].length;

        int direction = rng.nextInt(8);
        int depthX = (int) (width * (0.20 + rng.nextDouble() * 0.20));
        int depthY = (int) (height * (0.20 + rng.nextDouble() * 0.20));

        boolean[][] isWater = new boolean[height][width];

        double freq = 2 * Math.PI / (direction % 2 == 0 ? width : height);
        double offset = rng.nextDouble() * 2 * Math.PI;

        switch (direction) {
            case 0 -> fillTop(map, isWater, height, width, depthY, freq, offset, rng, harborTarget);
            case 1 -> fillRight(map, isWater, height, width, depthX, freq, offset, rng, harborTarget);
            case 2 -> fillBottom(map, isWater, height, width, depthY, freq, offset, rng, harborTarget);
            case 3 -> fillLeft(map, isWater, height, width, depthX, freq, offset, rng, harborTarget);
            case 4 -> {
                fillTop(map, isWater, height, width, depthY, freq, offset, rng, harborTarget);
                fillLeft(map, isWater, height, width, depthX, freq, offset + 1.0, rng, harborTarget);
            }
            case 5 -> {
                fillTop(map, isWater, height, width, depthY, freq, offset, rng, harborTarget);
                fillRight(map, isWater, height, width, depthX, freq, offset + 1.0, rng, harborTarget);
            }
            case 6 -> {
                fillBottom(map, isWater, height, width, depthY, freq, offset, rng, harborTarget);
                fillRight(map, isWater, height, width, depthX, freq, offset + 1.0, rng, harborTarget);
            }
            case 7 -> {
                fillBottom(map, isWater, height, width, depthY, freq, offset, rng, harborTarget);
                fillLeft(map, isWater, height, width, depthX, freq, offset + 1.0, rng, harborTarget);
            }
        }


        int minorFeatureCount = switch (width) {
            case 64 -> rng.nextInt(4);
            case 128 -> rng.nextInt(9);
            default -> rng.nextInt(21);
        };

        int numCapes = rng.nextInt(minorFeatureCount + 1);
        int numInlets = minorFeatureCount - numCapes;

        applyWaterToMap(map, isWater);
        addSandBuffer(map);
        detectCoastalWater(map);
        detectCoastalSand(map);
        addCapes(map, isWater, rng, numCapes);
        addInlets(map, isWater, rng, numInlets);

        if (rng.nextDouble() < 0.4) {
            generateSeasideRiver(map, isWater, rng);
            if (rng.nextDouble() < 0.25) {
                generateSeasideRiver(map, isWater, rng);
            }
        }

        if (!harborCreated[0] && rng.nextDouble() < 1.0 && harborTarget.direction != null) {
            Optional<int[]> anchor = coastalSandTiles.stream()
                .filter(tile -> switch (harborTarget.direction) {
                    case "TOP", "BOTTOM" -> tile[0] == harborTarget.wave; 
                    case "LEFT", "RIGHT" -> tile[1] == harborTarget.wave;
                    default -> false;
                })
                .min(Comparator.comparingInt(tile -> switch (harborTarget.direction) {
                    case "TOP"    -> Math.abs(tile[1]);
                    case "BOTTOM" -> Math.abs(tile[1] - (height - 1));
                    case "LEFT"   -> Math.abs(tile[0]);
                    case "RIGHT"  -> Math.abs(tile[0] - (width - 1));
                    default       -> Integer.MAX_VALUE;
                }));

            anchor.ifPresent(tile -> {
                carveHarborPocket(map, isWater, tile[0], tile[1], harborTarget.direction, rng);
                harborCreated[0] = true;
            });

        }

    }

    /**
     * Fills a sinusoidal coastline along the top edge of the map.
     *
     * @param map     Terrain map (not modified directly here).
     * @param water   Boolean grid representing water placement.
     * @param height  Map height.
     * @param width   Map width.
     * @param depth   Maximum vertical depth of the sea penetration.
     * @param freq    Frequency of sine wave.
     * @param offset  Phase shift of sine wave.
     * @param rng     Random number generator for harbor creation.
     * @param harborTarget Object to track the best harbor location.
     */
    private void fillTop(TerrainType[][] map, boolean[][] water, int height, int width, int depth,
                            double freq, double offset, Random rng, HarborTarget harborTarget) {
        for (int x = 0; x < width; x++) {
            double wave = Math.sin(freq * x + offset);
            int d = (int) (depth * (0.7 + 0.3 * wave));
            for (int y = 0; y < d && y < height; y++) {
                water[y][x] = true;
            }

            if (Math.abs(wave) > Math.abs(harborTarget.wave)) {
                harborTarget.wave = wave;
                harborTarget.axis = x;
                harborTarget.direction = "TOP";
            }
        }
    }




    /**
     * Fills a sinusoidal coastline along the bottom edge of the map.
     *
     * @param map     Terrain map (not modified directly here).
     * @param water   Boolean grid representing water placement.
     * @param height  Map height.
     * @param width   Map width.
     * @param depth   Maximum vertical depth of the sea penetration.
     * @param freq    Frequency of sine wave.
     * @param offset  Phase shift of sine wave.
     * @param rng     Random number generator for harbor creation.
     * @param harborTarget Object to track the best harbor location.
     */
    private void fillBottom(TerrainType[][] map, boolean[][] water, int height, int width, int depth,
                                double freq, double offset, Random rng, HarborTarget harborTarget) {
        for (int x = 0; x < width; x++) {
            double wave = Math.sin(freq * x + offset);
            int d = (int) (depth * (0.7 + 0.3 * wave));
            for (int y = height - 1; y >= height - d && y >= 0; y--) {
                water[y][x] = true;
            }

            if (Math.abs(wave) > Math.abs(harborTarget.wave)) {
                harborTarget.wave = wave;
                harborTarget.axis = x;
                harborTarget.direction = "BOTTOM";
            }
        }
    }



    /**
     * Fills a sinusoidal coastline along the left edge of the map.
     *
     * @param map     Terrain map (not modified directly here).
     * @param water   Boolean grid representing water placement.
     * @param height  Map height.
     * @param width   Map width.
     * @param depth   Maximum vertical depth of the sea penetration.
     * @param freq    Frequency of sine wave.
     * @param offset  Phase shift of sine wave.
     * @param rng     Random number generator for harbor creation.
     * @param harborTarget Object to track the best harbor location.
     */
    private void fillLeft(TerrainType[][] map, boolean[][] water, int height, int width, int depth,
                              double freq, double offset, Random rng, HarborTarget harborTarget) {
        for (int y = 0; y < height; y++) {
            double wave = Math.sin(freq * y + offset);
            int d = (int) (depth * (0.7 + 0.3 * wave));
            for (int x = 0; x < d && x < width; x++) {
                water[y][x] = true;
            }

            if (Math.abs(wave) > Math.abs(harborTarget.wave)) {
                harborTarget.wave = wave;
                harborTarget.axis = y;
                harborTarget.direction = "LEFT";
            }
        }
    }



    /**
     * Fills a sinusoidal coastline along the right edge of the map.
     *
     * @param map     Terrain map (not modified directly here).
     * @param water   Boolean grid representing water placement.
     * @param height  Map height.
     * @param width   Map width.
     * @param depth   Maximum vertical depth of the sea penetration.
     * @param freq    Frequency of sine wave.
     * @param offset  Phase shift of sine wave.
     * @param rng     Random number generator for harbor creation.
     * @param harborTarget Object to track the best harbor location.
     */
    private void fillRight(TerrainType[][] map, boolean[][] water, int height, int width, int depth,
                             double freq, double offset, Random rng, HarborTarget harborTarget) {
        for (int y = 0; y < height; y++) {
            double wave = Math.sin(freq * y + offset);
            int d = (int) (depth * (0.7 + 0.3 * wave));
            for (int x = width - 1; x >= width - d && x >= 0; x--) {
                water[y][x] = true;
            }

            if (Math.abs(wave) > Math.abs(harborTarget.wave)) {
                harborTarget.wave = wave;
                harborTarget.axis = y;
                harborTarget.direction = "RIGHT";
            }
        }
    }



    /**
     * Applies all `true` flags in the isWater boolean map to mark the corresponding tiles as WATER.
     * 
     * @param map      The terrain map to modify.
     * @param isWater  Boolean grid indicating where water should be placed.
     */
    private void applyWaterToMap(TerrainType[][] map, boolean[][] isWater) {
        int height = map.length;
        int width = map[0].length;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isWater[y][x]) {
                    map[y][x] = TerrainType.WATER;
                }
            }
        }
    }

    /**
     * Generates a single river that flows into the sea from a random land edge, terminating
     * at a coastal water tile. May also create a delta at the mouth of the river.
     *
     * @param map      The terrain map to modify.
     * @param isWater  Boolean map representing seaside water areas.
     * @param rng      Random generator with consistent seed.
     */
    private void generateSeasideRiver(TerrainType[][] map, boolean[][] isWater, Random rng) {
        int width = map[0].length;
        int height = map.length;

        int riverThickness = Math.max(1, Math.min(3, Math.min(width, height) / 32));

        // Choose a valid land-based start point
        int[] start = randomEdgePoint(width, height, rng);
        while (map[start[1]][start[0]] == TerrainType.WATER) {
            start = randomEdgePoint(width, height, rng);
        }

        // Gather all valid end points that are WATER and adjacent to SAND
        List<int[]> validEnds = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (map[y][x] == TerrainType.WATER && touchesSand(map, x, y)) {
                    validEnds.add(new int[]{x, y});
                }
            }
        }

        if (validEnds.isEmpty()) return;

        // Choose a random valid river endpoint and carve the main river
        int[] end = validEnds.get(rng.nextInt(validEnds.size()));
        carveRiverPath(map, start[0], start[1], end[0], end[1], rng, riverThickness);

    }

    // ============================================
    // =========== Feature Carving ================
    // ============================================

    /**
     * Carves a circular patch of water terrain centered at the given coordinates.
     * Used for shaping rivers, branches, and other rounded features.
     *
     * @param map     The terrain map to modify.
     * @param cx      Center X coordinate.
     * @param cy      Center Y coordinate.
     * @param radius  Radius of the circle to carve.
     */
    private void carveCircle(TerrainType[][] map, int cx, int cy, int radius) {
        int height = map.length;
        int width = map[0].length;

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = cx + dx;
                int y = cy + dy;

                // Ensure point is within map bounds and inside circle radius
                if (x >= 0 && x < width && y >= 0 && y < height && dx * dx + dy * dy <= radius * radius) {
                    map[y][x] = TerrainType.WATER;
                }
            }
        }
    }

    /**
     * Carves a linear branch of river terrain from the start point to the end point.
     * Adds mild curvature and irregularity using randomness for natural appearance.
     *
     * @param map       The terrain map to modify.
     * @param startX    Starting X coordinate.
     * @param startY    Starting Y coordinate.
     * @param endX      Ending X coordinate.
     * @param endY      Ending Y coordinate.
     * @param rng       Random generator with consistent seed.
     * @param thickness Radius of each circle along the branch path.
     */
    private void carveBranch(TerrainType[][] map, int startX, int startY, int endX, int endY, Random rng, int thickness) {
        int width = map[0].length;
        int height = map.length;
        int x = startX;
        int y = startY;

        while (x != endX || y != endY) {
            // Draw a water circle at current position
            carveCircle(map, x, y, Math.max(1, thickness));

            // Progress mostly toward end, with bias
            if (rng.nextDouble() < 0.6) {
                x += Integer.compare(endX, x);
            } else {
                y += Integer.compare(endY, y);
            }

            // Add lateral variation (wobble)
            if (rng.nextDouble() < 0.2) {
                x += rng.nextInt(3) - 1;
                y += rng.nextInt(3) - 1;
            }

            // Clamp to map bounds
            x = Math.max(0, Math.min(width - 1, x));
            y = Math.max(0, Math.min(height - 1, y));
        }
    }


    /**
     * Carves a cape (sand terrain extending into the sea) starting from a water tile.
     * Parameters vary based on map size to ensure appropriate scale.
     *
     * @param map  The terrain map to modify.
     * @param x    Starting X coordinate.
     * @param y    Starting Y coordinate.
     * @param dx   X direction vector away from land.
     * @param dy   Y direction vector away from land.
     * @param rng  Random generator with consistent seed.
     */
    private void carveNaturalCape(TerrainType[][] map, int x, int y, int dx, int dy, Random rng) {
        int width = map[0].length;

        // Determine parameters based on map size
        int minSteps   = (width == 64)  ? 4 : (width == 128) ? 6 : 9;
        int stepRange  = (width == 64)  ? 2 : (width == 128) ? 3 : 4;
        int maxRadius  = (width == 64)  ? 1 : (width == 128) ? 2 : 2 + rng.nextInt(2);

        // Carve a sand feature outward
        carveNaturalFeature(map, x, y, dx, dy, rng, TerrainType.SAND, minSteps, stepRange, maxRadius, Math.PI / 6, false);
    }


    /**
     * Carves an inlet (water extending into land) starting from a sand tile.
     * Parameters vary based on map size for scaling.
     *
     * @param map  The terrain map to modify.
     * @param x    Starting X coordinate.
     * @param y    Starting Y coordinate.
     * @param dx   X direction vector away from water.
     * @param dy   Y direction vector away from water.
     * @param rng  Random generator with consistent seed.
     */
    private void carveNaturalInlet(TerrainType[][] map, int x, int y, int dx, int dy, Random rng) {
        int width = map[0].length;

        // Define inlet scale based on map size
        int minSteps   = (width == 64)  ? 6  : (width == 128) ? 10 : 14;
        int stepRange  = (width == 64)  ? 4  : (width == 128) ? 4  : 6;
        int maxRadius  = (width == 64)  ? 1  : (width == 128) ? 2  : 3;

        // Carve a water path inward, surrounded by sand
        carveNaturalFeature(map, x, y, dx, dy, rng, TerrainType.WATER, minSteps, stepRange, maxRadius, Math.PI / 4, true);
    }


    /**
     * Carves a randomized, curving directional feature using a sequence of overlapping circles.
     * Used for both capes and inlets, supports optional sand padding around the feature.
     *
     * @param map            The terrain map to modify.
     * @param startX         Starting X coordinate.
     * @param startY         Starting Y coordinate.
     * @param dx             Initial direction vector X component.
     * @param dy             Initial direction vector Y component.
     * @param rng            Random generator with consistent seed.
     * @param type           Terrain type to carve (WATER or SAND).
     * @param minSteps       Minimum number of steps (length).
     * @param stepRange      Range to vary number of steps.
     * @param maxRadius      Maximum radius for carved area per step.
     * @param angleVariance  Maximum angular variance in direction between steps (in radians).
     * @param addSandAround  Whether to add sand padding around the carved area.
     */
    private void carveNaturalFeature(
        TerrainType[][] map, int startX, int startY, int dx, int dy, Random rng, TerrainType type,
        int minSteps, int stepRange, int maxRadius, double angleVariance, boolean addSandAround
    ) {
        List<int[]> carvedTiles = new ArrayList<>();

        // Normalize direction vector
        double len = Math.sqrt(dx * dx + dy * dy);
        double dirX = dx / len;
        double dirY = dy / len;

        int steps = minSteps + rng.nextInt(stepRange + 1);
        int x = startX;
        int y = startY;

        for (int i = 0; i < steps; i++) {
            // Carve main point
            if (inBounds(map, x, y)) {
                map[y][x] = type;
                carvedTiles.add(new int[]{x, y});
            }

            // Carve circular area around current point
            int radius = 1 + rng.nextInt(maxRadius);
            for (int oy = -radius; oy <= radius; oy++) {
                for (int ox = -radius; ox <= radius; ox++) {
                    int nx = x + ox;
                    int ny = y + oy;
                    if (inBounds(map, nx, ny) && ox * ox + oy * oy <= radius * radius) {
                        map[ny][nx] = type;
                        carvedTiles.add(new int[]{nx, ny});
                    }
                }
            }

            // Slight curve: rotate direction by small angle
            double angleChange = (rng.nextDouble() - 0.5) * angleVariance;
            double cos = Math.cos(angleChange);
            double sin = Math.sin(angleChange);
            double newDirX = dirX * cos - dirY * sin;
            double newDirY = dirX * sin + dirY * cos;
            dirX = newDirX;
            dirY = newDirY;

            // Advance position
            x += (int) Math.round(dirX);
            y += (int) Math.round(dirY);

            if (!inBounds(map, x, y)) break;
        }

        // Optionally add sand padding around feature
        if (addSandAround) {
            for (int[] tile : carvedTiles) {
                int cx = tile[0];
                int cy = tile[1];
                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        int nx = cx + ox;
                        int ny = cy + oy;
                        if (inBounds(map, nx, ny)) {
                            TerrainType t = map[ny][nx];
                            if (t != TerrainType.WATER && t != TerrainType.SAND) {
                                map[ny][nx] = TerrainType.SAND;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Carves a harbor pocket at the specified coordinates in the given direction.
     * The size of the pocket is determined by the map dimensions and random factors.
     * Currently in progress, not fully implemented.
     *
     * @param map       The terrain map to modify.
     * @param water     Boolean grid indicating water placement.
     * @param cx        Center X coordinate for the harbor pocket.
     * @param cy        Center Y coordinate for the harbor pocket.
     * @param direction Direction of the harbor (TOP, BOTTOM, LEFT, RIGHT).
     * @param rng       Random generator with consistent seed.
     */
    private void carveHarborPocket(TerrainType[][] map, boolean[][] water, int cx, int cy, String direction, Random rng) {
        System.out.println("Carving harbor pocket at (" + cx + ", " + cy + ") in direction: " + direction);
        int width = map[0].length;
        int height = map.length;

        int size = Math.max(3, Math.min(width, height) / 20); // size scales with map
        int radius = size + rng.nextInt(2);

        System.out.printf("Harbor carving at (%d, %d) direction=%s radius=%d\n", cx, cy, direction, radius);


        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = cx + dx;
                int y = cy + dy;

                if (x < 0 || y < 0 || x >= width || y >= height) continue;

                // Round harbor shape
                if (dx * dx + dy * dy <= radius * radius) {
                    switch (direction) {
                        case "TOP" -> {
                            if (dy >= 0) { water[y][x] = true; System.out.printf("Carved water at (%d, %d)\n", x, y); }
                        }
                        case "BOTTOM" -> {
                            if (dy <= 0) { water[y][x] = true; System.out.printf("Carved water at (%d, %d)\n", x, y); }
                        }
                        case "LEFT" -> {
                            if (dx >= 0) { water[y][x] = true; System.out.printf("Carved water at (%d, %d)\n", x, y); }
                        }
                        case "RIGHT" -> {
                            if (dx <= 0) { water[y][x] = true; System.out.printf("Carved water at (%d, %d)\n", x, y); }
                        }
                    }
                }
            }
        }
    }



    


    // ============================================
    // ===== Coastal Detection & Manipulation =====
    // ============================================



    /**
     * Finds a direction from the given tile that continues into water
     * and is not adjacent to any land. Used for carving capes.
     *
     * @param map The terrain map to examine.
     * @param x   X-coordinate of the origin tile.
     * @param y   Y-coordinate of the origin tile.
     * @return A direction vector {dx, dy}, or null if no valid direction found.
     */
    private int[] bestDirectionAwayFromLand(TerrainType[][] map, int x, int y) {
        int[][] dirs = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {-1, -1}, {-1, 1}, {1, -1}
        };

        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];

            // Look for neighboring water that does not touch land
            if (inBounds(map, nx, ny) &&
                map[ny][nx] == TerrainType.WATER &&
                !touchesLand(map, nx, ny)) {
                return dir;
            }
        }

        return null;
    }


    /**
     * Finds a direction from the given tile that leads away from water.
     * Used for carving inlets further into land.
     *
     * @param map The terrain map to examine.
     * @param x   X-coordinate of the origin tile.
     * @param y   Y-coordinate of the origin tile.
     * @return A direction vector {dx, dy}, or null if no valid direction found.
     */
    private int[] bestDirectionAwayFromWater(TerrainType[][] map, int x, int y) {
        int[][] dirs = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {-1, -1}, {-1, 1}, {1, -1}
        };

        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];

            // Look for a neighboring non-water tile
            if (inBounds(map, nx, ny) &&
                map[ny][nx] != TerrainType.WATER) {
                return dir;
            }
        }

        return null;
    }
   

    /**
     * Converts any null land tile adjacent to water into sand, creating a shoreline buffer.
     *
     * @param map The terrain map to modify.
     */
    private void addSandBuffer(TerrainType[][] map) {
        int height = map.length;
        int width = map[0].length;

        // Iterate through the entire map grid
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Only modify null (unassigned) tiles that touch water
                if (map[y][x] == null && touchesWater(map, x, y)) {
                    map[y][x] = TerrainType.SAND;
                }
            }
        }
    }


    /**
     * Populates the coastalWaterTiles list with water tiles that border land.
     * Used for features like capes or rivers flowing into the sea.
     *
     * @param map The terrain map to scan.
     */
    private void detectCoastalWater(TerrainType[][] map) {
        coastalWaterTiles.clear();

        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[0].length; x++) {
                // Find water tiles that touch at least one land tile
                if (map[y][x] == TerrainType.WATER && touchesLand(map, x, y)) {
                    coastalWaterTiles.add(new int[]{x, y});
                }
            }
        }
    }


    /**
     * Populates the coastalSandTiles list with sand tiles that border water.
     * Used for features like inlets that push further into land.
     *
     * @param map The terrain map to scan.
     */
    private void detectCoastalSand(TerrainType[][] map) {
        coastalSandTiles.clear();

        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[0].length; x++) {
                // Find sand tiles that touch at least one water tile
                if (map[y][x] == TerrainType.SAND && touchesWater(map, x, y)) {
                    coastalSandTiles.add(new int[]{x, y});
                }
            }
        }
    }


    /**
     * Adds a specified number of cape features extending outward from the coast into the sea.
     * Each cape originates on a water tile touching land and carves outward through sand.
     *
     * @param map      The terrain map to modify.
     * @param isWater  Boolean map indicating water tiles.
     * @param rng      Random generator with consistent seed.
     * @param count    Number of capes to attempt to add.
     */
    private void addCapes(TerrainType[][] map, boolean[][] isWater, Random rng, int count) {
        Collections.shuffle(coastalWaterTiles, rng); // Shuffle water edge candidates for variation

        for (int i = 0; i < Math.min(count, coastalWaterTiles.size()); i++) {
            int[] pos = coastalWaterTiles.get(i);
            int x = pos[0];
            int y = pos[1];

            // Find the best direction that leads further into water
            int[] dir = bestDirectionAwayFromLand(map, x, y);
            if (dir == null) continue;

            // Carve the cape outward from the coastline
            carveNaturalCape(map, x, y, dir[0], dir[1], rng);
        }
    }


    /**
     * Adds a specified number of inlet features extending inward from the shore.
     * Each inlet originates on a sand tile touching water and carves a natural water path into land.
     *
     * @param map      The terrain map to modify.
     * @param isWater  Boolean map indicating water tiles.
     * @param rng      Random generator with consistent seed.
     * @param count    Number of inlets to attempt to add.
     */
    private void addInlets(TerrainType[][] map, boolean[][] isWater, Random rng, int count) {
        Collections.shuffle(coastalSandTiles, rng); // Shuffle sand edge candidates

        for (int i = 0; i < Math.min(count, coastalSandTiles.size()); i++) {
            int[] pos = coastalSandTiles.get(i);
            int x = pos[0];
            int y = pos[1];

            // Determine the direction that leads away from the sea into land
            int[] dir = bestDirectionAwayFromWater(map, x, y);
            if (dir == null) continue;

            // Carve a natural water inlet into the terrain
            carveNaturalInlet(map, x, y, dir[0], dir[1], rng);
        }
    }

    /**
     * Represents a target location for harbor carving, including wave height and axis.
     * Used to track the best location for harbor creation based on terrain features.
     */
    public static class HarborTarget {
        public double wave = 0.0;
        public int axis = -1;
        public String direction = null;
    }


}

