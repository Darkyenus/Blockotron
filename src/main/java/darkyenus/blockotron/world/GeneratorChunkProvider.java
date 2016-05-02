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
    public Chunk borrowChunk(int x, int y, int z) {
        final Chunk chunk = new Chunk(world, x, y, z);
        generator.generateChunk(chunk);
        return chunk;
    }

    @Override
    public void returnChunk(Chunk chunk) {
        //TODO
    }

    @Override
    public void update(float delta) {

    }
}
