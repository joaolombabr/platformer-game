package com.lomba.platformer.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * ECS Components — pure data bags (no logic).
 *
 * Ashley's ComponentMapper<T>.getFor(T.class) gives O(1) access.
 * Group them in one file to keep imports clean; split if the project grows.
 */
public final class Components {

    // Prevent instantiation
    private Components() {}

    // ─── Transform ───────────────────────────────────────────────────────────

    /** World position and size in Box2D metres. */
    public static class TransformComponent implements Component {
        public final Vector2 position = new Vector2();
        public final Vector2 size     = new Vector2(1f, 1f);
        public float rotation = 0f;
    }

    // ─── Physics ─────────────────────────────────────────────────────────────

    /** Wraps the Box2D body. One body per physics entity. */
    public static class PhysicsComponent implements Component {
        public Body body;
    }

    // ─── Rendering ───────────────────────────────────────────────────────────

    /** Current frame to render. Updated by AnimationSystem. */
    public static class RenderComponent implements Component {
        public TextureRegion region;
        public boolean       flipX = false;
        public int           zIndex = 0;   // lower = drawn first
    }

    /** Sprite sheet animation state. */
    public static class AnimationComponent implements Component {
        public enum State { IDLE, RUN, JUMP, FALL, ATTACK, HURT, DEAD }

        public State     currentState  = State.IDLE;
        public State     previousState = State.IDLE;
        public float     stateTimer    = 0f;

        public Animation<TextureRegion> idleAnim;
        public Animation<TextureRegion> runAnim;
        public Animation<TextureRegion> jumpAnim;
        public Animation<TextureRegion> fallAnim;
        public Animation<TextureRegion> attackAnim;
        public Animation<TextureRegion> hurtAnim;
        public Animation<TextureRegion> deadAnim;

        /** Returns the animation matching the current state. */
        public Animation<TextureRegion> currentAnimation() {
            switch (currentState) {
                case RUN:    return runAnim;
                case JUMP:   return jumpAnim;
                case FALL:   return fallAnim;
                case ATTACK: return attackAnim;
                case HURT:   return hurtAnim;
                case DEAD:   return deadAnim;
                default:     return idleAnim;
            }
        }
    }

    // ─── Player ──────────────────────────────────────────────────────────────

    /** Player-specific state: health, input flags, ability cooldowns. */
    public static class PlayerComponent implements Component {
        public int   health    = 100;
        public int   maxHealth = 100;
        public float jumpForce = 12f;     // Box2D metres/s
        public float moveSpeed = 6f;

        // Input flags written by InputSystem, read by MovementSystem
        public boolean wantsJump   = false;
        public boolean wantsMoveL  = false;
        public boolean wantsMoveR  = false;
        public boolean isGrounded  = false;

        // Coyote time: allows jumping for a few frames after walking off a ledge
        public float coyoteTimer      = 0f;
        public static final float COYOTE_TIME = 0.12f;

        // Jump buffering: stores a jump input for a few frames before landing
        public float jumpBufferTimer  = 0f;
        public static final float JUMP_BUFFER = 0.1f;
    }

    // ─── Enemy ───────────────────────────────────────────────────────────────

    /** Basic enemy AI state. */
    public static class EnemyComponent implements Component {
        public enum AIState { PATROL, CHASE, ATTACK, HURT, DEAD }

        public AIState state          = AIState.PATROL;
        public float   patrolLeft     = 0f;
        public float   patrolRight    = 0f;
        public float   detectionRange = 6f;   // metres
        public float   attackRange    = 1.2f;
        public float   attackCooldown = 0f;
        public int     health         = 30;
    }

    // ─── Projectile ──────────────────────────────────────────────────────────

    /**
     * Projectile — pooled via ObjectPool to avoid GC pressure.
     * Lifetime controls auto-despawn.
     */
    public static class ProjectileComponent implements Component {
        public float damage   = 10f;
        public float lifetime = 3f;   // seconds until auto-despawn
        public boolean fromPlayer = true;
    }

    // ─── Collectible ─────────────────────────────────────────────────────────

    public static class CollectibleComponent implements Component {
        public enum Type { COIN, HEALTH, POWER_UP }
        public Type  type  = Type.COIN;
        public float value = 1f;
    }

    // ─── Tag components (no fields, used as markers) ─────────────────────────

    /** Marks an entity for removal at the end of the current frame. */
    public static class RemoveComponent implements Component {}

    /** Marks the camera-tracking entity (usually the player). */
    public static class CameraTargetComponent implements Component {}
}
