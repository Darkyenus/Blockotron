package darkyenus.blockotron.world;

/**
 * Observer of the world which, when registered to a world, gets notified of events happening in it.
 */
public interface WorldObserver {

    /** Called when added to a world. */
    void initialize(World world);

    /** Called when a new chunk is loaded into a world, or after initialize if the world already has chunks loaded. */
    void chunkLoaded(Chunk chunk);

    /** Called when a block in chunk changes. */
    void chunkChanged(Chunk chunk, boolean staticBlocks);

    /** Called when previously loaded chunk is unloaded from a world. */
    void chunkUnloaded(Chunk chunk);
}
