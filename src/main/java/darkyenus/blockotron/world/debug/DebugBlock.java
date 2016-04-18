package darkyenus.blockotron.world.debug;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockMesh;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.Side;

/**
 *
 */
public class DebugBlock extends Block {

    public static final DebugBlock DEBUG_BLOCK = new DebugBlock();

    private static final BlockFaceTexture texture = new BlockFaceTexture("debugBlock", 0, 0, 1, 1);

    private DebugBlock() {
        super("debugBlock", false, false);
    }

    @Override
    public void render(int x, int y, int z, BlockMesh mesh) {
        mesh.createBlock(x, y, z, (byte)-1/*Side.top*/, texture);
    }
}
