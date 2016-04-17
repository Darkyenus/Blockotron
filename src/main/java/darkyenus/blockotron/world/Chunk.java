package darkyenus.blockotron.world;

import darkyenus.blockotron.world.blocks.Air;

import java.util.Arrays;

/**
 *
 */
public final class Chunk {

    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 256;

    public final World world;
    public final int x, y;
    public boolean loaded = false;
    /** Blocks in 1d array for performance. X changes fastest, then Y then Z. Does not contain any null. */
    private final Block[] blocks = new Block[CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT];

    public Chunk(World world, int x, int y) {
        this.world = world;
        this.x = x;
        this.y = y;
        Arrays.fill(blocks, Air.AIR);
    }

    /** Maps chunk-local coordinates to unique int key. */
    private int coord(int x, int y, int z) {
        return x | y << 4 | z << 8;
    }

    /** Get block inside this chunk, using internal chunk coordinates.
     * Never returns null, undefined behavior if coordinates out of bounds. */
    public Block getBlock(int x, int y, int z) {
        return blocks[coord(x, y, z)];
    }

    public void setBlock(int x, int y, int z, Block block) {
        final Block[] blocks = this.blocks;
        final int coord = coord(x, y, z);
        final Block old = blocks[coord];
        blocks[coord] = block;

        if(loaded) {
            for (World.WorldObserver observer : world.observers()) {
                observer.chunkChanged(this, !old.dynamic || !block.dynamic);
            }
        }
    }

    public void forEachBlock(BlockIterator iterator){
        int i = 0;
        for (Block block : blocks) {
            iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, block);
            i++;
        }
    }

    public void forEachNonAirBlock(BlockIterator iterator){
        int i = 0;
        for (Block block : blocks) {
            if (block != Air.AIR) {
                iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, block);
            }
            i++;
        }
    }

    public interface BlockIterator {
        void block(int cX, int cY, int cZ, Block block);
    }
}
