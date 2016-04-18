package darkyenus.blockotron;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import darkyenus.blockotron.client.Game;

/**
 *
 */
public class Blockotron {
    public static void main(String[] args){
        final Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        final int msaa = 0;
        double scaleBy = 0.7;
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, msaa);
        Graphics.DisplayMode desktopMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
        config.setWindowedMode((int) (desktopMode.width * scaleBy), (int) (desktopMode.height * scaleBy));
        new Lwjgl3Application(new Game(), config);
    }
}
