package darkyenus.blockotron.world.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.github.antag99.retinazer.*;
import darkyenus.blockotron.render.WorldRenderer;
import darkyenus.blockotron.world.Side;
import darkyenus.blockotron.world.World;
import darkyenus.blockotron.world.components.*;

/**
 * System for controlling single player entity.
 * Player must have Player, Position, Kinematic, Orientation and SelfMotionCapable components, otherwise it will be removed!
 */
@SkipWire
public class PlayerSystem extends EntitySystem {

    private static final String LOG = "PlayerSystem";

    private @Wire Mapper<Player> playedMapper;
    private @Wire Mapper<Position> positionMapper;
    private @Wire Mapper<Orientation> orientationMapper;
    private @Wire Mapper<Kinematic> kinematicMapper;
    private @Wire Mapper<SelfMotionCapable> selfMotionCapableMapper;
    private @Wire Mapper<ChunkLoading> chunkLoadingMapper;

    private @Wire World world;
    private @Wire WorldRenderer worldRenderer;

    private final ObjectIntMap<String> playerEntities = new ObjectIntMap<>();
    private final IntArray deletedEntities = new IntArray(false, 8);
    /** Player name -> Chunk key for entities which are not loaded */
    private final ObjectMap<String, Long> unloadedPlayerEntityPositions = new ObjectMap<>();

    private final FamilyConfig playerFamilyConfig = Family.with(Player.class);

    private final String playerName;
    private int playerEntity = -1;

    private static final float degreesPerPixel = 0.5f;
    private static final float eyeHeight = 1.6f;

    public PlayerSystem(String playerName) {
        this.playerName = playerName;
    }

    @Override
    public void setup() {
        super.setup();

        engine.getFamily(playerFamilyConfig).addListener(new EntityListener() {
            @Override
            public void inserted(EntitySet entities) {
                final int[] entityArray = entities.getIndices().items;
                final int size = entities.getIndices().size;
                for (int i = 0; i < size; i++) {
                    final int entity = entityArray[i];
                    final Player player = playedMapper.get(entity);
                    final Position position = positionMapper.get(entity);
                    final Kinematic kinematic = kinematicMapper.get(entity);
                    final Orientation orientation = orientationMapper.get(entity);
                    final SelfMotionCapable selfMotionCapable = selfMotionCapableMapper.get(entity);
                    if(position == null || kinematic == null || orientation == null || selfMotionCapable == null){
                        Gdx.app.error(LOG,"Entity with Player component is missing one of essential components and will be removed (missing:"
                                +(position == null ? " position":"")+(kinematic == null ? " kinematic" : "")+(orientation == null?" orientation":"")+(selfMotionCapable == null ? " selfMotionCapable":"")+")");
                        deletedEntities.add(entity);
                        engine.destroyEntity(entity);
                    } else {
                        if(playerEntities.containsKey(player.playerName)){
                            Gdx.app.error(LOG,"Duplicate player entity, deleting (existing: "+playerEntities.get(player.playerName, -1)+" new: "+entity+" name: "+player.playerName+")");
                            deletedEntities.add(entity);
                            engine.destroyEntity(entity);
                        } else {
                            playerEntities.put(player.playerName, entity);
                            unloadedPlayerEntityPositions.remove(player.playerName);
                            if(playerName.equals(player.playerName)){
                                playerEntity = entity;
                            }
                        }
                    }
                }
            }

            @Override
            public void removed(EntitySet entities) {
                final int[] entityArray = entities.getIndices().items;
                final int size = entities.getIndices().size;
                for (int i = 0; i < size; i++) {
                    final int entity = entityArray[i];
                    if(entity == playerEntity){
                        playerEntity = -1;
                    }
                    if(!deletedEntities.removeValue(entity)){
                        //Was not deleted, store its position
                        final Player player = playedMapper.get(entity);
                        final Position position = positionMapper.get(entity);

                        playerEntities.remove(player.playerName, -1);
                        unloadedPlayerEntityPositions.put(player.playerName, position.toChunkKey());
                    }
                }
            }
        });
    }

    /** Load player which is not loaded yet and load it.
     * @return true if loaded, false if not existing or already loaded */
    public boolean loadPlayer(String name) {
        final Long keyOrNull = unloadedPlayerEntityPositions.get(name);
        if(keyOrNull == null) {
            return false;
        } else {
            engine.getSystem(ChunkLoadingSystem.class).insertTemporaryAnchor(keyOrNull, 1);
            return true;
        }
    }

    public void unloadPlayer(String name){
        final int entity = playerEntities.get(name, -1);
        if(entity != -1){
            engine.getSystem(ChunkLoadingSystem.class).disableAnchor(entity);
        }
    }

    private final Vector3 speedTMP = new Vector3(), positionTMP = new Vector3(), faceTMP = new Vector3();

    @Override
    protected void update(float delta) {
        final int entity = this.playerEntity;
        if(entity == -1)return;

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
