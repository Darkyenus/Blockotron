package darkyenus.blockotron.world.components;

import com.github.antag99.retinazer.Component;

/**
 * Position of block entity.
 *
 * Note: Chunks track entities based on their position. When changing the position,
 * use {@link darkyenus.blockotron.world.Chunk#addEntity(int)} and {@link darkyenus.blockotron.world.Chunk#removeEntity(int)}
 * to register the entity to the proper chunk.
 *
 * @see Position for normal entities. Mutually exclusive.
 */
public final class BlockPosition implements Component {
    public int x;
    public int y;
    public int z;

    public BlockPosition set(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
}
