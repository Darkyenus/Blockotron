package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.world.Block;

/**
 *
 */
public class Grass extends Block {

    public static final Grass GRASS = new Grass();

    private static final BlockFaceTexture TOP = BlockFaces.getBlockFace("grass_top");
    private static final BlockFaceTexture SIDE = BlockFaces.getBlockFace("grass_side");
    private static final BlockFaceTexture BOTTOM = BlockFaces.getBlockFace("grass_bottom");

    private Grass() {
        super("grass", false, false);
    }

    @Override
    public void render(int x, int y, int z, byte occlusion, RectangleMeshBatch mesh) {
        mesh.createBlock(x, y, z, occlusion, TOP, SIDE, BOTTOM);
    }
}
