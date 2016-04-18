package darkyenus.blockotron.world;

import darkyenus.blockotron.render.RectangleMeshBatch;

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

    public abstract void render(int x, int y, int z, byte occlusion, RectangleMeshBatch mesh);
}
