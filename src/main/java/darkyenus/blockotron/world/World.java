package darkyenus.blockotron.world;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.LongMap;
import com.github.antag99.retinazer.*;
import darkyenus.blockotron.utils.BoundingBox;
import darkyenus.blockotron.utils.RayCast;
import darkyenus.blockotron.utils.SelectionWireResolver;
import darkyenus.blockotron.world.blocks.Air;
import darkyenus.blockotron.world.components.Position;

/**
 * Holds all data of single world, either directly or through {@link Chunk}s.
 * Also serves to route various information and behavior to its {@link #observers}.
 */
public final class World {

    private final LongMap<Chunk> chunks = new LongMap<>();
    private final ChunkProvider chunkProvider;
    private final Array<WorldObserver> observers = new Array<>(false, 8, WorldObserver.class);

    private final Engine entityEngine;

    /** Update delta will never be larger than this. This does mean that in extreme cases, game will slow down.
     * Main goal is not to slow down but to prevent huge amount of processing when game loop stops for a long amount of
     * time and systems can't catch up, for example while debugging. */
    private static final float MAX_UPDATE_DELTA = 0.5f;

    public World (ChunkProvider chunkProvider, EngineConfig engineConfig) {
        this.chunkProvider = chunkProvider;
        chunkProvider.initialize(this);
        engineConfig.addWireResolver(new SelectionWireResolver(this));// Auto wire World instances
        final Engine engine = new Engine(engineConfig);
        entityEngine = engine;

        final EntityListener chunkPositionMapper = new EntityListener() {

            private @Wire Mapper<Position> positionMapper;

            @Override
            public void inserted(EntitySet entities) {
                process(entities, true);
            }

            @Override
            public void removed(EntitySet entities) {
                process(entities, false);
            }

            private void process(EntitySet entitySet, boolean add){
                final IntArray indices = entitySet.getIndices();
                final int[] entities = indices.items;
                final int size = indices.size;

                for (int i = 0; i < size; i++) {
                    final int entity = entities[i];

                    final Position position = positionMapper.get(entity);

                    final int chunkX, chunkY;
                    chunkX = World.chunkCoord(position.x);
                    chunkY = World.chunkCoord(position.y);

                    final Chunk chunk = getChunk(chunkX, chunkY);
                    if (add) chunk.addEntity(entity);
                    else chunk.removeEntity(entity);
                }
            }
        };
        engine.wire(chunkPositionMapper);
        engine.getFamily(Family.with(Position.class)).addListener(chunkPositionMapper);
    }

    /** Get unique long-key under which the chunk at given chunk coordinates is saved at {@link #chunks}. */
    public static long chunkCoordKey(int x, int y){
        return ((long)x << 32) | ((long)y & 0xFFFF_FFFFL);
    }

    /** Return chunk at given chunk-coordinates. Chunk is retrieved from ChunkProvider if not loaded. */
    public Chunk getChunk(int x, int y){
        final long key = chunkCoordKey(x, y);
        final Chunk existing = chunks.get(key);
        if(existing == null){
            final Chunk newChunk = chunkProvider.getChunk(x, y);
            chunks.put(key, newChunk);
            newChunk.loaded = true;
            for (WorldObserver observer : observers()) {
                observer.chunkLoaded(newChunk);
            }
            return newChunk;
        } else {
            return existing;
        }
    }

    /** Return loaded chunk at given chunk-coordinates or null of not loaded. */
    public Chunk getLoadedChunk(int x, int y){
        return chunks.get(chunkCoordKey(x, y));
    }

    public Iterable<Chunk> getLoadedChunks(){
        return chunks.values();
    }

    /** Translate a world x or y coordinate into a chunk-coordinate.
     * @see Chunk#x */
    public static int chunkCoord(double xy){
        double c = xy / Chunk.CHUNK_SIZE;
        return (int)Math.floor(c);
    }

    /** Translate a world x or y coordinate into a chunk-coordinate.
     * @see Chunk#x */
    public static int chunkCoord(int xy){
        return Math.floorDiv(xy, Chunk.CHUNK_SIZE);
    }

    /** Translate a world x or y coordinate into an in-chunk coordinate of its respective chunk.
     * Returned coordinate is always valid.
     * @see #inChunkCoordZ(double) */
    public static int inChunkCoordXY(double xy){
        double c = xy % Chunk.CHUNK_SIZE;
        if(c < 0) c += Chunk.CHUNK_SIZE;
        return (int)c;
    }

    /** Translate a world x or y coordinate into an in-chunk coordinate of its respective chunk.
     * Returned coordinate is always valid.
     * @see #inChunkCoordZ(double) */
    public static int inChunkCoordXY(int xy){
        return Math.floorMod(xy, Chunk.CHUNK_SIZE);
    }

    /** Translate a world z coordinate into an in-chunk coordinate of its respective chunk.
     * Returned coordinate may not be valid if z is < 0 or >= {@link Chunk#CHUNK_HEIGHT}.
     * @see #inChunkCoordXY(double) */
    public static int inChunkCoordZ(double z){
        return (int)z;
    }

    /** @return block on given world coordinates, retrieves the chunk if not loaded */
    public Block getBlock(double x, double y, double z) {
        final Chunk chunk = getChunk(chunkCoord(x), chunkCoord(y));
        final int cx = inChunkCoordXY(x);
        final int cy = inChunkCoordXY(y);
        final int cz = inChunkCoordZ(z);
        if(cz < 0 || cz >= Chunk.CHUNK_HEIGHT) return null;
        return chunk.getBlock(cx, cy, cz);
    }

    /** @return block on given world coordinates, retrieves the chunk if not loaded */
    public Block getBlock(int x, int y, int z) {
        final Chunk chunk = getChunk(chunkCoord(x), chunkCoord(y));
        final int cx = inChunkCoordXY(x);
        final int cy = inChunkCoordXY(y);
        if(z < 0 || z >= Chunk.CHUNK_HEIGHT) return Air.AIR;
        return chunk.getBlock(cx, cy, z);
    }

    /** @return block on given world coordinates, but only if it is already loaded, null otherwise. */
    public Block getLoadedBlock(double x, double y, double z){
        final int cz = inChunkCoordZ(z);
        if(cz < 0 || cz >= Chunk.CHUNK_HEIGHT) return null;
        final Chunk loadedChunk = getLoadedChunk(chunkCoord(x), chunkCoord(y));
        if(loadedChunk == null) return null;
        final int cx = inChunkCoordXY(x);
        final int cy = inChunkCoordXY(y);
        return loadedChunk.getBlock(cx, cy, cz);
    }

    /** @return block on given world coordinates, but only if it is already loaded, null otherwise. */
    public Block getLoadedBlock(int x, int y, int z){
        if(z < 0 || z >= Chunk.CHUNK_HEIGHT) return null;
        final Chunk loadedChunk = getLoadedChunk(chunkCoord(x), chunkCoord(y));
        if(loadedChunk == null) return null;
        final int cx = inChunkCoordXY(x);
        final int cy = inChunkCoordXY(y);
        return loadedChunk.getBlock(cx, cy, z);
    }

    /** Set the block on given world coordinates to given block.
     * Does nothing if coordinates are invalid. */
    public void setBlock(int x, int y, int z, Block newBlock) {
        final Chunk chunk = getChunk(chunkCoord(x), chunkCoord(y));
        final int cx = inChunkCoordXY(x);
        final int cy = inChunkCoordXY(y);
        if(z < 0 || z >= Chunk.CHUNK_HEIGHT) return;
        chunk.setBlock(cx, cy, z, newBlock);
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

    public void update(float rawDelta){
        if(rawDelta > MAX_UPDATE_DELTA){
            rawDelta = MAX_UPDATE_DELTA;
        }
        entityEngine.update(rawDelta);
    }

    public Engine entityEngine(){
        return entityEngine;
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
}
