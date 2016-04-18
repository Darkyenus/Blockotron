package darkyenus.blockotron.world;

/**
 *
 */
public interface BlockFilter {
    boolean accepts(Block block);

    BlockFilter NO_AIR = block -> block != Block.AIR;
}
