package darkyenus.blockotron.world.components;

import com.github.antag99.retinazer.Component;

/**
 * Encodes velocity of the entity.
 * Only for entities with Position.
 */
public final class Kinematic implements Component {

    /** Current velocity */
    public float velX, velY, velZ;

    /** Current acceleration */
    public float accX, accY, accZ;

    /** Friction in X and Y dimensions */
    public float xyFriction;

    /** Such entities tend to fall down. */
    public boolean affectedByGravity;

    public boolean onGround = false;

    public boolean noClip = true;

    public float hitboxHalfExtentXY = 0.4f;
    public float hitboxHeight = 1.8f;

    public Kinematic setup(float xyFriction, boolean affectedByGravity){
        this.xyFriction = xyFriction;
        this.affectedByGravity = affectedByGravity;
        return this;
    }

    public Kinematic setupHitbox(float hitboxHalfExtentXY, float hitboxHeight){
        this.noClip = false;
        this.hitboxHalfExtentXY = hitboxHalfExtentXY;
        this.hitboxHeight = hitboxHeight;
        return this;
    }
}
