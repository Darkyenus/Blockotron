package darkyenus.blockotron.world;

import java.util.Arrays;

/**
 *
 */
public final class Chunk {

    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 128;

    public final World world;
    public final int x, y;
    public boolean loaded = false;
    /** Blocks in 1d array for performance. X changes fastest, then Y then Z. Does not contain any null. */
    private final Block[] blocks = new Block[CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT];
    /** Indices correspond to blocks. For each block, contains which Sides are occluded (= on that side is an opaque block). */
    private final byte[] occlusion = new byte[blocks.length];

    //Iteration hints
    private int dynamicMin = 0, dynamicMax = -1, staticMin = 0, staticMax = -1;

    public Chunk(World world, int x, int y) {
        this.world = world;
        this.x = x;
        this.y = y;
        Arrays.fill(blocks, Block.AIR);
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

    public byte getOcclusionMask(int x, int y, int z){
        return occlusion[coord(x, y, z)];
    }

    public void setBlock(int x, int y, int z, Block block) {
        final Block[] blocks = this.blocks;
        final int coord = coord(x, y, z);
        final Block old = blocks[coord];
        blocks[coord] = block;

        //Update iterator hints
        if(block != Block.AIR) {
            if (block.isDynamic()) {
                expandDynamicIterationHint(coord);
            } else {
                expandStaticIterationHint(coord);
            }
        }

        final byte[] occlusion = this.occlusion;
        //Update neighbor occlusion masks
        if(block.isTransparent()){
            if(x > 0){
                occlusion[coord(x-1, y, z)] &= ~Side.east;
            }
            if(x < CHUNK_SIZE-1){
                occlusion[coord(x+1, y, z)] &= ~Side.west;
            }
            if(y > 0){
                occlusion[coord(x, y-1, z)] &= ~Side.north;
            }
            if(y < CHUNK_SIZE-1){
                occlusion[coord(x, y+1, z)] &= ~Side.south;
            }
            if(z > 0){
                occlusion[coord(x, y, z-1)] &= ~Side.top;
            }
            if(z < CHUNK_HEIGHT-1){
                occlusion[coord(x, y, z+1)] &= ~Side.bottom;
            }
        } else {
            if(x > 0){
                occlusion[coord(x-1, y, z)] |= Side.east;
            }
            if(x < CHUNK_SIZE-1){
                occlusion[coord(x+1, y, z)] |= Side.west;
            }
            if(y > 0){
                occlusion[coord(x, y-1, z)] |= Side.north;
            }
            if(y < CHUNK_SIZE-1){
                occlusion[coord(x, y+1, z)] |= Side.south;
            }
            if(z > 0){
                occlusion[coord(x, y, z-1)] |= Side.top;
            }
            if(z < CHUNK_HEIGHT-1){
                occlusion[coord(x, y, z+1)] |= Side.bottom;
            }
        }

        //Update own occlusion mask
        updateSelfOcclusion(x,y,z);

        if(loaded) {
            for (World.WorldObserver observer : world.observers()) {
                observer.chunkChanged(this, !old.isDynamic() || !block.isDynamic());
            }
        }
    }

    private void updateSelfOcclusion(int x, int y, int z){
        byte selfOcclusion = 0;
        if(x > 0){
            if(!blocks[coord(x - 1, y, z)].isTransparent()){
                selfOcclusion |= Side.west;
            }
        }
        if(x < CHUNK_SIZE-1){
            if(!blocks[coord(x + 1, y, z)].isTransparent()){
                selfOcclusion |= Side.east;
            }
        }
        if(y > 0){
            if(!blocks[coord(x, y - 1, z)].isTransparent()){
                selfOcclusion |= Side.south;
            }
        }
        if(y < CHUNK_SIZE-1){
            if(!blocks[coord(x, y + 1, z)].isTransparent()){
                selfOcclusion |= Side.north;
            }
        }
        if(z > 0){
            if(!blocks[coord(x, y, z - 1)].isTransparent()){
                selfOcclusion |= Side.bottom;
            }
        }
        if(z < CHUNK_HEIGHT-1){
            if(!blocks[coord(x, y, z + 1)].isTransparent()){
                selfOcclusion |= Side.top;
            }
        }
        occlusion[coord(x,y,z)] = selfOcclusion;
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
        final byte[] occlusion = this.occlusion;
        int i = staticMin;
        final int max = staticMax;
        no_blocks:{
            for (; i <= max; i++) {
                final Block block = blocks[i];
                if (block != Block.AIR && !block.isDynamic()) {
                    iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, occlusion[i], block);
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
            if (block != Block.AIR && !block.isDynamic()) {
                iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, occlusion[i], block);
                currentMax = i;
            }
        }
        staticMax = currentMax;
    }

    public void forEachDynamicNonAirBlock(BlockIterator iterator){
        final Block[] blocks = this.blocks;
        final byte[] occlusion = this.occlusion;
        int i = dynamicMin;
        final int max = dynamicMax;
        no_blocks:{
            for (; i <= max; i++) {
                final Block block = blocks[i];
                if (block != Block.AIR && block.isDynamic()) {
                    iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, occlusion[i], block);
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
            if (block != Block.AIR && block.isDynamic()) {
                iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, occlusion[i], block);
                currentMax = i;
            }
        }
        dynamicMax = currentMax;
    }

    public interface BlockIterator {
        void block(int cX, int cY, int cZ, byte occlusion, Block block);
    }
}
