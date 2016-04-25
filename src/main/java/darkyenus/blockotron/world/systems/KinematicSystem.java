package darkyenus.blockotron.world.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.github.antag99.retinazer.*;
import darkyenus.blockotron.world.BlockFilter;
import darkyenus.blockotron.world.Side;
import darkyenus.blockotron.world.World;
import darkyenus.blockotron.world.components.Kinematic;
import darkyenus.blockotron.world.components.Position;

/** Moves kinematic entities, colliding with blocks, but not with other entities. */
@SkipWire
public class KinematicSystem extends EntityProcessorSystem {

	private @Wire World world;
	private @Wire Mapper<Position> positionMapper;
	private @Wire Mapper<Kinematic> kinematicMapper;

	private static final Vector3 gravity = new Vector3(0, 0, -9.81f);

	public KinematicSystem () {
		super(Family.with(Position.class, Kinematic.class));
	}

	private final Vector3 TMP1 = new Vector3(), TMP2 = new Vector3();

	@Override
	protected void process (int entity) {
		final Position position = positionMapper.get(entity);
		final Kinematic kinematic = kinematicMapper.get(entity);

		final float timeD = Gdx.graphics.getDeltaTime();

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

		if (kinematic.noClip) {
			position.add(deltaX, deltaY, deltaZ);
		} else {
			moveBy(position, kinematic, (float)deltaX, (float)deltaY, deltaZ);
			if(position.z < 0){
				position.z = 0;
				if(kinematic.velZ < 0){
					kinematic.velZ = 0;
				}
				kinematic.onGround = true;
			}
		}
	}

	private void moveBy (Position position, Kinematic kinematic, float x, float y, float z) {
        //TODO This is only point collision, not full hitbox
        final Vector3 pos = position.toVector(TMP1);
        final Vector3 dir = TMP2.set(x,y,z);
        final float len = dir.len();
        dir.nor();

        //TODO Better filter than NO_AIR
        final World.RayCastResult castResult = world.getBlockOnRay(pos, dir, len, BlockFilter.NO_AIR);

        if(castResult == null){
            position.add(x,y,z);
        } else {
            final Side side = castResult.getSide();

            dir.scl(castResult.getT());
			final float remainingLen = len - castResult.getT();

            if(side != null){
                final float onionSkin = 0.001f;
                position.add(dir.x + side.offX * onionSkin, dir.y + side.offY * onionSkin, dir.z + side.offZ * onionSkin);

                //dir.scl(side.vector.dot(dir));

                if(side.offX != 0){
                    dir.x = 0;
                    //kinematic.velX = 0;
                } else if(side.offY != 0){
                    dir.y = 0;
                    //kinematic.velY = 0;
                } else if(side.offZ != 0) {
                    dir.z = 0;
                    kinematic.velZ = 0;

					if(side == Side.TOP){
						kinematic.onGround = true;
					}
                }

                if(dir.len2() > 0.000001f){
                    //Slide
					dir.nor().scl(remainingLen);
                    moveBy(position, kinematic, dir.x, dir.y, dir.z);
                }
            } else {
                position.add(dir.x, dir.y, dir.z);
            }
        }
	}

/** Move this entity in world by given amount. Checks for collisions. *//*

    public void moveBy(float x, float y, float z){
        //Implemented using discrete collision checking steps
        final double length = Math.sqrt(x * x + y * y + z * z);
        if(length <= 1){
            moveByDiscreteStep(x, y, z);
        } else {
            final int steps = MathUtils.ceilPositive((float)length);
            float stepX = x / steps;
            float stepY = y / steps;
            float stepZ = z / steps;
            for (int i = 0; i < steps; i++) {
                moveByDiscreteStep(stepX, stepY, stepZ);
            }
        }
    }

    private void moveByDiscreteStep(float x, float y, float z){
        final double positionX = this.positionX;
        final double positionY = this.positionY;
        final double positionZ = this.positionZ;
        double newX = positionX + x;
        double newY = positionY + y;
        double newZ = positionZ + z;

        //TODO Collision detection

        if(!isEqual(positionX, newX) || !isEqual(positionY, newY) || !isEqual(positionZ, newZ)){
            //Moved, save values and notify world
            this.positionX = newX;
            this.positionY = newY;
            this.positionZ = newZ;

            final int oldChunkX = World.chunkCoord(positionX);
            final int oldChunkY = World.chunkCoord(positionY);
            final int newChunkX = World.chunkCoord(newX);
            final int newChunkY = World.chunkCoord(newY);
            if(oldChunkX != newChunkX || oldChunkY != newChunkY){
                final Chunk oldChunk = world.getLoadedChunk(oldChunkX, oldChunkY);
                final Chunk newChunk = world.getLoadedChunk(newChunkX, newChunkY);
                if(oldChunk != null && newChunk != null){
                    //If removed, add to new
                    if (oldChunk.removeEntity(this)) {
                        newChunk.addEntity(this);
                    } else {
                        assert false : "Entity "+this+" was not present in chunk "+oldChunkX+" "+oldChunkY;
                    }
                }
            }
        }
    }

    private static boolean isEqual(double a, double b){
        return Math.abs(a - b) <= MathUtils.FLOAT_ROUNDING_ERROR;
    }
*/

}
