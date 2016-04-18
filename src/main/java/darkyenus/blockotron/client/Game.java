package darkyenus.blockotron.client;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import darkyenus.blockotron.render.BlockFaces;

import java.io.File;

/**
 *
 */
public class Game extends com.badlogic.gdx.Game {

    private static SpriteBatch uiBatch = null;

    public static SpriteBatch uiBatch(){
        if(uiBatch == null){
            uiBatch = new SpriteBatch();
        }
        return uiBatch;
    }

    @Override
    public void create() {
        BlockFaces.initialize();
        setScreen(new MenuScreen());
    }

    @Override
    public void setScreen(com.badlogic.gdx.Screen screen) {
        if(screen instanceof Screen){
            ((Screen) screen).game = this;
        }
        if(screen instanceof InputProcessor){
            Gdx.input.setInputProcessor((InputProcessor) screen);
        } else {
            Gdx.input.setInputProcessor(null);
        }
        super.setScreen(screen);
    }

    @Override
    public void dispose() {
        super.dispose();
        uiBatch.dispose();
        uiBatch = null;
    }

    private static FileHandle resourceRoot = null;

    public static FileHandle getResourceRoot(){
        if(resourceRoot == null){
            resourceRoot = new FileHandle(new File("resources"));
            if(!resourceRoot.isDirectory()){
                Gdx.app.error("Game.getResourceRoot()","Failed to find resource directory at "+resourceRoot.file().getAbsolutePath());
            }
        }
        return resourceRoot;
    }
}
