package darkyenus.blockotron.utils;

/**
 * Immutable bounding box used for blocks.
 */
public final class BlockBoundingBox {

    /** Simplest bounding box starting on origin with size of 1 in each of three directions. */
    public static final BlockBoundingBox UNIT_BOUNDING_BOX = new BlockBoundingBox(0,0,0,1,1,1);

    /** Bounding box offset from the origin. */
    public final float offsetX, offsetY, offsetZ;
    /** Size from origin. Must be positive. */
    public final float sizeX, sizeY, sizeZ;

    public BlockBoundingBox(float offsetX, float offsetY, float offsetZ, float sizeX, float sizeY, float sizeZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }
}
