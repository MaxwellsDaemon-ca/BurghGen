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
const TERRAIN_TILE_COLUMNS = 16;
const TERRAIN_TILESET_PATH = '/assets/tilesets/BurghGen-Terrain-Tiles.png';
const ROAD_TILESET_PATH = '/assets/tilesets/BurghGen-Road-Tiles.png';
const ROAD_TILE_COLUMNS = 32;

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

    if (!container) return;

    if (tiles.length === 0) {
      console.warn('[TileRenderer] Tile data is empty (skipping render)');
      return;
    }



    // Initialize PixiJS application
    const app = new PIXI.Application({
      width: width * TILE_SIZE,
      height: height * TILE_SIZE,
      backgroundColor: 0x1e1e1e,
      antialias: false,
    });

    // Remove old canvas if it exists
    if (container.firstChild) {
      container.removeChild(container.firstChild);
    }

    // Create and attach new PixiJS app
    container.appendChild(app.view as HTMLCanvasElement);
    appRef.current = app;


    // Convert flat tile list to a 2D array
    const tileMap: TerrainTile[][] = [];

    for (let y = 0; y < height; y++) {
      const row: TerrainTile[] = [];
      for (let x = 0; x < width; x++) {
        const index = y * width + x;
        const tile = tiles[index];
        


        if (!tile) {
          console.warn(`[TileRenderer] Missing tile at index ${index} (${x}, ${y})`);
          continue;
        }

        row.push(tile);
      }
      tileMap.push(row);
    }


    // Load tileset texture and render visible tiles
    Assets.load([TERRAIN_TILESET_PATH, ROAD_TILESET_PATH]).then((resources) => {
      const terrainTexture = resources[TERRAIN_TILESET_PATH] as PIXI.Texture;
      const roadTexture = resources[ROAD_TILESET_PATH] as PIXI.Texture;

      const terrainBase = terrainTexture.baseTexture;
      const roadBase = roadTexture.baseTexture;

      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const tile = tileMap[y][x];

          // --- Render base terrain tile ---
          const tileIndex = getCornerTileIndex(x, y, tileMap, seed);
          if (tileIndex !== -1) {
            const { x: tx, y: ty } = getTileCoords(tileIndex, TILE_SIZE, TILE_SIZE, TERRAIN_TILE_COLUMNS);
            const frame = new PIXI.Rectangle(tx, ty, TILE_SIZE, TILE_SIZE);
            const terrainTexture = new PIXI.Texture(terrainBase, frame);

            const terrainSprite = new PIXI.Sprite(terrainTexture);
            terrainSprite.x = x * TILE_SIZE;
            terrainSprite.y = y * TILE_SIZE;
            terrainSprite.zIndex = 0;
            app.stage.addChild(terrainSprite);
          }

          // --- Render road overlay if applicable ---
          if (tile.hasRoad && tile.roadTileId !== undefined && tile.roadTileId !== -1) {

            const { x: rx, y: ry } = getTileCoords(tile.roadTileId, TILE_SIZE, TILE_SIZE, ROAD_TILE_COLUMNS);
            const roadFrame = new PIXI.Rectangle(rx, ry, TILE_SIZE, TILE_SIZE);
            const roadTexture = new PIXI.Texture(roadBase, roadFrame);

            const roadSprite = new PIXI.Sprite(roadTexture);
            roadSprite.x = x * TILE_SIZE;
            roadSprite.y = y * TILE_SIZE;
            roadSprite.zIndex = 10;
            app.stage.addChild(roadSprite);
          }
        }
      }

      app.stage.sortableChildren = true;
    });


      // Cleanup PixiJS resources on unmount or tile change
      return () => {
        if (appRef.current) {
          // Manually remove children, but DO NOT destroy textures managed by Assets
          appRef.current.stage.removeChildren();
          appRef.current.destroy(false); // Don't destroy base textures
          appRef.current = null;
        }
      };
  }, [tiles, width, height, seed]);

    return <div ref={containerRef} style={{ marginTop: '1rem' }} />;
  };

export default TileRenderer;
