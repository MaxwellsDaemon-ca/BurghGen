/**
 * Defines the possible terrain types used in map generation.
 * 
 * These types map to visual styles and gameplay logic such as pathfinding,
 * tile properties, or generation rules.
 */
export type TerrainType = 'WATER' | 'SAND' | 'GRASS' | 'DIRT';

/**
 * Represents a single tile in the terrain grid.
 * 
 * Each tile stores its grid coordinates and associated terrain type.
 */
export interface TerrainTile {
  /** The horizontal position of the tile on the grid */
  x: number;

  /** The vertical position of the tile on the grid */
  y: number;

  /** The type of terrain this tile represents */
  type: TerrainType;
}
