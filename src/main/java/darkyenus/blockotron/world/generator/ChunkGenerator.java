package darkyenus.blockotron.world.generator;

/**
 * Generates basic shape of the world.
 */
public interface ChunkGenerator {
    /** Generate given column. MUST NOT modify blocks outside of the column. */
    void generateColumn(PersistentGeneratorChunkProvider.ChunkColumn column);
}
