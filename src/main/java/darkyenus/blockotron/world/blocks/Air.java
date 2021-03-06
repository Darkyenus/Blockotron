package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.World;

/**
 * Special block - absence of a block
 */
public class Air extends Block {

    public static final Air AIR = new Air();

    private Air() {
        super("air", TRANSPARENT | OCCLUDING);
    }

    @Override
    public void render(World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion, int skyLight, int blockLight, RectangleMeshBatch batch) {}
}
