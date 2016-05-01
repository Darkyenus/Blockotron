package darkyenus.blockotron.world.blocks;

import com.badlogic.gdx.math.MathUtils;
import darkyenus.blockotron.render.BlockFaceTexture;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.RectangleMeshBatch;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.World;
import darkyenus.blockotron.world.components.RandomBlockBehavior;

/**
 *
 */
public class Grass extends Block {

    public static final Grass GRASS = new Grass();

    private static final BlockFaceTexture TOP = BlockFaces.getBlockFace("grass_top");
    private static final BlockFaceTexture SIDE = BlockFaces.getBlockFace("grass_side");
    private static final BlockFaceTexture BOTTOM = BlockFaces.getBlockFace("grass_bottom");

    private Grass() {
        super("grass", OCCLUDING | COLLIDABLE | HAS_ENTITY);
    }

    @Override
    public void render(World world, int x, int y, int z, int drawX, int drawY, int drawZ, byte occlusion, RectangleMeshBatch batch) {
        batch.createBlock(drawX, drawY, drawZ, occlusion, TOP, SIDE, BOTTOM);
    }

    private static final RandomBlockBehavior.Behavior GRASS_BEHAVIOR = (world, worldX, worldY, worldZ, entity) -> {
        worldX += MathUtils.randomSign();
        worldY += MathUtils.randomSign();
        worldZ += MathUtils.randomSign();

        final Block maybeDirt = world.getLoadedBlock(worldX, worldY, worldZ);
        if(maybeDirt == null) return;
        if(maybeDirt == Dirt.DIRT && world.getBlock(worldX, worldY, worldZ + 1).isTransparent()){
            world.setBlock(worldX, worldY, worldZ, GRASS);
        }
    };

    @Override
    protected final void initializeEntity(World world, int entity) {
        final RandomBlockBehavior randomBlockBehavior = world.entityEngine().getMapper(RandomBlockBehavior.class).create(entity);
        randomBlockBehavior.halfLife = 30f;
        randomBlockBehavior.behavior = GRASS_BEHAVIOR;
    }
}
