package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.World;

/**
 *
 */
public class Grass extends Block {

    public static final Grass GRASS = new Grass();

    private static final BlockFaceTexture TOP = BlockFaces.getBlockFace("grass_top");
    private static final BlockFaceTexture SIDE = BlockFaces.getBlockFace("grass_side");
    private static final BlockFaceTexture BOTTOM = BlockFaces.getBlockFace("grass_bottom");

    private Grass() {
        super("grass", false, true, false);
    }

    @Override
    public void render(World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion, RectangleMeshBatch batch) {
        if(GRASS.equals(world.getLoadedBlock(x, y, z + 1))) {
            batch.createBlock(drawX, drawY, drawZ, occlusion, TOP, BOTTOM, BOTTOM);
        } else {
            batch.createBlock(drawX, drawY, drawZ, occlusion, TOP, SIDE, BOTTOM);
        }
    }
}
