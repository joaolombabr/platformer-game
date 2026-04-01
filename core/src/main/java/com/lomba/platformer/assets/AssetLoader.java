package com.lomba.platformer.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

/**
 * AssetLoader — carrega todos os assets do jogo.
 * Constrói o TextureAtlas manualmente a partir das imagens individuais,
 * evitando problemas de parsing do arquivo .atlas.
 */
public class AssetLoader {

    public static final String MAP_LEVEL_1 = "maps/level1.tmx";
    public static final String SFX_JUMP    = "audio/jump.wav";
    public static final String SFX_HIT     = "audio/hit.wav";
    public static final String MUSIC_GAME  = "audio/theme.ogg";

    private final AssetManager manager;
    private TextureAtlas atlas;

    public AssetLoader() {
        InternalFileHandleResolver resolver = new InternalFileHandleResolver();
        manager = new AssetManager(resolver);
        manager.setLoader(TiledMap.class, new TmxMapLoader(resolver));
    }

    public void queueAll() {
        manager.load(MAP_LEVEL_1, TiledMap.class);
        manager.load(SFX_JUMP,    Sound.class);
        manager.load(SFX_HIT,     Sound.class);
        manager.load(MUSIC_GAME,  Music.class);
    }

    public boolean update() {
        boolean done = manager.update();
        if (done && atlas == null) {
            buildAtlas();
        }
        return done && atlas != null;
    }

    private void buildAtlas() {
        atlas = new TextureAtlas();

        // Lista de todas as animações e seus frames
        String[][] animations = {
            {"player/attack", "0","1","2","3","4","5"},
            {"player/dead",   "0","1","2","3","4"},
            {"player/fall",   "0"},
            {"player/hurt",   "0","1","2"},
            {"player/idle",   "0","1","2","3","4","5","6","7","8","9","10"},
            {"player/jump",   "0"},
            {"player/run",    "0","1","2","3","4","5","6","7","8","9","10","11"},
        };

        for (String[] anim : animations) {
            String prefix = anim[0];
            for (int i = 1; i < anim.length; i++) {
                String frameName = prefix + "_" + anim[i];
                String path = "sprites/" + frameName + ".png";
                Texture tex = new Texture(Gdx.files.internal(path));
                atlas.addRegion(frameName, new TextureRegion(tex));
            }
        }
    }

    public float getProgress() {
        float base = manager.getProgress();
        return atlas != null ? 1f : base * 0.95f;
    }

    public TextureAtlas getAtlas()      { return atlas; }
    public TiledMap     getLevel1Map()  { return manager.get(MAP_LEVEL_1, TiledMap.class); }
    public Sound        getJumpSound()  { return manager.get(SFX_JUMP,    Sound.class);    }
    public Sound        getHitSound()   { return manager.get(SFX_HIT,     Sound.class);    }
    public Music        getThemeMusic() { return manager.get(MUSIC_GAME,  Music.class);    }

    public void dispose() {
        manager.dispose();
        if (atlas != null) atlas.dispose();
    }
}
