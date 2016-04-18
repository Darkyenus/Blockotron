package darkyenus.blockotron.world.debug;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.BlockMesh;
import darkyenus.blockotron.world.Block;

/**
 *
 */
public class DebugBlock extends Block {

    public static final DebugBlock DEBUG_BLOCK = new DebugBlock();

    private static final BlockFaceTexture texture = BlockFaces.getBlockFace("debugBlock");

    private DebugBlock() {
        super("debugBlock", false, false);
    }

    @Override
    public void render(int x, int y, int z, byte occlusion, BlockMesh mesh) {
        mesh.createBlock(x, y, z, (byte)-1/*Side.top*/, texture);
    }
}
