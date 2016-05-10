package darkyenus.blockotron.world.generator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.StreamUtils;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.antag99.retinazer.EntitySystem;
import com.github.antag99.retinazer.util.Mask;
import darkyenus.blockotron.world.*;
import darkyenus.blockotron.world.blocks.Air;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static darkyenus.blockotron.world.Dimensions.*;

/**
 * Delegates its work to {@link ChunkGenerator} and {@link ChunkPopulator}s.
 * Takes care of keeping around chunks needed for generating, populating, saving and loading.
 */
public final class PersistentGeneratorChunkProvider implements ChunkProvider {

    private static final String LOG = "PersistentGeneratorChunkProvider";

    private World world;
    private final LongMap<ChunkColumn> chunkColumns = new LongMap<>();
    private final ChunkGenerator generator;
    private final ChunkPopulator[] populators;
    private final File worldBase;

    public PersistentGeneratorChunkProvider(File worldBase, ChunkGenerator generator, ChunkPopulator... populators) {
        this.worldBase = worldBase;
        this.generator = generator;
        this.populators = populators;
    }

    @Override
    public void initialize(World world) {
        this.world = world;
        loadWorld();
    }

    @Override
    public void shutdown() {
        saveWorld();
    }

    private ChunkColumn getGeneratedColumn(int x, int y) {
        final long key = chunkColumnKey(x, y);
        ChunkColumn column = chunkColumns.get(key);
        if (column != null) {
            return column;
        } else {
            column = new ChunkColumn(x, y);
            if(!loadColumn(column)){
                generateColumn(column);
            }
            chunkColumns.put(key, column);
            return column;
        }
    }

    private ChunkColumn getPopulatedColumn(int x, int y) {
        final ChunkColumn column = getGeneratedColumn(x, y);
        if (!column.populated) {
            column.populated = true;
            populateColumn(column);
        }
        return column;
    }

    @Override
    public Chunk borrowChunk(int x, int y, int z) {
        final ChunkColumn populatedColumn = getPopulatedColumn(x, y);
        return populatedColumn.borrowChunk(z);
    }

    //region Persistence

    private File getWorldFile(){
        return new File(worldBase, "world.bin");
    }

    private File getChunkColumnFile(ChunkColumn column){
        //noinspection ResultOfMethodCallIgnored
        worldBase.mkdirs();
        return new File(worldBase, "chunk." + column.chunkX + "." + column.chunkY + ".bin");
    }

    private final Output output_TMP = new Output(1<<10);
    private final Input input_TMP = new Input(1<<10);

    private boolean loadColumn(ChunkColumn column){
        final Input in = input_TMP;
        final File file = getChunkColumnFile(column);
        if(!file.canRead()) return false;
        try {
            in.setInputStream(new FileInputStream(file));

            column.loadColumn(in);
            return true;
        } catch (Exception e) {
            Gdx.app.error(LOG, "Failed to load chunk", e);
        } finally {
            StreamUtils.closeQuietly(in);
        }
        return false;
    }

    private boolean saveColumn(ChunkColumn column) {
        final Output out = output_TMP;
        try {
            final File file = getChunkColumnFile(column);
            out.clear();
            out.setOutputStream(new FileOutputStream(file, false));

            column.saveColumn(out);

            out.close();
            return true;
        } catch (Exception e) {
            Gdx.app.error(LOG, "Failed to save chunk", e);
        } finally {
            StreamUtils.closeQuietly(out);
        }
        return false;
    }

    private void loadWorld(){
        final Input in = input_TMP;
        final File file = getWorldFile();
        if(!file.canRead()) return;
        try {
            in.setInputStream(new FileInputStream(file));

            final Kryo kryo = world.kryo();
            for (EntitySystem system : world.entityEngine().getSystems()) {
                if(system instanceof SelfSerializable){
                    ((SelfSerializable) system).deserialize(in, kryo);
                }
            }

        } catch (Exception e) {
            Gdx.app.error(LOG, "Failed to load world", e);
        } finally {
            StreamUtils.closeQuietly(in);
        }
    }

    private void saveWorld(){
        final Output out = output_TMP;
        try {
            final File file = getWorldFile();
            out.clear();
            out.setOutputStream(new FileOutputStream(file, false));

            final Kryo kryo = world.kryo();
            for (EntitySystem system : world.entityEngine().getSystems()) {
                if(system instanceof SelfSerializable){
                    ((SelfSerializable) system).serialize(out, kryo);
                }
            }

            out.close();
        } catch (Exception e) {
            Gdx.app.error(LOG, "Failed to save world", e);
        } finally {
            StreamUtils.closeQuietly(out);
        }
    }
    //endregion

    private void generateColumn(ChunkColumn column) {
        generator.generateColumn(column);
    }

    private void populateColumn(ChunkColumn column) {
        for (ChunkPopulator populator : populators) {
            populator.populateColumn(column);
        }
        column.ready = true;
    }

    @Override
    public void returnChunk(Chunk chunk) {
        final ChunkColumn column = getPopulatedColumn(chunk.x, chunk.y);
        column.returnChunk(chunk.z);
        if (column.canBeSaved()) {
            //Save it and throw away
            if(saveColumn(column)){
                chunkColumns.remove(chunkColumnKey(column.chunkX, column.chunkY));
            }
        }
    }

    @Override
    public void update(float delta) {

    }

    public final class ChunkColumn {
        public final int chunkX, chunkY;
        private final Chunk[] chunks = new Chunk[CHUNK_LAYERS];
        private final Mask borrowedChunks = new Mask();
        private final Mask readyChunks = new Mask();
        private boolean populated = false;
        private boolean ready = false;

        private ChunkColumn(int chunkX, int chunkY) {
            this.chunkX = chunkX;
            this.chunkY = chunkY;
        }

        public Chunk borrowChunk(int chunkZ) {
            final Chunk chunk = getChunk(chunkZ);
            borrowedChunks.set(chunkZ);
            return chunk;
        }

        public void returnChunk(int chunkZ) {
            borrowedChunks.clear(chunkZ);
        }

        public boolean canBeSaved() {
            return populated && borrowedChunks.isEmpty();
        }

        private Chunk getChunk(int chunkZ) {
            Chunk chunk = chunks[chunkZ];
            if (chunk == null) {
                chunk = chunks[chunkZ] = new Chunk(world, chunkX, chunkY, chunkZ);
            }
            if (ready && !readyChunks.get(chunkZ)) {
                chunk.endPopulating(null);
                readyChunks.set(chunkZ);
            }
            return chunk;
        }

        public void setBlock(int inChunkX, int inChunkY, int inColumnZ, Block block) {
            final int inChunkZ = inColumnZ & CHUNK_SIZE_MASK;
            final int chunkZ = inColumnZ >> CHUNK_SIZE_SHIFT;

            if (chunkZ < 0 || chunkZ >= CHUNK_LAYERS) return;

            if (validInChunkCoordinate(inChunkX) && validInChunkCoordinate(inChunkY)) {
                getChunk(chunkZ).setLocalBlock(inChunkX, inChunkY, inChunkZ, block);
            } else if (populated) {
                getGeneratedColumn(chunkX + ((inChunkX & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT), chunkY + ((inChunkY & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT))
                        .getChunk(chunkZ)
                        .setLocalBlock(inChunkX & CHUNK_SIZE_MASK, inChunkY & CHUNK_SIZE_MASK, inChunkZ, block);
            } else {
                throw new IllegalArgumentException("Generator is not permitted to modify chunks outside of its column");
            }
        }

        public void setBlockIfAir(int inChunkX, int inChunkY, int inColumnZ, Block block) {
            final int inChunkZ = inColumnZ & CHUNK_SIZE_MASK;
            final int chunkZ = inColumnZ >> CHUNK_SIZE_SHIFT;

            if (chunkZ < 0 || chunkZ >= CHUNK_LAYERS) return;

            final Chunk chunk;

            if (validInChunkCoordinate(inChunkX) && validInChunkCoordinate(inChunkY)) {
                chunk = getChunk(chunkZ);
            } else if (populated) {
                chunk = getGeneratedColumn(chunkX + ((inChunkX & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT), chunkY + ((inChunkY & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT))
                        .getChunk(chunkZ);
                inChunkX &= CHUNK_SIZE_MASK;
                inChunkY &= CHUNK_SIZE_MASK;
            } else {
                throw new IllegalArgumentException("Generator is not permitted to modify chunks outside of its column");
            }

            final Block presentBlock = chunk.getLocalBlock(inChunkX, inChunkY, inChunkZ);
            if (presentBlock == Air.AIR) {
                chunk.setLocalBlock(inChunkX, inChunkY, inChunkZ, block);
            }
        }

        public void setBlockColumn(int inChunkX, int inChunkY, int inColumnZ, int height, Block block) {
            final int maxColumnZ = inColumnZ + Math.min(height, CHUNK_LAYERS << CHUNK_SIZE_SHIFT - inColumnZ);
            if (maxColumnZ <= inColumnZ) return;

            final ChunkColumn column;

            if (validInChunkCoordinate(inChunkX) && validInChunkCoordinate(inChunkY)) {
                column = this;
            } else if (populated) {
                column = getGeneratedColumn(chunkX + inChunkX >> CHUNK_SIZE_SHIFT, chunkY + inChunkY >> CHUNK_SIZE_SHIFT);
                inChunkX &= CHUNK_SIZE_MASK;
                inChunkY &= CHUNK_SIZE_MASK;
            } else {
                throw new IllegalArgumentException("Generator is not permitted to modify chunks outside of its column");
            }

            int inChunkZ = inColumnZ & CHUNK_SIZE_MASK;
            Chunk chunk = column.getChunk(inColumnZ >> CHUNK_SIZE_SHIFT);
            while (inColumnZ++ < maxColumnZ) {
                chunk.setLocalBlock(inChunkX, inChunkY, inChunkZ, block);
                inChunkZ++;
                if (inChunkZ == CHUNK_SIZE) {
                    inChunkZ = 0;
                    chunk = column.getChunk(inColumnZ >> CHUNK_SIZE_SHIFT);
                }
            }
        }

        public Block getBlock(int inColumnX, int inColumnY, int inColumnZ) {
            final int inChunkZ = inColumnZ & CHUNK_SIZE_MASK;
            final int chunkZ = inColumnZ >> CHUNK_SIZE_SHIFT;

            if (chunkZ < 0 || chunkZ >= CHUNK_LAYERS) return Air.AIR;

            if (validInChunkCoordinate(inColumnX) && validInChunkCoordinate(inColumnY)) {
                return getChunk(chunkZ).getLocalBlock(inColumnX, inColumnY, inChunkZ);
            } else if (populated) {
                return getGeneratedColumn(chunkX + ((inColumnX & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT), chunkY + ((inColumnY & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT))
                        .getChunk(chunkZ)
                        .getLocalBlock(inColumnX & CHUNK_SIZE_MASK, inColumnY & CHUNK_SIZE_MASK, inChunkZ);
            } else {
                throw new IllegalArgumentException("Generator is not permitted to read chunks outside of its column");
            }
        }

        /**
         * @return Z coordinate of first block that is not air when going from top on given coordinates.
         * -1 if all blocks in that column are air.
         */
        public int getTopNonAirBlockZ(int inChunkX, int inChunkY) {
            for (int chunkZ = CHUNK_LAYERS - 1; chunkZ >= 0; chunkZ--) {
                final Chunk chunk = chunks[chunkZ];
                if (chunk == null || chunk.isEmpty()) continue;
                for (int inChunkZ = CHUNK_SIZE - 1; inChunkZ >= 0; inChunkZ--) {
                    final Block block = chunk.getLocalBlock(inChunkX, inChunkY, inChunkZ);
                    if (block != Air.AIR) return (chunkZ << CHUNK_SIZE_SHIFT) + inChunkZ;
                }
            }
            return -1;
        }

        private static final byte SAVE_BIT_NO_BLOCKS = 1;
        private static final byte SAVE_BIT_NO_ENTITIES = 1 << 1;

        void saveColumn(Output output) {
            for (int chunkZ = 0; chunkZ < CHUNK_LAYERS; chunkZ++) {
                final Chunk chunk = chunks[chunkZ];
                boolean hasBlocks = true;
                boolean hasEntities = true;
                {
                    byte chunkMask = 0;
                    if (chunk == null) {
                        chunkMask |= SAVE_BIT_NO_BLOCKS | SAVE_BIT_NO_ENTITIES;
                        hasBlocks = false;
                        hasEntities = false;
                    } else {
                        if (chunk.isEmpty()) {
                            chunkMask |= SAVE_BIT_NO_BLOCKS;
                            hasBlocks = false;
                        }
                        if (chunk.getEntityStorage() == null) {
                            chunkMask |= SAVE_BIT_NO_ENTITIES;
                            hasEntities = false;
                        }
                    }
                    output.writeByte(chunkMask);
                }
                if (hasBlocks) {
                    for (Block block : chunk.blocks) {
                        output.writeInt(block.getRegistryID(), true);
                    }
                }
                if (hasEntities) {
                    EntityStorage.saveAndFreeStorage(chunk.getEntityStorage(), output);
                }
            }
        }

        void loadColumn(Input input) {
            for (int chunkZ = 0; chunkZ < CHUNK_LAYERS; chunkZ++) {
                final byte chunkMask = input.readByte();
                final boolean hasBlocks = (chunkMask & SAVE_BIT_NO_BLOCKS) == 0;
                final boolean hasEntities = (chunkMask & SAVE_BIT_NO_ENTITIES) == 0;
                if(hasBlocks || hasEntities){
                    final Chunk chunk = new Chunk(world, chunkX, chunkY, chunkZ);
                    if (hasBlocks) {
                        for (int i = 0; i < chunk.blocks.length; i++) {
                            final int blockID = input.readInt(true);
                            final int x = Dimensions.inChunkKeyToX(i);
                            final int y = Dimensions.inChunkKeyToY(i);
                            final int z = Dimensions.inChunkKeyToZ(i);
                            chunk.setLocalBlock(x, y, z, Registry.block(blockID));
                        }
                    }
                    final EntityStorage entityStorage;
                    if(hasEntities) {
                        entityStorage = EntityStorage.obtainAndLoadStorage(input);
                    } else {
                        entityStorage = null;
                    }
                    chunk.endPopulating(entityStorage);
                    readyChunks.set(chunkZ);
                    chunks[chunkZ] = chunk;
                }
            }
            populated = true;
            ready = true;
        }
    }

}
