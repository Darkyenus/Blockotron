package darkyenus.blockotron.client;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Vector3;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.WorldRenderer;
import darkyenus.blockotron.world.GeneratorChunkProvider;
import darkyenus.blockotron.world.World;
import darkyenus.blockotron.world.debug.DebugWorldGenerator;

/**
 *
 */
public class GameScreen extends Screen {

    private WorldRenderer renderer;
    private World world;

    private FirstPersonCameraController controller;

    @Override
    public void show() {
        if(renderer == null){
            world = new World(new GeneratorChunkProvider(new DebugWorldGenerator()));
            renderer = new WorldRenderer();
            world.addObserver(renderer);

            controller = new FirstPersonCameraController(renderer.camera);
            Gdx.input.setInputProcessor(controller);
        }
    }

    @Override
    public void render(float delta) {
        update(delta);
        draw();
    }

    private void update(float delta){
        controller.update(delta);
    }

    private void draw(){
        BlockFaces.update();

        Gdx.gl.glClearColor(0.2f, 0.2f, 0.9f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderer.render();
    }
}
