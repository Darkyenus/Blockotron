package darkyenus.blockotron.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import darkyenus.blockotron.world.Side;

/**
 * Used to dynamically create a Mesh out of textured rectangles.
 *
 * Holds the mesh, takes care of its lifecycle, vertices and indices.
 * Provides a begin() - add - end() style API for simple rendering of shapes out of rectangles.
 * Also is a RenderableProvider and draws everything added during the last begin() - add - end() cycle.
 */
public class RectangleMeshBatch implements RenderableProvider {

    private final static VertexAttributes attributes = new VertexAttributes(
            VertexAttribute.Position(),//3
            VertexAttribute.TexCoords(0)//2
    );
    private final static int vertexSize = 5;

    private final Material material;
    private final Vector3 worldTranslation = new Vector3();
    private float[] vertices;
    /** Max amount of rectangular faces that can fit into the mesh. */
    private int maxFaces;
    /** Current amount of rectangular faces in the mesh (or buffer if after begin() but before end()). */
    private int faces = 0;
    private Mesh mesh;

    /** Note that this is a quite heavy object.
     * @param isStatic true if the mesh is not regenerated each frame/often
     * @param material of the mesh
     * @param maxFaces max rectangles to hold, can't draw more */
    public RectangleMeshBatch(boolean isStatic, Material material, int maxFaces) {
        this.material = material;
        this.maxFaces = maxFaces;
        final int maxIndices = facesToIndices(maxFaces);
        mesh = new Mesh(isStatic, facesToVertices(maxFaces), maxIndices, attributes);
        vertices = new float[facesToVertices(maxFaces) * vertexSize];
        mesh.setIndices(getIndices(maxIndices), 0, maxIndices);
    }

    /** Set the world translation of renderables of this mesh */
    public void setWorldTranslation(float x, float y, float z){
        worldTranslation.set(x, y, z);
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

    /** Return the amount of vertices that corresponds to given amount of rectangular faces. */
    private static int facesToVertices(int faces){
        return faces * 4;
    }

    /** Return the amount of indices that corresponds to given amount of rectangular faces. */
    private static int facesToIndices(int faces){
        return faces * 6;
    }

    /** Clear everything rendered and begin to add new shapes.
     * Do not call if already called and not {@link #end()}ed. */
    public void begin(){
        faces = 0;
    }

    /** Draw a block at given world coordinates.
     * Must be called between begin() and end().
     * @param faceMask occlusion mask, only faces which are not occluded will be drawn
     *                 (see {@link darkyenus.blockotron.world.Chunk#getOcclusionMask(int, int, int)})
     * @param texture of all faces */
    public void createBlock (int x, int y, int z, byte faceMask, BlockFaceTexture texture){
        if((faceMask & Side.east) == 0) createBlockFace(x, y, z, EAST_FACE_OFFSETS, texture);
        if((faceMask & Side.west) == 0) createBlockFace(x, y, z, WEST_FACE_OFFSETS, texture);
        if((faceMask & Side.north) == 0) createBlockFace(x, y, z, NORTH_FACE_OFFSETS, texture);
        if((faceMask & Side.south) == 0) createBlockFace(x, y, z, SOUTH_FACE_OFFSETS, texture);
        if((faceMask & Side.top) == 0) createBlockFace(x, y, z, TOP_FACE_OFFSETS, texture);
        if((faceMask & Side.bottom) == 0) createBlockFace(x, y, z, BOTTOM_FACE_OFFSETS, texture);
    }

    /** @see #createBlock(int, int, int, byte, BlockFaceTexture) */
    public void createBlock (int x, int y, int z, byte faceMask, BlockFaceTexture top, BlockFaceTexture sides, BlockFaceTexture bottom){
        if((faceMask & Side.east) == 0) createBlockFace(x, y, z, EAST_FACE_OFFSETS, sides);
        if((faceMask & Side.west) == 0) createBlockFace(x, y, z, WEST_FACE_OFFSETS, sides);
        if((faceMask & Side.north) == 0) createBlockFace(x, y, z, NORTH_FACE_OFFSETS, sides);
        if((faceMask & Side.south) == 0) createBlockFace(x, y, z, SOUTH_FACE_OFFSETS, sides);
        if((faceMask & Side.top) == 0) createBlockFace(x, y, z, TOP_FACE_OFFSETS, top);
        if((faceMask & Side.bottom) == 0) createBlockFace(x, y, z, BOTTOM_FACE_OFFSETS, bottom);
    }

    /** Draw a single face of a block. Most blocks should use one of createBlock() methods.
     * Must be called between begin() and end().
     * @param x (+ y,z) world coordinates of the block
     * @param faceOffsets offsets of the face vertices to the block origin (see {@link #TOP_FACE_OFFSETS} etc.)
     * @param texture to be drawn on the face */
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

    /** Update the mesh and end the edit block. */
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
        r.worldTransform.setToTranslation(worldTranslation);
        r.meshPart.mesh = mesh;
        r.meshPart.offset = 0;
        r.meshPart.primitiveType = GL20.GL_TRIANGLES;
        r.meshPart.size = indices;
        r.material = material;
        r.userData = null;
        renderables.add(r);
    }

    /** Release mesh. Instance can't be used anymore after this is called. */
    public void dispose(){
        mesh.dispose();
    }

    public static final float[] EAST_FACE_OFFSETS = {
            1, 1, 0,
            1, 1, 1,
            1, 0, 1,
            1, 0, 0
    };

    public static final float[] WEST_FACE_OFFSETS = {
            0, 0, 0,
            0, 0, 1,
            0, 1, 1,
            0, 1, 0
    };

    public static final float[] NORTH_FACE_OFFSETS = {
            0, 1, 0,
            0, 1, 1,
            1, 1, 1,
            1, 1, 0
    };

    public static final float[] SOUTH_FACE_OFFSETS = {
            1, 0, 0,
            1, 0, 1,
            0, 0, 1,
            0, 0, 0,
    };

    public static final float[] TOP_FACE_OFFSETS = {
            0, 0, 1,
            1, 0, 1,
            1, 1, 1,
            0, 1, 1
    };

    public static final float[] BOTTOM_FACE_OFFSETS = {
            0, 1, 0,
            1, 1, 0,
            1, 0, 0,
            0, 0, 0
    };
}
