
package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.utils.BoundingBox;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.Side;
import darkyenus.blockotron.world.World;

/**
 *
 */
public class Flowerpot extends Block {

	public static final Flowerpot FLOWERPOT = new Flowerpot();

	private Flowerpot () {
		super("flowerpot", COLLIDABLE, new BoundingBox(0.25f, 0.25f, 0f, 0.5f, 0.5f, 0.5f));
	}

	private static final BlockFaceTexture TOP = BlockFaces.getBlockFace("flowerpot_top");
	private static final BlockFaceTexture SIDE = BlockFaces.getBlockFace("flowerpot_side");
	private static final BlockFaceTexture BOTTOM = BlockFaces.getBlockFace("flowerpot_bottom");

	@Override
	public void render (World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion, RectangleMeshBatch batch) {
		// All but bottom faces are always drawn
		batch.createBlockFace(drawX + 0.25f, drawY + 0.25f, drawZ, RectangleMeshBatch.TOP_FACE_OFFSETS, TOP, 0.5f, 0.5f, 0.5f);
		batch.createBlockFace(drawX + 0.25f, drawY + 0.25f, drawZ, RectangleMeshBatch.EAST_FACE_OFFSETS, SIDE, 0.5f, 0.5f, 0.5f);
		batch.createBlockFace(drawX + 0.25f, drawY + 0.25f, drawZ, RectangleMeshBatch.WEST_FACE_OFFSETS, SIDE, 0.5f, 0.5f, 0.5f);
		batch.createBlockFace(drawX + 0.25f, drawY + 0.25f, drawZ, RectangleMeshBatch.NORTH_FACE_OFFSETS, SIDE, 0.5f, 0.5f, 0.5f);
		batch.createBlockFace(drawX + 0.25f, drawY + 0.25f, drawZ, RectangleMeshBatch.SOUTH_FACE_OFFSETS, SIDE, 0.5f, 0.5f, 0.5f);
		if ((occlusion & Side.bottom) != 0) {
			batch.createBlockFace(drawX + 0.25f, drawY + 0.25f, drawZ, RectangleMeshBatch.BOTTOM_FACE_OFFSETS, BOTTOM,
                    0.5f, 0.5f, 0.5f);
		}
	}
}
