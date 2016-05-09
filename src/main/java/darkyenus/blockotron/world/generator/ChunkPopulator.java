package darkyenus.blockotron.world.generator;

/**
 * Adds additional details to the map.
 */
public interface ChunkPopulator {
    /** Populate given chunk column. May modify blocks outside of the column. */
    void populateColumn(PersistentGeneratorChunkProvider.ChunkColumn column);
}
