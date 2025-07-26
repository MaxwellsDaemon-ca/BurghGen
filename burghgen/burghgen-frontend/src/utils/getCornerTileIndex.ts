import type { TerrainTile } from '../types/TerrainTile';

/**
 * Terrain name definitions used across the map.
 */
type TerrainName = 'DIRT' | 'GRASS' | 'SAND' | 'WATER';

/**
 * Maps terrain names to their corresponding numeric IDs.
 */
const TERRAIN_TYPE_TO_ID: Record<TerrainName, number> = {
  DIRT: 0,
  GRASS: 1,
  SAND: 2,
  WATER: 3,
};

/**
 * Terrain key type for priority resolution.
 */
type TerrainKey = 'WATER' | 'SAND' | 'GRASS' | 'DIRT';

/**
 * Defines priority for terrain types when resolving dominance in a group.
 * Higher numbers take precedence during sorting.
 */
const TERRAIN_PRIORITY: Record<TerrainKey, number> = {
  WATER: 3,
  SAND: 2,
  GRASS: 1,
  DIRT: 0,
};

/**
 * Alternate tile variations for terrain types.
 * Used to add visual variety when all four corners are the same.
 */
const ALT_TILE_IDS: Record<number, number[]> = {
  1: [259, 260, 261], // GRASS
  0: [256, 257, 258], // DIRT
  2: [262, 263, 264], // SAND
  3: [265, 266, 267], // WATER
};

/**
 * Generates a seeded pseudo-random number using a Linear Congruential Generator (LCG).
 * Ensures consistent randomness across coordinates and seed values.
 *
 * @param x - X-coordinate of tile
 * @param y - Y-coordinate of tile
 * @param seed - User-defined seed value
 * @returns A float between 0 and 1
 */
function seededRandom(x: number, y: number, seed: number): number {
  let n = x * 374761393 + y * 668265263 + seed * 982451653;
  n = (n ^ (n >> 13)) * 1274126177;
  return ((n ^ (n >> 16)) >>> 0) / 0xffffffff;
}

/**
 * Lookup table that maps a string key representing the terrain types
 * of the four corners of a tile (NW, NE, SW, SE) to a specific tile index.
 *
 * Example key format: '0,1,1,2' (corresponding to [DIRT, GRASS, GRASS, SAND])
 */
const TILE_LOOKUP_TABLE: Record<string, number> = {
    '0,0,0,0': 0, // DIRT NW, DIRT NE, DIRT SW, DIRT SE
    '1,1,1,1': 1, // GRASS NW, GRASS NE, GRASS SW, GRASS SE
    '2,2,2,2': 2, // SAND NW, SAND NE, SAND SW, SAND SE
    '3,3,3,3': 3, // WATER NW, WATER NE, WATER SW, WATER SE
    '3,3,3,1': 4, // WATER NW, WATER NE, WATER SW, GRASS SE
    '3,3,3,0': 5, // WATER NW, WATER NE, WATER SW, DIRT SE
    '3,3,3,2': 6, // WATER NW, WATER NE, WATER SW, SAND SE
    '3,3,1,3': 7, // WATER NW, WATER NE, GRASS SW, WATER SE
    '3,3,1,1': 8, // WATER NW, WATER NE, GRASS SW, GRASS SE
    '3,3,1,0': 9, // WATER NW, WATER NE, GRASS SW, DIRT SE
    '3,3,1,2': 10, // WATER NW, WATER NE, GRASS SW, SAND SE
    '3,3,0,3': 11, // WATER NW, WATER NE, DIRT SW, WATER SE
    '3,3,0,1': 12, // WATER NW, WATER NE, DIRT SW, GRASS SE
    '3,3,0,0': 13, // WATER NW, WATER NE, DIRT SW, DIRT SE
    '3,3,0,2': 14, // WATER NW, WATER NE, DIRT SW, SAND SE
    '3,3,2,3': 15, // WATER NW, WATER NE, SAND SW, WATER SE
    '3,3,2,1': 16, // WATER NW, WATER NE, SAND SW, GRASS SE
    '3,3,2,0': 17, // WATER NW, WATER NE, SAND SW, DIRT SE
    '3,3,2,2': 18, // WATER NW, WATER NE, SAND SW, SAND SE
    '3,1,3,3': 19, // WATER NW, GRASS NE, WATER SW, WATER SE
    '3,1,3,1': 20, // WATER NW, GRASS NE, WATER SW, GRASS SE
    '3,1,3,0': 21, // WATER NW, GRASS NE, WATER SW, DIRT SE
    '3,1,3,2': 22, // WATER NW, GRASS NE, WATER SW, SAND SE
    '3,1,1,3': 23, // WATER NW, GRASS NE, GRASS SW, WATER SE
    '3,1,1,1': 24, // WATER NW, GRASS NE, GRASS SW, GRASS SE
    '3,1,1,0': 25, // WATER NW, GRASS NE, GRASS SW, DIRT SE
    '3,1,1,2': 26, // WATER NW, GRASS NE, GRASS SW, SAND SE
    '3,1,0,3': 27, // WATER NW, GRASS NE, DIRT SW, WATER SE
    '3,1,0,1': 28, // WATER NW, GRASS NE, DIRT SW, GRASS SE
    '3,1,0,0': 29, // WATER NW, GRASS NE, DIRT SW, DIRT SE
    '3,1,0,2': 30, // WATER NW, GRASS NE, DIRT SW, SAND SE
    '3,1,2,3': 31, // WATER NW, GRASS NE, SAND SW, WATER SE
    '3,1,2,1': 32, // WATER NW, GRASS NE, SAND SW, GRASS SE
    '3,1,2,0': 33, // WATER NW, GRASS NE, SAND SW, DIRT SE
    '3,1,2,2': 34, // WATER NW, GRASS NE, SAND SW, SAND SE
    '3,0,3,3': 35, // WATER NW, DIRT NE, WATER SW, WATER SE
    '3,0,3,1': 36, // WATER NW, DIRT NE, WATER SW, GRASS SE
    '3,0,3,0': 37, // WATER NW, DIRT NE, WATER SW, DIRT SE
    '3,0,3,2': 38, // WATER NW, DIRT NE, WATER SW, SAND SE
    '3,0,1,3': 39, // WATER NW, DIRT NE, GRASS SW, WATER SE
    '3,0,1,1': 40, // WATER NW, DIRT NE, GRASS SW, GRASS SE
    '3,0,1,0': 41, // WATER NW, DIRT NE, GRASS SW, DIRT SE
    '3,0,1,2': 42, // WATER NW, DIRT NE, GRASS SW, SAND SE
    '3,0,0,3': 43, // WATER NW, DIRT NE, DIRT SW, WATER SE
    '3,0,0,1': 44, // WATER NW, DIRT NE, DIRT SW, GRASS SE
    '3,0,0,0': 45, // WATER NW, DIRT NE, DIRT SW, DIRT SE
    '3,0,0,2': 46, // WATER NW, DIRT NE, DIRT SW, SAND SE
    '3,0,2,3': 47, // WATER NW, DIRT NE, SAND SW, WATER SE
    '3,0,2,1': 48, // WATER NW, DIRT NE, SAND SW, GRASS SE
    '3,0,2,0': 49, // WATER NW, DIRT NE, SAND SW, DIRT SE
    '3,0,2,2': 50, // WATER NW, DIRT NE, SAND SW, SAND SE
    '3,2,3,3': 51, // WATER NW, SAND NE, WATER SW, WATER SE
    '3,2,3,1': 52, // WATER NW, SAND NE, WATER SW, GRASS SE
    '3,2,3,0': 53, // WATER NW, SAND NE, WATER SW, DIRT SE
    '3,2,3,2': 54, // WATER NW, SAND NE, WATER SW, SAND SE
    '3,2,1,3': 55, // WATER NW, SAND NE, GRASS SW, WATER SE
    '3,2,1,1': 56, // WATER NW, SAND NE, GRASS SW, GRASS SE
    '3,2,1,0': 57, // WATER NW, SAND NE, GRASS SW, DIRT SE
    '3,2,1,2': 58, // WATER NW, SAND NE, GRASS SW, SAND SE
    '3,2,0,3': 59, // WATER NW, SAND NE, DIRT SW, WATER SE
    '3,2,0,1': 60, // WATER NW, SAND NE, DIRT SW, GRASS SE
    '3,2,0,0': 61, // WATER NW, SAND NE, DIRT SW, DIRT SE
    '3,2,0,2': 62, // WATER NW, SAND NE, DIRT SW, SAND SE
    '3,2,2,3': 63, // WATER NW, SAND NE, SAND SW, WATER SE
    '3,2,2,1': 64, // WATER NW, SAND NE, SAND SW, GRASS SE
    '3,2,2,0': 65, // WATER NW, SAND NE, SAND SW, DIRT SE
    '3,2,2,2': 66, // WATER NW, SAND NE, SAND SW, SAND SE
    '1,3,3,3': 67, // GRASS NW, WATER NE, WATER SW, WATER SE
    '1,3,3,1': 68, // GRASS NW, WATER NE, WATER SW, GRASS SE
    '1,3,3,0': 69, // GRASS NW, WATER NE, WATER SW, DIRT SE
    '1,3,3,2': 70, // GRASS NW, WATER NE, WATER SW, SAND SE
    '1,3,1,3': 71, // GRASS NW, WATER NE, GRASS SW, WATER SE
    '1,3,1,1': 72, // GRASS NW, WATER NE, GRASS SW, GRASS SE
    '1,3,1,0': 73, // GRASS NW, WATER NE, GRASS SW, DIRT SE
    '1,3,1,2': 74, // GRASS NW, WATER NE, GRASS SW, SAND SE
    '1,3,0,3': 75, // GRASS NW, WATER NE, DIRT SW, WATER SE
    '1,3,0,1': 76, // GRASS NW, WATER NE, DIRT SW, GRASS SE
    '1,3,0,0': 77, // GRASS NW, WATER NE, DIRT SW, DIRT SE
    '1,3,0,2': 78, // GRASS NW, WATER NE, DIRT SW, SAND SE
    '1,3,2,3': 79, // GRASS NW, WATER NE, SAND SW, WATER SE
    '1,3,2,1': 80, // GRASS NW, WATER NE, SAND SW, GRASS SE
    '1,3,2,0': 81, // GRASS NW, WATER NE, SAND SW, DIRT SE
    '1,3,2,2': 82, // GRASS NW, WATER NE, SAND SW, SAND SE
    '1,1,3,3': 83, // GRASS NW, GRASS NE, WATER SW, WATER SE
    '1,1,3,1': 84, // GRASS NW, GRASS NE, WATER SW, GRASS SE
    '1,1,3,0': 85, // GRASS NW, GRASS NE, WATER SW, DIRT SE
    '1,1,3,2': 86, // GRASS NW, GRASS NE, WATER SW, SAND SE
    '1,1,1,3': 87, // GRASS NW, GRASS NE, GRASS SW, WATER SE
    '1,1,1,0': 88, // GRASS NW, GRASS NE, GRASS SW, DIRT SE
    '1,1,1,2': 89, // GRASS NW, GRASS NE, GRASS SW, SAND SE
    '1,1,0,3': 90, // GRASS NW, GRASS NE, DIRT SW, WATER SE
    '1,1,0,1': 91, // GRASS NW, GRASS NE, DIRT SW, GRASS SE
    '1,1,0,0': 92, // GRASS NW, GRASS NE, DIRT SW, DIRT SE
    '1,1,0,2': 93, // GRASS NW, GRASS NE, DIRT SW, SAND SE
    '1,1,2,3': 94, // GRASS NW, GRASS NE, SAND SW, WATER SE
    '1,1,2,1': 95, // GRASS NW, GRASS NE, SAND SW, GRASS SE
    '1,1,2,0': 96, // GRASS NW, GRASS NE, SAND SW, DIRT SE
    '1,1,2,2': 97, // GRASS NW, GRASS NE, SAND SW, SAND SE
    '1,0,3,3': 98, // GRASS NW, DIRT NE, WATER SW, WATER SE
    '1,0,3,1': 99, // GRASS NW, DIRT NE, WATER SW, GRASS SE
    '1,0,3,0': 100, // GRASS NW, DIRT NE, WATER SW, DIRT SE
    '1,0,3,2': 101, // GRASS NW, DIRT NE, WATER SW, SAND SE
    '1,0,1,3': 102, // GRASS NW, DIRT NE, GRASS SW, WATER SE
    '1,0,1,1': 103, // GRASS NW, DIRT NE, GRASS SW, GRASS SE
    '1,0,1,0': 104, // GRASS NW, DIRT NE, GRASS SW, DIRT SE
    '1,0,1,2': 105, // GRASS NW, DIRT NE, GRASS SW, SAND SE
    '1,0,0,3': 106, // GRASS NW, DIRT NE, DIRT SW, WATER SE
    '1,0,0,1': 107, // GRASS NW, DIRT NE, DIRT SW, GRASS SE
    '1,0,0,0': 108, // GRASS NW, DIRT NE, DIRT SW, DIRT SE
    '1,0,0,2': 109, // GRASS NW, DIRT NE, DIRT SW, SAND SE
    '1,0,2,3': 110, // GRASS NW, DIRT NE, SAND SW, WATER SE
    '1,0,2,1': 111, // GRASS NW, DIRT NE, SAND SW, GRASS SE
    '1,0,2,0': 112, // GRASS NW, DIRT NE, SAND SW, DIRT SE
    '1,0,2,2': 113, // GRASS NW, DIRT NE, SAND SW, SAND SE
    '1,2,3,3': 114, // GRASS NW, SAND NE, WATER SW, WATER SE
    '1,2,3,1': 115, // GRASS NW, SAND NE, WATER SW, GRASS SE
    '1,2,3,0': 116, // GRASS NW, SAND NE, WATER SW, DIRT SE
    '1,2,3,2': 117, // GRASS NW, SAND NE, WATER SW, SAND SE
    '1,2,1,3': 118, // GRASS NW, SAND NE, GRASS SW, WATER SE
    '1,2,1,1': 119, // GRASS NW, SAND NE, GRASS SW, GRASS SE
    '1,2,1,0': 120, // GRASS NW, SAND NE, GRASS SW, DIRT SE
    '1,2,1,2': 121, // GRASS NW, SAND NE, GRASS SW, SAND SE
    '1,2,0,3': 122, // GRASS NW, SAND NE, DIRT SW, WATER SE
    '1,2,0,1': 123, // GRASS NW, SAND NE, DIRT SW, GRASS SE
    '1,2,0,0': 124, // GRASS NW, SAND NE, DIRT SW, DIRT SE
    '1,2,0,2': 125, // GRASS NW, SAND NE, DIRT SW, SAND SE
    '1,2,2,3': 126, // GRASS NW, SAND NE, SAND SW, WATER SE
    '1,2,2,1': 127, // GRASS NW, SAND NE, SAND SW, GRASS SE
    '1,2,2,0': 128, // GRASS NW, SAND NE, SAND SW, DIRT SE
    '1,2,2,2': 129, // GRASS NW, SAND NE, SAND SW, SAND SE
    '0,3,3,3': 130, // DIRT NW, WATER NE, WATER SW, WATER SE
    '0,3,3,1': 131, // DIRT NW, WATER NE, WATER SW, GRASS SE
    '0,3,3,0': 132, // DIRT NW, WATER NE, WATER SW, DIRT SE
    '0,3,3,2': 133, // DIRT NW, WATER NE, WATER SW, SAND SE
    '0,3,1,3': 134, // DIRT NW, WATER NE, GRASS SW, WATER SE
    '0,3,1,1': 135, // DIRT NW, WATER NE, GRASS SW, GRASS SE
    '0,3,1,0': 136, // DIRT NW, WATER NE, GRASS SW, DIRT SE
    '0,3,1,2': 137, // DIRT NW, WATER NE, GRASS SW, SAND SE
    '0,3,0,3': 138, // DIRT NW, WATER NE, DIRT SW, WATER SE
    '0,3,0,1': 139, // DIRT NW, WATER NE, DIRT SW, GRASS SE
    '0,3,0,0': 140, // DIRT NW, WATER NE, DIRT SW, DIRT SE
    '0,3,0,2': 141, // DIRT NW, WATER NE, DIRT SW, SAND SE
    '0,3,2,3': 142, // DIRT NW, WATER NE, SAND SW, WATER SE
    '0,3,2,1': 143, // DIRT NW, WATER NE, SAND SW, GRASS SE
    '0,3,2,0': 144, // DIRT NW, WATER NE, SAND SW, DIRT SE
    '0,3,2,2': 145, // DIRT NW, WATER NE, SAND SW, SAND SE
    '0,1,3,3': 146, // DIRT NW, GRASS NE, WATER SW, WATER SE
    '0,1,3,1': 147, // DIRT NW, GRASS NE, WATER SW, GRASS SE
    '0,1,3,0': 148, // DIRT NW, GRASS NE, WATER SW, DIRT SE
    '0,1,3,2': 149, // DIRT NW, GRASS NE, WATER SW, SAND SE
    '0,1,1,3': 150, // DIRT NW, GRASS NE, GRASS SW, WATER SE
    '0,1,1,1': 151, // DIRT NW, GRASS NE, GRASS SW, GRASS SE
    '0,1,1,0': 152, // DIRT NW, GRASS NE, GRASS SW, DIRT SE
    '0,1,1,2': 153, // DIRT NW, GRASS NE, GRASS SW, SAND SE
    '0,1,0,3': 154, // DIRT NW, GRASS NE, DIRT SW, WATER SE
    '0,1,0,1': 155, // DIRT NW, GRASS NE, DIRT SW, GRASS SE
    '0,1,0,0': 156, // DIRT NW, GRASS NE, DIRT SW, DIRT SE
    '0,1,0,2': 157, // DIRT NW, GRASS NE, DIRT SW, SAND SE
    '0,1,2,3': 158, // DIRT NW, GRASS NE, SAND SW, WATER SE
    '0,1,2,1': 159, // DIRT NW, GRASS NE, SAND SW, GRASS SE
    '0,1,2,0': 160, // DIRT NW, GRASS NE, SAND SW, DIRT SE
    '0,1,2,2': 161, // DIRT NW, GRASS NE, SAND SW, SAND SE
    '0,0,3,3': 162, // DIRT NW, DIRT NE, WATER SW, WATER SE
    '0,0,3,1': 163, // DIRT NW, DIRT NE, WATER SW, GRASS SE
    '0,0,3,0': 164, // DIRT NW, DIRT NE, WATER SW, DIRT SE
    '0,0,3,2': 165, // DIRT NW, DIRT NE, WATER SW, SAND SE
    '0,0,1,3': 166, // DIRT NW, DIRT NE, GRASS SW, WATER SE
    '0,0,1,1': 167, // DIRT NW, DIRT NE, GRASS SW, GRASS SE
    '0,0,1,0': 168, // DIRT NW, DIRT NE, GRASS SW, DIRT SE
    '0,0,1,2': 169, // DIRT NW, DIRT NE, GRASS SW, SAND SE
    '0,0,0,3': 170, // DIRT NW, DIRT NE, DIRT SW, WATER SE
    '0,0,0,1': 171, // DIRT NW, DIRT NE, DIRT SW, GRASS SE
    '0,0,0,2': 172, // DIRT NW, DIRT NE, DIRT SW, SAND SE
    '0,0,2,3': 173, // DIRT NW, DIRT NE, SAND SW, WATER SE
    '0,0,2,1': 174, // DIRT NW, DIRT NE, SAND SW, GRASS SE
    '0,0,2,0': 175, // DIRT NW, DIRT NE, SAND SW, DIRT SE
    '0,0,2,2': 176, // DIRT NW, DIRT NE, SAND SW, SAND SE
    '0,2,3,3': 177, // DIRT NW, SAND NE, WATER SW, WATER SE
    '0,2,3,1': 178, // DIRT NW, SAND NE, WATER SW, GRASS SE
    '0,2,3,0': 179, // DIRT NW, SAND NE, WATER SW, DIRT SE
    '0,2,3,2': 180, // DIRT NW, SAND NE, WATER SW, SAND SE
    '0,2,1,3': 181, // DIRT NW, SAND NE, GRASS SW, WATER SE
    '0,2,1,1': 182, // DIRT NW, SAND NE, GRASS SW, GRASS SE
    '0,2,1,0': 183, // DIRT NW, SAND NE, GRASS SW, DIRT SE
    '0,2,1,2': 184, // DIRT NW, SAND NE, GRASS SW, SAND SE
    '0,2,0,3': 185, // DIRT NW, SAND NE, DIRT SW, WATER SE
    '0,2,0,1': 186, // DIRT NW, SAND NE, DIRT SW, GRASS SE
    '0,2,0,0': 187, // DIRT NW, SAND NE, DIRT SW, DIRT SE
    '0,2,0,2': 188, // DIRT NW, SAND NE, DIRT SW, SAND SE
    '0,2,2,3': 189, // DIRT NW, SAND NE, SAND SW, WATER SE
    '0,2,2,1': 190, // DIRT NW, SAND NE, SAND SW, GRASS SE
    '0,2,2,0': 191, // DIRT NW, SAND NE, SAND SW, DIRT SE
    '0,2,2,2': 192, // DIRT NW, SAND NE, SAND SW, SAND SE
    '2,3,3,3': 193, // SAND NW, WATER NE, WATER SW, WATER SE
    '2,3,3,1': 194, // SAND NW, WATER NE, WATER SW, GRASS SE
    '2,3,3,0': 195, // SAND NW, WATER NE, WATER SW, DIRT SE
    '2,3,3,2': 196, // SAND NW, WATER NE, WATER SW, SAND SE
    '2,3,1,3': 197, // SAND NW, WATER NE, GRASS SW, WATER SE
    '2,3,1,1': 198, // SAND NW, WATER NE, GRASS SW, GRASS SE
    '2,3,1,0': 199, // SAND NW, WATER NE, GRASS SW, DIRT SE
    '2,3,1,2': 200, // SAND NW, WATER NE, GRASS SW, SAND SE
    '2,3,0,3': 201, // SAND NW, WATER NE, DIRT SW, WATER SE
    '2,3,0,1': 202, // SAND NW, WATER NE, DIRT SW, GRASS SE
    '2,3,0,0': 203, // SAND NW, WATER NE, DIRT SW, DIRT SE
    '2,3,0,2': 204, // SAND NW, WATER NE, DIRT SW, SAND SE
    '2,3,2,3': 205, // SAND NW, WATER NE, SAND SW, WATER SE
    '2,3,2,1': 206, // SAND NW, WATER NE, SAND SW, GRASS SE
    '2,3,2,0': 207, // SAND NW, WATER NE, SAND SW, DIRT SE
    '2,3,2,2': 208, // SAND NW, WATER NE, SAND SW, SAND SE
    '2,1,3,3': 209, // SAND NW, GRASS NE, WATER SW, WATER SE
    '2,1,3,1': 210, // SAND NW, GRASS NE, WATER SW, GRASS SE
    '2,1,3,0': 211, // SAND NW, GRASS NE, WATER SW, DIRT SE
    '2,1,3,2': 212, // SAND NW, GRASS NE, WATER SW, SAND SE
    '2,1,1,3': 213, // SAND NW, GRASS NE, GRASS SW, WATER SE
    '2,1,1,1': 214, // SAND NW, GRASS NE, GRASS SW, GRASS SE
    '2,1,1,0': 215, // SAND NW, GRASS NE, GRASS SW, DIRT SE
    '2,1,1,2': 216, // SAND NW, GRASS NE, GRASS SW, SAND SE
    '2,1,0,3': 217, // SAND NW, GRASS NE, DIRT SW, WATER SE
    '2,1,0,1': 218, // SAND NW, GRASS NE, DIRT SW, GRASS SE
    '2,1,0,0': 219, // SAND NW, GRASS NE, DIRT SW, DIRT SE
    '2,1,0,2': 220, // SAND NW, GRASS NE, DIRT SW, SAND SE
    '2,1,2,3': 221, // SAND NW, GRASS NE, SAND SW, WATER SE
    '2,1,2,1': 222, // SAND NW, GRASS NE, SAND SW, GRASS SE
    '2,1,2,0': 223, // SAND NW, GRASS NE, SAND SW, DIRT SE
    '2,1,2,2': 224, // SAND NW, GRASS NE, SAND SW, SAND SE
    '2,0,3,3': 225, // SAND NW, DIRT NE, WATER SW, WATER SE
    '2,0,3,1': 226, // SAND NW, DIRT NE, WATER SW, GRASS SE
    '2,0,3,0': 227, // SAND NW, DIRT NE, WATER SW, DIRT SE
    '2,0,3,2': 228, // SAND NW, DIRT NE, WATER SW, SAND SE
    '2,0,1,3': 229, // SAND NW, DIRT NE, GRASS SW, WATER SE
    '2,0,1,1': 230, // SAND NW, DIRT NE, GRASS SW, GRASS SE
    '2,0,1,0': 231, // SAND NW, DIRT NE, GRASS SW, DIRT SE
    '2,0,1,2': 232, // SAND NW, DIRT NE, GRASS SW, SAND SE
    '2,0,0,3': 233, // SAND NW, DIRT NE, DIRT SW, WATER SE
    '2,0,0,1': 234, // SAND NW, DIRT NE, DIRT SW, GRASS SE
    '2,0,0,0': 235, // SAND NW, DIRT NE, DIRT SW, DIRT SE
    '2,0,0,2': 236, // SAND NW, DIRT NE, DIRT SW, SAND SE
    '2,0,2,3': 237, // SAND NW, DIRT NE, SAND SW, WATER SE
    '2,0,2,1': 238, // SAND NW, DIRT NE, SAND SW, GRASS SE
    '2,0,2,0': 239, // SAND NW, DIRT NE, SAND SW, DIRT SE
    '2,0,2,2': 240, // SAND NW, DIRT NE, SAND SW, SAND SE
    '2,2,3,3': 241, // SAND NW, SAND NE, WATER SW, WATER SE
    '2,2,3,1': 242, // SAND NW, SAND NE, WATER SW, GRASS SE
    '2,2,3,0': 243, // SAND NW, SAND NE, WATER SW, DIRT SE
    '2,2,3,2': 244, // SAND NW, SAND NE, WATER SW, SAND SE
    '2,2,1,3': 245, // SAND NW, SAND NE, GRASS SW, WATER SE
    '2,2,1,1': 246, // SAND NW, SAND NE, GRASS SW, GRASS SE
    '2,2,1,0': 247, // SAND NW, SAND NE, GRASS SW, DIRT SE
    '2,2,1,2': 248, // SAND NW, SAND NE, GRASS SW, SAND SE
    '2,2,0,3': 249, // SAND NW, SAND NE, DIRT SW, WATER SE
    '2,2,0,1': 250, // SAND NW, SAND NE, DIRT SW, GRASS SE
    '2,2,0,0': 251, // SAND NW, SAND NE, DIRT SW, DIRT SE
    '2,2,0,2': 252, // SAND NW, SAND NE, DIRT SW, SAND SE
    '2,2,2,3': 253, // SAND NW, SAND NE, SAND SW, WATER SE
    '2,2,2,1': 254, // SAND NW, SAND NE, SAND SW, GRASS SE
    '2,2,2,0': 255, // SAND NW, SAND NE, SAND SW, DIRT SE
};

/**
 * Returns the terrain type of a given tile, or 'DIRT' if undefined.
 *
 * @param tile - A TerrainTile object or undefined
 * @returns The terrain type string
 */
function getTerrain(tile: TerrainTile | undefined): string {
  return tile?.type ?? 'DIRT';
}

/**
 * Determines the dominant terrain type from a list.
 * In case of a tie, the one with higher terrain priority is returned.
 *
 * @param types - Array of terrain type strings
 * @returns The dominant terrain type
 */
function dominantTerrain(types: string[]): string {
  const counts: Record<string, number> = {};

  for (const t of types) {
    counts[t] = (counts[t] || 0) + 1;
  }

  return Object.entries(counts)
    .sort(
      (a, b) =>
        b[1] - a[1] ||
        TERRAIN_PRIORITY[b[0] as TerrainKey] - TERRAIN_PRIORITY[a[0] as TerrainKey]
    )[0][0];
}

/**
 * Computes the appropriate tile index for the tile at (x, y) by examining
 * the dominant terrain type in each of the four quadrants (NW, NE, SW, SE).
 * Optionally returns an alternate tile for visual variation if all four
 * corners are of the same terrain type.
 *
 * @param x - X-coordinate on the map
 * @param y - Y-coordinate on the map
 * @param tileMap - 2D grid of TerrainTile objects
 * @param seed - Seed value for consistent randomization
 * @returns The tile index to render
 */
export function getCornerTileIndex(
  x: number,
  y: number,
  tileMap: TerrainTile[][],
  seed: number
): number {
  const get = (dx: number, dy: number) => getTerrain(tileMap[y + dy]?.[x + dx]);

  const nw = dominantTerrain([get(-1, -1), get(0, -1), get(-1, 0), get(0, 0)]);
  const ne = dominantTerrain([get(0, -1), get(1, -1), get(0, 0), get(1, 0)]);
  const sw = dominantTerrain([get(-1, 0), get(0, 0), get(-1, 1), get(0, 1)]);
  const se = dominantTerrain([get(0, 0), get(1, 0), get(0, 1), get(1, 1)]);

  const key = [
    TERRAIN_TYPE_TO_ID[nw as TerrainName],
    TERRAIN_TYPE_TO_ID[ne as TerrainName],
    TERRAIN_TYPE_TO_ID[sw as TerrainName],
    TERRAIN_TYPE_TO_ID[se as TerrainName],
  ].join(',');

  const centerType = getTerrain(tileMap[y]?.[x]);
  const centerId = TERRAIN_TYPE_TO_ID[centerType as TerrainName];
  const fullKey = `${centerId},${centerId},${centerId},${centerId}`;

  // Use alternate tile if tile is uniform and alternates are available
  if (key === fullKey && ALT_TILE_IDS[centerId]) {
    const rand = seededRandom(x, y, seed);
    if (rand < 0.15) {
      const altTiles = ALT_TILE_IDS[centerId];
      const index = Math.floor((rand / 0.15) * altTiles.length);
      return altTiles[index];
    }
  }

  // Default to standard tile index; fallback is WATER (3) if key is not found
  return TILE_LOOKUP_TABLE[key] ?? 3;
}
