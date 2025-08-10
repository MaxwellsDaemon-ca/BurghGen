/**
 * Defines the possible terrain types used in map generation.
 */
export type TerrainType = 'WATER' | 'SAND' | 'GRASS' | 'DIRT';

/**
 * Optional road styles for road overlays.
 */
export type RoadStyle =
  | 'COBBLE_GREY'
  | 'COBBLE_LIGHTGREY'
  | 'FIELDSTONE_TAN'; // Expand as needed

/**
 * Represents a single tile in the terrain grid.
 */
export interface TerrainTile {
  /** The horizontal position of the tile on the grid */
  x: number;

  /** The vertical position of the tile on the grid */
  y: number;

  /** The base terrain type (grass, sand, etc.) */
  type: TerrainType;

  /** Whether this tile has a road overlay */
  hasRoad?: boolean;

  /** The road style (optional, cosmetic only) */
  roadStyle?: RoadStyle;

  /** The tile ID from the road tileset to overlay */
  roadTileId?: number;
}
