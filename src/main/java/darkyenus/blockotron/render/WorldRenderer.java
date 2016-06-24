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
import darkyenus.blockotron.world.*;
import darkyenus.blockotron.world.blocks.Air;

import static darkyenus.blockotron.world.Dimensions.*;

/**
 * WorldObserver which takes care of rendering the {@link World} and in-game HUD.
 */
public class WorldRenderer implements WorldObserver, RenderableProvider {

    public int debug_chunksConsidered, debug_chunksRendered;

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
        final long key = chunkKey(chunk.x, chunk.y, chunk.z);
        renderableChunks.put(key, chunkRenderablePool.obtain().setup(chunk));
    }

    @Override
    public void blockChanged(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ, Block from, Block to) {
        makeBlockDirty(chunk, inChunkX, inChunkY, inChunkZ);
    }

    @Override
    public void blockOcclusionChanged(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ, byte from, byte to) {
        makeBlockDirty(chunk, inChunkX, inChunkY, inChunkZ);
    }

    private void makeChunkDirty(int chunkX, int chunkY, int chunkZ){
        final ChunkRenderable renderable = renderableChunks.get(chunkKey(chunkX, chunkY, chunkZ));
        if(renderable != null) renderable.dirty = true;
    }

    private void makeBlockDirty(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ){
        makeChunkDirty(chunk.x, chunk.y, chunk.z);
        if(inChunkX == 0){
            makeChunkDirty(chunk.x-1, chunk.y, chunk.z);
        } else if(inChunkX == CHUNK_SIZE-1){
            makeChunkDirty(chunk.x+1, chunk.y, chunk.z);
        }
        if(inChunkY == 0){
            makeChunkDirty(chunk.x, chunk.y-1, chunk.z);
        } else if(inChunkY == CHUNK_SIZE-1){
            makeChunkDirty(chunk.x, chunk.y+1, chunk.z);
        }
        if(inChunkZ == 0){
            makeChunkDirty(chunk.x, chunk.y, chunk.z-1);
        } else if(inChunkZ == CHUNK_SIZE-1){
            makeChunkDirty(chunk.x, chunk.y, chunk.z+1);
        }
    }

    @Override
    public void chunkUnloaded(Chunk chunk) {
        chunkRenderablePool.free(renderableChunks.remove(chunkKey(chunk.x, chunk.y, chunk.z)));
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        int cameraChunkX = MathUtils.round(camera.position.x / CHUNK_SIZE);
        int cameraChunkY = MathUtils.round(camera.position.y / CHUNK_SIZE);
        int cameraChunkZ = MathUtils.round(camera.position.z / CHUNK_SIZE);
        int viewDistanceChunks = MathUtils.ceilPositive(camera.far / CHUNK_SIZE);
        final Frustum frustum = camera.frustum;

        int total = 0, passed = 0;

        for (int x = cameraChunkX - viewDistanceChunks; x < cameraChunkX + viewDistanceChunks; x++) {
            for (int y = cameraChunkY - viewDistanceChunks; y < cameraChunkY + viewDistanceChunks; y++) {
                for (int z = cameraChunkZ - viewDistanceChunks; z < cameraChunkZ + viewDistanceChunks; z++) {
                    total++;
                    final long key = chunkKey(x, y, z);
                    ChunkRenderable chunk = renderableChunks.get(key);

                    if (chunk == null || !frustum.boundsInFrustum(chunk.boundingBox)) {
                        continue;
                    }

                    passed++;
                    chunk.getRenderables(renderables, pool);
                }
            }
        }

        debug_chunksConsidered = total;
        debug_chunksRendered = passed;
    }

    /** Takes care of building chunk block meshes.
     * POOLED! */
    private static class ChunkRenderable implements RenderableProvider {

        private Chunk chunk;
        private final BoundingBox boundingBox = new BoundingBox();
        private final RectangleMeshBatch blockBatch;

        private boolean dirty = true;

        private ChunkRenderable() {
            blockBatch = new RectangleMeshBatch(true, BlockFaces.opaqueMaterial, BlockFaces.transparentMaterial, 1 << 10);
        }

        private ChunkRenderable setup(Chunk chunk){
            this.chunk = chunk;
            boundingBox.min.set(chunk.x << CHUNK_SIZE_SHIFT, chunk.y << CHUNK_SIZE_SHIFT, chunk.z << CHUNK_SIZE_SHIFT);
            boundingBox.max.set(boundingBox.min).add(CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE);

            blockBatch.setWorldTranslation(chunk.x << CHUNK_SIZE_SHIFT, chunk.y << CHUNK_SIZE_SHIFT, chunk.z << CHUNK_SIZE_SHIFT);
            this.dirty = true;
            return this;
        }

        @Override
        public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
            final RectangleMeshBatch blockBatch = this.blockBatch;
            if(dirty){
                this.dirty = false;

                blockBatch.begin();
                final Chunk chunk = this.chunk;
                if(!chunk.isEmpty()){
                    final World world = chunk.world;
                    final int worldX = chunk.x << CHUNK_SIZE_SHIFT;
                    final int worldY = chunk.y << CHUNK_SIZE_SHIFT;
                    final int worldZ = chunk.z << CHUNK_SIZE_SHIFT;

                    blockBatch.beginTransparent(0, 0, 0);
                    blockBatch.pauseTransparent();

                    final Block[] blocks = chunk.blocks;
                    final byte[] occlusion = chunk.occlusion;

                    for (int i = 0; i < blocks.length; i++) {
                        final Block block = blocks[i];
                        if (block != Air.AIR) {
                            final int cX = i & 0xF;
                            final int cY = (i >> 4) & 0xF;
                            final int cZ = (i >> 8) & 0xF;

                            final byte eastLight = chunk.getLight(cX+1, cY, cZ);
                            final byte westLight = chunk.getLight(cX-1, cY, cZ);
                            final byte northLight = chunk.getLight(cX, cY+1, cZ);
                            final byte southLight = chunk.getLight(cX, cY-1, cZ);
                            final byte topLight = chunk.getLight(cX, cY, cZ+1);
                            final byte bottomLight = chunk.getLight(cX, cY, cZ-1);
                            final byte selfLight = chunk.getLight(cX, cY, cZ);

                            final int skyLight = (eastLight & 0xF) | ((westLight & 0xF) << 4)
                                    | ((northLight & 0xF) << 8) | ((southLight & 0xF) << 12)
                                    | ((topLight & 0xF) << 16) | ((bottomLight & 0xF) << 20)
                                    | ((selfLight & 0xF) << 24);
                            final int blockLight = ((eastLight & 0xF0) >> 4) | ((westLight & 0xF0))
                                    | ((northLight & 0xF0) << 4) | ((southLight & 0xF0) << 8)
                                    | ((topLight & 0xF0) << 12) | ((bottomLight & 0xF0) << 16)
                                    | ((selfLight & 0xF0) << 20);

                            if(block.isTransparent()) {
                                blockBatch.resumeTransparent();
                                block.render(world, worldX + cX, worldY + cY, worldZ + cZ, cX, cY, cZ, occlusion[i], skyLight, blockLight, blockBatch);
                                blockBatch.pauseTransparent();
                            } else {
                                block.render(world, worldX + cX, worldY + cY, worldZ + cZ, cX, cY, cZ, occlusion[i], skyLight, blockLight, blockBatch);
                            }
                        }
                    }

                    blockBatch.resumeTransparent();
                    blockBatch.endTransparent();
                }
                blockBatch.end();
            }

            blockBatch.getRenderables(renderables, pool);
        }
    }

    private final Pool<ChunkRenderable> chunkRenderablePool = new Pool<ChunkRenderable>() {
        @Override
        protected ChunkRenderable newObject() {
            return new ChunkRenderable();
        }

        @Override
        protected void reset(ChunkRenderable object) {
            object.chunk = null;//Prevent leak
        }
    };
}
