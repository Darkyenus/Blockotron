package darkyenus.blockotron.world.components;

import com.github.antag99.retinazer.Component;
import darkyenus.blockotron.world.Dimensions;

/**
 * Position of block entity.
 *
 * DO NOT CHANGE THESE VALUES MANUALLY!
 * USE METHODS PROVIDED BY {@link darkyenus.blockotron.world.World}!
 *
 * Note: Chunks track entities based on their position. When changing the position,
 * use {@link darkyenus.blockotron.world.Chunk#addEntity(int)} and {@link darkyenus.blockotron.world.Chunk#removeEntity(int)}
 * to register the entity to the proper chunk.
 *
 * @see Position for normal entities. Mutually exclusive.
 */
public final class BlockPosition implements Component.Pooled {
    /** World block coordinates of this entity.
     * DO NOT CHANGE MANUALLY, ONLY THROUGH METHODS PROVIDED BY {@link darkyenus.blockotron.world.World}! */
    public int x, y, z;

    public int getChunkX() {
        return Dimensions.worldToChunk(x);
    }

    public int getChunkY() {
        return Dimensions.worldToChunk(y);
    }

    public int getChunkZ() {
        return Dimensions.worldToChunk(z);
    }
}
