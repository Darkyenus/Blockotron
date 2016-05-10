package darkyenus.blockotron.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import com.github.antag99.retinazer.Engine;
import darkyenus.blockotron.world.blocks.Air;
import darkyenus.blockotron.world.components.BlockPosition;

import java.util.Arrays;
import java.util.Random;

import static darkyenus.blockotron.world.Dimensions.*;

/**
 * Positioned cube in the {@link World}, holding blocks, entities and other world-grid related data, like lighting.
 * All coordinates in this class (except when noted otherwise) are in-chunk, that is,
 * they start on (0,0,0) (incl) and end on (CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE) (excl), in this chunk.
 */
public final class Chunk {

    /** World this chunk is part of. World does not necessarily have to know about this chunk.
     * @see #status */
    public final World world;
    /** Position of this chunk in the {@link #world}.
     * Chunk coordinates, multiply by {@link Dimensions#CHUNK_SIZE} to get the world coordinates of this chunk's origin. */
    public final int x, y, z;

    /** Blocks of this chunk in 1D array for performance. X changes fastest, then Y then Z. Does not contain any nulls.
     * <br/>WARNING: DIRECT USE EXPERT ONLY, DO NOT MODIFY
     * @see Dimensions#inChunkKey(int, int, int) */
    public final Block[] blocks = new Block[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];
    /** Indexing identical to of {@link #blocks}.
     * For each block, contains which Sides are visible.
     * Byte holds flags from {@link Side}
     * <br/>WARNING: DIRECT USE EXPERT ONLY, DO NOT MODIFY */
    public final byte[] occlusion = new byte[blocks.length];
    /** Indexing identical to of {@link #blocks}.
     * For each block, contains its light levels, packed: first (msb) 4 bits for block light, last (lsb) 4 bits for sky light. */
    final byte[] light = new byte[blocks.length];
    /** True if the light[] contains valid values, false if not yet computed */
    private boolean lightSettled = false;

    /** IDs of entities with {@link darkyenus.blockotron.world.components.Position} on this chunk */
    private final IntArray entities = new IntArray(false, 64);
    /** IDs of entities with {@link darkyenus.blockotron.world.components.BlockPosition} on this chunk
     * with key being their {@link Dimensions#inChunkKey(int, int, int)} of in-chunk coords. */
    private final IntIntMap blockEntities = new IntIntMap();

    /** Chunk is being generated or loaded. It may modify itself.
     * This is the default status and must be changed by endPopulating first. */
    private static final byte STATUS_POPULATING = 1;
    /** Chunk is inactive and may not be changed. */
    private static final byte STATUS_INACTIVE = 2;
    /** Chunk is actively used. */
    private static final byte STATUS_ACTIVE = 3;
    /** One of three above values. */
    private byte status = STATUS_POPULATING;

    /** Storage of entities (block and normal) on this chunk when not loaded.
     * Exists only if status is INACTIVE and block entities have been created. (INACTIVE + null = block entities not generated yet) */
    private EntityStorage entityStorage = null;

    //Iteration hints
    /** Amount of blocks in this chunk that are not air. */
    private int nonAirBlockCount = 0;

    public Chunk(World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        Arrays.fill(blocks, Air.AIR);
    }

	/** End populating this chunk and prepare it for its lifecycle.
	 * @param storage NON NULL if loaded from file and block entities have already been created, NULL if just generated and block
	 *           entities have not yet been created. */
	public void endPopulating (EntityStorage storage) {
		if (status != STATUS_POPULATING) throw new AssertionError("Chunk must be populating, is " + status);
		status = STATUS_INACTIVE;

        final Block[] blocks = this.blocks;

        int nonAirBlockCount = 0;
        for (int key = 0; key < blocks.length; key++) {
            Block block = blocks[key];
			if (block != Air.AIR) {
				nonAirBlockCount++;
				final int x = inChunkKeyToX(key);
				final int y = inChunkKeyToY(key);
				final int z = inChunkKeyToZ(key);
				updateLocalOcclusion(x, y, z);
			}
		}
		this.nonAirBlockCount = nonAirBlockCount;
		this.entityStorage = storage;
	}

	void makeActive () {
		if (status != STATUS_INACTIVE) throw new AssertionError("Chunk must be inactive, is " + status);

		final World world = this.world;
		final Engine entityEngine = world.entityEngine();

		// Deserialize or create entities
		if (entityStorage == null) {
			// Block entities never created
            final Block[] blocks = this.blocks;
			for (int key = 0; key < blocks.length; key++) {
				Block block = blocks[key];
				if (block.hasEntity()) {
					final int x = inChunkKeyToX(key);
					final int y = inChunkKeyToY(key);
					final int z = inChunkKeyToZ(key);

					final int entity = entityEngine.createEntity();
					final BlockPosition blockPosition = entityEngine.getMapper(BlockPosition.class).create(entity);
					blockPosition.x = this.x << CHUNK_SIZE_SHIFT + x;
					blockPosition.y = this.y << CHUNK_SIZE_SHIFT + y;
					blockPosition.z = this.z << CHUNK_SIZE_SHIFT + z;
					block.initializeEntity(world, entity);
				}
			}
		} else {
			entityStorage.loadEntities(world);
			EntityStorage.free(entityStorage);
			entityStorage = null;
		}

		this.status = STATUS_ACTIVE;
	}

	void makeInactive () {
		if (status != STATUS_ACTIVE) throw new AssertionError("Chunk must be active, is " + status);
		this.status = STATUS_INACTIVE;
		// Serialize entities
		final IntArray entities = this.entities;
		final IntIntMap blockEntities = this.blockEntities;

		final World world = this.world;
		final EntityStorage entityStorage = this.entityStorage = EntityStorage.obtain();

		// Standard entities
		entityStorage.storeEntities(world, entities.items, entities.size);
		entities.clear();

		// Block entities
		entityStorage.storeEntities(world, blockEntities.values());
		blockEntities.clear();
	}

    /** Get block inside this chunk, using in-chunk coordinates.
     * Never returns null, undefined behavior if invalid coordinates. */
    public Block getLocalBlock(int x, int y, int z) {
        return blocks[inChunkKey(x, y, z)];
    }

    /** Like {@link #getLocalBlock(int, int, int)}, but works even if the block is not from this chunk.
     * Searches only in loaded chunks and returns Air if the block is not loaded or is invalid. */
    public Block getBlock(int x, int y, int z) {
        if((x & CHUNK_SIZE_MASK) == x && (y & CHUNK_SIZE_MASK) == y && (z & CHUNK_SIZE_MASK) == z){
            return blocks[inChunkKey(x, y, z)];
        } else {
            final int xOff = x >> CHUNK_SIZE_SHIFT;
            final int yOff = y >> CHUNK_SIZE_SHIFT;
            final int zOff = z >> CHUNK_SIZE_SHIFT;
            final Chunk loadedChunk = world.getLoadedChunk(this.x + xOff, this.y + yOff, this.z + zOff);
            if(loadedChunk == null) return Air.AIR;
            else return loadedChunk.blocks[inChunkKey(x, y, z)];
        }
    }

    /** Get the occlusion mask of given block.
     * Undefined behavior if out of bounds.
     * @see #occlusion */
    public byte getOcclusionMask(int x, int y, int z) {
        return occlusion[inChunkKey(x, y, z)];
    }

    /** Set the block in given in-chunk coordinate.
     * Undefined behavior if invalid coordinates.
     * Updates the occlusion masks of neighbors (in the same chunk, for now).
     * If chunk is loaded, notifies world about the change. */
    public void setLocalBlock(int x, int y, int z, Block block) {
        if(status == STATUS_INACTIVE) throw new IllegalStateException("Do not modify inactive chunk");

        final Block[] blocks = this.blocks;
        final int coord = inChunkKey(x, y, z);
        final Block old = blocks[coord];
        if (old == block) return;
		blocks[coord] = block;

        if(status == STATUS_POPULATING) return;

		// Remove old block entity
		final Engine entityEngine = world.entityEngine();
		if (old.hasEntity()) {
            final int removed = blockEntities.get(coord, -1);
            assert removed != -1 : "Block " + block + " didn't have associated entity even though it should have.";
            entityEngine.destroyEntity(removed);
		}

		// Add new block entity
		if (block.hasEntity()) {
            final int entity = entityEngine.createEntity();
            final BlockPosition blockPosition = entityEngine.getMapper(BlockPosition.class).create(entity);
            blockPosition.x = this.x << CHUNK_SIZE_SHIFT + x;
            blockPosition.y = this.y << CHUNK_SIZE_SHIFT + y;
            blockPosition.z = this.z << CHUNK_SIZE_SHIFT + z;
            block.initializeEntity(world, entity);
		}

        //Update iterator hints
        if(old == Air.AIR) {
            nonAirBlockCount++;
        } else if(block == Air.AIR) {
            nonAirBlockCount--;
        }

        //Update own occlusion mask
        updateLocalOcclusion(x,y,z);

        //Update neighbor occlusion masks
        updateOcclusion(x-1, y, z);
        updateOcclusion(x+1, y, z);
        updateOcclusion(x, y-1, z);
        updateOcclusion(x, y+1, z);
        updateOcclusion(x, y, z-1);
        updateOcclusion(x, y, z+1);

        // Update light
        if(lightSettled){
            LightUpdater.updateChunk(this, x, y, z);
        }

        for (WorldObserver observer : world.observers()) {
            observer.blockChanged(this, x, y, z, old, block);
        }
    }

    /** Update occlusion at given in-chunk coordinates. Coordinates may be out of this chunk. */
    private void updateOcclusion(int x, int y, int z){
        if((x & CHUNK_SIZE_MASK) == x && (y & CHUNK_SIZE_MASK) == y && (z & CHUNK_SIZE_MASK) == z){
            updateLocalOcclusion(x, y, z);
        } else {
            final int xOff = x >> CHUNK_SIZE_SHIFT;
            final int yOff = y >> CHUNK_SIZE_SHIFT;
            final int zOff = z >> CHUNK_SIZE_SHIFT;
            final Chunk loadedChunk = world.getLoadedChunk(this.x + xOff, this.y + yOff, this.z + zOff);
            if(loadedChunk == null) return;
            loadedChunk.updateLocalOcclusion(x & CHUNK_SIZE_MASK, y & CHUNK_SIZE_MASK, z & CHUNK_SIZE_MASK);
        }
    }

    /** Update occlusion at given in-chunk coordinates.
     * Like {@link #updateOcclusion(int, int, int)} but without locality check. */
    private void updateLocalOcclusion(int x, int y, int z){
        final int coord = inChunkKey(x, y, z);
        final byte oldOcclusion = occlusion[coord];
        final Block myself = blocks[coord];
        byte newOcclusion = 0;
        if(isFaceVisible(myself, x-1, y, z)){
            newOcclusion |= Side.west;
        }
        if(isFaceVisible(myself, x+1, y, z)){
            newOcclusion |= Side.east;
        }
        if(isFaceVisible(myself, x, y-1, z)){
            newOcclusion |= Side.south;
        }
        if(isFaceVisible(myself, x, y+1, z)){
            newOcclusion |= Side.north;
        }
        if(isFaceVisible(myself, x, y, z-1)){
            newOcclusion |= Side.bottom;
        }
        if(isFaceVisible(myself, x, y, z+1)){
            newOcclusion |= Side.top;
        }
        if(newOcclusion != oldOcclusion){
            this.occlusion[coord] = newOcclusion;
            if(status == STATUS_ACTIVE){
                for (WorldObserver observer : world.observers()) {
                    observer.blockOcclusionChanged(this, x, y ,z, oldOcclusion, newOcclusion);
                }
            }
        }
    }

    /** Determine if my face is visible to the neighbor at given coordinates.
     * Rules have to consider transparency and kind of block when transparent:
     * Me -> Neighbor = Side visibility
     * Non-occluding -> * =                     VISIBLE
     * * -> Non-occluding =                     VISIBLE
     * Opaque -> Opaque =                       occluded
     * Opaque -> Transparent =                  VISIBLE
     * Transparent -> Opaque =                  occluded
     * Transparent -> Same transparent =        occluded
     * Transparent -> Different transparent =   VISIBLE */
    private boolean isFaceVisible(Block me, int nX, int nY, int nZ){
        if(!me.isOccluding()) return true;

        final Block neighbor = getBlock(nX, nY, nZ);

        if(!neighbor.isOccluding()) return true;

        if(me.isTransparent()){
            return neighbor.isTransparent() && !me.equals(neighbor);
        } else {
            return neighbor.isTransparent();
        }
    }

    /** Call the iterator with each non-air block in the chunk, in order from in-chunk 0,0,0 up. */
    public void forEachNonAirBlock(BlockIterator iterator) {
        final Block[] blocks = this.blocks;
        final byte[] occlusion = this.occlusion;

        int nonAirRemaining = nonAirBlockCount;
        for (int i = 0; i < blocks.length && nonAirRemaining > 0; i++) {
            final Block block = blocks[i];
            if (block != Air.AIR) {
                iterator.block(i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xFF, occlusion[i], block);
                nonAirRemaining--;
            }
        }
    }

    public byte getLight(int inChunkX, int inChunkY, int inChunkZ){
        if((inChunkX & CHUNK_SIZE_MASK) == inChunkX && (inChunkY & CHUNK_SIZE_MASK) == inChunkY && (inChunkZ & CHUNK_SIZE_MASK) == inChunkZ){
            return getLight()[inChunkKey(inChunkX, inChunkY, inChunkZ)];
        } else {
            final int xOff = inChunkX >> CHUNK_SIZE_SHIFT;
            final int yOff = inChunkY >> CHUNK_SIZE_SHIFT;
            final int zOff = inChunkZ >> CHUNK_SIZE_SHIFT;
            final Chunk loadedChunk = world.getLoadedChunk(this.x + xOff, this.y + yOff, this.z + zOff);
            if(loadedChunk == null) return -1;
            else return loadedChunk.getLight()[inChunkKey(inChunkX, inChunkY, inChunkZ)];
        }
    }

    public byte[] getLight() {
        final byte[] light = this.light;
        if (lightSettled || status == STATUS_POPULATING) {
            return light;
        } else {
            //Update light
            lightSettled = true;
            LightUpdater.updateChunk(this);
            return light;
        }
    }

    /** Register entity with this chunk */
    public void addEntity(int entity){
        entities.add(entity);
    }

    /** Un-register entity from this chunk
     * @return true if removed, false if not found */
    public boolean removeEntity(int entity){
        return entities.removeValue(entity);
    }

    void setBlockEntity(int entity, int inChunkKey){
        blockEntities.put(inChunkKey, entity);
    }

    void removeBlockEntity(int inChunkKey){
        blockEntities.remove(inChunkKey, -1);
    }

    /** Get the list of all non-block entities on this chunk. Do not modify, use for iteration only. */
    public IntArray entities() {
        return entities;
    }

    /** Get the list of all block entities on this chunk. Do not modify, use for iteration only. */
    public IntIntMap blockEntities() {
        return blockEntities;
    }

    /** Returns the entity storage, which exists only if the chunk is not loaded. */
    public EntityStorage getEntityStorage(){
        return entityStorage;
    }

    public boolean isEmpty() {
        return status != STATUS_POPULATING && nonAirBlockCount == 0;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    protected void tick() {
        final Random random = MathUtils.random;
        int offX = this.x << CHUNK_SIZE_SHIFT;
        int offY = this.y << CHUNK_SIZE_SHIFT;
        int offZ = this.z << CHUNK_SIZE_SHIFT;

        for (int i = 0; i < 3; i++) {
            final int x = random.nextInt(CHUNK_SIZE);
            final int y = random.nextInt(CHUNK_SIZE);
            final int z = random.nextInt(CHUNK_SIZE);
            blocks[inChunkKey(x,y,z)].randomTick(world, offX + x, offY + y, offZ + z);
        }
    }

    public interface BlockIterator {
        /** @param cX (+ cY, cZ) in-chunk coordinates of returned block
         *  @param occlusion Occlusion mask of returned block
         *  @param block instance of returned block */
        void block(int cX, int cY, int cZ, byte occlusion, Block block);
    }
}
