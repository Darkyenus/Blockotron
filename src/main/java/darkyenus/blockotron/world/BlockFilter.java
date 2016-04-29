package darkyenus.blockotron.world;

import darkyenus.blockotron.world.blocks.Air;

/**
 *
 */
public interface BlockFilter {
    boolean accepts(Block block);

    BlockFilter NO_AIR = block -> block != Air.AIR;

    BlockFilter COLLIDABLE = Block::isCollidable;
}
