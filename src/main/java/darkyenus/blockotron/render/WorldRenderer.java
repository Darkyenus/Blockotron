package darkyenus.blockotron.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.antag99.retinazer.EntitySystem;
import darkyenus.blockotron.world.*;

/**
 * WorldObserver which takes care of rendering the {@link World} and in-game HUD.
 */
public class WorldRenderer implements WorldObserver, RenderableProvider {

    public final PerspectiveCamera camera = new PerspectiveCamera();{
        camera.fieldOfView = 75;
        camera.up.set(0,0,1);
        camera.near = 0.1f;
        camera.far = 128f;//View distance
    }
    private final Viewport viewport = new ExtendViewport(100f, 100f, camera);

    private final ModelBatch modelBatch;
    {
        final DefaultShader.Config config = new DefaultShader.Config();
        config.defaultCullFace = GL20.GL_BACK;
        config.numBones = 0;
        config.numDirectionalLights = 2;
        config.numPointLights = 0;
        config.numSpotLights = 0;
        final DefaultShaderProvider defaultShaderProvider = new DefaultShaderProvider(config);
        modelBatch = new ModelBatch(defaultShaderProvider, new BiasedRenderableSorter());
    }

    private final Environment environment = new Environment();
    {
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.9f, 0.9f, 0.9f, 1f));
        environment.add(new DirectionalLight().set(100, 100, 100, 0, -1, 0));
    }

    private final WorldCursorOverlay cursorOverlay = new WorldCursorOverlay();

    private World world;
    private final LongMap<ChunkRenderable> renderableChunks = new LongMap<>();

    public void setCamera(Vector3 newPosition, Vector3 newDirection){
        camera.position.set(newPosition);
        camera.direction.set(newDirection);
    }

    public void render() {
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cursorOverlay.update(world, camera, 20f);

        modelBatch.begin(camera);
        {
            modelBatch.render(this, environment);
            modelBatch.render(cursorOverlay, environment);
        }
        modelBatch.end();
    }

    @Override
    public void initialize(World world) {
        this.world = world;
    }

    @Override
    public void chunkLoaded(Chunk chunk) {
        final long key = World.chunkCoordKey(chunk.x, chunk.y);
        renderableChunks.put(key, new ChunkRenderable(chunk));
    }

    @Override
    public void blockChanged(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ, Block from, Block to) {
        if(!from.dynamic || !to.dynamic) {
            renderableChunks.get(World.chunkCoordKey(chunk.x, chunk.y)).staticDirty = true;
        }
    }

    @Override
    public void blockOcclusionChanged(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ, byte from, byte to) {
        if(!chunk.getBlock(inChunkX, inChunkY, inChunkZ).dynamic){
            renderableChunks.get(World.chunkCoordKey(chunk.x, chunk.y)).staticDirty = true;
        }
    }

    @Override
    public void chunkUnloaded(Chunk chunk) {
        renderableChunks.get(World.chunkCoordKey(chunk.x, chunk.y)).dispose();
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        int cameraChunkX = MathUtils.round(camera.position.x / Chunk.CHUNK_SIZE);
        int cameraChunkY = MathUtils.round(camera.position.y / Chunk.CHUNK_SIZE);
        int viewDistanceChunks = MathUtils.ceilPositive(camera.far / Chunk.CHUNK_SIZE);
        final Frustum frustum = camera.frustum;

        for (int x = cameraChunkX - viewDistanceChunks; x < cameraChunkX + viewDistanceChunks; x++) {
            for (int y = cameraChunkY - viewDistanceChunks; y < cameraChunkY + viewDistanceChunks; y++) {
                final long key = World.chunkCoordKey(x, y);
                ChunkRenderable chunk = renderableChunks.get(key);
                if(chunk == null){
                    //continue;
                    world.getChunk(x, y);
                    chunk = renderableChunks.get(key);
                    if(chunk == null)continue;
                }
                if(!frustum.boundsInFrustum(chunk.boundingBox)) {
                    continue;
                }

                chunk.getRenderables(renderables, pool);
            }
        }
    }

    /** Takes care of building chunk block meshes. */
    private static class ChunkRenderable implements RenderableProvider {

        private final Chunk chunk;
        private final BoundingBox boundingBox = new BoundingBox();
        private final RectangleMeshBatch staticBatch, dynamicBatch;

        private boolean staticDirty = true;

        private ChunkRenderable(Chunk chunk) {
            this.chunk = chunk;
            boundingBox.min.set(chunk.x * Chunk.CHUNK_SIZE, chunk.y * Chunk.CHUNK_SIZE, 0);
            boundingBox.max.set(boundingBox.min).add(Chunk.CHUNK_SIZE, Chunk.CHUNK_SIZE, Chunk.CHUNK_HEIGHT);

            staticBatch = new RectangleMeshBatch(true, BlockFaces.opaqueMaterial, BlockFaces.transparentMaterial, 1 << 10);
            dynamicBatch = new RectangleMeshBatch(false, BlockFaces.opaqueMaterial, BlockFaces.transparentMaterial, 1 << 8);
            staticBatch.setWorldTranslation(chunk.x * Chunk.CHUNK_SIZE, chunk.y * Chunk.CHUNK_SIZE, 0);
            dynamicBatch.setWorldTranslation(chunk.x * Chunk.CHUNK_SIZE, chunk.y * Chunk.CHUNK_SIZE, 0);
        }

        private void dispose() {
            staticBatch.dispose();
            dynamicBatch.dispose();
        }

        private void rebuildStaticMesh(){
            final World world = chunk.world;
            final int worldX = chunk.x * Chunk.CHUNK_SIZE;
            final int worldY = chunk.y * Chunk.CHUNK_SIZE;

            final RectangleMeshBatch batch = this.staticBatch;
            batch.begin();
            chunk.forEachStaticNonAirBlock((cX, cY, cZ, occlusion, block) -> {
                if(block.transparent){
                    batch.beginTransparent(cX, cY, cZ);
                    block.render(world, worldX + cX, worldY + cY, cZ, cX, cY, cZ, occlusion, batch);
                    batch.endTransparent();
                } else {
                    block.render(world, worldX + cX, worldY + cY, cZ, cX, cY, cZ, occlusion, batch);
                }
            });
            batch.end();
        }

        private void rebuildDynamicMesh(){
            final World world = chunk.world;
            final int worldX = chunk.x * Chunk.CHUNK_SIZE;
            final int worldY = chunk.y * Chunk.CHUNK_SIZE;

            final RectangleMeshBatch batch = this.dynamicBatch;
            batch.begin();
            chunk.forEachDynamicNonAirBlock((cX, cY, cZ, occlusion, block) -> {
                if(block.transparent){
                    batch.beginTransparent(cX, cY, cZ);
                    block.render(world, worldX + cX, worldY + cY, cZ, cX, cY, cZ, occlusion, batch);
                    batch.endTransparent();
                } else {
                    block.render(world, worldX + cX, worldY + cY, cZ, cX, cY, cZ, occlusion, batch);
                }
            });
            batch.end();
        }

        @Override
        public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
            if(staticDirty){
                rebuildStaticMesh();
                staticDirty = false;
            }
            rebuildDynamicMesh();

            staticBatch.getRenderables(renderables, pool);
            dynamicBatch.getRenderables(renderables, pool);
        }
    }
}
