package darkyenus.blockotron.world;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.BlockMesh;
import darkyenus.blockotron.world.resources.Resource;

/**
 * Copyright Chris Browne 2016
 */
public enum Block {
    AIR("air", true, false),
    GRASS("grass", false, false),
    DEBUG_BLOCK("debugBlock", true);

    private final BlockFaceTexture TOP;
    private final BlockFaceTexture SIDE;
    private final BlockFaceTexture BOTTOM;
    private final String name;
    private final boolean occlusionOverride;
    private final boolean transparent;
    private final boolean dynamic;

    // special-case constructor for debug blocks
    Block(String textureName, boolean occlusionOverride)
    {
        this.name = textureName;
        this.occlusionOverride = occlusionOverride;
        dynamic = false;
        transparent = false;
        TOP = SIDE = BOTTOM = BlockFaces.getBlockFace(textureName);
    }

    Block(String textureName, boolean transparent, boolean dynamic) {
        this.name = textureName;
        this.transparent = transparent;
        this.dynamic = dynamic;

        this.occlusionOverride = false;
        TOP = BlockFaces.getBlockFace(name + "_top");
        SIDE = BlockFaces.getBlockFace(name + "_side");
        BOTTOM = BlockFaces.getBlockFace(name + "_bottom");
    }

    public void render(int x, int y, int z, byte occlusion, BlockMesh mesh) {
        if(occlusionOverride) {
            occlusion = (byte)0;
        }
        if(!transparent) {
            mesh.createBlock(x, y, z, occlusion, TOP, SIDE, BOTTOM);
        }
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public boolean isTransparent() {
        return transparent;
    }
}
