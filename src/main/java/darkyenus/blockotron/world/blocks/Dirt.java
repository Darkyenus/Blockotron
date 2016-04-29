package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.World;

/**
 *
 */
public class Dirt extends Block {

    public static final Dirt DIRT = new Dirt();

    private static final BlockFaceTexture TEXTURE = BlockFaces.getBlockFace("grass_bottom");

    private Dirt() {
        super("dirt", OCCLUDING | COLLIDABLE);
    }

    @Override
    public void render(World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion, RectangleMeshBatch batch) {
        batch.createBlock(drawX, drawY, drawZ, occlusion, TEXTURE);
    }
}
