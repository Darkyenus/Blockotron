package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.World;

/**
 * Base for most simple blocks.
 */
public class SimpleBlock extends Block {

    private final BlockFaceTexture top, sides, bottom;

    public SimpleBlock(SimpleBlockBuilder builder){
        super(builder.id, builder.flags);
        this.top = builder.top;
        this.sides = builder.sides;
        this.bottom = builder.bottom;
    }

    @Override
    public final void render(World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion, int skyLight, int blockLight, RectangleMeshBatch batch) {
        batch.createBlock(drawX, drawY, drawZ, occlusion, top, sides, bottom);
    }

    @Override
    protected void initializeEntity(World world, int entity) {
        throw new UnsupportedOperationException("Subclasses with entities must override initializeEntity");
    }

    public static SimpleBlockBuilder create(String id){
        return new SimpleBlockBuilder(id);
    }

    public static SimpleBlockBuilder create(String id, int flags){
        return new SimpleBlockBuilder(id).flags(flags);
    }

    public static final class SimpleBlockBuilder {
        private final String id;
        private int flags = 0;
        private BlockFaceTexture top, sides, bottom;

        public SimpleBlockBuilder(String id) {
            this.id = id;
        }

        public SimpleBlockBuilder flags(int flags){
            this.flags |= flags;
            return this;
        }

        public SimpleBlockBuilder withTexture(String textureName){
            this.top = this.sides = this.bottom = BlockFaces.getBlockFace(textureName);
            return this;
        }

        public SimpleBlockBuilder withTopTexture(String textureName){
            this.top = BlockFaces.getBlockFace(textureName);
            return this;
        }

        public SimpleBlockBuilder withSideTexture(String textureName){
            this.sides = BlockFaces.getBlockFace(textureName);
            return this;
        }

        public SimpleBlockBuilder withBottomTexture(String textureName){
            this.bottom = BlockFaces.getBlockFace(textureName);
            return this;
        }

        public SimpleBlock build(){
            return new SimpleBlock(this);
        }
    }
}
