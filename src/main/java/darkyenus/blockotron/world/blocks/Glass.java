package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.World;

/**
 *
 */
public class Glass extends Block {

    public static final Glass GLASS = new Glass();

    private static final BlockFaceTexture TEXTURE = BlockFaces.getBlockFace("glass");

    private Glass() {
        super("glass", true, true, false);
    }

    @Override
    public void render(World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion, RectangleMeshBatch batch) {
        batch.createBlock(drawX, drawY, drawZ, occlusion, TEXTURE);
    }
}
