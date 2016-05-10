package darkyenus.blockotron.world.debug;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.World;

/**
 *
 */
public class DebugBlock extends Block {

    public static final DebugBlock DEBUG_BLOCK = new DebugBlock();

    private static final BlockFaceTexture texture = BlockFaces.getBlockFace("debugBlock");

    private DebugBlock() {
        super("debugBlock", OCCLUDING | COLLIDABLE);
    }

    @Override
    public void render(World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion, int skyLight, int blockLight, RectangleMeshBatch batch) {
        batch.createBlock(drawX, drawY, drawZ, (byte)~0, texture);
    }
}
