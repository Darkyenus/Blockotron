package darkyenus.blockotron;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import darkyenus.blockotron.client.Game;

/**
 *
 */
public class Blockotron {
    public static void main(String[] args){
        final LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        new LwjglApplication(new Game(), config);
    }
}
