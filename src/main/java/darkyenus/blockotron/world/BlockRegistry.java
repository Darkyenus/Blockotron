package darkyenus.blockotron.world;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import darkyenus.blockotron.world.blocks.*;

import java.util.NoSuchElementException;

/**
 * Static registry of all registered and usable blocks, together with their IDs.
 * IDs are used only for networking and persistence (saving).
 */
public class BlockRegistry {

    static {
        //TODO: Temporary block registration place
        register(Air.AIR);
        register(BasicBlocks.DIRT);
        register(Flowerpot.FLOWERPOT);
        register(BasicBlocks.GLASS);
        register(BasicBlocks.GRASS);
        register(BasicBlocks.WOOD_LOG);
        register(BasicBlocks.LEAVES);
    }

    private static final ObjectMap<String, Block> registeredBlocks = new ObjectMap<>();
    private static final IntMap<Block> assignedIDs = new IntMap<>();
    private static int nextBlockID = 0;

    public static void register(Block block){
        final Block old = registeredBlocks.put(block.id, block);
        if(old != null) {
            registeredBlocks.put(old.id, old);
            throw new IllegalArgumentException("Block with ID " + block.id + " already registered! (OLD: "+old+" NEW: "+block+")");
        }
        assignedIDs.put(nextBlockID++, block);
    }

    /** Retrieve registered block with given ID.
     * @throws java.util.NoSuchElementException if such block does not exist */
    public static Block get(String id){
        final Block block = registeredBlocks.get(id);
        if(block == null) throw new NoSuchElementException("No block with ID \""+id+"\" registered");
        return block;
    }

    /** Retrieve registered block with given ID or null of no such block exists. */
    public static Block findBlock(String id) {
        return registeredBlocks.get(id);
    }
}
