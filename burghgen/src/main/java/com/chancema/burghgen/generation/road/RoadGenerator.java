package com.chancema.burghgen.generation.road;

import com.chancema.burghgen.model.MapSize;
import com.chancema.burghgen.model.RoadStyle;
import com.chancema.burghgen.model.TerrainTile;
import com.chancema.burghgen.model.TerrainType;

import java.awt.Point;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Responsible for generating the primary and secondary road network for the town.
 * Combines Voronoi-style node distribution with a handcrafted radial logic
 * to simulate a natural but cohesive medieval road system.
 */
public class RoadGenerator {

    /**
     * Generates roads on the terrain map.
     *
     * @param map   2D array of terrain tiles
     * @param width  Map width
     * @param height Map height
     * @param seed   Random seed for reproducibility
     */
    public static void generateRoadNetwork(TerrainTile[][] map, int width, int height, long seed, MapSize size, TerrainType[][] terrainMap, String type) {
        Random rng = new Random(seed);
        Point townCenter;
        if (type.equalsIgnoreCase("river")) {
            townCenter = RoadUtils.getSmartTownCenterRiver(terrainMap, rng);
        } else if (type.equalsIgnoreCase("lake")) {
            Set<Point> lakeTiles = RoadUtils.getLakeCenterRegion(terrainMap);
            townCenter = RoadUtils.getSmartTownCenterLake(terrainMap, lakeTiles, rng);
        } else {
            townCenter = RoadUtils.getSmartTownCenterSeaside(terrainMap, rng);
        }

        // 1. Select road style based on map size
        RoadStyle style;
        if (width <= 64) {
            style = RoadStyle.FIELD_TAN;
        } else if (width <= 128) {
            style = RoadStyle.COBBLE_LIGHTGRAY;
        } else {
            style = RoadStyle.COBBLE_GRAY;
        }

        // 2. Generate major road nodes
        List<RoadNode> majorNodes = RoadUtils.generateMajorRoadNodes(townCenter, width, height, rng, size);

        // 3. Connect nodes via MST / Voronoi-style paths
        List<List<RoadNode>> majorPaths = RoadUtils.connectWithVoronoiGraph(majorNodes, rng);

        // 4. Carve roads using selected style
        for (List<RoadNode> path : majorPaths) {
            RoadUtils.carveRoadPath(map, path, rng, style, size);
        }
    }




}
