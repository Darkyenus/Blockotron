package darkyenus.blockotron.world.components;

import com.github.antag99.retinazer.Component;
import darkyenus.blockotron.world.Dimensions;

/**
 * Component of entities which require world to be loaded around them.
 */
public final class ChunkLoading implements Component {
    /** True = Chunks must be loaded because world on this machine is rendered around this entity.
     * False = Chunk must be loaded for processing only. */
    public boolean rendering;

    /** Radius, in CHUNK sizes, around which all chunks will be loaded. */
    public int radius = 1;

    public void setup(boolean rendering, int radius){
        this.rendering = rendering;
        this.radius = radius;
    }
}
