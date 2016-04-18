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

    //Iteration hints
    private int dynamicMin = 0, dynamicMax = -1, staticMin = 0, staticMax = -1;

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

        if(block != Air.AIR) {
            if (block.dynamic) {
                expandDynamicIterationHint(coord);
            } else {
                expandStaticIterationHint(coord);
            }
        }

        if(loaded) {
            for (World.WorldObserver observer : world.observers()) {
                observer.chunkChanged(this, !old.dynamic || !block.dynamic);
            }
        }
    }

    private void expandDynamicIterationHint(int key){
        dynamicMin = Math.min(dynamicMin, key);
        dynamicMax = Math.max(dynamicMax, key);
    }

    private void expandStaticIterationHint(int key){
        staticMin = Math.min(staticMin, key);
        staticMax = Math.max(staticMax, key);
    }

    public void forEachStaticNonAirBlock(BlockIterator iterator){
        final Block[] blocks = this.blocks;
        int i = staticMin;
        final int max = staticMax;
        no_blocks:{
            for (; i <= max; i++) {
                final Block block = blocks[i];
                if (block != Air.AIR && !block.dynamic) {
                    iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, block);
                    staticMin = i;
                    break no_blocks;
                }
            }
            // We iterated whole array and didn't find single static block.
            staticMin = 0;
            staticMax = -1;
            return;
        }
        int currentMax = i;
        i++;//Advance so we don't iterate twice over the same block
        for (; i <= max; i++) {
            final Block block = blocks[i];
            if (block != Air.AIR && !block.dynamic) {
                iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, block);
                currentMax = i;
            }
        }
        staticMax = currentMax;
    }

    public void forEachDynamicNonAirBlock(BlockIterator iterator){
        final Block[] blocks = this.blocks;
        int i = dynamicMin;
        final int max = dynamicMax;
        no_blocks:{
            for (; i <= max; i++) {
                final Block block = blocks[i];
                if (block != Air.AIR && block.dynamic) {
                    iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, block);
                    dynamicMin = i;
                    break no_blocks;
                }
            }
            // We iterated whole array and didn't find single dynamic block.
            dynamicMin = 0;
            dynamicMax = -1;
            return;
        }
        int currentMax = i;
        i++;//Advance so we don't iterate twice over the same block
        for (; i <= max; i++) {
            final Block block = blocks[i];
            if (block != Air.AIR && block.dynamic) {
                iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, block);
                currentMax = i;
            }
        }
        dynamicMax = currentMax;
    }

    public interface BlockIterator {
        void block(int cX, int cY, int cZ, Block block);
    }
}
