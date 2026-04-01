package com.lomba.platformer.entities;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.lomba.platformer.components.Components.*;
import com.lomba.platformer.world.GameWorld;

/**
 * EntityFactory — static helpers that assemble entities by attaching the
 * correct components and creating the Box2D body in one call.
 *
 * Keeps entity construction out of screens and systems, keeping them lean.
 */
public final class EntityFactory {

    public static final float PPM = 32f;   // pixels per metre

    private EntityFactory() {}

    // ─── Player ──────────────────────────────────────────────────────────────

    public static Entity createPlayer(Engine engine, GameWorld world,
                                      TextureAtlas atlas, float spawnX, float spawnY) {
        Entity entity = new Entity();

        // Transform
        TransformComponent tc = new TransformComponent();
        tc.position.set(spawnX, spawnY);
        tc.size.set(0.9f, 1.8f);   // metres
        entity.add(tc);

        // Physics — dynamic body with fixed rotation (no toppling)
        BodyDef bd = new BodyDef();
        bd.type             = BodyDef.BodyType.DynamicBody;
        bd.position.set(spawnX, spawnY);
        bd.fixedRotation    = true;

        Body body = world.getB2World().createBody(bd);
        body.setUserData(entity);   // back-reference for contact listener

        // Main capsule-ish hitbox
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.4f, 0.85f);
        FixtureDef fd = new FixtureDef();
        fd.shape    = shape;
        fd.density  = 1f;
        fd.friction = 0.4f;
        body.createFixture(fd);
        shape.dispose();

        // Foot sensor — detects ground contact
        PolygonShape foot = new PolygonShape();
        foot.setAsBox(0.35f, 0.05f, new Vector2(0, -0.85f), 0);
        FixtureDef footFd = new FixtureDef();
        footFd.shape    = foot;
        footFd.isSensor = true;
        Fixture footFixture = body.createFixture(footFd);
        footFixture.setUserData("foot_sensor");
        foot.dispose();

        PhysicsComponent pc = new PhysicsComponent();
        pc.body = body;
        entity.add(pc);

        // Player state
        entity.add(new PlayerComponent());

        // Render
        RenderComponent rc = new RenderComponent();
        rc.region = atlas.findRegion("player/idle_0");
        entity.add(rc);

        // Animation (populate from atlas regions)
        AnimationComponent ac = buildPlayerAnimations(atlas);
        entity.add(ac);

        // Camera target
        entity.add(new CameraTargetComponent());

        engine.addEntity(entity);
        return entity;
    }

    // ─── Static ground tile ─────────────────────────────────────────────────

    /**
     * Creates a static Box2D body for a ground tile.
     * TiledMapSystem calls this for every collidable tile layer cell.
     */
    public static void createGroundBody(GameWorld world,
                                        float x, float y, float w, float h) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(x + w / 2f, y + h / 2f);

        Body body = world.getB2World().createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(w / 2f, h / 2f);
        Fixture f = body.createFixture(shape, 0f);
        f.setUserData("ground");
        shape.dispose();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static AnimationComponent buildPlayerAnimations(TextureAtlas atlas) {
        AnimationComponent ac = new AnimationComponent();
        float fps = 1f / 10f;   // 10 FPS for all sprite animations

        ac.idleAnim   = buildAnimation(atlas, "player/idle",   11, fps);
        ac.runAnim    = buildAnimation(atlas, "player/run",    12, fps);
        ac.jumpAnim   = buildAnimation(atlas, "player/jump",   1, fps);
        ac.fallAnim   = buildAnimation(atlas, "player/fall",   1, fps);
        ac.attackAnim = buildAnimation(atlas, "player/attack", 6, fps * 0.8f);
        ac.hurtAnim   = buildAnimation(atlas, "player/hurt",   3, fps);
        ac.deadAnim   = buildAnimation(atlas, "player/dead",   5, fps * 1.5f);

        return ac;
    }

    /**
     * Builds an Animation from numbered atlas regions: prefix_0, prefix_1 …
     */
    private static com.badlogic.gdx.graphics.g2d.Animation<TextureRegion>
    buildAnimation(TextureAtlas atlas, String prefix, int frames, float frameDuration) {
        TextureRegion[] regions = new TextureRegion[frames];
        for (int i = 0; i < frames; i++) {
            regions[i] = atlas.findRegion(prefix + "_" + i);
        }
        return new com.badlogic.gdx.graphics.g2d.Animation<>(frameDuration, regions);
    }
}
