package com.lomba.platformer.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.lomba.platformer.PlatformerGame;
import com.lomba.platformer.entities.EntityFactory;
import com.lomba.platformer.systems.Systems.*;
import com.lomba.platformer.world.GameWorld;

/**
 * GameScreen — the main gameplay screen.
 *
 * Responsibilities:
 *  1. Create and configure the Ashley Engine with all Systems
 *  2. Initialise Box2D world and build static bodies from TiledMap collision layer
 *  3. Run the fixed-timestep physics loop (prevents spiral of death)
 *  4. Render: TiledMap (background) → ECS RenderSystem → Box2D debug (dev only)
 *
 * Fixed timestep:
 *  PHYSICS_STEP = 1/60 s regardless of render FPS.
 *  Accumulator pattern ensures deterministic simulation on all devices.
 */
public class GameScreen extends ScreenAdapter {

    // ─── Physics constants ───────────────────────────────────────────────────
    private static final float PHYSICS_STEP      = 1f / 60f;
    private static final float MAX_FRAME_TIME    = 0.25f;  // prevents spiral of death
    public  static final float PPM               = EntityFactory.PPM;
    public  static final float VIEWPORT_WIDTH    = 20f;    // world units visible

    // ─── Core references ─────────────────────────────────────────────────────
    private final PlatformerGame game;
    private final Engine         engine;
    private final GameWorld      world;
    private final OrthographicCamera camera;

    // ─── Map rendering ───────────────────────────────────────────────────────
    private TiledMap tiledMap = null;
    private OrthogonalTiledMapRenderer mapRenderer = null;

    // ─── Physics debug (disable in release builds) ───────────────────────────
    private final Box2DDebugRenderer debugRenderer;
    private static final boolean DEBUG_PHYSICS = true;

    // ─── Timing ──────────────────────────────────────────────────────────────
    private float accumulator = 0f;

    public GameScreen(PlatformerGame game) {
        this.game   = game;
        this.engine = new Engine();
        this.world  = new GameWorld();

        // Camera
        float aspect = (float) Gdx.graphics.getWidth() / Gdx.graphics.getHeight();
        camera = new OrthographicCamera(VIEWPORT_WIDTH, VIEWPORT_WIDTH / aspect);
        camera.position.set(VIEWPORT_WIDTH / 2f, VIEWPORT_WIDTH / aspect / 2f, 0);
        camera.update();

        // TiledMap — 1 tile = 1 metre (PPM already handled by renderer unitScale)
        tiledMap    = game.assets.getLevel1Map();
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1f / PPM, game.batch);

        // Build static Box2D bodies from TiledMap "collision" object layer
        buildCollisionBodiesFromMap();

        // Entities
        TextureAtlas atlas = game.assets.getAtlas();
        EntityFactory.createPlayer(engine, world, atlas, 3f, 5f);

        // Register systems (priority order defined inside each class)
        engine.addSystem(new InputSystem());
        engine.addSystem(new MovementSystem());
        engine.addSystem(new PhysicsSyncSystem());
        engine.addSystem(new AnimationSystem());
        engine.addSystem(new RenderSystem(game.batch, camera));
        engine.addSystem(new CameraSystem(camera));
        engine.addSystem(new CleanupSystem());

        debugRenderer = new Box2DDebugRenderer();
    }

    @Override
    public void render(float delta) {
        // ── Fixed timestep accumulator ─────────────────────────────────────
        float frameTime = Math.min(delta, MAX_FRAME_TIME);
        accumulator += frameTime;
        while (accumulator >= PHYSICS_STEP) {
            world.step(PHYSICS_STEP);
            accumulator -= PHYSICS_STEP;
        }

        // ── Clear ──────────────────────────────────────────────────────────
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ── TiledMap background layers ─────────────────────────────────────
        if (mapRenderer != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }

        // ── ECS update (physics sync + animation + render) ─────────────────
        engine.update(delta);

        // ── Box2D debug overlay ────────────────────────────────────────────
        if (DEBUG_PHYSICS) {
            debugRenderer.render(world.getB2World(), camera.combined);
        }
    }

    @Override
    public void resize(int width, int height) {
        float aspect = (float) width / height;
        camera.viewportWidth  = VIEWPORT_WIDTH;
        camera.viewportHeight = VIEWPORT_WIDTH / aspect;
        camera.update();
    }

    @Override
    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
        // mapRenderer disposed with AssetManager; batch owned by PlatformerGame
    }

    // ─── TiledMap → Box2D collision build ────────────────────────────────────

    /**
     * Iterates the "collision" tile layer and creates a static Box2D body
     * for every non-empty cell.
     *
     * Uses a greedy horizontal merge heuristic:
     *  - Consecutive filled cells in the same row are merged into one wide body
     *  - Reduces Box2D body count → fewer contact checks per frame
     */
    private void buildCollisionBodiesFromMap() {
        TiledMapTileLayer layer = (TiledMapTileLayer) tiledMap.getLayers().get("collision");
        if (layer == null) {
            Gdx.app.log("GameScreen", "No 'collision' layer found in TiledMap.");
            return;
        }

        float tw = layer.getTileWidth()  / PPM;   // tile width  in metres
        float th = layer.getTileHeight() / PPM;   // tile height in metres
        int cols = layer.getWidth();
        int rows = layer.getHeight();

        for (int row = 0; row < rows; row++) {
            int col = 0;
            while (col < cols) {
                if (layer.getCell(col, row) == null) { col++; continue; }

                // Start of a run — find how far it extends
                int runStart = col;
                while (col < cols && layer.getCell(col, row) != null) col++;
                int runEnd = col;  // exclusive

                float bodyW = (runEnd - runStart) * tw;
                float bodyX = runStart * tw;
                float bodyY = row * th;

                EntityFactory.createGroundBody(world, bodyX, bodyY, bodyW, th);
            }
        }
    }
}
