package darkyenus.blockotron.world.blocks;

import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.EntityArchetype;
import darkyenus.blockotron.world.World;

/**
 * Base for most simple blocks.
 */
public final class SimpleBlock extends Block {

    private final EntityArchetype entityArchetype;
    private final BlockFaceTexture top, sides, bottom;

    public SimpleBlock(String id, int flags, EntityArchetype archetype, BlockFaceTexture top, BlockFaceTexture sides, BlockFaceTexture bottom) {
        super(id, flags);
        assert (archetype != null) == hasEntity() : "Archetype flag mismatch";
        this.entityArchetype = archetype;
        this.top = top;
        this.sides = sides;
        this.bottom = bottom;
    }

    @Override
    public void render(World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion, RectangleMeshBatch batch) {
        batch.createBlock(drawX, drawY, drawZ, occlusion, top, sides, bottom);
    }

    @Override
    protected void initializeEntity(World world, int entity) {
        entityArchetype.populate(world, entity);
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
        private EntityArchetype archetype;
        private BlockFaceTexture top, sides, bottom;

        public SimpleBlockBuilder(String id) {
            this.id = id;
        }

        public SimpleBlockBuilder flags(int flags){
            this.flags |= flags;
            return this;
        }

        public SimpleBlockBuilder withEntity(EntityArchetype archetype){
            this.flags |= Block.HAS_ENTITY;
            this.archetype = archetype;
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
            return new SimpleBlock(id, flags, archetype, top, sides, bottom);
        }
    }
}
