package com.lomba.platformer.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.lomba.platformer.components.Components.PlayerComponent;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;

/**
 * GameWorld wraps the Box2D {@link World} and its {@link ContactListener}.
 *
 * Box2D units: 1 metre = 1 tile = 32 pixels (PPM constant in GameScreen).
 * All physics bodies are created here or via EntityFactory.
 *
 * Performance notes:
 *  - velocityIterations=8, positionIterations=3 balances accuracy vs CPU cost
 *  - Time step is FIXED (1/60 s) with accumulator in GameScreen for determinism
 */
public class GameWorld {

    public static final float GRAVITY = -20f;   // m/s² — snappier than real gravity

    private final World world;
    private static final ComponentMapper<PlayerComponent> playerM =
            ComponentMapper.getFor(PlayerComponent.class);

    public GameWorld() {
        world = new World(new Vector2(0, GRAVITY), /* sleepingAllowed= */ true);
        world.setContactListener(new PlatformerContactListener());
    }

    /**
     * Step the simulation.
     * Called from GameScreen with a fixed 1/60 s timestep.
     */
    public void step(float timeStep) {
        world.step(timeStep, 8, 3);
    }

    public World getB2World() { return world; }

    public void dispose() { world.dispose(); }

    // ─── Contact Listener ────────────────────────────────────────────────────

    /**
     * Handles collision events between Box2D fixtures.
     *
     * Uses user-data tags on fixtures to identify entity pairs without
     * expensive instanceof chains.
     */
    private static class PlatformerContactListener implements ContactListener {

        @Override
        public void beginContact(Contact contact) {
            Fixture a = contact.getFixtureA();
            Fixture b = contact.getFixtureB();

            handleGroundContact(a, b, true);
            handleGroundContact(b, a, true);
        }

        @Override
        public void endContact(Contact contact) {
            Fixture a = contact.getFixtureA();
            Fixture b = contact.getFixtureB();

            handleGroundContact(a, b, false);
            handleGroundContact(b, a, false);
        }

        @Override public void preSolve(Contact contact, Manifold oldManifold) {}
        @Override public void postSolve(Contact contact, ContactImpulse impulse) {}

        /**
         * If 'sensor' is the player's foot sensor and 'other' is ground,
         * set isGrounded accordingly.
         */
        private void handleGroundContact(Fixture sensor, Fixture other, boolean grounded) {
            if (!"foot_sensor".equals(sensor.getUserData())) return;
            if (!"ground".equals(other.getUserData()) && !"platform".equals(other.getUserData())) return;

            Object bodyData = sensor.getBody().getUserData();
            if (!(bodyData instanceof Entity)) return;

            Entity entity = (Entity) bodyData;
            PlayerComponent pc = playerM.get(entity);
            if (pc != null) {
                pc.isGrounded = grounded;
            }
        }
    }
}
