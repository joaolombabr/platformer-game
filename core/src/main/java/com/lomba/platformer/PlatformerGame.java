package com.lomba.platformer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.lomba.platformer.assets.AssetLoader;
import com.lomba.platformer.screens.LoadingScreen;

/**
 * PlatformerGame — root Game class.
 *
 * Owns the single SpriteBatch (expensive to create) and the AssetLoader,
 * both shared across all Screens to avoid redundant GPU uploads.
 */
public class PlatformerGame extends Game {

    /** One batch for the entire game — never recreate it per-screen. */
    public SpriteBatch batch;

    /** Wrapper around libGDX AssetManager with typed helpers. */
    public AssetLoader assets;

    @Override
    public void create() {
        batch  = new SpriteBatch();
        assets = new AssetLoader();
        setScreen(new LoadingScreen(this));
    }

    @Override
    public void dispose() {
        // Screens dispose their own resources; Game disposes shared ones.
        if (screen != null) screen.dispose();
        batch.dispose();
        assets.dispose();
    }
}
