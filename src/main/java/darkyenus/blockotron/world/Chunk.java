package darkyenus.blockotron.world;

import darkyenus.blockotron.world.blocks.Air;

import java.util.Arrays;

/**
 * Vertical square slice of the {@link World}, holding blocks, entities and other world-grid related data, like lighting.
 * All coordinates in this class (except when noted otherwise) are in-chunk, that is,
 * they start on (0,0,0) (incl) and end on (CHUNK_SIZE, CHUNK_SIZE, CHUNK_HEIGHT) (excl), in this chunk.
 */
public final class Chunk {

    /** Amount of blocks in the chunk in X and Y dimensions. */
    public static final int CHUNK_SIZE = 16;
    /** Amount of blocks in the chunk in the Z dimension. */
    public static final int CHUNK_HEIGHT = 128;

    /** World this chunk is part of. World does not necessarily have to know about this chunk.
     * @see #loaded */
    public final World world;
    /** Position of this chunk in the {@link #world}.
     * Chunk coordinates, multiply by {@link #CHUNK_SIZE} to get the world coordinates of this chunk's origin. */
    public final int x, y;
    /** Set by {@link #world}. Loaded chunk may interact with other chunks, non-loaded must not. */
    public boolean loaded = false;
    /** Blocks of this chunk in 1D array for performance. X changes fastest, then Y then Z. Does not contain any nulls.
     * @see #coord(int, int, int) */
    private final Block[] blocks = new Block[CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT];
    /** Indexing identical to of {@link #blocks}.
     * For each block, contains which Sides are visible.
     * Byte holds flags from {@link Side} */
    private final byte[] occlusion = new byte[blocks.length];

    //Iteration hints
    private int dynamicMin = 0, dynamicMax = -1, staticMin = 0, staticMax = -1;

    public Chunk(World world, int x, int y) {
        this.world = world;
        this.x = x;
        this.y = y;
        Arrays.fill(blocks, Air.AIR);
    }

    /** Maps in-chunk coordinates to unique int key, which is its index in {@link #blocks}.
     * Correct result is guaranteed only when coordinates are valid:
     * x and y >= 0 && < CHUNK_SIZE, z >= 0 && < CHUNK_HEIGHT */
    private int coord(int x, int y, int z) {
        return x | y << 4 | z << 8;
    }

    /** Get block inside this chunk, using in-chunk coordinates.
     * Never returns null, undefined behavior if invalid coordinates. */
    public Block getBlock(int x, int y, int z) {
        return blocks[coord(x, y, z)];
    }

    /** Get the occlusion mask of given block.
     * Undefined behavior if out of bounds. */
    public byte getOcclusionMask(int x, int y, int z){
        return occlusion[coord(x, y, z)];
    }

    /** Set the block in given in-chunk coordinate.
     * Undefined behavior if invalid coordinates.
     * Updates the occlusion masks of neighbors (in the same chunk, for now).
     * If chunk is loaded, notifies world about the change. */
    public void setBlock(int x, int y, int z, Block block) {
        final Block[] blocks = this.blocks;
        final int coord = coord(x, y, z);
        final Block old = blocks[coord];
        if (old == block) return;
        blocks[coord] = block;

        //Update iterator hints
        if(block != Air.AIR) {
            if (block.dynamic) {
                dynamicMin = Math.min(dynamicMin, coord);
                dynamicMax = Math.max(dynamicMax, coord);
            } else {
                staticMin = Math.min(staticMin, coord);
                staticMax = Math.max(staticMax, coord);
            }
        }

        //Update own occlusion mask
        updateOcclusion(x,y,z);

        //Update neighbor occlusion masks
        if(x > 0){
            updateOcclusion(x-1, y, z);
        } else {
            updateOcclusionAtNeighbor(this.x - 1, y, CHUNK_SIZE-1, y, z);
        }
        if(x < CHUNK_SIZE-1){
            updateOcclusion(x+1, y, z);
        } else {
            updateOcclusionAtNeighbor(this.x + 1, y, 0, y, z);
        }
        if(y > 0){
            updateOcclusion(x, y-1, z);
        } else {
            updateOcclusionAtNeighbor(this.x, y - 1, x, CHUNK_SIZE-1, z);
        }
        if(y < CHUNK_SIZE-1){
            updateOcclusion(x, y+1, z);
        }else{
            updateOcclusionAtNeighbor(this.x, y + 1, x, 0, z);
        }
        if(z > 0){
            updateOcclusion(x, y, z-1);
        }
        if(z < CHUNK_HEIGHT-1){
            updateOcclusion(x, y, z+1);
        }

        if(loaded) {
            for (WorldObserver observer : world.observers()) {
                observer.chunkChanged(this, !old.dynamic || !block.dynamic);
            }
        }
    }

    /** Utility method for {@link #setBlock(int, int, int, Block)} for updating occlusion masks at neighboring chunks */
    private void updateOcclusionAtNeighbor(int chunkX, int chunkY, int inChunkX, int inChunkY, int inChunkZ){
        final Chunk loadedChunk = world.getLoadedChunk(chunkX, chunkY);
        if(loadedChunk == null)return;
        loadedChunk.updateOcclusion(inChunkX, inChunkY, inChunkZ);
    }

    /** Update occlusion at given in-chunk coordinates.
     * Rules have to consider transparency and kind of block when transparent:
     * Me -> Neighbor = Side visibility
     * Opaque -> Opaque =                       occluded
     * Opaque -> Transparent =                  VISIBLE
     * Transparent -> Opaque =                  occluded
     * Transparent -> Same transparent =        occluded
     * Transparent -> Different transparent =   VISIBLE
     */
    private void updateOcclusion(int x, int y, int z){
        final int coord = coord(x, y, z);
        final Block myself = blocks[coord];
        byte selfOcclusion = 0;
        if(isFaceVisible(myself, x-1, y, z)){
            selfOcclusion |= Side.west;
        }
        if(isFaceVisible(myself, x+1, y, z)){
            selfOcclusion |= Side.east;
        }
        if(isFaceVisible(myself, x, y-1, z)){
            selfOcclusion |= Side.south;
        }
        if(isFaceVisible(myself, x, y+1, z)){
            selfOcclusion |= Side.north;
        }
        if(isFaceVisible(myself, x, y, z-1)){
            selfOcclusion |= Side.bottom;
        }
        if(isFaceVisible(myself, x, y, z+1)){
            selfOcclusion |= Side.top;
        }
        occlusion[coord] = selfOcclusion;
    }

    /** @see #updateOcclusion(int, int, int) for rules */
    private boolean isFaceVisible(Block me, int nX, int nY, int nZ){
        final Block neighbor;
        if(nX == -1){
            final Chunk chunk = world.getLoadedChunk(this.x - 1, this.y);
            if(chunk == null) return true;
            neighbor = chunk.blocks[coord(CHUNK_SIZE-1, nY, nZ)];
        } else if(nX == CHUNK_SIZE){
            final Chunk chunk = world.getLoadedChunk(this.x + 1, this.y);
            if(chunk == null) return true;
            neighbor = chunk.blocks[coord(0, nY, nZ)];
        } else if(nY == -1){
            final Chunk chunk = world.getLoadedChunk(this.x, this.y - 1);
            if(chunk == null) return true;
            neighbor = chunk.blocks[coord(nX, CHUNK_SIZE-1, nZ)];
        } else if(nY == CHUNK_SIZE){
            final Chunk chunk = world.getLoadedChunk(this.x, this.y + 1);
            if(chunk == null) return true;
            neighbor = chunk.blocks[coord(nX, 0, nZ)];
        } else if(nZ == -1 || nZ == CHUNK_HEIGHT){
            return true;
        } else {
            neighbor = blocks[coord(nX, nY, nZ)];
        }

        if(me.transparent){
            return neighbor.transparent && !me.equals(neighbor);
        } else {
            return neighbor.transparent;
        }
    }

    /** Call the iterator with each static non-air block in the chunk, in order from in-chunk 0,0,0 up. */
    public void forEachStaticNonAirBlock(BlockIterator iterator){
        final Block[] blocks = this.blocks;
        final byte[] occlusion = this.occlusion;
        int i = staticMin;
        final int max = staticMax;
        no_blocks:{
            for (; i <= max; i++) {
                final Block block = blocks[i];
                if (block != Air.AIR && !block.dynamic) {
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
            if (block != Air.AIR && !block.dynamic) {
                iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, occlusion[i], block);
                currentMax = i;
            }
        }
        staticMax = currentMax;
    }

    /** Call the iterator with each dynamic non-air block in the chunk, in order from in-chunk 0,0,0 up. */
    public void forEachDynamicNonAirBlock(BlockIterator iterator){
        final Block[] blocks = this.blocks;
        final byte[] occlusion = this.occlusion;
        int i = dynamicMin;
        final int max = dynamicMax;
        no_blocks:{
            for (; i <= max; i++) {
                final Block block = blocks[i];
                if (block != Air.AIR && block.dynamic) {
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
            if (block != Air.AIR && block.dynamic) {
                iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, occlusion[i], block);
                currentMax = i;
            }
        }
        dynamicMax = currentMax;
    }

    public interface BlockIterator {
        /** @param cX (+ cY, cZ) in-chunk coordinates of returned block
         *  @param occlusion Occlusion mask of returned block
         *  @param block instance of returned block */
        void block(int cX, int cY, int cZ, byte occlusion, Block block);
    }
}
