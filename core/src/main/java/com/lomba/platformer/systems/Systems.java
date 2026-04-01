package com.lomba.platformer.systems;

import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.lomba.platformer.components.Components.*;
import com.lomba.platformer.entities.EntityFactory;
import com.lomba.platformer.world.GameWorld;

/**
 * All ECS Systems in one file (split into sub-files as the project grows).
 *
 * Priority ordering (lower number = processed first each frame):
 *   1. InputSystem        — read hardware input, set flags
 *   2. MovementSystem     — apply forces to Box2D bodies
 *   3. PhysicsSyncSystem  — copy Box2D positions → TransformComponent
 *   4. AnimationSystem    — pick animation frame from state
 *   5. RenderSystem       — batch draw all visible entities
 *   6. CameraSystem       — smooth camera follow
 *   7. CleanupSystem      — remove entities tagged RemoveComponent
 */
public final class Systems {

    private Systems() {}

    // ─── 1. InputSystem ──────────────────────────────────────────────────────

    /**
     * Reads keyboard/touch input and writes intent flags into PlayerComponent.
     * Decoupling input from physics allows remapping and multi-platform support.
     */
    public static class InputSystem extends IteratingSystem {

        private static final ComponentMapper<PlayerComponent> pm =
                ComponentMapper.getFor(PlayerComponent.class);

        public InputSystem() {
            super(Family.all(PlayerComponent.class).get(), 1);
        }

        @Override
        protected void processEntity(Entity entity, float delta) {
            PlayerComponent pc = pm.get(entity);

            pc.wantsMoveL = Gdx.input.isKeyPressed(Input.Keys.LEFT)
                         || Gdx.input.isKeyPressed(Input.Keys.A);
            pc.wantsMoveR = Gdx.input.isKeyPressed(Input.Keys.RIGHT)
                         || Gdx.input.isKeyPressed(Input.Keys.D);

            // Jump: keyDown fires once — use jumpBuffer to smooth the input
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)
             || Gdx.input.isKeyJustPressed(Input.Keys.W)
             || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                pc.jumpBufferTimer = PlayerComponent.JUMP_BUFFER;
            }
        }
    }

    // ─── 2. MovementSystem ───────────────────────────────────────────────────

    /**
     * Translates intent flags into Box2D impulses.
     *
     * Implements:
     *  - Horizontal velocity clamping (direct velocity set for snappy feel)
     *  - Coyote time (grace jump after walking off ledge)
     *  - Jump buffering (accepts jump input slightly before landing)
     *  - Variable-height jump (release space early → lower arc)
     */
    public static class MovementSystem extends IteratingSystem {

        private static final float LOW_JUMP_MULTIPLIER = 2.5f;   // short hop gravity scale
        private static final float FALL_MULTIPLIER     = 2.0f;   // fast fall gravity scale

        private static final ComponentMapper<PlayerComponent> pm =
                ComponentMapper.getFor(PlayerComponent.class);
        private static final ComponentMapper<PhysicsComponent> phm =
                ComponentMapper.getFor(PhysicsComponent.class);
        private static final ComponentMapper<AnimationComponent> am =
                ComponentMapper.getFor(AnimationComponent.class);

        public MovementSystem() {
            super(Family.all(PlayerComponent.class, PhysicsComponent.class).get(), 2);
        }

        @Override
        protected void processEntity(Entity entity, float delta) {
            PlayerComponent  pc   = pm.get(entity);
            PhysicsComponent phc  = phm.get(entity);
            AnimationComponent ac = am.get(entity);
            Body body = phc.body;
            Vector2 vel = body.getLinearVelocity();

            // ── Coyote time countdown ──────────────────────────────────────
            if (!pc.isGrounded) {
                pc.coyoteTimer = Math.max(0, pc.coyoteTimer - delta);
            } else {
                pc.coyoteTimer = PlayerComponent.COYOTE_TIME;
            }

            // ── Jump buffer countdown ──────────────────────────────────────
            if (pc.jumpBufferTimer > 0) {
                pc.jumpBufferTimer = Math.max(0, pc.jumpBufferTimer - delta);
            }

            // ── Jump ──────────────────────────────────────────────────────
            boolean canJump = pc.isGrounded || pc.coyoteTimer > 0;
            if (pc.jumpBufferTimer > 0 && canJump) {
                body.setLinearVelocity(vel.x, 0);
                body.applyLinearImpulse(
                        new Vector2(0, pc.jumpForce * body.getMass()),
                        body.getWorldCenter(), true);
                pc.jumpBufferTimer = 0;
                pc.coyoteTimer     = 0;
            }

            // ── Better jump: asymmetric gravity ───────────────────────────
            float gy = GameWorld.GRAVITY;
            if (vel.y < 0) {
                // Falling → extra gravity for snappier arcs
                body.applyForceToCenter(
                        new Vector2(0, gy * (FALL_MULTIPLIER - 1) * body.getMass()), true);
            } else if (vel.y > 0 && !Gdx.input.isKeyPressed(Input.Keys.SPACE)
                                  && !Gdx.input.isKeyPressed(Input.Keys.W)
                                  && !Gdx.input.isKeyPressed(Input.Keys.UP)) {
                // Rising but button released → short hop
                body.applyForceToCenter(
                        new Vector2(0, gy * (LOW_JUMP_MULTIPLIER - 1) * body.getMass()), true);
            }

            // ── Horizontal movement (direct velocity) ─────────────────────
            float targetVX = 0;
            if (pc.wantsMoveL) targetVX -= pc.moveSpeed;
            if (pc.wantsMoveR) targetVX += pc.moveSpeed;

            body.setLinearVelocity(targetVX, MathUtils.clamp(vel.y, -30, 30));

            // ── Update animation state ─────────────────────────────────────
            if (ac != null) {
                if (vel.y > 0.5f)          ac.currentState = AnimationComponent.State.JUMP;
                else if (vel.y < -0.5f)    ac.currentState = AnimationComponent.State.FALL;
                else if (Math.abs(vel.x) > 0.2f) ac.currentState = AnimationComponent.State.RUN;
                else                       ac.currentState = AnimationComponent.State.IDLE;

                if (ac.currentState != ac.previousState) {
                    ac.stateTimer    = 0;
                    ac.previousState = ac.currentState;
                }
            }
        }
    }

    // ─── 3. PhysicsSyncSystem ────────────────────────────────────────────────

    /**
     * After Box2D steps, copy body positions back into TransformComponent.
     * RenderSystem reads TransformComponent only — it never touches Box2D directly.
     */
    public static class PhysicsSyncSystem extends IteratingSystem {

        private static final ComponentMapper<TransformComponent> tm =
                ComponentMapper.getFor(TransformComponent.class);
        private static final ComponentMapper<PhysicsComponent> pm =
                ComponentMapper.getFor(PhysicsComponent.class);

        public PhysicsSyncSystem() {
            super(Family.all(TransformComponent.class, PhysicsComponent.class).get(), 3);
        }

        @Override
        protected void processEntity(Entity entity, float delta) {
            TransformComponent tc = tm.get(entity);
            PhysicsComponent   pc = pm.get(entity);
            Vector2 pos = pc.body.getPosition();
            tc.position.set(pos.x, pos.y);
            tc.rotation = MathUtils.radiansToDegrees * pc.body.getAngle();
        }
    }

    // ─── 4. AnimationSystem ──────────────────────────────────────────────────

    /** Advances animation timers and writes the current frame into RenderComponent. */
    public static class AnimationSystem extends IteratingSystem {

        private static final ComponentMapper<AnimationComponent> am =
                ComponentMapper.getFor(AnimationComponent.class);
        private static final ComponentMapper<RenderComponent> rm =
                ComponentMapper.getFor(RenderComponent.class);
        private static final ComponentMapper<PhysicsComponent> pm =
                ComponentMapper.getFor(PhysicsComponent.class);

        public AnimationSystem() {
            super(Family.all(AnimationComponent.class, RenderComponent.class).get(), 4);
        }

        @Override
        protected void processEntity(Entity entity, float delta) {
            AnimationComponent ac = am.get(entity);
            RenderComponent    rc = rm.get(entity);

            ac.stateTimer += delta;
            var anim = ac.currentAnimation();
            if (anim != null) {
                rc.region = anim.getKeyFrame(ac.stateTimer, /* looping= */ true);
            }

            // Flip sprite based on velocity direction
            PhysicsComponent pc = pm.get(entity);
            if (pc != null) {
                float vx = pc.body.getLinearVelocity().x;
                if (vx < -0.1f)      rc.flipX = true;
                else if (vx > 0.1f)  rc.flipX = false;
            }
        }
    }

    // ─── 5. RenderSystem ─────────────────────────────────────────────────────

    /**
     * Renders all entities with TransformComponent + RenderComponent.
     *
     * Performance:
     *  - Single SpriteBatch.begin/end per frame (shared across all entities)
     *  - All sprites from one TextureAtlas → zero texture binds mid-batch
     *  - Frustum culling: skip entities outside the camera's viewport
     */
    public static class RenderSystem extends IteratingSystem {

        private static final ComponentMapper<TransformComponent> tm =
                ComponentMapper.getFor(TransformComponent.class);
        private static final ComponentMapper<RenderComponent> rm =
                ComponentMapper.getFor(RenderComponent.class);

        private final SpriteBatch        batch;
        private final OrthographicCamera camera;

        public RenderSystem(SpriteBatch batch, OrthographicCamera camera) {
            super(Family.all(TransformComponent.class, RenderComponent.class).get(), 5);
            this.batch  = batch;
            this.camera = camera;
        }

        @Override
        public void update(float delta) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            super.update(delta);
            batch.end();
        }

        @Override
        protected void processEntity(Entity entity, float delta) {
            TransformComponent tc = tm.get(entity);
            RenderComponent    rc = rm.get(entity);
            if (rc.region == null) return;

            // Frustum culling — skip off-screen entities
            float hw = tc.size.x / 2f;
            float hh = tc.size.y / 2f;
            float camHW = camera.viewportWidth  * camera.zoom / 2f;
            float camHH = camera.viewportHeight * camera.zoom / 2f;
            if (tc.position.x + hw < camera.position.x - camHW) return;
            if (tc.position.x - hw > camera.position.x + camHW) return;
            if (tc.position.y + hh < camera.position.y - camHH) return;
            if (tc.position.y - hh > camera.position.y + camHH) return;

            float drawX = tc.position.x - hw;
            float drawY = tc.position.y - hh;

            if (rc.flipX) {
                batch.draw(rc.region,
                        drawX + tc.size.x, drawY,        // flipped origin
                        -tc.size.x, tc.size.y);
            } else {
                batch.draw(rc.region, drawX, drawY, tc.size.x, tc.size.y);
            }
        }
    }

    // ─── 6. CameraSystem ─────────────────────────────────────────────────────

    /**
     * Smooth camera follow with lerp + configurable dead zone.
     * Runs after rendering so the camera is ready for the NEXT frame.
     */
    public static class CameraSystem extends IteratingSystem {

        private static final float LERP_SPEED  = 5f;
        private static final float LOOK_AHEAD  = 2f;   // metres ahead of movement dir

        private static final ComponentMapper<TransformComponent> tm =
                ComponentMapper.getFor(TransformComponent.class);
        private static final ComponentMapper<PhysicsComponent> pm =
                ComponentMapper.getFor(PhysicsComponent.class);

        private final OrthographicCamera camera;

        public CameraSystem(OrthographicCamera camera) {
            super(Family.all(TransformComponent.class,
                             CameraTargetComponent.class,
                             PhysicsComponent.class).get(), 6);
            this.camera = camera;
        }

        @Override
        protected void processEntity(Entity entity, float delta) {
            TransformComponent tc = tm.get(entity);
            PhysicsComponent   pc = pm.get(entity);

            float targetX = tc.position.x + Math.signum(pc.body.getLinearVelocity().x) * LOOK_AHEAD;
            float targetY = tc.position.y + 1f;   // offset upward slightly

            camera.position.x += (targetX - camera.position.x) * LERP_SPEED * delta;
            camera.position.y += (targetY - camera.position.y) * LERP_SPEED * delta;
            camera.update();
        }
    }

    // ─── 7. CleanupSystem ────────────────────────────────────────────────────

    /**
     * Removes entities tagged with RemoveComponent at the end of each frame.
     * Deferred removal avoids ConcurrentModificationException in other systems.
     */
    public static class CleanupSystem extends EntitySystem {

        private final ComponentMapper<RemoveComponent> rm =
                ComponentMapper.getFor(RemoveComponent.class);

        private ImmutableArray<Entity> entities;

        public CleanupSystem() { super(7); }

        @Override
        public void addedToEngine(Engine engine) {
            entities = engine.getEntitiesFor(Family.all(RemoveComponent.class).get());
        }

        @Override
        public void update(float delta) {
            // Snapshot to avoid mutation during iteration
            Entity[] toRemove = entities.toArray(Entity.class);
            for (Entity e : toRemove) {
                getEngine().removeEntity(e);
            }
        }
    }
}
