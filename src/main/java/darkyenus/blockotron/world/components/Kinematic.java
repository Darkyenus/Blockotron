package darkyenus.blockotron.world.components;

import com.github.antag99.retinazer.Component;
import darkyenus.blockotron.utils.BoundingBox;

/**
 * Encodes velocity of the entity.
 * Only for entities with Position.
 */
public final class Kinematic implements Component.Pooled {

    /** Current velocity */
    public float velX, velY, velZ;

    /** Current acceleration */
    public float accX, accY, accZ;

    /** Friction in X and Y dimensions */
    public float xyFriction;

    /** Such entities tend to fall down. */
    public boolean affectedByGravity;

    public boolean onGround = false;

    /** Hitbox of the entity, positioned at entity's position.
     * Entities without any hitbox do not collide with anything. */
    public BoundingBox hitBox = null;

    public Kinematic setup(float xyFriction, boolean affectedByGravity){
        this.xyFriction = xyFriction;
        this.affectedByGravity = affectedByGravity;
        return this;
    }

    public Kinematic setupHitbox(float hitboxHalfExtentXY, float hitboxHeight){
        this.hitBox = new BoundingBox(-hitboxHalfExtentXY, -hitboxHalfExtentXY, 0f, hitboxHalfExtentXY * 2f, hitboxHalfExtentXY*2f, hitboxHeight);
        return this;
    }
}
