package darkyenus.blockotron.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import darkyenus.blockotron.world.Chunk;
import darkyenus.blockotron.world.World;

/**
 *
 */
public class WorldRenderer implements World.WorldObserver, RenderableProvider {

    private final PerspectiveCamera camera = new PerspectiveCamera();{camera.fieldOfView = 75;}
    private final Viewport viewport = new ExtendViewport(100f, 100f, camera);

    private final ModelBatch modelBatch;
    {
        final DefaultShader.Config config = new DefaultShader.Config();
        config.defaultCullFace = GL20.GL_BACK;
        config.numBones = 0;
        config.numDirectionalLights = 0;
        config.numPointLights = 0;
        config.numSpotLights = 0;
        final DefaultShaderProvider defaultShaderProvider = new DefaultShaderProvider(config);
        modelBatch = new ModelBatch(defaultShaderProvider);
    }

    private final LongMap<ChunkRenderable> renderableChunks = new LongMap<>();


    public void setCameraPosition(Vector3 newPosition, Vector3 newDirection){
        camera.position.set(newPosition);
        camera.direction.set(newDirection);
    }

    public void resize(int width, int height){
        viewport.setScreenSize(width, height);
    }


    @Override
    public void chunkLoaded(Chunk chunk) {
        final long key = World.chunkCoordKey(chunk.x, chunk.y);
        renderableChunks.put(key, new ChunkRenderable(chunk));
    }

    @Override
    public void chunkChanged(Chunk chunk, boolean staticBlocks) {
        if(staticBlocks) {
            renderableChunks.get(World.chunkCoordKey(chunk.x, chunk.y)).staticDirty = true;
        }
    }

    @Override
    public void chunkUnloaded(Chunk chunk) {
        renderableChunks.get(World.chunkCoordKey(chunk.x, chunk.y)).dispose();
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {

    }

    private static class ChunkRenderable {

        private final Chunk chunk;

        private boolean staticDirty = false;

        private ChunkRenderable(Chunk chunk) {
            this.chunk = chunk;
        }

        private void dispose(){

        }
    }
}
