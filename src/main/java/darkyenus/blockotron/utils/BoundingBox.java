package darkyenus.blockotron.utils;

import darkyenus.blockotron.world.Side;

/** Immutable bounding box used for blocks and entities. */
public final class BoundingBox {

	/** Simplest bounding box starting on origin with size of 1 in each of three directions. */
	public static final BoundingBox UNIT_BOUNDING_BOX = new BoundingBox(0, 0, 0, 1, 1, 1);

	/** Bounding box offset from the origin. */
	public final float offsetX, offsetY, offsetZ;
	/** Size from origin. Must be positive. */
	public final float sizeX, sizeY, sizeZ;

	public BoundingBox(float offsetX, float offsetY, float offsetZ, float sizeX, float sizeY, float sizeZ) {
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
	}

	/** @param oX origin in bounding box coordinate space
	 * @param dX direction (does not have to be normalized)
	 * @param result optional object holding additional intersection info, but only if intersection did happen (returned true)
	 * @return true if this bounding box intersects given ray */
	public boolean intersectsRay (float oX, float oY, float oZ, float dX, float dY, float dZ, BoundingBoxIntersectResult result) {
		/*
		 * Works by treating bounding box as three axis-aligned slices of space, perpendicular to each dimension. Then it calculates
		 * when the ray enters and leaves each of the slices. If the ray enters all slices before leaving any of it, then it
		 * intersects.
		 */

		final float rayXEnterDist, rayYEnterDist, rayZEnterDist;
		final float rayXLeaveDist, rayYLeaveDist, rayZLeaveDist;

		if(dX < 0){
			rayXEnterDist = (offsetX + sizeX) - (oX);
			rayXLeaveDist = (offsetX) - (oX);
		} else {
			rayXEnterDist = (offsetX) - (oX);
			rayXLeaveDist = (offsetX + sizeX) - (oX);
		}

		if(dY < 0){
			rayYEnterDist = (offsetY + sizeY) - (oY);
			rayYLeaveDist = (offsetY) - (oY);
		} else {
			rayYEnterDist = (offsetY) - (oY);
			rayYLeaveDist = (offsetY + sizeY) - (oY);
		}

		if(dZ < 0){
			rayZEnterDist = (offsetZ + sizeZ) - (oZ);
			rayZLeaveDist = (offsetZ) - (oZ);
		} else {
			rayZEnterDist = (offsetZ) - (oZ);
			rayZLeaveDist = (offsetZ + sizeZ) - (oZ);
		}

		// This creates infinities when direction is 0, but the algo works fine with them
		final float idX = 1f / dX;
		final float idY = 1f / dY;
		final float idZ = 1f / dZ;

		final float rayXEnterT = rayXEnterDist * idX, rayYEnterT = rayYEnterDist * idY, rayZEnterT = rayZEnterDist * idZ;
		final float rayXLeaveT = rayXLeaveDist * idX, rayYLeaveT = rayYLeaveDist * idY, rayZLeaveT = rayZLeaveDist * idZ;

		return resolveIntersect(rayXEnterT, rayXLeaveT, rayYEnterT, rayYLeaveT, rayZEnterT, rayZLeaveT, dX, dY, dZ, result);
	}

	private boolean resolveIntersect(float rayXEnterT, float rayXLeaveT, float rayYEnterT, float rayYLeaveT, float rayZEnterT, float rayZLeaveT, float dX, float dY, float dZ, BoundingBoxIntersectResult result){
		final float allIn = max(rayXEnterT, rayYEnterT, rayZEnterT);
		final float anyOut = min(rayXLeaveT, rayYLeaveT, rayZLeaveT);

		//Reject if allIn is negative, because it means that the ray points in the opposite direction
		//Return true if all are in before any is out
		if(allIn >= -0.001f && allIn < anyOut){
			if(result != null){
				result.t = allIn;

				if(allIn == rayXEnterT){
					result.side = dX < 0 ? Side.EAST : Side.WEST;//Or west!
				} else if(allIn == rayYEnterT){
					result.side = dY < 0 ? Side.NORTH : Side.SOUTH;//Or south
				} else {
					result.side = dZ < 0 ? Side.TOP : Side.BOTTOM;//Or bottom!
				}
			}
			return true;
		} else return false;
	}

	/** Similar to {@link #intersectsRay(float, float, float, float, float, float, BoundingBoxIntersectResult)}, but at the origin of ray is given bounding box.
	 * @param oX origin in bounding box coordinate space
	 * @param dX direction (does not have to be normalized)
	 * @param result optional object holding additional intersection info, but only if intersection did happen (returned true)
	 * @return true if this bounding box intersects given ray */
	public boolean intersectsBox (BoundingBox box, float oX, float oY, float oZ, float dX, float dY, float dZ, BoundingBoxIntersectResult result) {
		//See intersectRay for implementation comments
		final float rayXEnterDist, rayYEnterDist, rayZEnterDist;
		final float rayXLeaveDist, rayYLeaveDist, rayZLeaveDist;

		if(dX < 0){
			rayXEnterDist = (offsetX + sizeX) - (oX + box.offsetX);
			rayXLeaveDist = (offsetX) - (oX + box.offsetX + box.sizeX);
		} else {
			rayXEnterDist = (offsetX) - (oX + box.offsetX + box.sizeX);
			rayXLeaveDist = (offsetX + sizeX) - (oX + box.offsetX);
		}

		if(dY < 0){
			rayYEnterDist = (offsetY + sizeY) - (oY + box.offsetY);
			rayYLeaveDist = (offsetY) - (oY + box.offsetY + box.sizeY);
		} else {
			rayYEnterDist = (offsetY) - (oY + box.offsetY + box.sizeY);
			rayYLeaveDist = (offsetY + sizeY) - (oY + box.offsetY);
		}

		if(dZ < 0){
			rayZEnterDist = (offsetZ + sizeZ) - (oZ + box.offsetZ);
			rayZLeaveDist = (offsetZ) - (oZ + box.offsetZ + box.sizeZ);
		} else {
			rayZEnterDist = (offsetZ) - (oZ + box.offsetZ + box.sizeZ);
			rayZLeaveDist = (offsetZ + sizeZ) - (oZ + box.offsetZ);
		}

		// This creates infinities when direction is 0, but the algo works fine with them
		final float idX = 1f / dX;
		final float idY = 1f / dY;
		final float idZ = 1f / dZ;

		final float rayXEnterT = rayXEnterDist * idX, rayYEnterT = rayYEnterDist * idY, rayZEnterT = rayZEnterDist * idZ;
		final float rayXLeaveT = rayXLeaveDist * idX, rayYLeaveT = rayYLeaveDist * idY, rayZLeaveT = rayZLeaveDist * idZ;

		return resolveIntersect(rayXEnterT, rayXLeaveT, rayYEnterT, rayYLeaveT, rayZEnterT, rayZLeaveT, dX, dY, dZ, result);
	}

	private static float max (float f1, float f2, float f3) {
		if (f1 > f2) {
			if (f1 > f3) {
				return f1;
			} else {
				return f3;
			}
		} else {
			if (f2 > f3) {
				return f2;
			} else {
				return f3;
			}
		}
	}

	private static float min (float f1, float f2, float f3) {
		if (f1 < f2) {
			if (f1 < f3) {
				return f1;
			} else {
				return f3;
			}
		} else {
			if (f2 < f3) {
				return f2;
			} else {
				return f3;
			}
		}
	}

	/** Struct holding additional info from intersect methods. */
	public static final class BoundingBoxIntersectResult {
		private float t;
		private Side side;

		/** Origin + T * Direction = point of ray's impact */
		public float getT() {
			return t;
		}

		/** Side of impact */
		public Side getSide() {
			return side;
		}
	}

	private static void testRay(String name, BoundingBox target, BoundingBox box, float oX, float oY, float oZ, float dX, float dY, float dZ, boolean correctResult, Side correctSide){
		final BoundingBoxIntersectResult result = new BoundingBoxIntersectResult();
		final boolean collided;
		if(box == null){
			collided = target.intersectsRay(oX, oY, oZ, dX, dY, dZ, result);
		} else {
			collided = target.intersectsBox(box, oX, oY, oZ, dX, dY, dZ, result);
		}

		if(collided && correctResult && result.getSide() == correctSide){
			System.out.println(name+" collided to correct side");
			System.out.flush();
		}else if(!collided && !correctResult) {
			System.out.println(name+" correctly did not collide");
			System.out.flush();
		} else {
			final StringBuilder sb = new StringBuilder();
			sb.append(name).append("\n\tExpected: ");
			if(correctResult){
				sb.append("Collision with ").append(correctSide);
			} else {
				sb.append("No collision");
			}
			sb.append("\n\tGot: ");
			if(collided){
				sb.append("Collision with ").append(result.getSide());
			} else {
				sb.append("No collision");
			}
			System.err.println(sb);
			System.err.flush();
		}
	}

    public static void main(String[] args){
		testRay("X", UNIT_BOUNDING_BOX, null, 0.5f-1f, 0.5f, 0.5f, 1f, 0f, 0f, true, Side.WEST);

		testRay("1D", UNIT_BOUNDING_BOX, null, 0.5f, 0.5f, 5f, 0f, 0f, -1f, true, null);
        testRay("2D", UNIT_BOUNDING_BOX, null, 0.5f, 5f, 5f, 0f, -1f, -1f, true, null);
        testRay("3D", UNIT_BOUNDING_BOX, null, -1, -1, -1, 1, 1, 1, true, null);

        testRay("!1D", UNIT_BOUNDING_BOX, null, 0.5f, 0.5f, 5f, 0f, -1f, 0f, false, null);
        testRay("!2D", UNIT_BOUNDING_BOX, null, 0.5f, 5f, 5f, -1f, 0f, -1f, false, null);
        testRay("!3D", UNIT_BOUNDING_BOX, null, -1, -1, -1, 1, 0, 0, false, null);

        testRay("-1D", UNIT_BOUNDING_BOX, null, 0.5f, 0.5f, 5f, 0f, 0f, 1f, false, null);
        testRay("-2D", UNIT_BOUNDING_BOX, null, 0.5f, 5f, 5f, 0f, 1f, 1f, false, null);
        testRay("-3D", UNIT_BOUNDING_BOX, null, -1, -1, -1, -1, -1, -1, false, null);
    }
}