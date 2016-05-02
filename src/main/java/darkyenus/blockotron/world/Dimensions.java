package darkyenus.blockotron.world;

import com.badlogic.gdx.math.MathUtils;

/**
 *
 */
public class Dimensions {
    /** Amount of blocks in the chunk in X, Y and Z dimensions. */
    public static final int CHUNK_SIZE = 16;

    public static final int CHUNK_SIZE_MASK = 0xF;

    public static final int CHUNK_SIZE_SHIFT = 4;

    /** Amount of Chunks on top of each other. */
    public static final int CHUNK_LAYERS = 16;

    /** Maps in-chunk coordinates to unique int key, which is its index in {@link Chunk#blocks}.
     * Correct result is guaranteed only when coordinates are valid:
     * x and y >= 0 && < CHUNK_SIZE, z >= 0 && < CHUNK_HEIGHT */
    public static int inChunkKey(int x, int y, int z) {
        //ZZZZ YYYY XXXX
        return ((z & CHUNK_SIZE_MASK) << 8) | ((y & CHUNK_SIZE_MASK) << 4) | (x & CHUNK_SIZE_MASK);
    }

    /** Get unique long-key under which the chunk at given chunk coordinates is saved at {@link World#chunks}. */
    public static long chunkKey(int chunkX, int chunkY, int chunkZ) {
        // 8 bits for Z, 28 bits for Y and 28 bits for X, in that order (Z is most significant)
        // Reserves 2^8 CHUNK_LAYERS, that is probably way too much, but other two dimensions don't need that much fidelity
        // Max addressing space with CHUNK_SIZE = 16
        // UP: 4km
        // X & Y: 4.2 * 10^9 m (~100x around the earth)
        return (chunkZ & 0xFFL) << 56 | (chunkY & 0xFFFF_FFFL) << 28 | chunkX & 0xFFFF_FFFL;
    }

    public static int chunkKeyToX(long chunkKey){
        return (int) (chunkKey & 0xFFFF_FFFL);
    }

    public static int chunkKeyToY(long chunkKey){
        return (int) ((chunkKey >>> 28) & 0xFFFF_FFFL);
    }

    public static int chunkKeyToZ(long chunkKey){
        return (int) ((chunkKey >>> 56) & 0xFFL);
    }

    /** Does chunkKey but on world coordinates which are Z clamped */
    public static long worldToClampedChunkKey(int worldX, int worldY, int worldZ){
        return chunkKey(worldX >> CHUNK_SIZE_SHIFT, worldY >> CHUNK_SIZE_SHIFT, MathUtils.clamp(worldZ >> CHUNK_SIZE_SHIFT, 0, CHUNK_LAYERS));
    }

    public static boolean worldCoordinatesInSameChunk(int x1, int y1, int z1, int x2, int y2, int z2){
        return (x1 >> CHUNK_SIZE_SHIFT) == (x2 >> CHUNK_SIZE_SHIFT)
                && (y1 >> CHUNK_SIZE_SHIFT) == (y2 >> CHUNK_SIZE_SHIFT)
                && (z1 >> CHUNK_SIZE_SHIFT) == (z2 >> CHUNK_SIZE_SHIFT);
    }

    /** Translate a world x, y or z coordinate into a chunk-coordinate.
     * @see Chunk#x */
    public static int worldToChunk(double xyz){
        return ((int) Math.floor(xyz)) >> CHUNK_SIZE_SHIFT;
    }

    /** Translate a world x, y or z coordinate into a chunk-coordinate.
     * @see Chunk#x */
    public static int worldToChunk(int xyz){
        return xyz >> CHUNK_SIZE_SHIFT;
    }

    /** Translate a world x, y or z coordinate into an in-chunk coordinate of its respective chunk.
     * Returned coordinate is always valid.
     * @see #worldToInChunk(int) */
    public static int worldToInChunk(double xyz){
        return ((int) Math.floor(xyz)) & CHUNK_SIZE_MASK;
    }

    /** Translate a world x, y or z coordinate into an in-chunk coordinate of its respective chunk.
     * Returned coordinate is always valid.
     * @see #worldToInChunk(double) */
    public static int worldToInChunk(int xyz) {
        return xyz & CHUNK_SIZE_MASK;
    }
}
