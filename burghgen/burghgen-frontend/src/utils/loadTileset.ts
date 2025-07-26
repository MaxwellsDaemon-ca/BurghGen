/**
 * Represents the x and y coordinates of a tile within a tileset image.
 */
export interface TileCoordinates {
  x: number;
  y: number;
}

/**
 * Calculates the pixel coordinates of a tile within a tileset image based on its ID.
 *
 * @param tileId - The numeric ID of the tile
 * @param tileWidth - Width of a single tile in pixels
 * @param tileHeight - Height of a single tile in pixels
 * @param tilesPerRow - Number of tiles per row in the tileset image
 * @returns An object containing the x and y pixel positions of the tile
 */
export function getTileCoords(
  tileId: number,
  tileWidth: number,
  tileHeight: number,
  tilesPerRow: number
): TileCoordinates {
  const x = (tileId % tilesPerRow) * tileWidth;
  const y = Math.floor(tileId / tilesPerRow) * tileHeight;

  return { x, y };
}
