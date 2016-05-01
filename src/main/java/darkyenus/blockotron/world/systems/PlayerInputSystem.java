package darkyenus.blockotron.world.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import com.github.antag99.retinazer.*;
import darkyenus.blockotron.render.WorldRenderer;
import darkyenus.blockotron.world.Side;
import darkyenus.blockotron.world.components.*;

/**
 * System for controlling single player entity.
 * Player must be played, have position and orientation.
 */
@SkipWire
public class PlayerInputSystem extends EntityProcessorSystem {

    private @Wire Mapper<Played> playedMapper;
    private @Wire Mapper<Position> positionMapper;
    private @Wire Mapper<Orientation> orientationMapper;
    private @Wire Mapper<Kinematic> kinematicMapper;
    private @Wire Mapper<SelfMotionCapable> selfMotionCapableMapper;

    private @Wire WorldRenderer worldRenderer;

    private static final float degreesPerPixel = 0.5f;
    private static final float eyeHeight = 1.6f;

    public PlayerInputSystem() {
        super(new FamilyConfig().with(Played.class, Position.class, Kinematic.class, Orientation.class, SelfMotionCapable.class));
    }

    @Override
    protected void processEntities(float delta) {
        IntArray indices = getEntities().getIndices();
        int[] items = indices.items;
        if(indices.size >= 1){
            process(items[0], delta);
            for (int i = 1, n = indices.size; i < n; i++) {
                System.out.println("Removing additional player tag on entity "+items[i]);
                playedMapper.remove(items[i]);
            }
        }
    }

    private final Vector3 speedTMP = new Vector3(), positionTMP = new Vector3(), faceTMP = new Vector3();

    @Override
    protected void process(int entity, float delta) {
        final Position position = positionMapper.get(entity);
        final Kinematic kinematic = kinematicMapper.get(entity);
        final Orientation orientation = orientationMapper.get(entity);

        if(Gdx.input.isTouched()){
            final float deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
            final float deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;

            orientation.yaw += deltaX;
            orientation.pitch += deltaY;
            orientation.normalize();
        }

        final SelfMotionCapable selfMotionCapable = selfMotionCapableMapper.get(entity);
        final Vector3 speed = speedTMP.setZero();

        if(Gdx.input.isKeyPressed(Input.Keys.W)){
            speed.add(Side.NORTH.vector);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S)){
            speed.add(Side.SOUTH.vector);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.A)){
            speed.add(Side.WEST.vector);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D)){
            speed.add(Side.EAST.vector);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.SPACE) && kinematic.onGround){
            kinematic.velZ = selfMotionCapable.jumpPower;
        }

        final Vector3 walkSpeed = orientation.rotateByYaw(speed.nor().scl(selfMotionCapable.speed));
        kinematic.accX = walkSpeed.x;
        kinematic.accY = walkSpeed.y;
        kinematic.accZ = walkSpeed.z;

        worldRenderer.setCamera(position.toVector(positionTMP).add(0, 0, eyeHeight), orientation.toFaceVector(faceTMP));
    }
}
