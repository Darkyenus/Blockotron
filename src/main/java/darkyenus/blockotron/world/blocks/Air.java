package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.world.Block;

/**
 * Special block - absence of a block
 */
public class Air extends Block {

    public static final Air AIR = new Air();

    private Air() {
        super("air", true, false);
    }
}
