package darkyenus.blockotron.render;

/**
 *
 */
public final class BlockFaceTexture {

    public final String id;

    public float u,v,u2,v2;

    public BlockFaceTexture(String id) {
        this.id = id;
    }

    public BlockFaceTexture(String id, float u, float v, float u2, float v2) {
        this.id = id;
        this.u = u;
        this.v = v;
        this.u2 = u2;
        this.v2 = v2;
    }

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
