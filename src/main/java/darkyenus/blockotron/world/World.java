package darkyenus.blockotron.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import darkyenus.blockotron.utils.BlockBoundingBox;
import darkyenus.blockotron.utils.RayCast;

/**
 * Holds all data of single world, either directly or through {@link Chunk}s.
 * Also serves to route various information and behavior to its {@link #observers}.
 */
public final class World {

    private final LongMap<Chunk> chunks = new LongMap<>();
    private final ChunkProvider chunkProvider;
    private final Array<WorldObserver> observers = new Array<>(false, 8, WorldObserver.class);

    public World(ChunkProvider chunkProvider) {
        this.chunkProvider = chunkProvider;
        chunkProvider.initialize(this);
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

    /** Translate a world x or y coordinate into a chunk-coordinate.
     * @see Chunk#x */
    public static int chunkCoord(float xy){
        float c = xy / Chunk.CHUNK_SIZE;
        return MathUtils.floor(c);
    }

    /** Translate a world x or y coordinate into a chunk-coordinate.
     * @see Chunk#x */
    public static int chunkCoord(int xy){
        return Math.floorDiv(xy, Chunk.CHUNK_SIZE);
    }

    /** Translate a world x or y coordinate into an in-chunk coordinate of its respective chunk.
     * Returned coordinate is always valid.
     * @see #inChunkCoordZ(float) */
    public static int inChunkCoordXY(float xy){
        float c = xy % Chunk.CHUNK_SIZE;
        if(c < 0)c += Chunk.CHUNK_SIZE;
        return (int)c;
    }

    /** Translate a world x or y coordinate into an in-chunk coordinate of its respective chunk.
     * Returned coordinate is always valid.
     * @see #inChunkCoordZ(float) */
    public static int inChunkCoordXY(int xy){
        return Math.floorMod(xy, Chunk.CHUNK_SIZE);
    }

    /** Translate a world z coordinate into an in-chunk coordinate of its respective chunk.
     * Returned coordinate may not be valid if z is < 0 or >= {@link Chunk#CHUNK_HEIGHT}.
     * @see #inChunkCoordXY(float) */
    public static int inChunkCoordZ(float z){
        return (int)z;
    }

    /** @return block on given world coordinates, retrieves the chunk if not loaded */
    public Block getBlock(float x, float y, float z) {
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
        if(z < 0 || z >= Chunk.CHUNK_HEIGHT) return null;
        return chunk.getBlock(cx, cy, z);
    }

    /** @return block on given world coordinates, but only if it is already loaded, null otherwise. */
    public Block getLoadedBlock(float x, float y, float z){
        final Chunk loadedChunk = getLoadedChunk(chunkCoord(x), chunkCoord(y));
        if(loadedChunk == null)return null;
        final int cx = inChunkCoordXY(x);
        final int cy = inChunkCoordXY(y);
        final int cz = inChunkCoordZ(z);
        if(cz < 0 || cz >= Chunk.CHUNK_HEIGHT) return null;
        return loadedChunk.getBlock(cx, cy, cz);
    }

    /** @return block on given world coordinates, but only if it is already loaded, null otherwise. */
    public Block getLoadedBlock(int x, int y, int z){
        final Chunk loadedChunk = getLoadedChunk(chunkCoord(x), chunkCoord(y));
        if(loadedChunk == null)return null;
        final int cx = inChunkCoordXY(x);
        final int cy = inChunkCoordXY(y);
        if(z < 0 || z >= Chunk.CHUNK_HEIGHT) return null;
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

        private Block block;
        private Side side;
        private int x,y,z;

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
                BlockBoundingBox hitBox = block.hitBox;
                if(hitBox != BlockBoundingBox.UNIT_BOUNDING_BOX){
                    final Vector3 origin = this.origin;
                    final Vector3 direction = this.direction;

					if (!hitBox.intersectsRay(origin.x - x, origin.y - y, origin.z - z, direction.x, direction.y, direction.z)) {
						// Does not hit the custom hit box, keep searching
						return false;
					}
                }

                this.block = block;
                this.x = x;
                this.y = y;
                this.z = z;
                this.side = side;
                return true;//Done
            }
            return false;//Keep searching
        }
    }
}
