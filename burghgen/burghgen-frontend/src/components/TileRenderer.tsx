import { useEffect, useRef } from 'react';
import * as PIXI from 'pixi.js';
import type { TerrainTile } from '../types/TerrainTile';

const TILE_SIZE = 16;

/**
 * Maps terrain types to their respective fill colors.
 */
const terrainColors: Record<string, number> = {
  WATER: 0x3366cc,
  SAND: 0xf4e285,
  GRASS: 0x77dd77,
  DIRT: 0xb97a57,
};

interface Props {
  /** Array of tiles to render on the map */
  tiles: TerrainTile[];
  /** Width of the map in tiles */
  width: number;
  /** Height of the map in tiles */
  height: number;
}

/**
 * TileRenderer is a React component that renders a 2D grid of terrain tiles using PixiJS.
 * 
 * It creates and mounts a PixiJS application, draws colored rectangles based on terrain type,
 * and attaches the canvas to a container div. It automatically handles cleanup on unmount.
 */
const TileRenderer = ({ tiles, width, height }: Props) => {
  const pixiContainer = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Initialize PixiJS application
    const app = new PIXI.Application({
      width: width * TILE_SIZE,
      height: height * TILE_SIZE,
      backgroundColor: 0x2f2f2f,
      antialias: true,
    });

    // Attach canvas to DOM
    if (pixiContainer.current) {
      pixiContainer.current.innerHTML = '';
      const canvas = app.view as unknown as HTMLCanvasElement;

      if (canvas instanceof HTMLCanvasElement) {
        pixiContainer.current.appendChild(canvas);
      }
    }

    // Draw each tile as a filled rectangle
    tiles.forEach((tile) => {
      const rect = new PIXI.Graphics();
      rect.beginFill(terrainColors[tile.type]);
      rect.drawRect(
        tile.x * TILE_SIZE,
        tile.y * TILE_SIZE,
        TILE_SIZE,
        TILE_SIZE
      );
      rect.endFill();
      app.stage.addChild(rect);
    });

    // Destroy PixiJS application on unmount
    return () => {
      app.destroy(true, { children: true });
    };
  }, [tiles, width, height]);

  return <div ref={pixiContainer} />;
};

export default TileRenderer;
