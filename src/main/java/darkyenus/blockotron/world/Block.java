
package darkyenus.blockotron.world;

import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.utils.BoundingBox;

/**
 *
 */
public abstract class Block {

	/** Render: Does have transparent textures/sides? */
	public static final byte TRANSPARENT = 1;
	/** Render: Does this completely obscure faces of neighbors? */
	public static final byte OCCLUDING = 1 << 1;
	/** Render: Does need to be rendered each frame? (Is it animated?) (static blocks are buffered) */
	public static final byte DYNAMIC = 1 << 2;
	/** Do entities collide with this block or is it walk-through? */
	public static final byte COLLIDABLE = 1 << 3;
	/** Can the player replace this block by placing block on this block? (For example for "tall grass" on which player most
	 * certainly does not want to build on) */
	public static final byte REPLACEABLE = 1 << 4;
    /** Should this block have associated entity?
     * @see #initializeEntity(World, int)  */
    public static final byte HAS_ENTITY = 1 << 5;

	/** Unique ID of this Block type. For base block is just simple name. For mod provided blocks, it has form of
	 * "mod_classifier.name". */
	public final String id;

	/** Contains bit flags with characteristics of this block.
	 * @see #TRANSPARENT
	 * @see #OCCLUDING
	 * @see #DYNAMIC */
	public final byte flags;

	/** Non-null collision bounding block of this block. */
	public final BoundingBox hitBox;

	protected Block (String id, int flags) {
		this.id = id;
		this.flags = (byte)flags;
		this.hitBox = BoundingBox.UNIT_BOUNDING_BOX;
	}

	protected Block (String id, int flags, BoundingBox hitBox) {
		this.id = id;
		this.flags = (byte)flags;
		this.hitBox = hitBox;
	}

	/** @see #TRANSPARENT */
	public final boolean isTransparent () {
		return (flags & TRANSPARENT) != 0;
	}

	/** @see #OCCLUDING */
	public final boolean isOccluding () {
		return (flags & OCCLUDING) != 0;
	}

	/** @see #DYNAMIC */
	public final boolean isDynamic () {
		return (flags & DYNAMIC) != 0;
	}

	/** @see #COLLIDABLE */
	public final boolean isCollidable () {
		return (flags & COLLIDABLE) != 0;
	}

	/** @see #REPLACEABLE */
	public final boolean isReplaceable () {
		return (flags & REPLACEABLE) != 0;
	}

    /** @see #HAS_ENTITY */
    public final boolean hasEntity() {
        return (flags & HAS_ENTITY) != 0;
    }

    /** Called when block of this type is added to a world.
     * The entity is already created and has BlockPosition component.
     * This method should add and setup necessary components. */
    protected void initializeEntity(World world, int entity){}

	/** Draw the block in the world.
	 * @param world in which the blocks is being rendered
	 * @param x (y, z) of the block in the world
	 * @param drawX (drawY, drawZ) to pass to the batch
	 * @param occlusion mask of the block. Can be passed directly to the batch. See {@link Chunk#getOcclusionMask(int, int, int)}
	 * @param batch to be used for drawing */
	public abstract void render (World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion,
		RectangleMeshBatch batch);
}
