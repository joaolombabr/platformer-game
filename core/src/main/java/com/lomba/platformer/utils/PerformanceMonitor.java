package com.lomba.platformer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.profiling.GLProfiler;

/**
 * PerformanceMonitor — dev-mode overlay showing:
 *  - FPS
 *  - Frame time (ms)
 *  - GL draw calls per frame  (via GLProfiler)
 *  - Texture binds per frame  (via GLProfiler)
 *  - Heap memory (MB)
 *
 * GLProfiler instruments every OpenGL call, so disable it in release builds
 * by calling disable() or wrapping usage in BuildConfig.DEBUG.
 *
 * Usage in GameScreen:
 *   monitor = new PerformanceMonitor();
 *   // in render():
 *   monitor.frameStart();
 *   // ... normal rendering ...
 *   monitor.renderOverlay(batch, font);
 */
public class PerformanceMonitor {

    private final GLProfiler profiler;
    private boolean enabled;

    // Rolling average over last N frames
    private static final int   SAMPLE_SIZE  = 60;
    private final float[]      frameTimes   = new float[SAMPLE_SIZE];
    private int                sampleIndex  = 0;
    private float              avgFrameTime = 0;

    public PerformanceMonitor() {
        profiler = new GLProfiler(Gdx.graphics);
        enable();
    }

    public void enable()  { enabled = true;  profiler.enable();  }
    public void disable() { enabled = false; profiler.disable(); }

    /** Call at the top of render() before any drawing. */
    public void frameStart() {
        if (!enabled) return;
        profiler.reset();

        float dt = Gdx.graphics.getDeltaTime() * 1000f;
        frameTimes[sampleIndex % SAMPLE_SIZE] = dt;
        sampleIndex++;
        float sum = 0;
        int count = Math.min(sampleIndex, SAMPLE_SIZE);
        for (int i = 0; i < count; i++) sum += frameTimes[i];
        avgFrameTime = sum / count;
    }

    /**
     * Renders the HUD overlay in screen-space coordinates.
     * Call AFTER the main game rendering, with a separate camera if needed.
     */
    public void renderOverlay(SpriteBatch batch, BitmapFont font) {
        if (!enabled) return;

        long heapMB = (Runtime.getRuntime().totalMemory()
                     - Runtime.getRuntime().freeMemory()) / (1024 * 1024);

        String info = String.format(
                "FPS: %d  |  frame: %.1f ms  |  draws: %d  |  binds: %d  |  heap: %d MB",
                Gdx.graphics.getFramesPerSecond(),
                avgFrameTime,
                profiler.getDrawCalls(),
                profiler.getTextureBindings(),
                heapMB
        );

        batch.begin();
        font.draw(batch, info, 10, Gdx.graphics.getHeight() - 10);
        batch.end();
    }
}
