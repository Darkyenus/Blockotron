package darkyenus.blockotron.world.components;

import com.badlogic.gdx.math.Vector3;
import com.github.antag99.retinazer.Component;
import darkyenus.blockotron.world.Dimensions;

/**
 * World entity position.
 *
 * Note: Chunks track entities based on their position. When changing the position,
 * use {@link darkyenus.blockotron.world.Chunk#addEntity(int)} and {@link darkyenus.blockotron.world.Chunk#removeEntity(int)}
 * to register the entity to the proper chunk.
 *
 * @see BlockPosition for block entities. Mutually exclusive.
 */
public final class Position implements Component {
    public double x;
    public double y;
    public float z;

    public Position set(double x, double y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Position add(double x, double y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public long toChunkKey(){
        return Dimensions.worldToClampedChunkKey((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
    }

    public Vector3 toVector(Vector3 v) {
        return v.set((float)x, (float)y, z);
    }
}
