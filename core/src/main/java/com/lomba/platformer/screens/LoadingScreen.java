package com.lomba.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.lomba.platformer.PlatformerGame;

/**
 * LoadingScreen — renders a progress bar while assets load asynchronously.
 *
 * Uses ShapeRenderer (no texture required) so it works before the atlas is ready.
 * Transitions to GameScreen once AssetManager reports 100 %.
 */
public class LoadingScreen extends ScreenAdapter {

    private static final float BAR_WIDTH  = 400f;
    private static final float BAR_HEIGHT = 20f;

    private final PlatformerGame game;
    private final ShapeRenderer  shapes;

    public LoadingScreen(PlatformerGame game) {
        this.game   = game;
        this.shapes = new ShapeRenderer();
        game.assets.queueAll();   // kick off async loading
    }

    @Override
    public void render(float delta) {
        // Advance loading pipeline
        boolean done = game.assets.update();

        // Clear
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw progress bar
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float x = (screenW - BAR_WIDTH) / 2f;
        float y = (screenH - BAR_HEIGHT) / 2f;
        float progress = game.assets.getProgress();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.2f, 0.2f, 0.25f, 1f);
        shapes.rect(x, y, BAR_WIDTH, BAR_HEIGHT);

        shapes.setColor(0.4f, 0.8f, 1.0f, 1f);
        shapes.rect(x, y, BAR_WIDTH * progress, BAR_HEIGHT);
        shapes.end();

        if (done) {
            game.setScreen(new GameScreen(game));
        }
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
