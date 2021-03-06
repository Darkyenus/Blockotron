package darkyenus.blockotron.world.systems;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.github.antag99.retinazer.*;
import darkyenus.blockotron.utils.BoundingBox;
import darkyenus.blockotron.world.*;
import darkyenus.blockotron.world.components.Kinematic;
import darkyenus.blockotron.world.components.Position;

/** Moves kinematic entities, colliding with blocks, but not with other entities. */
@SkipWire
public class KinematicSystem extends EntityProcessorSystem {

	private @Wire World world;
	private @Wire Mapper<Position> positionMapper;
	private @Wire Mapper<Kinematic> kinematicMapper;

	private static final Vector3 gravity = new Vector3(0, 0, -14f);

	public KinematicSystem () {
		super(Family.with(Position.class, Kinematic.class));
	}

	private final Vector3 TMP1 = new Vector3(), TMP2 = new Vector3();

	@Override
	protected void process (final int entity, final float timeD) {
		final Position position = positionMapper.get(entity);
		final Kinematic kinematic = kinematicMapper.get(entity);

		final long oldChunkKey = position.toChunkKey();

		double velX = kinematic.velX;
		double velY = kinematic.velY;
		float velZ = kinematic.velZ;

		double accX = kinematic.accX;
		double accY = kinematic.accY;
		float accZ = kinematic.accZ;
		if (kinematic.affectedByGravity) {
			accX += gravity.x;
			accY += gravity.y;
			accZ += gravity.z;
		}

		kinematic.onGround = false;

		// TODO Frame rate dependent
		double realAccX = accX - velX * kinematic.xyFriction;
		double realAccY = accY - velY * kinematic.xyFriction;

		final float timeD2 = timeD * timeD;

		double deltaX = 0.5 * realAccX * timeD2 + velX * timeD;
		double deltaY = 0.5 * realAccY * timeD2 + velY * timeD;
		float deltaZ = 0.5f * accZ * timeD2 + velZ * timeD;

		kinematic.velX += realAccX * timeD;
		kinematic.velY += realAccY * timeD;
		kinematic.velZ += accZ * timeD;

		if(accX == 0 && MathUtils.isZero(kinematic.velX, 1e-3f)){
			kinematic.velX = 0;
		}

		if(accY == 0 && MathUtils.isZero(kinematic.velY, 1e-3f)){
			kinematic.velY = 0;
		}

		if (kinematic.hitBox == null) {
			position.add(deltaX, deltaY, deltaZ);
		} else {
			moveBy(position, kinematic, (float)deltaX, (float)deltaY, deltaZ, 2);
			if(position.z < 0){
				position.z = 0;
				if(kinematic.velZ < 0){
					kinematic.velZ = 0;
				}
				kinematic.onGround = true;
			}
		}

		final long newChunkKey = position.toChunkKey();
		if(newChunkKey != oldChunkKey){
			// Register to different chunk
			final Chunk oldChunk = world.getLoadedChunk(oldChunkKey);
			final Chunk newChunk = world.getLoadedChunk(newChunkKey);
			if(oldChunk != null){
				oldChunk.removeEntity(entity);
			}
			if(newChunk != null){
				newChunk.addEntity(entity);
			}
		}
	}

	private void moveBy (Position position, Kinematic kinematic, float x, float y, float z, int remainingBounces) {
        final Vector3 pos = position.toVector(TMP1);
        final Vector3 dir = TMP2.set(x,y,z);
        final float len = dir.len();
        dir.nor();

		final BoundingBox hitBox = kinematic.hitBox;
		final World.SweepRayCastResult castResult = world.getBlockOnSweepRay(hitBox, pos, dir, len, BlockFilter.COLLIDABLE);

        if(castResult == null){
            position.add(x,y,z);
		} else {
            final Side side = castResult.getSide();

			final float dPosX = dir.x * castResult.getT();
			final float dPosY = dir.y * castResult.getT();
			final float dPosZ = dir.z * castResult.getT();

            if(side != null){
				//Set the position to be exactly next to the side
				double newX = position.x + dPosX;
				double newY = position.y + dPosY;
				float newZ = position.z + dPosZ;

				dir.scl(len - castResult.getT());

				final BoundingBox blockBox = castResult.getBlock().hitBox;

				switch (side) {
					case EAST:
						newX = castResult.getX() + blockBox.offsetX + blockBox.sizeX - hitBox.offsetX;
						dir.x = 0;
						kinematic.velX = 0;
						break;
					case WEST:
						newX = castResult.getX() + blockBox.offsetX - (hitBox.offsetX + hitBox.sizeX);
						dir.x = 0;
						kinematic.velX = 0;
						break;
					case NORTH:
						newY = castResult.getY() + blockBox.offsetY + blockBox.sizeY - hitBox.offsetY;
						dir.y = 0;
						kinematic.velY = 0;
						break;
					case SOUTH:
						newY = castResult.getY() + blockBox.offsetY - (hitBox.offsetY + hitBox.sizeY);
						dir.y = 0;
						kinematic.velY = 0;
						break;
					case TOP:
						newZ = castResult.getZ() + blockBox.offsetZ + blockBox.sizeZ - hitBox.offsetZ;
						dir.z = 0;
						kinematic.velZ = 0;
						kinematic.onGround = true;
						break;
					case BOTTOM:
						newZ = castResult.getZ() + blockBox.offsetZ - (hitBox.offsetZ + hitBox.sizeZ);
						dir.z = 0;
						kinematic.velZ = 0;
						break;
				}

				position.set(newX, newY, newZ);

                if(dir.len2() > 0.00001f && remainingBounces > 0){
                    //Slide
                    moveBy(position, kinematic, dir.x, dir.y, dir.z, --remainingBounces);
                }
            } else {
				position.add(dPosX, dPosY, dPosZ);
			}
        }
	}
}
