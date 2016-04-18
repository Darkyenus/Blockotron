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
import darkyenus.blockotron.world.Chunk;
import darkyenus.blockotron.world.World;

/**
 *
 */
public class WorldRenderer implements World.WorldObserver, RenderableProvider {

    public final PerspectiveCamera camera = new PerspectiveCamera();{
        camera.fieldOfView = 75;
        camera.up.set(0,0,1);
        camera.near = 0.1f;
        camera.far = 128f;//View distance
        camera.position.set(0, 0, 30);
        camera.direction.set(1, 0, 0);
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

    public void render(){
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cursorOverlay.update(world, camera, 200f);

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

    private static class ChunkRenderable implements RenderableProvider {

        private final Chunk chunk;
        private final BoundingBox boundingBox = new BoundingBox();
        private final BlockMesh staticOpaque, staticTransparent, dynamicOpaque, dynamicTransparent;

        private boolean staticDirty = true;

        private ChunkRenderable(Chunk chunk) {
            this.chunk = chunk;
            boundingBox.min.set(chunk.x * Chunk.CHUNK_SIZE, chunk.y * Chunk.CHUNK_SIZE, 0);
            boundingBox.max.set(boundingBox.min).add(Chunk.CHUNK_SIZE, Chunk.CHUNK_SIZE, Chunk.CHUNK_HEIGHT);

            staticOpaque = new BlockMesh(true, BlockFaces.opaqueMaterial, 2 << 12);
            staticTransparent = new BlockMesh(true, BlockFaces.transparentMaterial, 2 << 8);
            dynamicOpaque = new BlockMesh(false, BlockFaces.opaqueMaterial, 2 << 8);
            dynamicTransparent = new BlockMesh(false, BlockFaces.transparentMaterial, 2 << 8);
        }

        private void dispose(){
            staticOpaque.dispose();
            staticTransparent.dispose();
            dynamicOpaque.dispose();
            dynamicTransparent.dispose();
        }

        private void rebuildStaticMesh(){
            final int xOff = chunk.x * Chunk.CHUNK_SIZE;
            final int yOff = chunk.y * Chunk.CHUNK_SIZE;
            final BlockMesh staticOpaque = this.staticOpaque;
            final BlockMesh staticTransparent = this.staticTransparent;
            staticOpaque.begin();
            staticTransparent.begin();
            int[] blocks = {0};
            chunk.forEachStaticNonAirBlock((cX, cY, cZ, occlusion, block) -> {
                block.render(xOff + cX, yOff + cY, cZ, occlusion, block.isTransparent() ? staticTransparent : staticOpaque);
                blocks[0]++;
            });
            staticOpaque.end();
            staticTransparent.end();
        }

        private void rebuildDynamicMesh(){
            final int xOff = chunk.x * Chunk.CHUNK_SIZE;
            final int yOff = chunk.y * Chunk.CHUNK_SIZE;
            final BlockMesh dynamicOpaque = this.dynamicOpaque;
            final BlockMesh dynamicTransparent = this.dynamicTransparent;
            dynamicOpaque.begin();
            dynamicTransparent.begin();
            chunk.forEachDynamicNonAirBlock((cX, cY, cZ, occlusion, block) -> {
                if (!block.isDynamic()) return;
                block.render(xOff + cX, yOff + cY, cZ, occlusion, block.isTransparent() ? dynamicTransparent : dynamicOpaque);
            });
            dynamicOpaque.end();
            dynamicTransparent.end();
        }

        @Override
        public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
            if(staticDirty){
                rebuildStaticMesh();
                staticDirty = false;
            }
            rebuildDynamicMesh();

            staticOpaque.getRenderables(renderables, pool);
            staticTransparent.getRenderables(renderables, pool);
            dynamicOpaque.getRenderables(renderables, pool);
            dynamicTransparent.getRenderables(renderables, pool);
        }
    }
}
