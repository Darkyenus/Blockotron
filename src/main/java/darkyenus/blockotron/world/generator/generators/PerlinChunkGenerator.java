package darkyenus.blockotron.world.generator.generators;

import com.badlogic.gdx.math.MathUtils;
import darkyenus.blockotron.world.Dimensions;
import darkyenus.blockotron.world.blocks.BasicBlocks;
import darkyenus.blockotron.world.generator.ChunkGenerator;
import darkyenus.blockotron.world.generator.PersistentGeneratorChunkProvider;
import org.lwjgl.stb.STBPerlin;

/**
 *
 */
public class PerlinChunkGenerator implements ChunkGenerator {

    @Override
    public void generateColumn(PersistentGeneratorChunkProvider.ChunkColumn column) {
        float scale = 1f/40f;
        final int xOff = column.chunkX << Dimensions.CHUNK_SIZE_SHIFT;
        final int yOff = column.chunkY << Dimensions.CHUNK_SIZE_SHIFT;
        for (int x = 0; x < Dimensions.CHUNK_SIZE; x++) {
            for (int y = 0; y < Dimensions.CHUNK_SIZE; y++) {
                final float rand = STBPerlin.stb_perlin_noise3((xOff + x) * scale, (yOff + y) * scale, 0, 0, 0, 0);
                final int height = MathUtils.round(20 + rand * 20);
                column.setBlockColumn(x, y, 0, height, BasicBlocks.DIRT);
                column.setBlock(x, y, height, BasicBlocks.GRASS);
            }
        }
    }
}
