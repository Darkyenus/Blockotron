package darkyenus.blockotron.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import darkyenus.blockotron.client.Game;

/**
 * Singleton which takes care of loading block face textures.
 *
 * Separate images are loaded from "terrain" directory in resource root and stitched together to a texture atlas.
 * Currently supports only single page texture atlases.
 */
public final class BlockFaces {

    private static final String LOG = "BlockFaces";

    private static final TextureAttribute textureAttribute = TextureAttribute.createDiffuse((Texture)null);
    public static final Material opaqueMaterial = new Material("blockOpaque", textureAttribute);
    public static final Material transparentMaterial = new Material("blockTransparent", textureAttribute, new BlendingAttribute());

    /** XY size of single texture face in pixels. This class can handle other sizes, but is optimized for this one. */
    private static final int BLOCK_FACE_SIZE = 16;
    /** Amount of standard size ({@link #BLOCK_FACE_SIZE}) faces in one dimension of texture atlas. */
    private static final int BLOCK_FACE_DENSITY = 32;
    /** Size of atlas page single atlas page in pixels */
    private static final int ATLAS_SIZE = BLOCK_FACE_SIZE * BLOCK_FACE_DENSITY;
    /** Value added to UV coordinates to offset textures from neighbors to prevent bleeding. */
    private static final float OFFSET_PX = 0.1f / ATLAS_SIZE;

    /** Holds texture region of a block face that could not be loaded. */
    private static final BlockFaceTexture missingBlockFace = new BlockFaceTexture("~missing~");
    /** ID - BlockTextureFace map of already packed textures. ID is the name of the region and of the file. */
    private static final ObjectMap<String, BlockFaceTexture> loadedBlockFaces = new ObjectMap<>();
    /** Responsible for packing all faces into one texture. */
    private static final PixmapPacker blockFacePacker = new PixmapPacker(ATLAS_SIZE, ATLAS_SIZE, Pixmap.Format.RGBA8888, 0, false, new PixmapPacker.SkylineStrategy());
    /** True if new textures have been packed since last  {@link #update()} call. */
    private static boolean atlasDirty = true;
    /** Atlas holding the packed images. Only first page is used. */
    private static final TextureAtlas blockFaceAtlas = new TextureAtlas();

    /** Directory from which the faces are loaded. */
    private static final FileHandle terrainDirectory = Game.getResourceRoot().child("terrain");

    /** Must be called before any other method. */
    public static void initialize(){
        final Pixmap missingImage = new Pixmap(BLOCK_FACE_SIZE, BLOCK_FACE_SIZE, Pixmap.Format.RGBA8888);
        missingImage.setColor(Color.MAGENTA);
        missingImage.fillRectangle(0,0,BLOCK_FACE_SIZE, BLOCK_FACE_SIZE);
        missingImage.setColor(Color.BLACK);
        missingImage.drawLine(0,0,BLOCK_FACE_SIZE-1,BLOCK_FACE_SIZE-1);
        missingImage.drawLine(0,BLOCK_FACE_SIZE-1,BLOCK_FACE_SIZE-1,0);
        packFace(missingBlockFace, missingImage);
    }

    /** Use this to retrieve faces for blocks.
     * If not already loaded, loads respective image from [resource root]/terrain/[id].png,
     * packs it and returns respective BlockFaceTexture.
     * Calling multiple times with same id is not expensive, but caching is preferred.
     * If the image does not exist or can't be loaded, placeholder image is used instead.
     *
     * Can be called during initialization, but is not thread safe. */
    public static BlockFaceTexture getBlockFace(String id){
        final BlockFaceTexture loaded = loadedBlockFaces.get(id);
        if(loaded != null){
            return loaded;
        } else {
            final BlockFaceTexture created = new BlockFaceTexture(id);
            loadedBlockFaces.put(id, created);

            final FileHandle image = terrainDirectory.child(id + ".png");
            if(!image.exists()){
                Gdx.app.error(LOG, "Failed to load texture: "+image);
                created.set(missingBlockFace);
            } else {
                try {
                    final Pixmap pixmap = new Pixmap(image);
                    packFace(created, pixmap);
                } catch (GdxRuntimeException e) {
                    Gdx.app.error(LOG, "Failed to load texture: "+image, e);
                    created.set(missingBlockFace);
                }
            }
            return created;
        }
    }

    /** Pack given pixmap, dispose it and populate given blockFaceTexture with correct UV. */
    private static void packFace(BlockFaceTexture blockFaceTexture, Pixmap image){
        final Rectangle rectangle = blockFacePacker.pack(blockFaceTexture.id, image);
        image.dispose();
        blockFaceTexture.set(
                rectangle.x / ATLAS_SIZE + OFFSET_PX,
                rectangle.y / ATLAS_SIZE + OFFSET_PX,
                (rectangle.x + rectangle.width) / ATLAS_SIZE - OFFSET_PX,
                (rectangle.y + rectangle.height) / ATLAS_SIZE - OFFSET_PX);
        atlasDirty = true;
    }

    /** Should be called each frame, or at least after loading new faces, on render thread, to update the texture data. */
    public static void update(){
        if(atlasDirty){
            blockFacePacker.updateTextureAtlas(blockFaceAtlas, Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest, false);
            final Array<PixmapPacker.Page> pages = blockFacePacker.getPages();
            if(pages.size > 1){
                Gdx.app.error(LOG, "Too much textures loaded! Some textures will be wrong.");
            }
            if(pages.size != 0){
                final Texture texture = pages.first().getTexture();
                assert texture != null;
                textureAttribute.textureDescription.texture = texture;
            }
            atlasDirty = false;
        }
    }
}
