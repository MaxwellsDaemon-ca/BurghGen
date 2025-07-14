package com.chancema.burghgen.controller;

import com.chancema.burghgen.model.TerrainTile;
import com.chancema.burghgen.service.TerrainGenerationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for handling map generation requests.
 * 
 * Exposes a `/generate` endpoint that returns a list of terrain tiles
 * based on the given map type, seed, and dimensions.
 */
@RestController
@RequestMapping("/generate")
public class MapGenerationController {

    private final TerrainGenerationService terrainService;

    /**
     * Injects the terrain generation service.
     *
     * @param terrainService service used to procedurally generate terrain
     */
    public MapGenerationController(TerrainGenerationService terrainService) {
        this.terrainService = terrainService;
    }

    /**
     * Generates a procedurally generated terrain map based on input parameters.
     * 
     * @param type   the generation mode (e.g. "seaside", "lake", "river")
     * @param seed   the random seed for consistent generation
     * @param width  the width of the grid in tiles
     * @param height the height of the grid in tiles
     * @return list of {@link TerrainTile} objects representing the generated map
     */
    @GetMapping
    public List<TerrainTile> generateMap(
            @RequestParam(defaultValue = "seaside") String type,
            @RequestParam(defaultValue = "1234") long seed,
            @RequestParam(defaultValue = "256") int width,
            @RequestParam(defaultValue = "256") int height
    ) {
        return terrainService.generateGrid(type, seed, width, height);
    }
}
