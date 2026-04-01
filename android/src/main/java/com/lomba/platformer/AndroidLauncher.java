package com.lomba.platformer;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;   // fullscreen on Android
        config.useAccelerometer = false;  // disable unused sensors → saves battery
        config.useCompass = false;
        initialize(new PlatformerGame(), config);
    }
}
