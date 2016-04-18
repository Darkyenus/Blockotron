package darkyenus.blockotron.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import darkyenus.blockotron.world.Side;

/**
 *
 */
public class BlockMesh implements RenderableProvider {

    private final static VertexAttributes attributes = new VertexAttributes(
            VertexAttribute.Position(),//3
            VertexAttribute.TexCoords(0)//2
    );
    private final static int vertexSize = 5;

    private final Material material;
    private float[] vertices;
    private int maxFaces;
    private int faces = 0;
    private Mesh mesh;

    public BlockMesh(boolean isStatic, Material material, int maxFaces) {
        this.material = material;
        this.maxFaces = maxFaces;
        final int maxIndices = facesToIndices(maxFaces);
        mesh = new Mesh(isStatic, facesToVertices(maxFaces), maxIndices, attributes);
        vertices = new float[facesToVertices(maxFaces) * vertexSize];
        mesh.setIndices(getIndices(maxIndices), 0, maxIndices);
    }

    private static short[] indicesCache;
    /** Since all indices are the same, we generate them once and then serve cached version.
     * Cached version may be larger than what is requested, so be prepared to handle that. */
    private short[] getIndices(int length){
        if(indicesCache != null && indicesCache.length >= length){
            return indicesCache;
        }
        short[] indices = new short[length];
        short j = 0;
        for (int i = 0; i < length; i += 6, j += 4) {
            indices[i] = j;
            indices[i + 1] = (short)(j + 1);
            indices[i + 2] = (short)(j + 2);
            indices[i + 3] = (short)(j + 2);
            indices[i + 4] = (short)(j + 3);
            indices[i + 5] = j;
        }
        return indicesCache = indices;
    }

    private static int facesToVertices(int faces){
        return faces * 4;
    }

    private static int facesToIndices(int faces){
        return faces * 6;
    }

    public void begin(){
        faces = 0;
    }

    public void createBlock (int x, int y, int z, byte faceMask, BlockFaceTexture texture){
        if((faceMask & Side.top) != 0) createBlockFace(x, y, z, TOP_OFFSETS, texture);
        if((faceMask & Side.bottom) != 0) createBlockFace(x, y, z, BOTTOM_OFFSETS, texture);
        if((faceMask & Side.left) != 0) createBlockFace(x, y, z, LEFT_OFFSETS, texture);
        if((faceMask & Side.right) != 0) createBlockFace(x, y, z, RIGHT_OFFSETS, texture);
        if((faceMask & Side.back) != 0) createBlockFace(x, y, z, BACK_OFFSETS, texture);
        if((faceMask & Side.front) != 0) createBlockFace(x, y, z, FRONT_OFFSETS, texture);
    }

    public void createBlock (int x, int y, int z, byte faceMask, BlockFaceTexture top, BlockFaceTexture sides, BlockFaceTexture bottom){
        if((faceMask & Side.top) != 0) createBlockFace(x, y, z, TOP_OFFSETS, top);
        if((faceMask & Side.bottom) != 0) createBlockFace(x, y, z, BOTTOM_OFFSETS, bottom);
        if((faceMask & Side.left) != 0) createBlockFace(x, y, z, LEFT_OFFSETS, sides);
        if((faceMask & Side.right) != 0) createBlockFace(x, y, z, RIGHT_OFFSETS, sides);
        if((faceMask & Side.back) != 0) createBlockFace(x, y, z, BACK_OFFSETS, sides);
        if((faceMask & Side.front) != 0) createBlockFace(x, y, z, FRONT_OFFSETS, sides);
    }

    public void createBlockFace (int x, int y, int z, float[] faceOffsets, BlockFaceTexture texture){
        if(faces == maxFaces){
            return;
        }

        //Vertices
        int vertexOffset = facesToVertices(faces) * vertexSize;
        final float[] v = vertices;

        //Fill vertices
        int faceOffset = 0;
        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset++];
        v[vertexOffset++] = texture.u2;
        v[vertexOffset++] = texture.v2;

        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset++];
        v[vertexOffset++] = texture.u2;
        v[vertexOffset++] = texture.v;

        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset++];
        v[vertexOffset++] = texture.u;
        v[vertexOffset++] = texture.v;

        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset];
        v[vertexOffset++] = texture.u;
        v[vertexOffset] = texture.v2;

        faces++;
    }

    public void end(){
        final int verticesSize = facesToVertices(faces) * 6;

        assert verticesSize <= vertices.length : "Somehow generated more vertices than can fit: "+verticesSize+" > "+vertices.length;
        assert (mesh.getMaxVertices() * vertexSize) >= verticesSize : "Mesh can't hold enough vertices: " + mesh.getMaxVertices() + " * " + vertexSize + " < " + verticesSize;

        mesh.setVertices(vertices, 0, verticesSize);
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        final int indices = facesToIndices(faces);
        if(indices == 0)return;
        final Renderable r = pool.obtain();
        r.meshPart.mesh = mesh;
        r.meshPart.offset = 0;
        r.meshPart.primitiveType = GL20.GL_TRIANGLES;
        r.meshPart.size = indices;
        r.material = material;
        renderables.add(r);
    }

    public void dispose(){
        mesh.dispose();
    }

    private static final float[] TOP_OFFSETS = {
            0, 0, 1,
            1, 0, 1,
            1, 1, 1,
            0, 1, 1
    };

    private static final float[] BOTTOM_OFFSETS = {
            0, 1, 0,
            1, 1, 0,
            1, 0, 0,
            0, 0, 0
    };

    private static final float[] LEFT_OFFSETS = {
            0, 0, 0,
            0, 0, 1,
            0, 1, 1,
            0, 1, 0
    };

    private static final float[] RIGHT_OFFSETS = {
            1, 1, 0,
            1, 1, 1,
            1, 0, 1,
            1, 0, 0
    };

    private static final float[] FRONT_OFFSETS = {
            1, 0, 0,
            1, 0, 1,
            0, 0, 1,
            0, 0, 0,
    };

    private static final float[] BACK_OFFSETS = {
            0, 1, 0,
            0, 1, 1,
            1, 1, 1,
            1, 1, 0
    };
}
