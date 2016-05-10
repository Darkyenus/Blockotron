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
    void blockChanged(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ, Block from, Block to);

    /** Called when block's occlusion mask changes. */
    void blockOcclusionChanged(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ, byte from, byte to);

    /** Called when previously loaded chunk is unloaded from a world. */
    void chunkUnloaded(Chunk chunk);

    abstract class WorldObserverAdapter implements WorldObserver {
        @Override
        public void initialize(World world) {

        }

        @Override
        public void chunkLoaded(Chunk chunk) {

        }

        @Override
        public void blockChanged(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ, Block from, Block to) {

        }

        @Override
        public void blockOcclusionChanged(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ, byte from, byte to) {

        }

        @Override
        public void chunkUnloaded(Chunk chunk) {

        }
    }
}
