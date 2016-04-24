package darkyenus.blockotron.world.components;

import com.github.antag99.retinazer.Component;

/**
 * Position of block entity.
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
