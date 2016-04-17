package darkyenus.blockotron.world;

import darkyenus.blockotron.world.generator.WorldGenerator;

/**
 *
 */
public class GeneratorChunkProvider implements ChunkProvider {

    private final WorldGenerator generator;
    private World world;

    public GeneratorChunkProvider(WorldGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void initialize(World world) {
        this.world = world;
    }

    @Override
    public Chunk getChunk(int x, int y) {
        final Chunk chunk = new Chunk(world, x, y);
        generator.generateChunk(chunk);
        return chunk;
    }

    @Override
    public void update(float delta) {

    }
}
