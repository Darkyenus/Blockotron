package darkyenus.blockotron.world;

/**
 *
 */
public abstract class Block {

    public final String id;
    /** Render: Does have transparent textures/sides? */
    public final boolean transparent;
    /** Render: Does need to be rendered each frame? (Is it animated?) (static blocks are buffered) */
    public final boolean dynamic;

    protected Block(String id, boolean transparent, boolean dynamic) {
        this.id = id;
        this.transparent = transparent;
        this.dynamic = dynamic;
    }


}
