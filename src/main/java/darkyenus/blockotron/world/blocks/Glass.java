package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.world.Block;

/**
 *
 */
public class Glass extends Block {

    public static final Glass GLASS = new Glass();

    private static final BlockFaceTexture TEXTURE = BlockFaces.getBlockFace("glass");

    private Glass() {
        super("glass", true, false);
    }

    @Override
    public void render(int x, int y, int z, byte occlusion, RectangleMeshBatch mesh) {
        mesh.createBlock(x, y, z, occlusion, TEXTURE);
    }
}
