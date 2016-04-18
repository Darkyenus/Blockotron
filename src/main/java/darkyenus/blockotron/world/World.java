package darkyenus.blockotron.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;

/**
 *
 */
public class World {

    private final LongMap<Chunk> chunks = new LongMap<>();
    private final ChunkProvider chunkProvider;
    private final Array<WorldObserver> observers = new Array<>(false, 8, WorldObserver.class);

    public World(ChunkProvider chunkProvider) {
        this.chunkProvider = chunkProvider;
        chunkProvider.initialize(this);
    }

    public static long chunkCoordKey(int x, int y){
        return ((long)x << 32) | ((long)y & 0xFFFF_FFFFL);
    }

    /** Return chunk at given chunk-coordinates. */
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

    public static int outerChunkCoordinate(float xy){
        float c = xy / Chunk.CHUNK_SIZE;
        return MathUtils.floor(c);
    }

    public static int innerChunkCoordinateXY(float xy){
        float c = xy % Chunk.CHUNK_SIZE;
        if(c < 0)c += Chunk.CHUNK_SIZE;
        return (int)c;
    }

    public static int innerChunkCoordinateZ(float z){
        return (int)z;
    }

    public Block getLoadedBlock(float x, float y, float z){
        final Chunk loadedChunk = getLoadedChunk(outerChunkCoordinate(x), outerChunkCoordinate(y));
        if(loadedChunk == null)return null;
        final int cx = innerChunkCoordinateXY(x);
        final int cy = innerChunkCoordinateXY(y);
        final int cz = innerChunkCoordinateZ(z);
        if(cz < 0 || cz >= Chunk.CHUNK_HEIGHT) return null;
        return loadedChunk.getBlock(cx, cy, cz);
    }

    private static float intbound(float s, float ds) {
        // Find the smallest positive t such that s+t*ds is an integer.
        if(ds < 0){
            s = -s;
            ds = -ds;
        }
        //Positive modulo: s % 1f
        s = s - MathUtils.floor(s);
        /*
        s = s % 1f;
        if(s < 0){
            s += 1f;
        }
        */
        return (1f - s)/ds;
    }

    private final RayCastResult getBlockOnRay_TMP = new RayCastResult();
	public RayCastResult getBlockOnRay (Vector3 origin, Vector3 direction, float maxDistance, BlockFilter filter) {
        // http://gamedev.stackexchange.com/questions/47362/cast-ray-to-select-block-in-voxel-game
        /* Algorithm derived from:
         * https://github.com/kpreid/cubes/blob/c5e61fa22cb7f9ba03cd9f22e5327d738ec93969/world.js#L307
         * Copyright 2011-2012 Kevin Reid under the terms of the MIT License <http://opensource.org/licenses/MIT>
         * Based on:
         * "A Fast Voxel Traversal Algorithm for Ray Tracing"
         * by John Amanatides and Andrew Woo, 1987
         * <http://www.cse.yorku.ca/~amana/research/grid.pdf>
         * <http://citeseer.ist.psu.edu/viewdoc/summary?doi=10.1.1.42.3443> */

		float stepX = Math.signum(direction.x);
		float stepY = Math.signum(direction.y);
		float stepZ = Math.signum(direction.z);
		float tDeltaX = stepX / direction.x;
		float tDeltaY = stepY / direction.y;
		float tDeltaZ = stepZ / direction.z;
		float tMaxX = intbound(origin.x, direction.x);
		float tMaxY = intbound(origin.y, direction.y);
		float tMaxZ = intbound(origin.z, direction.z);

		float x = MathUtils.floor(origin.x);
		float y = MathUtils.floor(origin.y);
		float z = MathUtils.floor(origin.z);

        Side side = null;

		for (;;) {
			final Block block = getLoadedBlock(x, y, z);
			if (block == null) return null;
			if (filter.accepts(block)) {
                final RayCastResult result = getBlockOnRay_TMP;
                result.block = block;
                result.x = MathUtils.floor(x);
                result.y = MathUtils.floor(y);
                result.z = MathUtils.floor(z);
                result.side = side;
                return result;
            }

			if (tMaxX < tMaxY) {
				if (tMaxX < tMaxZ) {
                    if(tMaxX > maxDistance) return null;
					x += stepX;
					tMaxX += tDeltaX;
                    side = stepX < 0 ? Side.EAST : Side.WEST;
				} else {
                    if(tMaxZ > maxDistance) return null;
					z += stepZ;
					tMaxZ += tDeltaZ;
                    side = stepZ < 0 ? Side.TOP : Side.BOTTOM;
				}
			} else {
				if (tMaxY < tMaxZ) {
                    if(tMaxY > maxDistance) return null;
					y += stepY;
					tMaxY += tDeltaY;
                    side = stepY < 0 ? Side.NORTH : Side.SOUTH;
				} else {
                    if(tMaxZ > maxDistance) return null;
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                    side = stepZ < 0 ? Side.TOP : Side.BOTTOM;
				}
			}
		}
	}

    public void addObserver(WorldObserver observer){
        observer.initialize(this);
        observers.add(observer);
    }

    public Iterable<WorldObserver> observers() {
        return observers;
    }

    public interface WorldObserver {
        void initialize(World world);
        void chunkLoaded(Chunk chunk);
        void chunkChanged(Chunk chunk, boolean staticBlocks);
        void chunkUnloaded(Chunk chunk);
    }

    public static final class RayCastResult {
        private Block block;
        private Side side;
        private int x,y,z;

        public Block getBlock() {
            return block;
        }

        public Side getSide() {
            return side;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }
    }
}
