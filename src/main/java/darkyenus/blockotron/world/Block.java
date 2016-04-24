package darkyenus.blockotron.world;

import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.utils.BoundingBox;

/**
 *
 */
public abstract class Block {

    public final String id;
    /** Render: Does have transparent textures/sides? */
    public final boolean transparent;
    /** Render: Does this completely obscure faces of neighbors? */
    public final boolean occluding;
    /** Render: Does need to be rendered each frame? (Is it animated?) (static blocks are buffered) */
    public final boolean dynamic;
    /** Non-null collision bounding block of this block. */
    public final BoundingBox hitBox;

    protected Block(String id, boolean transparent, boolean occluding, boolean dynamic) {
        this.id = id;
        this.transparent = transparent;
        this.occluding = occluding;
        this.dynamic = dynamic;
        this.hitBox = BoundingBox.UNIT_BOUNDING_BOX;
    }

    protected Block(String id, boolean transparent, boolean occluding, boolean dynamic, BoundingBox hitBox) {
        this.id = id;
        this.transparent = transparent;
        this.occluding = occluding;
        this.dynamic = dynamic;
        this.hitBox = hitBox;
    }

    /** Draw the block in the world.
     * @param world in which the blocks is being rendered
     * @param x (y, z) of the block in the world
     * @param drawX (drawY, drawZ) to pass to the batch
     * @param occlusion mask of the block. Can be passed directly to the batch. See {@link Chunk#getOcclusionMask(int, int, int)}
     * @param batch to be used for drawing */
    public abstract void render(World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion, RectangleMeshBatch batch);
}
