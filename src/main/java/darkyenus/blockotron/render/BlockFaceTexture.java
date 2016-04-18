package darkyenus.blockotron.render;

/**
 * Represents a texture (region) of a terrain texture of given id.
 *
 * Holds necessary information (UV) for rendering.
 *
 * Created by {@link BlockFaces#getBlockFace(String)}.
 */
public final class BlockFaceTexture {

    public final String id;

    public float u,v,u2,v2;

    protected BlockFaceTexture(String id) {
        this.id = id;
    }

    /** Set UV of this to the UV of argument */
    public void set(BlockFaceTexture other){
        this.u = other.u;
        this.v = other.v;
        this.u2 = other.u2;
        this.v2 = other.v2;
    }

    public void set(float u, float v, float u2, float v2){
        this.u = u;
        this.v = v;
        this.u2 = u2;
        this.v2 = v2;
    }
}
