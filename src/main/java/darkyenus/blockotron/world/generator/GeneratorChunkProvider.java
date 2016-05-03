package darkyenus.blockotron.world.generator;

import com.badlogic.gdx.utils.LongMap;
import com.github.antag99.retinazer.util.Mask;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.Chunk;
import darkyenus.blockotron.world.ChunkProvider;
import darkyenus.blockotron.world.World;

import static darkyenus.blockotron.world.Dimensions.*;

/**
 * Delegates its work to {@link ChunkGenerator} and {@link ChunkPopulator}s.
 * Takes care of keeping around chunks needed for generating, populating, saving and loading (TODO: in the future).
 */
public final class GeneratorChunkProvider implements ChunkProvider {

    private World world;
    private final LongMap<ChunkColumn> chunkColumns = new LongMap<>();
    private final ChunkGenerator generator;
    private final ChunkPopulator[] populators;

    public GeneratorChunkProvider(ChunkGenerator generator, ChunkPopulator... populators) {
        this.generator = generator;
        this.populators = populators;
    }

    @Override
    public void initialize(World world) {
        this.world = world;
    }

    private ChunkColumn getGeneratedColumn(int x, int y){
        final long key = chunkColumnKey(x, y);
        ChunkColumn column = chunkColumns.get(key);
        if(column != null) {
            return column;
        } else {
            column = new ChunkColumn(x, y);
            generateColumn(column);
            chunkColumns.put(key, column);
            return column;
        }
    }

    private ChunkColumn getPopulatedColumn(int x, int y){
        final ChunkColumn column = getGeneratedColumn(x, y);
        if(!column.populated){
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

    private void generateColumn(ChunkColumn column) {
        generator.generateColumn(column);
    }

    private void populateColumn(ChunkColumn column){
        for (ChunkPopulator populator : populators) {
            populator.populateColumn(column);
        }
    }

    @Override
    public void returnChunk(Chunk chunk) {
        getPopulatedColumn(chunk.x, chunk.y).returnChunk(chunk.z);
    }

    @Override
    public void update(float delta) {

    }

    public final class ChunkColumn {
        public final int chunkX, chunkY;
        private final Chunk[] chunks = new Chunk[CHUNK_LAYERS];
        private final Mask borrowedChunks = new Mask();
        private boolean populated = false;

        private ChunkColumn(int chunkX, int chunkY) {
            this.chunkX = chunkX;
            this.chunkY = chunkY;
        }

        public Chunk borrowChunk(int chunkZ){
            final Chunk chunk = chunks[chunkZ];
            if(chunk == null){
                return chunks[chunkZ] = new Chunk(world, chunkX, chunkY, chunkZ);
            }
            borrowedChunks.set(chunkZ);
            return chunk;
        }

        public void returnChunk(int chunkZ){
            borrowedChunks.clear(chunkZ);
        }

        private Chunk getChunk(int chunkZ) {
            final Chunk chunk = chunks[chunkZ];
            if(chunk == null){
                return chunks[chunkZ] = new Chunk(world, chunkX, chunkY, chunkZ);
            }
            return chunk;
        }

        public void setBlock(int inChunkX, int inChunkY, int inColumnZ, Block block){
            final int inChunkZ = inColumnZ & CHUNK_SIZE_MASK;
            final int chunkZ = inColumnZ >> CHUNK_SIZE_SHIFT;

            if(chunkZ < 0 || chunkZ >= CHUNK_LAYERS) return;

            if(validInChunkCoordinate(inChunkX) && validInChunkCoordinate(inChunkY)) {
                getChunk(chunkZ).setLocalBlock(inChunkX, inChunkY, inChunkZ, block);
            } else if(populated) {
                getGeneratedColumn(chunkX + inChunkX >> CHUNK_SIZE_SHIFT, chunkY + inChunkY >> CHUNK_SIZE_SHIFT)
                        .getChunk(chunkZ)
                        .setLocalBlock(inChunkX & CHUNK_SIZE_MASK, inChunkY & CHUNK_SIZE_MASK, inChunkZ, block);
            } else {
                throw new IllegalArgumentException("Generator is not permitted to modify chunks outside of its column");
            }
        }

        public void setBlockColumn(int inChunkX, int inChunkY, int inColumnZ, int height, Block block) {
            final int maxColumnZ = inColumnZ + Math.min(height, CHUNK_LAYERS << CHUNK_SIZE_SHIFT - inColumnZ);
            if(maxColumnZ <= inColumnZ) return;

            final ChunkColumn column;

            if(validInChunkCoordinate(inChunkX) && validInChunkCoordinate(inChunkY)) {
                column = this;
            } else if(populated) {
                column = getGeneratedColumn(chunkX + inChunkX >> CHUNK_SIZE_SHIFT, chunkY + inChunkY >> CHUNK_SIZE_SHIFT);
                inChunkX &= CHUNK_SIZE_MASK;
                inChunkY &= CHUNK_SIZE_MASK;
            } else {
                throw new IllegalArgumentException("Generator is not permitted to modify chunks outside of its column");
            }

            int inChunkZ = inColumnZ & CHUNK_SIZE_MASK;
            Chunk chunk = column.getChunk(inColumnZ >> CHUNK_SIZE_SHIFT);
            while(inColumnZ++ < maxColumnZ){
                chunk.setLocalBlock(inChunkX, inChunkY, inChunkZ, block);
                inChunkZ++;
                if(inChunkZ == CHUNK_SIZE){
                    inChunkZ = 0;
                    chunk = column.getChunk(inColumnZ >> CHUNK_SIZE_SHIFT);
                }
            }
        }

        public int getTopNonAirBlockZ(int inChunkX, int inChunkZ){
            return 30;//TODO
        }
    }

}
