package darkyenus.blockotron.world;

/**
 *
 */
public enum Side {
    TOP(0, 0, 1), BOTTOM(0, 0, -1), LEFT(-1, 0, 0), RIGHT(1, 0, 0), FRONT(0, -1, 0), BACK(0, 1, 0);

    public final byte flag;
    public final int offX, offY, offZ;

    Side(int offX, int offY, int offZ) {
        this.flag = (byte) (1 << ordinal());
        this.offX = offX;
        this.offY = offY;
        this.offZ = offZ;
    }

    // Optimized access to Side.flag (can be inlined by compiler)
    public static final byte top = 1;
    public static final byte bottom = 1 << 1;
    public static final byte left = 1 << 2;
    public static final byte right = 1 << 3;
    public static final byte front = 1 << 4;
    public static final byte back = 1 << 5;
}
