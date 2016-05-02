package darkyenus.blockotron.world;

/**
 * Provides chunks for World, either by loading them, generating them or fetching from network.
 */
public interface ChunkProvider {

    /** Called when added to a world. One instance may be assigned to only one world. */
    void initialize(World world);

    /** Return chunk at given chunk-coordinates. Chunk may be empty, but never null. */
    Chunk borrowChunk(int x, int y, int z);

    void returnChunk(Chunk chunk);

    /** Update this chunk provider. Previously loaded chunks may be modified here if necessary (new data from server). */
    void update(float delta);
}
