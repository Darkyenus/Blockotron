package darkyenus.blockotron.world;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.esotericsoftware.kryo.Kryo;
import com.github.antag99.retinazer.Component;
import darkyenus.blockotron.utils.BoundingBox;
import darkyenus.blockotron.utils.kryo.BoundingBoxSerializer;
import darkyenus.blockotron.world.blocks.Air;
import darkyenus.blockotron.world.blocks.BasicBlocks;
import darkyenus.blockotron.world.blocks.Flowerpot;
import darkyenus.blockotron.world.components.*;

import java.util.NoSuchElementException;

/**
 * Static registry of all blocks and components, together with their IDs.
 * IDs are used only for networking and persistence (saving).
 */
public final class Registry {

    private Registry() {
    }

    private static final int BLOCK_ID_BIT = 0;
    private static final int COMPONENT_ID_BIT = 1;

    private static final ObjectMap<String, Block> registeredBlocks = new ObjectMap<>();
    private static final IntMap<Block> assignedBlockIDs = new IntMap<>();
    /** Block IDs are positive and even (end with 0 in binary) */
    private static int nextBlockID = 0;

    public static void register(Block block){
        final Block old = registeredBlocks.put(block.id, block);
        if(old != null) {
            registeredBlocks.put(old.id, old);
            throw new IllegalArgumentException("Block with ID " + block.id + " already registered! (OLD: "+old+" NEW: "+block+")");
        }
        //noinspection PointlessBitwiseExpression
        int id = ((nextBlockID++) << 1) | BLOCK_ID_BIT;
        block.registryID = id;
        assignedBlockIDs.put(id, block);
    }

    /** Retrieve registered block with given ID.
     * @throws java.util.NoSuchElementException if such block does not exist */
    public static Block block(String id){
        final Block block = registeredBlocks.get(id);
        if(block == null) throw new NoSuchElementException("No block with ID \""+id+"\" registered");
        return block;
    }

    public static Block block(int id){
        final Block block = assignedBlockIDs.get(id);
        if(block == null) throw new NoSuchElementException("No block with ID \""+id+"\" registered");
        return block;
    }

    /** Retrieve registered block with given ID or null of no such block exists. */
    public static Block findBlock(String id) {
        return registeredBlocks.get(id);
    }

    private static final IntMap<Class<? extends Component>> assignedComponentIDs = new IntMap<>();
    private static final ObjectIntMap<Class<? extends Component>> reverseAssignedComponentIDs = new ObjectIntMap<>();
    /** Component IDs are positive and odd (end with 1 in binary) */
    private static int nextComponentID = 0;

    public static void register(Class<? extends Component> component){
        final int id = ((nextComponentID++) << 1) | COMPONENT_ID_BIT;
        assignedComponentIDs.put(id, component);
        reverseAssignedComponentIDs.put(component, id);
    }

    public static int idForComponent(Class<? extends Component> component){
        return reverseAssignedComponentIDs.get(component, -1);
    }

    public static Class<? extends Component> componentForID(int id){
        final Class<? extends Component> component = assignedComponentIDs.get(id);
        if(component == null) throw new IllegalArgumentException("Illegal component ID "+id);
        return component;
    }

    private static final int KRYO_REGISTER_OFFSET = 16;

    public static Kryo createKryo(){
        Kryo kryo = new Kryo();
        kryo.setAsmEnabled(true);
        kryo.setReferences(true);
        kryo.setAutoReset(false);
        kryo.setRegistrationRequired(true);

        for (IntMap.Entry<Class<? extends Component>> entry : assignedComponentIDs.entries()) {
            kryo.register(entry.value, entry.key + KRYO_REGISTER_OFFSET);//Offset for default kryo types
        }

        int nonComponentID = KRYO_REGISTER_OFFSET-2;//Offset for default kryo types
        //Non-component blocks use even numbers, because components use odd numbers
        //(Even IDs are assigned to blocks and those are not serialized this way)
        kryo.register(BoundingBox.class, BoundingBoxSerializer.INSTANCE, nonComponentID += 2);
        return kryo;
    }

    static {
        //TODO: Temporary block registration place
        register(Air.AIR);
        register(BasicBlocks.DIRT);
        register(Flowerpot.FLOWERPOT);
        register(BasicBlocks.GLASS);
        register(BasicBlocks.GRASS);
        register(BasicBlocks.WOOD_LOG);
        register(BasicBlocks.LEAVES);

        register(Position.class);
        register(BlockPosition.class);
        register(ChunkLoading.class);
        register(Kinematic.class);
        register(Orientation.class);
        register(Player.class);
        register(SelfMotionCapable.class);
    }
}
