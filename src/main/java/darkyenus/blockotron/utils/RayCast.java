package darkyenus.blockotron.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import darkyenus.blockotron.world.Side;

/**
 * Utility class for ray-casting, form of collision detection
 */
public final class RayCast {

	private static final com.badlogic.gdx.math.collision.BoundingBox gridBoundingBoxRayCast_BB_TMP = new com.badlogic.gdx.math.collision.BoundingBox();
	private static final com.badlogic.gdx.math.collision.BoundingBox gridBoundingBoxRayCast_BB2_TMP = new com.badlogic.gdx.math.collision.BoundingBox();
	private static final BoundingBox.BoundingBoxIntersectResult gridBoundingBoxRayCast_BBIR_TMP = new BoundingBox.BoundingBoxIntersectResult();

	/** Do a generic ray cast from origin in given direction using a bounding box. Direction MUST be normalized for meaningful T.
	 * Listener may be invoked with isValid for all grid broad-phase voxels and then once again for found result if any, with
	 * found().
	 * @see RayCastListener for listener parameters
	 * @return total units travelled (maxDistance if not found) */
	public static float gridBoundingBoxRayCast (Vector3 origin, Vector3 direction, BoundingBox sweepBox, float maxDistance,
		BoundingBoxRayCastListener listener) {
		final com.badlogic.gdx.math.collision.BoundingBox broadPhaseBox = gridBoundingBoxRayCast_BB_TMP;
		broadPhaseBox.min.set(sweepBox.offsetX, sweepBox.offsetY, sweepBox.offsetZ).add(origin);
		broadPhaseBox.max.set(broadPhaseBox.min).add(sweepBox.sizeX, sweepBox.sizeY, sweepBox.sizeZ);

		final com.badlogic.gdx.math.collision.BoundingBox finalBox = gridBoundingBoxRayCast_BB2_TMP;
		finalBox.min.set(broadPhaseBox.min).mulAdd(direction, maxDistance);
		finalBox.max.set(broadPhaseBox.max).mulAdd(direction, maxDistance);

		broadPhaseBox.ext(finalBox);
		final float EPSILON = 0.01f;
		final int fromX = MathUtils.floor(broadPhaseBox.min.x - EPSILON);
		final int fromY = MathUtils.floor(broadPhaseBox.min.y - EPSILON);
		final int fromZ = MathUtils.floor(broadPhaseBox.min.z - EPSILON);
		final int toX = MathUtils.floor(broadPhaseBox.max.x + EPSILON);
		final int toY = MathUtils.floor(broadPhaseBox.max.y + EPSILON);
		final int toZ = MathUtils.floor(broadPhaseBox.max.z + EPSILON);

		final BoundingBox.BoundingBoxIntersectResult internalResult = gridBoundingBoxRayCast_BBIR_TMP;
		float bestT = Float.POSITIVE_INFINITY;
		int bestX = 0, bestY = 0, bestZ = 0;
		Side bestSide = null;

		for (int x = fromX; x <= toX; x++) {
			for (int y = fromY; y <= toY; y++) {
				for (int z = fromZ; z <= toZ; z++) {
					if (listener.intersects(x, y, z, sweepBox, origin.x - x, origin.y - y, origin.z - z, direction.x, direction.y,
						direction.z, internalResult)) {
						final float t = internalResult.getT();
						if (t < bestT && t < maxDistance) {
							bestT = t;
							bestX = x;
							bestY = y;
							bestZ = z;
							bestSide = internalResult.getSide();
						}
					}
				}
			}
		}

		if (bestT != Float.POSITIVE_INFINITY) {
			listener.foundIntersected(bestX, bestY, bestZ, bestT, bestSide);
			return bestT;
		} else {
			return maxDistance;
		}
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

    public interface BoundingBoxRayCastListener {

        default boolean intersects(int x, int y, int z, BoundingBox sweepBox, float testOriginX, float testOriginY, float testOriginZ, float dirX, float dirY, float dirZ, BoundingBox.BoundingBoxIntersectResult intersectResult){
            return BoundingBox.UNIT_BOUNDING_BOX.intersectsBox(sweepBox, testOriginX, testOriginY, testOriginZ, dirX, dirY, dirZ, intersectResult);
        }

        void foundIntersected(int x, int y, int z, float t, Side side);
    }
}
