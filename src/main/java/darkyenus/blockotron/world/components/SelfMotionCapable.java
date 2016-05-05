package darkyenus.blockotron.world.components;

import com.github.antag99.retinazer.Component;

/**
 * Collection of motion attributes for entities which can move by themselves.
 */
public class SelfMotionCapable implements Component.Pooled {
    /** Kinematic acceleration of the entity */
    public float speed;
    /** Velocity of jump */
    public float jumpPower;

    public SelfMotionCapable setup(float speed, float jumpPower){
        this.speed = speed;
        this.jumpPower = jumpPower;
        return this;
    }
}
