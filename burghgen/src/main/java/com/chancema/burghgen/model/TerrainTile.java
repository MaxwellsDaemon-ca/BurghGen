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
    private boolean hasRoad = false;
    private RoadStyle roadStyle;
    private int roadTileId;

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
     * Constructs a TerrainTile with a specified position, terrain type, and road properties.
     * 
     * @param x the horizontal grid coordinate
     * @param y the vertical grid coordinate
     * @param type the terrain type for this tile
     * @param hasRoad whether this tile has a road
     * @param roadStyle the style of the road on this tile
     * @param roadTileId the ID of the road tile used for rendering
     */
    public TerrainTile(int x, int y, TerrainType type, boolean hasRoad, RoadStyle roadStyle, int roadTileId) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.hasRoad = hasRoad;
        this.roadStyle = roadStyle;
        this.roadTileId = roadTileId;
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

    public void setType(TerrainType type) {
        this.type = type;
    }

    public boolean getHasRoad() {
        return hasRoad;
    }

    public void setHasRoad(boolean hasRoad) {
        this.hasRoad = hasRoad;
    }

    public RoadStyle getRoadStyle() {
        return roadStyle;
    }

    public void setRoadStyle(RoadStyle roadStyle) {
        this.roadStyle = roadStyle;
    }

    public int getRoadTileId() {
        return roadTileId;
    }

    public void setRoadTileId(int roadTileId) {
        this.roadTileId = roadTileId;
    }
}
