import * as PIXI from 'pixi.js';
import { useEffect, useRef } from 'react';
import { getCornerTileIndex } from '../utils/getCornerTileIndex';
import { getTileCoords } from '../utils/loadTileset';
import type { TerrainTile } from '../types/TerrainTile';
import { Assets } from 'pixi.js';

interface Props {
  /** Flat array of terrain tiles to render */
  tiles: TerrainTile[];

  /** Width of the map in tiles */
  width: number;

  /** Height of the map in tiles */
  height: number;

  /** Seed value used for procedural terrain variation */
  seed: number;
}

// Constants for tile rendering
const TILE_SIZE = 32;
const TILE_COLUMNS = 16;
const TILESET_IMAGE_PATH = '/assets/tilesets/BurghGen-Terrain-Tiles.png';

/**
 * TileRenderer component
 *
 * Uses PixiJS to render a 2D terrain tile map based on terrain data and a seeded corner tile algorithm.
 */
const TileRenderer = ({ tiles, width, height, seed }: Props) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const appRef = useRef<PIXI.Application | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container || tiles.length === 0) return;

    // Initialize PixiJS application
    const app = new PIXI.Application({
      width: width * TILE_SIZE,
      height: height * TILE_SIZE,
      backgroundColor: 0x1e1e1e,
      antialias: false,
    });

    container.appendChild(app.view as HTMLCanvasElement);
    appRef.current = app;

    // Convert flat tile list to a 2D array
    const tileMap: TerrainTile[][] = [];
    for (let y = 0; y < height; y++) {
      const row: TerrainTile[] = [];
      for (let x = 0; x < width; x++) {
        row.push(tiles[y * width + x]);
      }
      tileMap.push(row);
    }

    // Load tileset texture and render visible tiles
    Assets.load(TILESET_IMAGE_PATH).then((texture: PIXI.Texture) => {
      const baseTexture = texture.baseTexture;

      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const tileIndex = getCornerTileIndex(x, y, tileMap, seed);
          if (tileIndex === -1) continue;

          const { x: tx, y: ty } = getTileCoords(tileIndex, TILE_SIZE, TILE_SIZE, TILE_COLUMNS);
          const frame = new PIXI.Rectangle(tx, ty, TILE_SIZE, TILE_SIZE);
          const tileTexture = new PIXI.Texture(baseTexture, frame);

          const sprite = new PIXI.Sprite(tileTexture);
          sprite.x = x * TILE_SIZE;
          sprite.y = y * TILE_SIZE;

          app.stage.addChild(sprite);
        }
      }
    });

    // Cleanup PixiJS resources on unmount or tile change
    return () => {
      app.destroy(true, true);
      appRef.current = null;
    };
  }, [tiles, width, height, seed]);

  return <div ref={containerRef} style={{ marginTop: '1rem' }} />;
};

export default TileRenderer;
