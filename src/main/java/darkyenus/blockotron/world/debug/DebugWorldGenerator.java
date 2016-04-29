package darkyenus.blockotron.world.debug;

import com.badlogic.gdx.math.MathUtils;
import darkyenus.blockotron.world.Chunk;
import darkyenus.blockotron.world.blocks.Dirt;
import darkyenus.blockotron.world.blocks.Flowerpot;
import darkyenus.blockotron.world.blocks.Glass;
import darkyenus.blockotron.world.blocks.Grass;
import darkyenus.blockotron.world.generator.WorldGenerator;
import org.lwjgl.stb.STBPerlin;

/**
 *
 */
public class DebugWorldGenerator implements WorldGenerator {

    @Override
    public void generateChunk(Chunk chunk) {
        generateChunkPerlin(chunk);
    }


    public void generateChunkDebugPlain(Chunk chunk) {

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                for (int z = 0; z < 3; z++) {
                    chunk.setBlock(x, y, z, Grass.GRASS);
                }
            }
        }

        chunk.setBlock(0, 0, 3, DebugBlock.DEBUG_BLOCK);
        chunk.setBlock(0, 0, 4, DebugBlock.DEBUG_BLOCK);
        chunk.setBlock(0, 0, 6, DebugBlock.DEBUG_BLOCK);

        chunk.setBlock(5, 5, 3, Glass.GLASS);
        chunk.setBlock(5, 5, 4, Glass.GLASS);
        chunk.setBlock(5, 5, 5, Glass.GLASS);
        chunk.setBlock(5, 6, 3, Glass.GLASS);
        chunk.setBlock(5, 6, 4, Glass.GLASS);
        chunk.setBlock(5, 6, 5, Glass.GLASS);
        chunk.setBlock(6, 5, 3, Glass.GLASS);
        chunk.setBlock(6, 5, 4, Glass.GLASS);
        chunk.setBlock(6, 5, 5, Glass.GLASS);
        chunk.setBlock(7, 5, 3, Glass.GLASS);
        chunk.setBlock(7, 5, 4, Glass.GLASS);
        chunk.setBlock(7, 5, 5, Glass.GLASS);
        chunk.setBlock(7, 5, 6, Flowerpot.FLOWERPOT);
    }

    public void generateChunkPerlin(Chunk chunk) {
        float scale = 1f/40f;
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                final float rand = STBPerlin.stb_perlin_noise3((chunk.x * Chunk.CHUNK_SIZE + x) * scale, (chunk.y * Chunk.CHUNK_SIZE + y) * scale, 0, 0, 0, 0);
                final int height = MathUtils.round(20 + rand * 20);
                for (int z = 0; z < height; z++) {
                    chunk.setBlock(x,y,z, Dirt.DIRT);
                }
                chunk.setBlock(x,y,height, Grass.GRASS);

            }
        }
    }
}
