package com.chancema.burghgen.model;

/**
 * Enum representing different types of terrain tiles used in map generation.
 * 
 * These types may be used to drive rendering, pathfinding, or simulation logic.
 */
public enum TerrainType {
    
    /** Represents deep or shallow water (e.g., ocean, lake, river) */
    WATER,

    /** Represents beach, shoreline, or desert-like terrain */
    SAND,

    /** Represents natural grassy land or vegetation */
    GRASS,

    /** Represents dry earth, dirt paths, or cleared land */
    DIRT
}
