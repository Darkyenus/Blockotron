package darkyenus.blockotron.world.components;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.github.antag99.retinazer.Component;
import darkyenus.blockotron.world.Side;

/**
 *
 */
public final class Orientation implements Component {

    /** Looking north/south/east/west?
     * In degrees, 0-360. 0 = North */
    public float yaw;
    /** Looking up/down?
     * In degrees, -90 - 90. -90 = Down, 90 = Up */
    public float pitch;

    /** Clamp/normalize values to legal ranges. */
    public void normalize(){
        float yaw = (this.yaw % 360);
        if(yaw < 0)yaw += 360;
        this.yaw = yaw;

        pitch = MathUtils.clamp(pitch, -89, 89);//Not 90 because camera freaks out when that happens
    }

    /** Convert yaw to direction vector. */
    public Vector3 toBodyVector(Vector3 v){
        return v.set(Side.NORTH.vector).rotate(Side.TOP.vector, yaw);
    }

    public Vector3 rotateByYaw(Vector3 v){
        return v.rotate(Side.TOP.vector, yaw);
    }

    /** Convert yaw and pitch to direction vector. */
    public Vector3 toFaceVector(Vector3 v){
        return v.set(Side.NORTH.vector).rotate(pitch, 1, 0, 0).rotate(Side.TOP.vector, yaw);
    }
}
