package darkyenus.blockotron;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import darkyenus.blockotron.client.Game;

/**
 *
 */
public class Blockotron {
    public static void main(String[] args){
        final Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        final int msaa = 0;
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, msaa);

        new Lwjgl3Application(new Game(), config);
    }
}
