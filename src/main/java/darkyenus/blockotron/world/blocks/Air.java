package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.render.BlockMesh;
import darkyenus.blockotron.world.Block;

/**
 * Special block - absence of a block
 */
public class Air extends Block {

    public static final Air AIR = new Air();

    private Air() {
        super("air", true, false);
    }

    @Override
    public void render(int x, int y, int z, BlockMesh mesh) {}
}
