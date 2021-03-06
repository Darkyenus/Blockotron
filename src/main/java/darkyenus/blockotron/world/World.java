package darkyenus.blockotron.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.*;
import com.esotericsoftware.kryo.Kryo;
import com.github.antag99.retinazer.*;
import darkyenus.blockotron.utils.BoundingBox;
import darkyenus.blockotron.utils.EntityListenerAdapter;
import darkyenus.blockotron.utils.RayCast;
import darkyenus.blockotron.utils.SelectionWireResolver;
import darkyenus.blockotron.world.blocks.Air;
import darkyenus.blockotron.world.components.BlockPosition;
import darkyenus.blockotron.world.components.Position;
import darkyenus.blockotron.world.systems.ChunkLoadingSystem;
import darkyenus.blockotron.world.systems.PlayerSystem;
import org.objenesis.instantiator.ObjectInstantiator;

import static darkyenus.blockotron.world.Dimensions.*;

/**
 * Holds all data of single world, either directly or through {@link Chunk}s.
 * Also serves to route various information and behavior to its {@link #observers}.
 */
public final class World {

    private final LongMap<Chunk> chunks = new LongMap<>();
    private final ChunkProvider chunkProvider;
    private final Array<WorldObserver> observers = new Array<>(false, 8, WorldObserver.class);

    private final Engine entityEngine;
    private final Kryo kryo;

    private float tickCountdown = 0f;
    private boolean shutdown = false;

    /** Time between logic ticks */
    private static final float TICK_TIME = 1f/20f;

    /** Update delta will never be larger than this. This does mean that in extreme cases, game will slow down.
     * Main goal is not to slow down but to prevent huge amount of processing when game loop stops for a long amount of
     * time and systems can't catch up, for example while debugging. */
    private static final float MAX_UPDATE_DELTA = 1f/5f;

    public World (ChunkProvider chunkProvider, EngineConfig engineConfig) {
        this.chunkProvider = chunkProvider;
        engineConfig.addWireResolver(new SelectionWireResolver(this));// Auto wire World instances
        final Engine engine = new Engine(engineConfig);
        entityEngine = engine;

        final EntityListenerAdapter positionListener = new EntityListenerAdapter(){

            private @Wire Mapper<Position> positionMapper;

            @Override
            protected void inserted(int entity) {
                final Position position = positionMapper.get(entity);

                final Chunk chunk = loadChunk(position.getChunkX(), position.getChunkY(), position.getClampedChunkZ());
                if (chunk == null) {
                    Gdx.app.error("PositionListener","New entity "+entity+" failed to load chunk at "+position+" and is in limbo");
                    return;
                }

                chunk.addEntity(entity);
            }

            @Override
            protected void removed(int entity) {
                final Position position = positionMapper.get(entity);

                final Chunk chunk = loadChunk(position.getChunkX(), position.getChunkY(), position.getClampedChunkZ());
                if (chunk == null) {
                    Gdx.app.error("PositionListener","New entity "+entity+" failed to load chunk at "+position+" and is in limbo");
                    return;
                }

                chunk.removeEntity(entity);
            }
        };
        engine.wire(positionListener);
        engine.getFamily(Family.with(Position.class)).addListener(positionListener);

        final EntityListenerAdapter blockPositionListener = new EntityListenerAdapter(){

            private @Wire Mapper<BlockPosition> positionMapper;

            @Override
            protected void inserted(int entity) {
                final BlockPosition position = positionMapper.get(entity);

                final Chunk chunk = loadChunk(position.getChunkX(), position.getChunkY(), position.getChunkZ());
                if (chunk == null) {
                    Gdx.app.error("PositionListener","New entity "+entity+" failed to load chunk at "+position+" and is in limbo");
                    return;
                }

                chunk.setBlockEntity(entity, inChunkKey(position.getChunkX(), position.getChunkY(), position.getChunkZ()));
            }

            @Override
            protected void removed(int entity) {
                final BlockPosition position = positionMapper.get(entity);

                final Chunk chunk = loadChunk(position.getChunkX(), position.getChunkY(), position.getChunkZ());
                if (chunk == null) {
                    Gdx.app.error("PositionListener","New entity "+entity+" failed to load chunk at "+position+" and is in limbo");
                    return;
                }

                chunk.removeBlockEntity(inChunkKey(position.getChunkX(), position.getChunkY(), position.getChunkZ()));
            }
        };
        engine.wire(blockPositionListener);
        engine.getFamily(Family.with(BlockPosition.class)).addListener(blockPositionListener);

        kryo = Registry.createKryo();
        kryo.setInstantiatorStrategy(new ComponentInstantiationStrategy());

        chunkProvider.initialize(this);
    }

    /** Return chunk at given chunk-coordinates. Chunk is retrieved from ChunkProvider if not loaded.
     * May return null if the chunk is out of boundaries. */
    public Chunk loadChunk(int chunkX, int chunkY, int chunkZ){
        if(chunkZ < 0 || chunkZ >= CHUNK_LAYERS) return null;
        final long key = Dimensions.chunkKey(chunkX, chunkY, chunkZ);
        final Chunk existing = chunks.get(key);
        if(existing == null) {
            final Chunk newChunk = chunkProvider.borrowChunk(chunkX, chunkY, chunkZ);
            chunks.put(key, newChunk);
            newChunk.makeActive();
            for (WorldObserver observer : observers()) {
                observer.chunkLoaded(newChunk);
            }
            return newChunk;
        } else {
            return existing;
        }
    }

    public void unloadChunk (int chunkX, int chunkY, int chunkZ) {
        if(chunkZ < 0 || chunkZ >= CHUNK_LAYERS) return;
        final long key = Dimensions.chunkKey(chunkX, chunkY, chunkZ);
        final Chunk loaded = chunks.remove(key);
        if (loaded != null) {
            for (WorldObserver observer : observers()) {
                observer.chunkUnloaded(loaded);
            }
            loaded.makeInactive();
            chunkProvider.returnChunk(loaded);
        }
    }

    /** Return loaded chunk at given VALID chunk-coordinates or null of not loaded. */
    public Chunk getLoadedChunk(int chunkX, int chunkY, int chunkZ){
        if(chunkZ < 0 || chunkZ >= CHUNK_LAYERS) return null;
        return chunks.get(Dimensions.chunkKey(chunkX, chunkY, chunkZ));
    }

    public Chunk getLoadedChunk(long chunkKey) {
        return chunks.get(chunkKey);
    }

    /** @return block on given world coordinates, but only if it is already loaded, Air otherwise. */
    public Block getLoadedBlock(int x, int y, int z){
        final Chunk chunk = getLoadedChunk(worldToChunk(x), worldToChunk(y), worldToChunk(z));
        if(chunk == null) return Air.AIR;
        final int cx = worldToInChunk(x);
        final int cy = worldToInChunk(y);
        final int cz = worldToInChunk(z);
        if((cz & CHUNK_SIZE_MASK) != cz) return Air.AIR;
        return chunk.getLocalBlock(cx, cy, cz);
    }

    /** Set the block on given world coordinates to given block.
     * Does nothing if coordinates are invalid. */
    public void setBlock(int x, int y, int z, Block newBlock) {
        final Chunk chunk = loadChunk(worldToChunk(x), worldToChunk(y), worldToChunk(z));
        if(chunk == null) return;
        final int cx = worldToInChunk(x);
        final int cy = worldToInChunk(y);
        final int cz = worldToInChunk(z);
        if((cz & CHUNK_SIZE_MASK) != cz) return;
        chunk.setLocalBlock(cx, cy, cz, newBlock);
    }

    /** Instance of return value of getBlockOnRay, for GC reasons. */
    private final RayCastResult getBlockOnRay_TMP = new RayCastResult();
    /** Cast a ray from given origin (world coordinated) in given direction (must be normalized)
     * and return the first block hit which satisfies given filter. When search is not successful in maxDistance units,
     * returns null.
     *
     * When successful returns instance of RayCastResult. Null when for any reason unsuccessful.
     * NOTE: Returned instance is the same for each invocation (for GC reasons), so do not keep it around! */
	public RayCastResult getBlockOnRay (Vector3 origin, Vector3 direction, float maxDistance, BlockFilter filter) {
        final RayCastResult result = getBlockOnRay_TMP;
        result.reset(filter, origin, direction);
        RayCast.gridRayCast(origin, direction, maxDistance, result);
        if(result.block == null){
            return null;
        } else {
            return result;
        }
	}

    /** Instance of return value of getBlockOnSweepRay, for GC reasons. */
    private final SweepRayCastResult getBlockOnSweepRay_TMP = new SweepRayCastResult();
    /** Cast a sweep ray (ray with bounding box) from given origin (world coordinated) in given direction (must be normalized)
     * and return the first block hit which satisfies given filter. When search is not successful in maxDistance units,
     * returns null.
     *
     * When successful returns instance of SweepRayCastResult. Null when for any reason unsuccessful.
     * NOTE: Returned instance is the same for each invocation (for GC reasons), so do not keep it around! */
    public SweepRayCastResult getBlockOnSweepRay (BoundingBox sweepBox, Vector3 origin, Vector3 direction, float maxDistance, BlockFilter filter) {
        final SweepRayCastResult result = getBlockOnSweepRay_TMP;
        result.reset(filter);
        RayCast.gridBoundingBoxRayCast(origin, direction, sweepBox, maxDistance, result);
        if(result.block == null){
            return null;
        } else {
            return result;
        }
    }

    public void update(float rawDelta) {
        if(shutdown) throw new IllegalStateException("Illegal update, World is in shutdown");
        if(rawDelta > MAX_UPDATE_DELTA) {
            rawDelta = MAX_UPDATE_DELTA;
        }

        tickCountdown -= rawDelta;
        while(tickCountdown < 0) {
            tickCountdown += TICK_TIME;
            tick();
        }

        entityEngine.update(rawDelta);
    }

    private void tick(){
        //Block tick
        for (Chunk chunk : chunks.values()) {
            //chunk.tick();//TODO QQQ UNCOMMENT
        }
    }

    public Engine entityEngine(){
        return entityEngine;
    }

    public Kryo kryo(){
        final Kryo kryo = this.kryo;
        kryo.reset();
        return kryo;
    }

    /** Can't be removed.
     * @see WorldObserver */
    public void addObserver(WorldObserver observer){
        observer.initialize(this);
        observers.add(observer);
        if(chunks.size > 0){
            for (Chunk chunk : chunks.values()) {
                observer.chunkLoaded(chunk);
            }
        }
    }

    /** Get all observers of this world. Mostly used to notify observers. */
    public Iterable<WorldObserver> observers() {
        return observers;
    }

    public void shutdown() {
        shutdown = true;
        //Unload all players
        entityEngine.getSystem(PlayerSystem.class).unloadAllPlayers();
        entityEngine.update(0f);
        //Unload everything
        entityEngine.getSystem(ChunkLoadingSystem.class).shutdown();
        entityEngine.update(0f);//Flush entity unloads
        //Save remaining entities (entities without a place)
        //TODO
        //Shutdown
        chunkProvider.shutdown();
    }

    /** Result of block ray-casting methods. */
    public final class RayCastResult implements RayCast.RayCastListener {
        private BlockFilter filter;
        private final Vector3 origin = new Vector3();
        private final Vector3 direction = new Vector3();
        private final BoundingBox.BoundingBoxIntersectResult bBoxResult = new BoundingBox.BoundingBoxIntersectResult();

        private Block block;
        private Side side;
        private int x,y,z;
        private float t;

        private RayCastResult() {
        }

        /** Found block. Never null. */
        public Block getBlock() {
            return block;
        }

        /** Side through which the ray hit the block. MAY BE NULL if the ray started in this block. */
        public Side getSide() {
            return side;
        }

        /** World coordinate of found block. */
        public int getX() {
            return x;
        }

        /** World coordinate of found block. */
        public int getY() {
            return y;
        }

        /** World coordinate of found block. */
        public int getZ() {
            return z;
        }

        /** Distance travelled by the ray. */
        public float getT() {
            return t;
        }

        protected void reset(BlockFilter filter, Vector3 origin, Vector3 direction){
            this.block = null;
            this.filter = filter;
            this.origin.set(origin);
            this.direction.set(direction);
        }

        @Override
        public boolean found(int x, int y, int z, float t, Side side) {
            final Block block = getLoadedBlock(x, y, z);
            if (block == null) return true;//Were done
            if (filter.accepts(block)) {
                //Check hit box (unit hit box is always right)
                BoundingBox hitBox = block.hitBox;
                if(hitBox != BoundingBox.UNIT_BOUNDING_BOX){
                    final Vector3 origin = this.origin;
                    final Vector3 direction = this.direction;
                    final BoundingBox.BoundingBoxIntersectResult bBoxResult = this.bBoxResult;

                    if (!hitBox.intersectsRay(origin.x - x, origin.y - y, origin.z - z, direction.x, direction.y, direction.z, bBoxResult)) {
						// Does not hit the custom hit box, keep searching
						return false;
					}

                    side = bBoxResult.getSide();
                }

                this.block = block;
                this.x = x;
                this.y = y;
                this.z = z;
                this.t = t;
                this.side = side;
                return true;//Done
            }
            return false;//Keep searching
        }
    }

    public final class SweepRayCastResult implements RayCast.BoundingBoxRayCastListener {

        private BlockFilter filter;

        private Block block;
        private Side side;
        private int x,y,z;
        private float t;

        private SweepRayCastResult() {
        }

        /** Found block. Never null. */
        public Block getBlock() {
            return block;
        }

        /** Side through which the ray hit the block. MAY BE NULL if the ray started in this block. */
        public Side getSide() {
            return side;
        }

        /** World coordinate of found block. */
        public int getX() {
            return x;
        }

        /** World coordinate of found block. */
        public int getY() {
            return y;
        }

        /** World coordinate of found block. */
        public int getZ() {
            return z;
        }

        /** Distance travelled by the ray. */
        public float getT() {
            return t;
        }

        @Override
        public boolean intersects(int x, int y, int z, BoundingBox sweepBox, float testOriginX, float testOriginY, float testOriginZ, float dirX, float dirY, float dirZ, BoundingBox.BoundingBoxIntersectResult intersectResult) {
            final Block block = getLoadedBlock(x, y, z);
            //noinspection SimplifiableIfStatement
            if (block == null || !filter.accepts(block)) {
                return false;
            }
            return block.hitBox.intersectsBox(sweepBox, testOriginX, testOriginY, testOriginZ, dirX, dirY, dirZ, intersectResult);
        }

        @Override
        public void foundIntersected(int x, int y, int z, float t, Side side) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.t = t;
            this.side = side;
            this.block = getLoadedBlock(x, y, z);
        }

        public void reset(BlockFilter filter) {
            this.block = null;
            this.filter = filter;
        }
    }

    private class ComponentInstantiationStrategy extends Kryo.DefaultInstantiatorStrategy {
        @Override
        public ObjectInstantiator newInstantiatorOf(Class type) {
            if(Component.class.isAssignableFrom(type)){
                final Mapper mapper = entityEngine.getMapper(type);
                return mapper::createComponent;
            } else {
                return super.newInstantiatorOf(type);
            }
        }
    }
}
