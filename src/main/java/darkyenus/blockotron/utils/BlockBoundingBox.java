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

    /**
     * @param oX origin in bounding box coordinate space
     * @param dX direction (does not have to be normalized)
     * @return true if this bounding box intersects given ray
     */
    public boolean intersectsRay(float oX, float oY, float oZ, float dX, float dY, float dZ){
        final float idX = 1f/dX;
        final float idY = 1f/dY;
        final float idZ = 1f/dZ;

        final float x1 = (offsetX - oX) * idX;
        final float x2 = (offsetX - oX + sizeX) * idX;
        final float xIn;// = Math.min(x1, x2);
        final float xOut;// = Math.max(x1, x2);
        if(x1 < x2){
            xIn = x1;
            xOut = x2;
        } else {
            xIn = x2;
            xOut = x1;
        }

        final float y1 = (offsetY - oY) * idY;
        final float y2 = (offsetY - oY + sizeY) * idY;
        final float yIn;// = Math.min(y1, y2);
        final float yOut;// = Math.max(y1, y2);
        if(y1 < y2){
            yIn = y1;
            yOut = y2;
        } else {
            yIn = y2;
            yOut = y1;
        }

        final float z1 = (offsetZ - oZ) * idZ;
        final float z2 = (offsetZ - oZ + sizeZ) * idZ;
        final float zIn;// = Math.min(z1, z2);
        final float zOut;// = Math.max(z1, z2);
        if(z1 < z2){
            zIn = z1;
            zOut = z2;
        } else {
            zIn = z2;
            zOut = z1;
        }

        final float maxIn = Math.max(xIn, Math.max(yIn, zIn));
        final float minOut = Math.min(xOut, Math.min(yOut, zOut));
        return maxIn >= -0.001f && maxIn < minOut;
    }

    /*public static void main(String[] args){
        final boolean shouldBeTrue1D = UNIT_BOUNDING_BOX.intersectsRay(0.5f, 0.5f, 5f, 0f, 0f, -1f);
        final boolean shouldBeTrue2D = UNIT_BOUNDING_BOX.intersectsRay(0.5f, 5f, 5f, 0f, -1f, -1f);
        final boolean shouldBeTrue3D = UNIT_BOUNDING_BOX.intersectsRay(-1, -1, -1, 1, 1, 1);
        System.out.println("1D "+(shouldBeTrue1D ? "passed" : "failed"));
        System.out.println("2D "+(shouldBeTrue2D ? "passed" : "failed"));
        System.out.println("3D "+(shouldBeTrue3D ? "passed" : "failed"));

        final boolean shouldBeFalse1D = UNIT_BOUNDING_BOX.intersectsRay(0.5f, 0.5f, 5f, 0f, -1f, 0f);
        final boolean shouldBeFalse2D = UNIT_BOUNDING_BOX.intersectsRay(0.5f, 5f, 5f, -1f, 0f, -1f);
        final boolean shouldBeFalse3D = UNIT_BOUNDING_BOX.intersectsRay(-1, -1, -1, 1, 0, 0);
        System.out.println("!1D "+(!shouldBeFalse1D ? "passed" : "failed"));
        System.out.println("!2D "+(!shouldBeFalse2D ? "passed" : "failed"));
        System.out.println("!3D "+(!shouldBeFalse3D ? "passed" : "failed"));

        final boolean shouldBeFalse1DRev = UNIT_BOUNDING_BOX.intersectsRay(0.5f, 0.5f, 5f, 0f, 0f, 1f);
        final boolean shouldBeFalse2DRev = UNIT_BOUNDING_BOX.intersectsRay(0.5f, 5f, 5f, 0f, 1f, 1f);
        final boolean shouldBeFalse3DRev = UNIT_BOUNDING_BOX.intersectsRay(-1, -1, -1, -1, -1, -1);
        System.out.println("-1D "+(!shouldBeFalse1DRev ? "passed" : "failed"));
        System.out.println("-2D "+(!shouldBeFalse2DRev ? "passed" : "failed"));
        System.out.println("-3D "+(!shouldBeFalse3DRev ? "passed" : "failed"));
    }*/
}
