package darkyenus.blockotron.world.generator.populators;

import com.badlogic.gdx.math.MathUtils;
import darkyenus.blockotron.world.Dimensions;
import darkyenus.blockotron.world.blocks.BasicBlocks;
import darkyenus.blockotron.world.generator.ChunkPopulator;
import darkyenus.blockotron.world.generator.GeneratorChunkProvider;

/**
 *
 */
public class TreePopulator implements ChunkPopulator {

    @Override
    public void populateColumn(GeneratorChunkProvider.ChunkColumn column) {
        final int amountOfTrees = MathUtils.random(0, 3);
        for (int i = 0; i < amountOfTrees; i++) {
            final int x = MathUtils.random.nextInt(Dimensions.CHUNK_SIZE), y = MathUtils.random.nextInt(Dimensions.CHUNK_SIZE);
            final int z = column.getTopNonAirBlockZ(x, y);
            spawnTree(column, x, y, z);
        }
    }

    private void spawnTree(GeneratorChunkProvider.ChunkColumn column, int x, int y, int z){
        int trunkHeight = MathUtils.random(4, 8);
        for (int oX = -2; oX <= 2; oX++) {
            for (int oY = -2; oY <= 2; oY++) {
                for (int oZ = trunkHeight-3; oZ < trunkHeight + 2; oZ++) {
                    column.setBlock(x + oX, y + oY, z + oZ, BasicBlocks.LEAVES);
                }
            }
        }
        for (int i = 0; i < trunkHeight; i++) {
            column.setBlock(x, y, z+i, BasicBlocks.WOOD_LOG);
        }
    }
}