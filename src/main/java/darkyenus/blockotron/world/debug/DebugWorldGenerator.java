package darkyenus.blockotron.world.debug;

import darkyenus.blockotron.world.Chunk;
import darkyenus.blockotron.world.blocks.Glass;
import darkyenus.blockotron.world.blocks.Grass;
import darkyenus.blockotron.world.generator.WorldGenerator;

/**
 *
 */
public class DebugWorldGenerator implements WorldGenerator {
    @Override
    public void generateChunk(Chunk chunk) {
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                for (int z = 14; z < 15; z++) {
                    chunk.setBlock(x, y, z, Grass.GRASS);
                }
            }
        }
        chunk.setBlock(0, 0, 15, DebugBlock.DEBUG_BLOCK);
        chunk.setBlock(0, 0, 16, DebugBlock.DEBUG_BLOCK);
        chunk.setBlock(0, 0, 18, DebugBlock.DEBUG_BLOCK);

        chunk.setBlock(5, 5, 15, Glass.GLASS);
        chunk.setBlock(5, 5, 16, Glass.GLASS);
        chunk.setBlock(5, 5, 17, Glass.GLASS);
        chunk.setBlock(5, 6, 15, Glass.GLASS);
        chunk.setBlock(5, 6, 16, Glass.GLASS);
        chunk.setBlock(5, 6, 17, Glass.GLASS);
        chunk.setBlock(6, 5, 15, Glass.GLASS);
        chunk.setBlock(6, 5, 16, Glass.GLASS);
        chunk.setBlock(6, 5, 17, Glass.GLASS);
        chunk.setBlock(7, 5, 15, Glass.GLASS);
        chunk.setBlock(7, 5, 16, Glass.GLASS);
        chunk.setBlock(7, 5, 17, Glass.GLASS);
    }
}
