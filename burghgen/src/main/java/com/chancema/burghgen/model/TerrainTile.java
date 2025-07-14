package com.chancema.burghgen.model;

/**
 * Represents a single tile on the map grid with a specific terrain type.
 * 
 * Each tile holds its X and Y position in the map, and the {@link TerrainType}
 * used for rendering, logic, or simulation purposes.
 */
public class TerrainTile {

    private int x;
    private int y;
    private TerrainType type;

    /**
     * Constructs a TerrainTile with a specified position and terrain type.
     * 
     * @param x the horizontal grid coordinate
     * @param y the vertical grid coordinate
     * @param type the terrain type for this tile
     */
    public TerrainTile(int x, int y, TerrainType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    /**
     * Returns the X (horizontal) coordinate of the tile.
     * 
     * @return X position
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the Y (vertical) coordinate of the tile.
     * 
     * @return Y position
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the terrain type of the tile.
     * 
     * @return terrain type
     */
    public TerrainType getType() {
        return type;
    }
}
