package darkyenus.blockotron.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 *
 */
public enum Side {
    RIGHT(1, 0, 0),
    LEFT(-1, 0, 0),
    BACK(0, 1, 0),
    FRONT(0, -1, 0),
    TOP(0, 0, 1),
    BOTTOM(0, 0, -1),
    ;

    public final byte flag;
    public final int offX, offY, offZ;

    Side(int offX, int offY, int offZ) {
        this.flag = (byte) (1 << ordinal());
        this.offX = offX;
        this.offY = offY;
        this.offZ = offZ;
    }

    /** Return direction of normalized vector or null if invalid. */
    public static Side matchDirection(Vector3 of){
        int x = MathUtils.round(of.x);
        int y = MathUtils.round(of.y);
        int z = MathUtils.round(of.z);
        for (Side side : values()) {
            if(side.offX == x && side.offY == y && side.offZ == z) return side;
        }
        return null;
    }

    // Optimized access to Side.flag (can be inlined by compiler)
    public static final byte right =    1;
    public static final byte left =     1 << 1;
    public static final byte back =     1 << 2;
    public static final byte front =    1 << 3;
    public static final byte top =      1 << 4;
    public static final byte bottom =   1 << 5;
}
