package darkyenus.blockotron.render;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Pool;
import darkyenus.blockotron.world.Side;

import java.nio.FloatBuffer;

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
            VertexAttribute.TexCoords(0),//2
            VertexAttribute.ColorPacked()//1
    );
    private final static int vertexSize = 6;
    private final static float white = Color.WHITE.toFloatBits();

    private final Material opaqueMaterial, transparentMaterial;
    private final Vector3 worldTranslation = new Vector3();
    /** Opaque and transparent vertices. Opaque are filled from 0, transparent from the end, but not in reverse. */
    private float[] vertices;
    /** LXYZLXYZ... encoded positions and length of transparent vertices.
     * L = meshPart.size
     * X,Y,Z = worldTransform */
    private float[] transparentMeshPositions;
    private static final int TRANSPARENT_MESH_POS_STRIDE = 4;
    /** Max amount of rectangular faces that can fit into the mesh, opaque or transparent */
    private int maxMeshFaces;
    /** Current amount of rectangular faces in the mesh (or buffer if after begin() but before end()). */
    private int opaqueFaces = 0, transparentFaces = 0, transparentBatches = 0;
    private final boolean isStatic;
    private Mesh mesh;

    private int tBaseX, tBaseY, tBaseZ;
    private int batchedTransparent;
    private boolean drawingTransparent;

    /** Note that this is a quite heavy object.
     * @param isStatic true if the mesh is not regenerated each frame/often
     * @param opaqueMaterial of the opaque part of the mesh
     * @param transparentMaterial of the transparent part of the mesh */
    public RectangleMeshBatch(boolean isStatic, Material opaqueMaterial, Material transparentMaterial, int initialMaxFaces) {
        this.isStatic = isStatic;
        this.opaqueMaterial = opaqueMaterial;
        this.transparentMaterial = transparentMaterial;
        //Enlarge to the next power of two for efficiency (kept as is if already POT)
        initialMaxFaces = MathUtils.nextPowerOfTwo(initialMaxFaces);
        this.maxMeshFaces = initialMaxFaces;

        final int maxIndices = facesToIndices(initialMaxFaces);
        mesh = new Mesh(isStatic, facesToVertices(initialMaxFaces), maxIndices, attributes);
        vertices = new float[facesToVertices(initialMaxFaces) * vertexSize];
        mesh.setIndices(getIndices(maxIndices), 0, maxIndices);
    }

    /** Set the world translation of renderables of this mesh */
    public void setWorldTranslation(float x, float y, float z){
        worldTranslation.set(x, y, z);
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
        opaqueFaces = 0;
        transparentFaces = 0;
        transparentBatches = 0;
    }

    /** Faces drawn between begin/endTransparent will be ordered as if they were on these coordinates. */
    public void beginTransparent(int baseX, int baseY, int baseZ) {
        assert !drawingTransparent;
        tBaseX = baseX;
        tBaseY = baseY;
        tBaseZ = baseZ;
        batchedTransparent = 0;
        drawingTransparent = true;
    }

    /** Allows to draw opaque blocks inside begin/endTransparent.
     * Must be matched with resumeTransparent, before endTransparent! */
    public void pauseTransparent(){
        assert drawingTransparent;
        drawingTransparent = false;
    }

    /** @see #pauseTransparent() */
    public void resumeTransparent(){
        assert !drawingTransparent;
        drawingTransparent = true;
    }

    public void endTransparent() {
        assert drawingTransparent;
        drawingTransparent = false;
        if(batchedTransparent > 0){
            int off = transparentBatches * TRANSPARENT_MESH_POS_STRIDE;
            final int additionalLength = TRANSPARENT_MESH_POS_STRIDE;

            final float[] tMePos;

            //Resize transparentMeshPositions if needed
            if(transparentMeshPositions == null){
                assert off == 0;
                tMePos = transparentMeshPositions = new float[MathUtils.nextPowerOfTwo(additionalLength)];
            } else if (transparentMeshPositions.length < off + additionalLength){
                final float[] newTransparentMeshPositions = new float[MathUtils.nextPowerOfTwo(off + additionalLength)];
                System.arraycopy(transparentMeshPositions, 0, newTransparentMeshPositions, 0, off);
                tMePos = transparentMeshPositions = newTransparentMeshPositions;
            } else {
                tMePos = transparentMeshPositions;
            }

            tMePos[off++] = facesToIndices(batchedTransparent);
            tMePos[off++] = tBaseX;
            tMePos[off++] = tBaseY;
            tMePos[off] = tBaseZ;

            batchedTransparent = 0;
            transparentBatches++;
        }
    }

    /** Draw a block at given world coordinates.
     * Must be called between begin() and end().
     * @param faceMask occlusion mask, only faces which are not occluded will be drawn
     *                 (see {@link darkyenus.blockotron.world.Chunk#getOcclusionMask(int, int, int)})
     * @param texture of all faces */
    public void createBlock (int x, int y, int z, byte faceMask, BlockFaceTexture texture) {
        if((faceMask & Side.east) != 0) createBlockFace(x, y, z, EAST_FACE_OFFSETS, texture);
        if((faceMask & Side.west) != 0) createBlockFace(x, y, z, WEST_FACE_OFFSETS, texture);
        if((faceMask & Side.north) != 0) createBlockFace(x, y, z, NORTH_FACE_OFFSETS, texture);
        if((faceMask & Side.south) != 0) createBlockFace(x, y, z, SOUTH_FACE_OFFSETS, texture);
        if((faceMask & Side.top) != 0) createBlockFace(x, y, z, TOP_FACE_OFFSETS, texture);
        if((faceMask & Side.bottom) != 0) createBlockFace(x, y, z, BOTTOM_FACE_OFFSETS, texture);
    }

    /** @see #createBlock(int, int, int, byte, BlockFaceTexture) */
    public void createBlock (int x, int y, int z, byte faceMask, BlockFaceTexture top, BlockFaceTexture sides, BlockFaceTexture bottom) {
        if((faceMask & Side.east) != 0) createBlockFace(x, y, z, EAST_FACE_OFFSETS, sides);
        if((faceMask & Side.west) != 0) createBlockFace(x, y, z, WEST_FACE_OFFSETS, sides);
        if((faceMask & Side.north) != 0) createBlockFace(x, y, z, NORTH_FACE_OFFSETS, sides);
        if((faceMask & Side.south) != 0) createBlockFace(x, y, z, SOUTH_FACE_OFFSETS, sides);
        if((faceMask & Side.top) != 0) createBlockFace(x, y, z, TOP_FACE_OFFSETS, top);
        if((faceMask & Side.bottom) != 0) createBlockFace(x, y, z, BOTTOM_FACE_OFFSETS, bottom);
    }

    /** Draw a single face of a block. Most blocks should use one of createBlock() methods.
     * Must be called between begin() and end().
     * @param x (+ y,z) world coordinates of the block
     * @param faceOffsets offsets of the face vertices to the block origin (see {@link #TOP_FACE_OFFSETS} etc.)
     * @param texture to be drawn on the face */
    public void createBlockFace (int x, int y, int z, float[] faceOffsets, BlockFaceTexture texture){
        if(opaqueFaces + transparentFaces + 1 > maxMeshFaces){
            resizeMesh(opaqueFaces + transparentFaces + 1);
        }

        //Vertices
        final float[] v = vertices;
        int vertexOffset;
        if(drawingTransparent){
            vertexOffset = v.length - (facesToVertices(transparentFaces + 1) * vertexSize);
            transparentFaces++;
            batchedTransparent++;
            x -= tBaseX;
            y -= tBaseY;
            z -= tBaseZ;
        } else {
            vertexOffset = facesToVertices(opaqueFaces) * vertexSize;
            opaqueFaces++;
        }

        //Fill vertices
        int faceOffset = 0;
        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset++];
        v[vertexOffset++] = texture.u2;
        v[vertexOffset++] = texture.v2;
        v[vertexOffset++] = white;

        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset++];
        v[vertexOffset++] = texture.u2;
        v[vertexOffset++] = texture.v;
        v[vertexOffset++] = white;

        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset++];
        v[vertexOffset++] = texture.u;
        v[vertexOffset++] = texture.v;
        v[vertexOffset++] = white;

        v[vertexOffset++] = x + faceOffsets[faceOffset++];
        v[vertexOffset++] = y + faceOffsets[faceOffset++];
        v[vertexOffset++] = z + faceOffsets[faceOffset];
        v[vertexOffset++] = texture.u;
        v[vertexOffset++] = texture.v2;
        v[vertexOffset] = white;
    }

    /** Draw a single face of a block. Advanced parameters.
     * Must be called between begin() and end().
     * @param x (+ y,z) world coordinates of the block
     * @param faceOffsets offsets of the face vertices to the block origin (see {@link #TOP_FACE_OFFSETS} etc.)
     * @param texture to be drawn on the face
     * @param sclX (+ sclY, sclZ) scale of the face offsets */
    public void createBlockFace (float x, float y, float z, float[] faceOffsets, BlockFaceTexture texture,
                                 float sclX, float sclY, float sclZ){
        if(opaqueFaces + transparentFaces + 1 > maxMeshFaces){
            resizeMesh(opaqueFaces + transparentFaces + 1);
        }

        //Vertices
        final float[] v = vertices;
        int vertexOffset;
        if(drawingTransparent){
            vertexOffset = v.length - (facesToVertices(transparentFaces + 1) * vertexSize);
            transparentFaces++;
            batchedTransparent++;
            x -= tBaseX;
            y -= tBaseY;
            z -= tBaseZ;
        } else {
            vertexOffset = facesToVertices(opaqueFaces) * vertexSize;
            opaqueFaces++;
        }

        //Fill vertices
        int faceOffset = 0;
        v[vertexOffset++] = x + faceOffsets[faceOffset++] * sclX;
        v[vertexOffset++] = y + faceOffsets[faceOffset++] * sclY;
        v[vertexOffset++] = z + faceOffsets[faceOffset++] * sclZ;
        v[vertexOffset++] = texture.u2;
        v[vertexOffset++] = texture.v2;
        v[vertexOffset++] = white;

        v[vertexOffset++] = x + faceOffsets[faceOffset++] * sclX;
        v[vertexOffset++] = y + faceOffsets[faceOffset++] * sclY;
        v[vertexOffset++] = z + faceOffsets[faceOffset++] * sclZ;
        v[vertexOffset++] = texture.u2;
        v[vertexOffset++] = texture.v;
        v[vertexOffset++] = white;

        v[vertexOffset++] = x + faceOffsets[faceOffset++] * sclX;
        v[vertexOffset++] = y + faceOffsets[faceOffset++] * sclY;
        v[vertexOffset++] = z + faceOffsets[faceOffset++] * sclZ;
        v[vertexOffset++] = texture.u;
        v[vertexOffset++] = texture.v;
        v[vertexOffset++] = white;

        v[vertexOffset++] = x + faceOffsets[faceOffset++] * sclX;
        v[vertexOffset++] = y + faceOffsets[faceOffset++] * sclY;
        v[vertexOffset++] = z + faceOffsets[faceOffset] * sclZ;
        v[vertexOffset++] = texture.u;
        v[vertexOffset++] = texture.v2;
        v[vertexOffset] = white;
    }

    /** Update the mesh and end the edit block. */
    public void end(){
        final int opaqueVerticesSize = facesToVertices(opaqueFaces) * vertexSize;
        final int transparentVerticesSize = facesToVertices(transparentFaces) * vertexSize;

        // Assign vertices
        // This has to be done manually, because it is quite advanced usage
        final FloatBuffer vertexBuf = mesh.getVerticesBuffer();
        // Assign opaque vertices from beginning of vertices to beginning of vertexBuf
        if(opaqueVerticesSize != 0){
            BufferUtils.copy(vertices, vertexBuf, opaqueVerticesSize, 0);
        }
        // Assign transparent vertices from the end of vertices buffer to the position after opaque vertices in vertexBuf
        if(transparentVerticesSize != 0){
            vertexBuf.position(opaqueVerticesSize);
            BufferUtils.copy(vertices, vertices.length - transparentVerticesSize, vertexBuf, transparentVerticesSize);
        }
    }

    /** Enlarge the buffer in power of two sizes until this value (in faces) */
    private static final int POT_STEPS_THRESHOLD = 1024;
    /** When POT_STEPS_THRESHOLD is reached, enlarge the mesh in steps this big. */
    private static final int POST_POT_SIZE_STEPS = 512;

    private void resizeMesh(int totalFacesRequired){
        int newMaxMeshFaces = maxMeshFaces;
        while(newMaxMeshFaces < totalFacesRequired && newMaxMeshFaces < POT_STEPS_THRESHOLD){
            newMaxMeshFaces = newMaxMeshFaces << 1;
        }
        while(newMaxMeshFaces < totalFacesRequired){
            newMaxMeshFaces += POST_POT_SIZE_STEPS;
        }

        //System.out.println("Resizing mesh from "+maxMeshFaces+" to "+newMaxMeshFaces);

        final int maxIndices = facesToIndices(newMaxMeshFaces);
        final Mesh newMesh = new Mesh(isStatic, facesToVertices(newMaxMeshFaces), maxIndices, attributes);
        final float[] newVertices = new float[facesToVertices(newMaxMeshFaces) * vertexSize];
        newMesh.setIndices(getIndices(maxIndices), 0, maxIndices);

        //Copy existing data
        final int opaqueVerticesSize = facesToVertices(opaqueFaces) * vertexSize;
        final int transparentVerticesSize = facesToVertices(transparentFaces) * vertexSize;

        final float[] oldVertices = this.vertices;
        System.arraycopy(oldVertices, 0, newVertices, 0, opaqueVerticesSize);
        System.arraycopy(oldVertices, oldVertices.length - transparentVerticesSize, newVertices, newVertices.length - transparentVerticesSize, transparentVerticesSize);

        //Dispose old and replace with new data
        this.mesh.dispose();
        this.mesh = newMesh;
        this.vertices = newVertices;
        this.maxMeshFaces = newMaxMeshFaces;
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        //Opaque
        final int opaqueIndicesSize = facesToIndices(opaqueFaces);
        if(opaqueIndicesSize != 0){
            final Renderable r = pool.obtain();
            r.worldTransform.setToTranslation(worldTranslation);
            r.meshPart.mesh = mesh;
            r.meshPart.offset = 0;
            r.meshPart.primitiveType = GL20.GL_TRIANGLES;
            r.meshPart.size = opaqueIndicesSize;
            r.material = opaqueMaterial;
            r.userData = null;
            renderables.add(r);
        }
        //Transparent
        final int transparentBatches = this.transparentBatches;
        if(transparentBatches != 0){
            final int transparentIndicesSize = facesToIndices(transparentFaces);
            final float[] transparentMeshPositions = this.transparentMeshPositions;

            int baseOffset = opaqueIndicesSize + transparentIndicesSize;

            for (int batch = 0; batch < transparentBatches; batch++) {
                final Renderable r = pool.obtain();
                r.worldTransform.setToTranslation(
                        transparentMeshPositions[(batch << 2) + 1],
                        transparentMeshPositions[(batch << 2) + 2],
                        transparentMeshPositions[(batch << 2) + 3]).translate(worldTranslation);
                r.meshPart.mesh = mesh;
                r.meshPart.primitiveType = GL20.GL_TRIANGLES;
                final int size = (int) transparentMeshPositions[batch << 2];
                baseOffset -= size;
                r.meshPart.offset = baseOffset;
                r.meshPart.size = size;
                r.material = transparentMaterial;
                r.userData = null;
                renderables.add(r);
            }
        }
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

    private static short[] indicesCache;
    /** Since all indices are the same, we generate them once and then serve cached version.
     * Cached version may be larger than what is requested, so be prepared to handle that. */
    private static short[] getIndices(int length){
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
}
