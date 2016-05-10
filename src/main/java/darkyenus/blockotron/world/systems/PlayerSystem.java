package darkyenus.blockotron.world.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.github.antag99.retinazer.*;
import darkyenus.blockotron.render.WorldRenderer;
import darkyenus.blockotron.world.EntityStorage;
import darkyenus.blockotron.world.SelfSerializable;
import darkyenus.blockotron.world.Side;
import darkyenus.blockotron.world.World;
import darkyenus.blockotron.world.components.*;

/**
 * System for controlling single player entity.
 * Player must have Player, Position, Kinematic, Orientation and SelfMotionCapable components, otherwise it will be removed!
 */
@SkipWire
public class PlayerSystem extends EntitySystem implements SelfSerializable {

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
    /** Player name -> EntityStorage data for entities which are not loaded */
    private final ObjectMap<String, byte[]> inactivePlayerEntities = new ObjectMap<>();

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
                            inactivePlayerEntities.remove(player.playerName);
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
                        //Was not deleted, store it
                        final Player player = playedMapper.get(entity);

                        playerEntities.remove(player.playerName, -1);
                        inactivePlayerEntities.put(player.playerName, EntityStorage.saveEntitiy(world, entity));
                    }
                }
            }
        });
    }

    /** Load player which is not loaded yet and load it.
     * @return true if loaded, false if not existing or already loaded */
    public boolean loadPlayer(String name) {
        final byte[] entity = inactivePlayerEntities.get(name);
        if(entity == null) {
            return false;
        } else {
            EntityStorage.loadEntity(world, entity);
            return true;
        }
    }

    public void unloadPlayer(String name){
        final int entity = playerEntities.get(name, -1);
        if(entity != -1){
            world.entityEngine().destroyEntity(entity);
        }
    }


    public void unloadAllPlayers() {
        for (String playerName : playerEntities.keys()) {
            unloadPlayer(playerName);
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

        if(Gdx.input.isKeyPressed(Input.Keys.W)) {
            speed.add(Side.NORTH.vector);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S)) {
            speed.add(Side.SOUTH.vector);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            speed.add(Side.WEST.vector);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            speed.add(Side.EAST.vector);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.SPACE) && kinematic.onGround) {
            kinematic.velZ = selfMotionCapable.jumpPower;
        }

        final Vector3 walkSpeed = orientation.rotateByYaw(speed.nor().scl(selfMotionCapable.speed));
        kinematic.accX = walkSpeed.x;
        kinematic.accY = walkSpeed.y;
        kinematic.accZ = walkSpeed.z;

        worldRenderer.setCamera(position.toVector(positionTMP).add(0, 0, eyeHeight), orientation.toFaceVector(faceTMP));
    }

    @Override
    public void serialize(Output out, Kryo kryo) {
        final ObjectMap<String, byte[]> playerEntities = this.inactivePlayerEntities;
        out.writeInt(playerEntities.size, true);
        for (ObjectMap.Entry<String, byte[]> entry : playerEntities) {
            out.writeString(entry.key);
            kryo.writeObject(out, entry.value);
        }
    }

    @Override
    public void deserialize(com.esotericsoftware.kryo.io.Input input, Kryo kryo) {
        final ObjectMap<String, byte[]> playerEntities = inactivePlayerEntities;
        playerEntities.clear();
        final int size = input.readInt(true);
        playerEntities.ensureCapacity(size);
        for (int i = 0; i < size; i++) {
            final String playerName = input.readString();
            final byte[] playerEntity = kryo.readObject(input, byte[].class);
            playerEntities.put(playerName, playerEntity);
        }
    }
}
