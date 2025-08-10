# BurghGen — Procedural Medieval Town & City Generator

BurghGen is a **web-based 2D procedural map generator** that creates medieval-style towns, cities, and settlements with natural terrain and roads.  
It is developed as part of a senior project and aims to combine **procedural generation techniques** with **pixel art assets** to produce visually appealing, grid-based maps.

---

## Project Overview

BurghGen procedurally generates a medieval map by layering multiple systems:

1. **Terrain Generation** — Creates natural environments such as grasslands, dirt, sand, and water bodies using Perlin-like noise and special generation modes.
2. **Water Features** — Supports realistic lakes, rivers, and seaside coastlines with natural shapes and gradients.
3. **Road Generation** — Implements Voronoi-style node placement and MST-style pathfinding to form road networks that adapt to the terrain.
4. **Tile Rendering** — Uses corner-based 32×32 pixel tiles from Tiled (`.tmx` / `.tsx`) files for seamless blending between terrain types.
5. **Viewport & Controls** — Zooming, panning, and a responsive viewport ensure the map can be explored in detail.

---

## Key Features Implemented

- **Procedural Terrain Types**
  - Grass, Dirt, Sand, Water
  - Smooth terrain blending
  - Sand gradients near coastlines and inlets
- **Water Generation Modes**
  - **Lake Mode** — Connected central lakes with possible river outlets
  - **Seaside Mode** — Realistic coastlines with capes, inlets, harbors, and optional rivers
  - **River Mode** — Continuous, winding rivers with branching possibilities
- **Road System**
  - Voronoi-style placement of road nodes (town center, gates, district centers)
  - MST-based road connection
  - Terrain-aware carving (avoids deep sand/water)
- **Tile-Based Rendering**
  - Corner-based terrain tiles for seamless blending
  - Roads layered over terrain without overwriting it
- **User Interaction**
  - Pan and zoom support
  - Seed-based generation for reproducible maps
  - Map export as PNG and configuration export as JSON (planned)

---

## Tech Stack

**Frontend**
- [React](https://react.dev/) + [TypeScript](https://www.typescriptlang.org/)  
- [PixiJS](https://pixijs.com/) for tile rendering  

**Backend**
- [Java](https://www.oracle.com/java/) 17  
- [Spring Boot](https://spring.io/projects/spring-boot) for running the generation engine  
- No database — generation is entirely in-memory and session-based  

**Data & Assets**
- Tiled Map Editor (`.tmx`/`.tsx`) parsing for terrain/road tiles  
- Modular pixel art assets for terrain, roads, and decorations  

**Hosting**
- GitHub Pages (frontend deployment)  

---

## Project Structure

### **Frontend**
```plaintext
burghgen/
└── burghgen-frontend/                 # React + TypeScript frontend
    ├── public/
    │   └── assets/
    │       └── tilesets/              # Pixel art tilesets and Tiled .xml/.tsx files
    └── src/
        ├── components/                # Rendering components (e.g., TileRenderer)
        ├── types/                     # TypeScript type definitions (e.g., TerrainTile)
        └── utils/                     # Utility functions (tileset loading, tile index calculation)
```

### **Backend**
```plaintext
burghgen/
└── src/main/java/com/chancema/burghgen/
    ├── controller/                     # Main controller for generation requests
    ├── generation/
    │   └── road/                       # Road generation logic
    ├── model/                          # Data models and enums
    ├── service/                        # Core terrain and map generation services
    └── util/                           # Supporting utilities for procedural generation
```

---

## Current Status

BurghGen is **mid-development** with the following completed:
- Procedural terrain generation with blending
- Fully implemented lake, seaside, and river modes
- Road network generation with MST/Voronoi logic
- Tile rendering with corner-based terrain tiles
- Functional viewport with pan and zoom

**Planned for next iterations**:
- Procedural placement of buildings and landmarks
- District generation along roads
- Expanded decoration/prop placement (trees, rocks, docks)
- Save/load configurations
- Additional UI options for fine-tuning generation

---

## Preview

<img width="512" height="512" alt="image" src="https://github.com/user-attachments/assets/768ed59d-2589-4390-8991-06a28922cad5" />


---

## Credits

BurghGen uses free and open pixel art assets for terrain, roads, and decorations.  
All assets are credited to their respective creators:

"[LPC] Bricks" by bluecarrot16, Guido Bos, keith karnage, Lanea Zimmerman (Sharm), Casper Nilsson, Leonard Pabin, and Buch

Flowers, buildings and boxes; Interior wooden tiles; some old castle stuff 
Guido Bos
GPL v3 / CC-BY-SA 3.0
https://opengameart.org/content/flowers-buildings-and-boxes-interior-wooden-tiles-some-old-castle-stuff

Medieval town
keith karnage
CC-BY 3.0
https://opengameart.org/content/medieval-town-0

Liberated Pixel Cup (LPC) Base Assets 
Lanea Zimmerman (Sharm)
CC-BY-SA 3.0 / CC-BY 3.0 / GPL 3.0
https://opengameart.org/content/liberated-pixel-cup-lpc-base-assets-sprites-map-tiles

LPC C.Nilsson
Casper Nilsson
CC-BY-SA 3.0 / GPL 3.0
https://opengameart.org/content/lpc-cnilsson

Whispers of Avalon: Grassland Tileset
Leonard Pabin
CC-BY 3.0 / GPL 3.0 / GPL 2.0
https://opengameart.org/content/whispers-of-avalon-grassland-tileset

Outdoor 32x32 tileset
Buch
CC0
https://opengameart.org/content/outdoor-32x32-tileset

"[LPC] Terrains" by bluecarrot16, Lanea Zimmerman (Sharm), Daniel Eddeland (Daneeklu), Richard Kettering (Jetrel), Zachariah Husiar (Zabin), Hyptosis, Casper Nilsson, Buko Studios, Nushio, ZaPaper, billknye, William Thompson, caeles, Redshrike, Bertram, and Rayane Félix (RayaneFLX)

Liberated Pixel Cup (LPC) Base Assets (sprites & map tiles)
Lanea Zimmerman (Sharm)
CC-BY 3.0 / CC-BY-SA 3.0 / GPL 3.0
https://opengameart.org/content/liberated-pixel-cup-lpc-base-assets-sprites-map-tiles

[LPC] Farming tilesets, magic animations and UI elements
Daniel Eddeland (Daneeklu)
CC-BY-SA 3.0 / GPL 3.0
https://opengameart.org/content/lpc-farming-tilesets-magic-animations-and-ui-elements

ZRPG Tiles
Richard Kettering (Jetrel), Zachariah Husiar (Zabin), Hyptosis, Lanea Zimmerman (Sharm), and Open Pixel Project.
CC-BY-SA 3.0+
https://opengameart.org/content/zrpg-tiles

LPC C.Nilsson
Casper Nilsson
CC-BY-SA 3.0 / GPL 3.0
https://opengameart.org/content/lpc-cnilsson

Frozen Lake [LPC]
Buko Studios (http://www.buko-studios.com/) Commissioned by PlayCraft: (www.playcraftapp.com)
CC-BY 3.0
https://opengameart.org/content/frozen-lake-lpc

LPC Animated Water and waterfalls
ZaPaper
CC-BY-SA 3.0
https://opengameart.org/content/lpc-animated-water-and-waterfalls

LPC More Water Transitions
billknye
CC-BY-SA 3.0 / GPL 3.0
https://opengameart.org/content/lpc-more-water-transitions

[LPC] Sand+Rock Alt Colors
William.Thompsonj, Daniel Eddeland
CC-BY-SA 3.0 / GPL 3.0
https://opengameart.org/content/lpc-sandrock-alt-colors

[LPC] Colorful Sand + Deep Water!
Nushio
CC-BY-SA 3.0 / GPL 3.0
https://opengameart.org/content/lpc-colorful-sand-deep-water

LPC terrain extension
caeles
CC-BY-SA 3.0 / GPL 3.0
https://opengameart.org/content/lpc-terrain-extension

RPG Tiles: Cobble stone paths & town objects
Zachariah Husiar (Zabin), Daniel Eddeland (Daneeklu), Richard Kettering (Jetrel), Hyptosis, Redshrike, Bertram
CC-BY-SA 3.0
https://opengameart.org/content/rpg-tiles-cobble-stone-paths-town-objects

RPG Terrains
Rayane Félix (RayaneFLX)
CC-BY-SA 3.0
https://opengameart.org/content/rpg-terrains

---

## License

This project is developed for educational purposes as part of a senior capstone project.  
Asset usage is limited to free-for-use tilesets credited appropriately.
