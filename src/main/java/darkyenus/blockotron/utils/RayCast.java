package darkyenus.blockotron.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import darkyenus.blockotron.world.Side;

/**
 * Utility class for ray-casting, form of collision detection
 */
public final class RayCast {

    private static final BoundingBoxRayCastListener gridBoundingBoxRayCast_TMP = new BoundingBoxRayCastListener();

    public static float gridBoundingBoxRayCast(Vector3 origin, Vector3 direction, BoundingBox boundingBox, float maxDistance, RayCastListener listener){
        final float originOffX, originOffY, originOffZ;
        if(direction.x < 0){
            originOffX = -boundingBox.offsetX;
        } else {
            originOffX = boundingBox.offsetX + boundingBox.sizeX;
        }
        if(direction.y < 0){
            originOffY = -boundingBox.offsetY;
        } else {
            originOffY = boundingBox.offsetY + boundingBox.sizeY;
        }
        if(direction.z < 0){
            originOffZ = -boundingBox.offsetZ;
        } else {
            originOffZ = boundingBox.offsetZ + boundingBox.sizeZ;
        }
        //Origin + originOff(set) = position of the corner that will cross boundaries first

        final BoundingBoxRayCastListener boundingBoxListener = gridBoundingBoxRayCast_TMP;
        boundingBoxListener.reset(boundingBox, listener, originOffX, originOffY, originOffZ, origin, direction);

        return gridRayCast(origin.x + originOffX, origin.y + originOffY, origin.z + originOffZ, direction.x, direction.y, direction.z, maxDistance, listener);
    }

    /** 
     * @see #gridRayCast(float, float, float, float, float, float, float, RayCastListener) */
    public static float gridRayCast(Vector3 origin, Vector3 direction, float maxDistance, RayCastListener listener) {
        return gridRayCast(origin.x, origin.y, origin.z, direction.x, direction.y, direction.z, maxDistance, listener);
    }

    /**
     * Do a generic ray cast from origin in given direction.
     * Direction MUST be normalized.
     * On origin and then on each collision point, listener is called to determine if the casting should end.
     * Listener is guaranteed to be called at least once for origin point.
     * @see RayCastListener for listener parameters
     * @return total units travelled
     */
    public static float gridRayCast(
            float originX, float originY, float originZ,
            float directionX, float directionY, float directionZ,
            float maxDistance, RayCastListener listener) {
        // http://gamedev.stackexchange.com/questions/47362/cast-ray-to-select-block-in-voxel-game
        /* Algorithm derived from:
         * https://github.com/kpreid/cubes/blob/c5e61fa22cb7f9ba03cd9f22e5327d738ec93969/world.js#L307
         * Copyright 2011-2012 Kevin Reid under the terms of the MIT License <http://opensource.org/licenses/MIT>
         * Based on:
         * "A Fast Voxel Traversal Algorithm for Ray Tracing"
         * by John Amanatides and Andrew Woo, 1987
         * <http://www.cse.yorku.ca/~amana/research/grid.pdf>
         * <http://citeseer.ist.psu.edu/viewdoc/summary?doi=10.1.1.42.3443> */

        int stepX = (int)Math.signum(directionX);
        int stepY = (int)Math.signum(directionY);
        int stepZ = (int)Math.signum(directionZ);
        float tDeltaX = stepX / directionX;
        float tDeltaY = stepY / directionY;
        float tDeltaZ = stepZ / directionZ;
        float tMaxX = intBound(originX, directionX);
        float tMaxY = intBound(originY, directionY);
        float tMaxZ = intBound(originZ, directionZ);

        int x = MathUtils.floor(originX);
        int y = MathUtils.floor(originY);
        int z = MathUtils.floor(originZ);

        float t = 0;

        Side side = null;

        for (;;) {
            if(listener.found(x, y, z, t, side)){
                return t;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if(tMaxX > maxDistance) return maxDistance;
                    x += stepX;
                    t = tMaxX;
                    tMaxX += tDeltaX;
                    side = stepX < 0 ? Side.EAST : Side.WEST;
                } else {
                    if(tMaxZ > maxDistance) return maxDistance;
                    z += stepZ;
                    t = tMaxZ;
                    tMaxZ += tDeltaZ;
                    side = stepZ < 0 ? Side.TOP : Side.BOTTOM;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if(tMaxY > maxDistance) return maxDistance;
                    y += stepY;
                    t = tMaxY;
                    tMaxY += tDeltaY;
                    side = stepY < 0 ? Side.NORTH : Side.SOUTH;
                } else {
                    if(tMaxZ > maxDistance) return maxDistance;
                    z += stepZ;
                    t = tMaxZ;
                    tMaxZ += tDeltaZ;
                    side = stepZ < 0 ? Side.TOP : Side.BOTTOM;
                }
            }
        }
    }

    /** Utility method for block ray-casting.
     * Find the smallest positive t such that s+t*ds is an integer. */
    private static float intBound(float s, float ds) {
        if(ds < 0){
            s = -s;
            ds = -ds;
        }
        //Positive modulo: s % 1f
        s = s - MathUtils.floor(s);
        return (1f - s)/ds;
    }



    public interface RayCastListener {
        /** Called when rayCast crosses block boundary or to check origin.
         * @param x (+y, z) position of the block
         * @param t distance travelled by the ray to hit this
         * @param side of impact, collision surface normal. Null if this is the first block.  @return true to complete the search, false to continue searching */
        boolean found(int x, int y, int z, float t, Side side);
    }

    private static class BoundingBoxRayCastListener implements RayCastListener {

        private BoundingBox boundingBox;
        private RayCastListener baseListener;
        private float originOffX, originOffY, originOffZ;
        private final Vector3 origin = new Vector3(), direction = new Vector3(), tmp = new Vector3();

        public void reset(BoundingBox boundingBox, RayCastListener baseListener,float originOffX, float originOffY, float originOffZ, Vector3 origin, Vector3 direction) {
            this.boundingBox = boundingBox;
            this.baseListener = baseListener;
            this.originOffX = originOffX;
            this.originOffY = originOffY;
            this.originOffZ = originOffZ;
            this.origin.set(origin);
            this.direction.set(direction);
        }

        @Override
        public boolean found(int x, int y, int z, float t, Side side) {
            final Vector3 bBoxPosition = tmp.set(origin).mulAdd(direction, t);
            return false;
        }
    }


}
