# AGENTS.md — FreightGame / TrainGame code base overview

A train-management sim ("TrainsInSpace", name pending; see `README.md` for game mechanics). Java, Maven (`pom.xml`, artifact `trains-in-space`), LWJGL 3 + JOML for graphics/math, Jackson for mod JSON.
The game is built on **NGN**, a separate engine library (`io.github.ieperen3039:ngn:1.2`, packages `io.github.ieperen3039.ngn.*`).

Naming note: the repo is called FreightGame, the main class is `TrainGame`, and the Java package is `io.github.ieperen3039.trainsinspace`. All three refer to the same project.

Source root: `src/io/github/ieperen3039/trainsinspace/` (abbreviated `…/` below). Tests: `test/…` (JUnit; GJK collision, Vector3fx, and some primitive-shape tests).

## Directory listing

```
pom.xml, README.md
res/                     runtime assets loaded through NGN's Asset system
  scenarios/			 non-code scenario data
  shaders/   			 GLSL for the render sequences
  ... 					 fonts, models, images, textures
mod/                     directory for json based plugin for adding assets to the game
  base/                  the base tracks, trains, cargo etc.
src/…/
  Boot.java              main(); parses -debug / -quiet / -lwjgl-debug, runs new TrainGame().root()
  Core/                  composition root and shared interfaces
    TrainGame            implements Game + ModLoader; creates/wires/cleans up every aspect; save/load
    Game                 interface: extends NGN Main, exposes state(), map(), lights(), particles(), objectTypes(), playerStatus()
    GameAspect           interface: init(Game) / cleanup() lifecycle for singletons
    ModLoader            interface: startGame/stopGame/save/load/initMods/cleanMods/allMods
    JarModReader         loads Mod classes from jars (URLClassLoader)
    PlayerStatus         money, owned trains and stations (serialized)
    Coloring             entity tint + prioritized "marking" colours (synchronized)
  GameState/             GameState (interface) and GameLoop (the simulation thread)
  Entities/              everything that lives in the world (all extend GameEntity)
    GameEntity           abstract base: entityId, spawn/despawn time, update frequency
    ...                  entity instances; rolling stock, stations and cargo storage
  Tracks/                track geometry and train movement
    RailMovement         per-train motion/braking/path reservation engine
    RailTools            static track builders: create/connect/split/remove track, place signals
    TrackPiece, TrackType, TrackTypeBase    interfaces and abstract track classes
    ...                  track instances (straight or circular), per-type meshes, speed, price
  Network/               routing graph on top of the tracks
    NetworkNode, RailNode, SpecialNetworkNode, NetworkPosition   graph nodes + "targets" (stations)
    Signal, SignalEntity   signals and path reservation;  Schedule, ScheduleElement   train timetables (+ schedule UI)
  Tools/                 collection of utility classes, and path finding classes
  GameMap/               GameMap (interface), GridMap -> HeightMap; MapGeneratorMod + Default/Fixed/Image/Copy generators;
                         MapSelector (mouse picking against terrain), IntersectionTester
  Camera/                TycoonFixedCamera (NGN Camera impl), CameraLight
  InputHandling/         MouseTool and MouseTargets impl, keybinds (InputMapping)
  Menu/                  Main/{MainMenu, NewGameFrame}; InGame/{TrainGameUI, Build/*, Overviews/TrainOverview}; S3DModelDisplay
  Mods/                  Mod, InitialisationMod, SoftMod (JSON-defined content), TypeCollection (registry), CargoType
  Content/Scenario/      Scenario (template: mods -> map -> entities -> camera -> light -> startGame) and concrete scenarios
  Particles/             GameParticles, ParticleCloud, Particles (factory), ParticleShader
  DataStructures/        CargoCollection, Valuta (money), collisions
  Settings/Settings      extends NGN Settings; game-specific tunables
```

## Architecture overview

**Composition root.** `TrainGame` owns one instance of every major subsystem and hands them out through the `Game` interface, which every other class receives (`init(Game)` or constructor argument). There is no DI framework and no globals except `Directory`, `Logger` (NGN) and static counters. `Game extends io.github.ieperen3039.ngn.Core.Main`, so NGN components that only know `Main` (camera, renderer, GUI, input) can work with the game object too.

**Lifecycle.** `Boot` → `new TrainGame()` (constructs aspects, discovers soft mods) → `root()` → `init()` (calls `init(this)` on every `GameAspect`, builds render sequences, shows `MainMenu`, starts the game loop thread *paused*) → `window.open()`; `renderer.run()` (blocks on the main thread until quit) → `cleanMods()` / `cleanup()`.
Starting a game goes through `Scenario.apply(game)`: pick mods → `ModLoader.initMods` → generate the map → add entities → camera/light → `ModLoader.startGame()` (swaps the main menu for `TrainGameUI`, unpauses the loop). `stopGame()` reverses this.

**Content / mods.** `Mod` is a `GameAspect` that fills the `TypeCollection` (track, cargo, locomotive, wagon, industry types). `SoftMod` is data-driven (`description.json` + meshes, e.g. `mod/base`). Jar mods are loaded by `JarModReader`; `InitialisationMod`s run once at startup and stay loaded (`permanentMods`), others live per game (`activeMods`). Mod names are stored in save files.

**World model.**
- `GameState`/`GameLoop` holds all `GameEntity` objects. `GameEntity` implements NGN's `Entity` (`update()` / `draw(SGL)`), adds an id, spawn/despawn times and an `UpdateFrequency` hint. Entities despawn by time and are purged each tick.
- Tracks are chains of `StraightTrack` / `CircleTrack` (`TrackPiece`) with a `TrackType`. Each piece connects two `RailNode`s.
- The `Network` package is a graph over the tracks: `NetworkNode` keeps a "compressed" view that skips plain nodes and only links to switches/ends/signals ("network-critical" nodes). `NetworkPathFinder` runs Dijkstra on that view. `Signal.reservePath` finds a free path to the next signal and marks `TrackPiece`s occupied.
- `Train` (list of `TrainElement`: `Locomotive`/`Wagon`) delegates movement to `RailMovement` (fixed 1/128 s integration, speed targets, reversal, reserved path) and follows a circular `Schedule` of `NetworkPosition`s (stations). `Station`/`StationImpl` handles load/unload with nearby `Industry` objects via `Storage`.
- `GameMap`/`HeightMap` is the terrain (grid of heights, chunked meshes, ray intersection); `MapGeneratorMod`s produce the height array.
- `PlayerStatus` holds money and the train/station lists. `Valuta` is the money type.

**Input / UI.** NGN's `MouseToolCallbacks` dispatches clicks through three selection layers set in `TrainGame`: `GameLoop` (entity picking via the colour-id `ClickShader`), `MapSelector` (terrain ray), `UISelector` (NGN GUI). The active `MouseTool` (build tools in `Menu/InGame/Build`, `EntityActionTool`) receives `MouseTarget`s (`MouseTargetEntity`, `MouseTargetMap`). Windows are NGN `SFrame`s managed by `FrameManagerImpl`.

**Rendering.** `RenderLoop.renderSequence(shader).add(gl -> …)` in `TrainGame.init`: (1) BlinnPhong: lights, map, entities; (2) Particle shader: particles; (3) Click shader: entities again for picking; plus the HUD (`frameManager::draw`). Entities draw themselves with NGN `SGL` (matrix stack + shader), meshes/shaders loaded as NGN `Asset`s.

**Persistence.** `TrainGame.saveGame/loadGame` pause the game loop and write a Java-serialization stream: version, mod names, settings, timer, camera, map (`Externalizable`), lights, particles, all entities (`GameLoop.writeTo`), player status, `"END"`. Transient fields (`game`, track `type`, asset handles) must be restored via `init`/`restore` after loading.

## Threading overview

| Thread | Owner | Runs |
|---|---|---|
| Main / render thread | `TrainGame.root()` → `RenderLoop.run()` (`TARGET_FPS`) | all GL calls, `draw(SGL)` of entities/map/particles, GUI drawing, GLFW event polling (input callbacks, tool `onPress`/`onHover`, camera input) |
| `Gameloop` thread | `GameLoop extends GenericThreadLoop` (`TARGET_TPS`) | `GameLoop.update`: `entity.update()` for all entities, `PlayerStatus.update()`, entity add/remove; also train movement, signals, cargo, industries |

Both loops derive from NGN's `GenericThreadLoop`, which provides: fixed-rate ticking, `pause()`/`unPause()`/`isPaused()` (a `CountDownLatch`), `initiateStop()`, and `defer(Runnable)` (a queue drained after each tick on that loop's own thread).

Cross-thread mechanisms in this code base:
- `Game.executeOnRenderThread(Runnable)` (`TrainGame`): runs immediately if already on the main thread, otherwise `renderer.defer`. Use it for GL resource creation/disposal (e.g. `mesh::dispose`) and `Asset.dropAll`.
- `TrainGame.doAfterGameTick(Runnable)`: `gameState.defer`; use it to mutate game state from the UI/input thread safely.
- `GameLoop`: `entities` is a `CopyOnWriteArrayList` (render thread iterates while the game thread mutates); new entities go through a `newEntities` buffer (`synchronized`) and are merged at the end of a tick. `entityIdMap` (id → entity, used for click picking) is guarded by `synchronized`.
- Save/load: `pause()` then spin on `isPaused()` before touching state.
- Other locks: `Coloring` (synchronized), `HeightMap.generate/cleanup/writeExternal` (`synchronized(this)`), `GameParticles` (`synchronized(this)`), `RailMovement.update/setProperties/setPosition/executeReversal` (`synchronized`), `GameEntity.nextEntityId` (`AtomicInteger`), `Tools/AutoLock` (try-with-resources `ReentrantLock` wrapper; currently unused).
- Everything else (network graph, `Signal` reservations, `Schedule`, stations, trains, build tools) has **no locking** and assumes a single owner thread. Treat the game thread as the owner of the world and mutate it from the UI thread only via `doAfterGameTick`/`GameState.addEntity`.

## Relation to the NGN library

NGN supplies the engine; this project supplies the game. Usage frequency below is the number of imports in this project.

| NGN area | What the game uses | Where |
|---|---|---|
| `Core` (`Main`, `GenericThreadLoop`) | `Game extends Main`; `GameLoop extends GenericThreadLoop` | `Core/Game`, `GameState/GameLoop` |
| `Rendering` (`RenderLoop`, `GLFWWindow`, `Lights`, `MatrixStack.SGL`, `Shaders.*`, `Material`, `Shapes.*`, `MeshLoading.Mesh`) | window, render sequences, lights, all drawing; `SGL` appears in 23 files, `Mesh` in 17, `MaterialShader` in 15 | `Core/TrainGame`, `Entities/*`, `Tracks/*`, `GameMap/HeightMap`, `Particles/*` |
| `AssetHandling` (`Asset`, `GeneratorAsset`, `Resource`) | lazily loaded/generated meshes and files; `Asset.dropAll` for hot reload | `Entities`, `Tracks`, `Mods/SoftMod`, build tools |
| `Scene.Entity` | interface implemented by `GameEntity` (`update()`, `draw(SGL)`) | `Entities/GameEntity` |
| `Camera.Camera` | `TycoonFixedCamera implements Camera` | `Camera/` |
| `InputHandling` (`MouseToolCallbacks`, `MouseTool`, `MouseTarget`, `MouseSelector`, `KeyControl`, `UISelector`) | picking layers and tools: `GameLoop` is a `MouseSelector`, build tools and `EntityActionTool` are `MouseTool`s, `MouseTargetEntity/Map` are `MouseTarget`s | `GameState/GameLoop`, `InputHandling/`, `Menu/InGame/Build/` |
| `UserInterface` (`SFrame`, `SButton`, `SContainer`, layouts, `FrameManagerImpl`, `SFrameLookAndFeel`, `NGFont`) | all menus and in-game windows (train/station/schedule UIs) | `Menu/`, `Entities/Train`, `Entities/StationImpl`, `Network/Schedule` |
| `Settings` | `Settings` extends NGN's settings with game fields | `Settings/Settings` |
| `DataStructures.Generic` (`Color4f`, `Pair`, `PairList`, `AveragingQueue`), `Interpolation`, `Tracked` | colours, pairs, smoothing, motion interpolators in `RailMovement` | widely |
| `Tools` (`Logger`, `Toolbox`, `Vectors`, `TickTime`, `TickTimeTimer`, `RealTimeTimer`) and `Version` | logging, helpers, game time (`Game.timer()`), version/mod compatibility | widely |

Rules of thumb:
- Rendering, GUI widgets, input dispatch, camera interface, time and the loop thread classes belong to NGN; change them in `D:\Projects\NGN`, not here.
- Game rules (tracks, network, trains, cargo, mods, scenarios, map generation, save format) belong to this repo.
- Classes in this repo that mirror an NGN concept and may be candidates for moving upstream: `Tools/Vector3fx` (fixed-point vectors), `Tools/AStarGrid`, `Tools/Rays`, `Particles/*` (renderer-level pieces). Unverified: check NGN before adding similar helpers (`Tools/TimeObserver` may overlap with NGN's `RenderLoop.timer`).
- NGN's `Main` already defines `timer()`, `camera()`, `window()`, `inputHandling()`, `gui()`, `keyControl()`; add new engine-level services there and game-level ones to `Game`.
