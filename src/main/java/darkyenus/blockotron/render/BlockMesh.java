package darkyenus.blockotron.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.utils.*;
import darkyenus.blockotron.world.Chunk;
import darkyenus.blockotron.world.Side;

/**
 *
 */
public class BlockMesh implements RenderableProvider {

    private final static VertexAttributes attributes = new VertexAttributes(VertexAttribute.Position(), VertexAttribute.TexCoords(0));
    private final static int stride = attributes.vertexSize / 4;

    private final Chunk chunk;

    private final Material material;
    private FloatArray vertices = new FloatArray();
    private ShortArray indices = new ShortArray();
    private Mesh mesh;

    private static void ensureCapacity(FloatArray array, int additional){
        int minSize = array.size + additional;
        if(minSize > array.items.length){
            int newSize = array.items.length << 1;
            while(newSize < minSize){
                newSize <<= 1;
            }
            array.ensureCapacity(newSize - array.size);
        }
    }

    private static void ensureCapacity(ShortArray array, int additional){
        int minSize = array.size + additional;
        if(minSize > array.items.length){
            int newSize = array.items.length << 1;
            while(newSize < minSize){
                newSize <<= 1;
            }
            array.ensureCapacity(newSize - array.size);
        }
    }

    public BlockMesh(Chunk chunk, boolean isStatic, Material material, int maxFaces) {
        this.chunk = chunk;
        this.material = material;
        mesh = new Mesh(isStatic, facesToVertices(maxFaces), facesToIndices(maxFaces), attributes);
        vertices.ensureCapacity(facesToVertices(maxFaces) * 5);
        indices.ensureCapacity(facesToIndices(maxFaces));
    }

    private static int facesToVertices(int faces){
        return faces * 4;
    }

    private static int facesToIndices(int faces){
        return faces * 6;
    }

    public void begin(){
        vertices.clear();
        indices.clear();
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
        //Vertices
        final FloatArray vertices = this.vertices;
        ensureCapacity(vertices, stride << 2);
        int vertexOffset = vertices.size;
        vertices.size += 4 * stride;
        final float[] v = vertices.items;

        //Indices
        final ShortArray indices = this.indices;
        ensureCapacity(indices, 6);
        int indexOffset = indices.size;
        indices.size += 6;
        final short[] i = indices.items;

        //Fill indices
        i[indexOffset++] = (short) vertexOffset;
        i[indexOffset++] = (short) (vertexOffset + 1);
        i[indexOffset++] = (short) (vertexOffset + 2);
        i[indexOffset++] = (short) (vertexOffset + 2);
        i[indexOffset++] = (short) (vertexOffset + 3);
        i[indexOffset] = (short) vertexOffset;

        //Fill vertices
        int faceOffset = 0;
        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset++];
        v[vertexOffset++] = texture.u;
        v[vertexOffset++] = texture.v;

        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset++];
        v[vertexOffset++] = texture.u2;
        v[vertexOffset++] = texture.v2;

        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset++];
        v[vertexOffset++] = texture.u2;
        v[vertexOffset++] = texture.v2;

        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset];
        v[vertexOffset++] = texture.u;
        v[vertexOffset] = texture.v2;
    }

    public void end(){
        if ((mesh.getMaxVertices() * stride) < vertices.size)
            throw new GdxRuntimeException("Mesh can't hold enough vertices: " + mesh.getMaxVertices() + " * " + stride + " < "
                    + vertices.size);
        if (mesh.getMaxIndices() < indices.size)
            throw new GdxRuntimeException("Mesh can't hold enough indices: " + mesh.getMaxIndices() + " < " + indices.size);

        mesh.setVertices(vertices.items, 0, vertices.size);
        mesh.setIndices(indices.items, 0, indices.size);
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        final int indices = mesh.getNumIndices();
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
            0, 1, 0,
            1, 1, 0,
            1, 1, 1,
            0, 1, 1
    };

    private static final float[] BOTTOM_OFFSETS = {
            0, 0, 0,
            1, 0, 0,
            1, 0, 1,
            0, 0, 1
    };

    private static final float[] LEFT_OFFSETS = {
            0, 0, 0,
            0, 1, 0,
            0, 1, 1,
            0, 0, 1
    };

    private static final float[] RIGHT_OFFSETS = {
            1, 0, 0,
            1, 1, 0,
            1, 1, 1,
            1, 0, 1
    };

    private static final float[] FRONT_OFFSETS = {
            0, 0, 0,
            1, 0, 0,
            1, 1, 0,
            0, 1, 0
    };

    private static final float[] BACK_OFFSETS = {
            0, 0, 1,
            0, 1, 1,
            1, 1, 1,
            1, 0, 1
    };
}
