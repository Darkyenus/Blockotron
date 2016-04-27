package darkyenus.blockotron;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import darkyenus.blockotron.client.Configuration;
import darkyenus.blockotron.client.Game;

/**
 *
 */
public class Blockotron {

    public static void main(String[] args) {
        final Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        Configuration.loadConfiguration();
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, Configuration.aaSamples.value);

        int windowWidth = Configuration.windowWidth.value;
        int windowHeight = Configuration.windowHeight.value;
        if(Configuration.fullscreen.value){
            final Graphics.DisplayMode[] displayModes = Lwjgl3ApplicationConfiguration.getDisplayModes();
            Graphics.DisplayMode closest = Lwjgl3ApplicationConfiguration.getDisplayMode();
            int closestDifference = Integer.MAX_VALUE;

            if (windowWidth > 0 && windowHeight > 0) {
                for (Graphics.DisplayMode mode : displayModes) {
                    if(mode.width == windowWidth && mode.height == windowHeight){
                        closest = mode;
                        break;
                    }

                    final int needed = windowWidth * windowHeight;
                    final int got = mode.width * mode.height;
                    final int diff = Math.abs(needed - got);
                    if(diff < closestDifference){
                        closestDifference = diff;
                        closest = mode;
                    }
                }
            }

            config.setFullscreenMode(closest);
        } else {
            Graphics.DisplayMode desktopMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
            if(windowWidth <= 0 || windowHeight <= 0 || windowWidth > desktopMode.width || windowHeight > desktopMode.height){
                float scale = 0.7f;
                windowWidth = (int) (desktopMode.width * scale);
                windowHeight = (int) (desktopMode.height * scale);
            }
            config.setWindowedMode(windowWidth, windowHeight);
        }

        config.useVsync(Configuration.vSync.value);

        new Lwjgl3Application(new Game(), config);
    }
}
