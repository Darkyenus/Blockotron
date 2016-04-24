package darkyenus.blockotron.world.components;

import com.badlogic.gdx.math.Vector3;
import com.github.antag99.retinazer.Component;

/**
 * World entity position
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

    public Vector3 toVector(Vector3 v) {
        return v.set((float)x, (float)y, z);
    }
}
