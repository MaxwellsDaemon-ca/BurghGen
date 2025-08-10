import { useEffect, useRef, useState } from 'react';
import TileRenderer from './components/TileRenderer';
import type { TerrainTile } from './types/TerrainTile';



/**
 * The main UI component for rendering and interacting with the BurghGen map.
 * 
 * Features:
 * - Seed and type selection for procedural map generation
 * - Adjustable map size
 * - Zoom and pan support
 * - PixiJS-based tile renderings
 */
function App() {
  const [tiles, setTiles] = useState<TerrainTile[]>([]);
  const [seed, setSeed] = useState(1234);
  const [type, setType] = useState('river');
  const [width, setWidth] = useState(256);
  const [height, setHeight] = useState(256);
  const [zoom, setZoom] = useState(1);
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState<{ x: number; y: number } | null>(null);
  const [scrollStart, setScrollStart] = useState<{ left: number; top: number } | null>(null);

  const TILE_SIZE = 32;
  const MIN_ZOOM = 0.25;
  const viewportRef = useRef<HTMLDivElement>(null);
  const API_BASE = import.meta.env.VITE_API_BASE;

  /**
   * Fetches terrain data from the backend and updates tile state.
   */
  const fetchTiles = async () => {
    const url = new URL('/generate', API_BASE);
    url.searchParams.set('type', String(type));
    url.searchParams.set('seed', String(seed));
    url.searchParams.set('width', String(width));
    url.searchParams.set('height', String(height));

    const res = await fetch(url.toString(), {
      method: 'GET',
    });

    if (!res.ok) {
      throw new Error(`Generation failed: ${res.status}`);
    }

    const data = await res.json();
    setTiles(data);
  };


  // Initial map generation
  useEffect(() => {
    fetchTiles();
  }, []);

  // Recalculate zoom to best-fit the map inside the viewport
  useEffect(() => {
    const container = viewportRef.current;
    if (!container) return;

    const mapPixelWidth = width * TILE_SIZE;
    const mapPixelHeight = height * TILE_SIZE;

    const viewportW = container.clientWidth;
    const viewportH = container.clientHeight;

    const zoomX = viewportW / mapPixelWidth;
    const zoomY = viewportH / mapPixelHeight;

    const bestFit = Math.min(zoomX, zoomY, 1);
    setZoom(bestFit);
  }, [width, height]);

  /**
   * Handles changing the map dimensions from the dropdown.
   */
  const handleMapSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const size = e.target.value;
    if (size === 'small') {
      setWidth(64);
      setHeight(64);
    } else if (size === 'medium') {
      setWidth(128);
      setHeight(128);
    } else {
      setWidth(256);
      setHeight(256);
    }
  };

  /**
   * Zooms in or out based on scroll wheel input.
   */
  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    setZoom((z) => Math.min(3, Math.max(MIN_ZOOM, +(z + delta).toFixed(2))));
  };

  /**
   * Begins panning the map when the mouse is pressed.
   */
  const handleMouseDown = (e: React.MouseEvent) => {
    const container = viewportRef.current;
    if (!container) return;

    setIsDragging(true);
    setDragStart({ x: e.clientX, y: e.clientY });
    setScrollStart({ left: container.scrollLeft, top: container.scrollTop });
  };

  /**
   * Updates the scroll position while dragging the map.
   */
  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging || !dragStart || !scrollStart) return;

    const dx = e.clientX - dragStart.x;
    const dy = e.clientY - dragStart.y;

    const container = viewportRef.current;
    if (container) {
      container.scrollLeft = scrollStart.left - dx;
      container.scrollTop = scrollStart.top - dy;
    }
  };

  /**
   * Ends drag-based scrolling.
   */
  const handleMouseUp = () => {
    setIsDragging(false);
    setDragStart(null);
    setScrollStart(null);
  };

  const maxViewportWidth = width * TILE_SIZE * MIN_ZOOM;
  const maxViewportHeight = height * TILE_SIZE * MIN_ZOOM;

  return (
    <div style={{ padding: '1em', userSelect: isDragging ? 'none' : 'auto' }}>
      <h1>BurghGen Preview</h1>

      {/* Controls: Seed and Randomize */}
      <div style={{ marginBottom: '1em' }}>
        <label>
          Seed:
          <input
            type="number"
            value={seed}
            onChange={(e) => setSeed(Number(e.target.value))}
            style={{ margin: '0 0.5em' }}
          />
        </label>
        <button onClick={() => setSeed(Math.floor(Math.random() * 1_000_000_000))}>
          Randomize Seed
        </button>
      </div>

      {/* Controls: Type and Size */}
      <div style={{ marginBottom: '1em' }}>
        <label>
          Type:
          <select value={type} onChange={(e) => setType(e.target.value)} style={{ marginLeft: '0.5em' }}>
            <option value="seaside">Seaside</option>
            <option value="river">River</option>
            <option value="lake">Lake</option>
          </select>
        </label>

        <label style={{ marginLeft: '1em' }}>
          Map Size:
          <select onChange={handleMapSizeChange} defaultValue="large" style={{ marginLeft: '0.5em' }}>
            <option value="small">Small (64×64)</option>
            <option value="medium">Medium (128×128)</option>
            <option value="large">Large (256×256)</option>
          </select>
        </label>

        <button onClick={fetchTiles} style={{ marginLeft: '1em' }}>
          Generate
        </button>
      </div>

      {/* Scrollable & Zoomable Viewport */}
      <div
        ref={viewportRef}
        onWheel={handleWheel}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        style={{
          resize: 'both',
          overflow: 'hidden',
          border: '2px solid black',
          background: '#222',
          width: '512px',
          height: '512px',
          minWidth: '256px',
          minHeight: '256px',
          maxWidth: `${maxViewportWidth}px`,
          maxHeight: `${maxViewportHeight}px`,
          marginBottom: '0.5em',
          position: 'relative',
          cursor: isDragging ? 'grabbing' : 'grab',
        }}
      >
        <div
          style={{
            position: 'relative',
            width: width * TILE_SIZE * zoom,
            height: height * TILE_SIZE * zoom,
            transform: `scale(${zoom})`,
            transformOrigin: 'top left',
            transition: 'transform 0.3s ease',
            pointerEvents: 'none',
          }}
        >
          <TileRenderer tiles={tiles} width={width} height={height} seed={seed} />
        </div>
      </div>

      {/* Zoom percentage display */}
      <div
        style={{
          textAlign: 'right',
          fontSize: '0.8em',
          color: 'white',
          backgroundColor: 'rgba(0, 0, 0, 0.6)',
          padding: '4px 10px',
          borderRadius: '4px',
          width: 'fit-content',
          marginLeft: 'auto',
          marginRight: '8px',
          marginBottom: '1em',
        }}
      >
        Zoom: {(zoom * 100).toFixed(0)}%
      </div>
    </div>
  );
}

export default App;
