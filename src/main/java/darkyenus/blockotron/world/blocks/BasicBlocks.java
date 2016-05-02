package darkyenus.blockotron.world.blocks;

import com.badlogic.gdx.math.MathUtils;
import darkyenus.blockotron.world.Block;
import darkyenus.blockotron.world.EntityArchetype;
import darkyenus.blockotron.world.World;
import darkyenus.blockotron.world.components.RandomBlockBehavior;

import static darkyenus.blockotron.world.Block.*;

/**
 * Collection of all basic blocks
 */
public final class BasicBlocks {
    public static final Block GRASS = SimpleBlock.create("grass", OCCLUDING | COLLIDABLE)
            .withTopTexture("grass_top").withSideTexture("grass_side").withBottomTexture("grass_bottom")
            .withEntity(new EntityArchetype() {

                private final RandomBlockBehavior.Behavior GRASS_BEHAVIOR = (world, worldX, worldY, worldZ, entity) -> {
                    worldX += MathUtils.randomSign();
                    worldY += MathUtils.randomSign();
                    worldZ += MathUtils.randomSign();

                    final Block maybeDirt = world.getLoadedBlock(worldX, worldY, worldZ);
                    if(maybeDirt == BasicBlocks.DIRT && world.getLoadedBlock(worldX, worldY, worldZ + 1).isTransparent()){
                        world.setBlock(worldX, worldY, worldZ, GRASS);
                    }
                };

                @Override
                public void populate(World world, int entity) {
                    final RandomBlockBehavior randomBlockBehavior = world.entityEngine().getMapper(RandomBlockBehavior.class).create(entity);
                    randomBlockBehavior.halfLife = 30f;
                    randomBlockBehavior.behavior = GRASS_BEHAVIOR;
                }
            }).build();
    public static final Block DIRT = SimpleBlock.create("dirt", OCCLUDING | COLLIDABLE).withTexture("grass_bottom").build();
    public static final Block GLASS = SimpleBlock.create("glass", TRANSPARENT | OCCLUDING | COLLIDABLE).withTexture("glass").build();

    public static final Block WOOD_LOG = SimpleBlock.create("wood_log", OCCLUDING | COLLIDABLE).withTexture("wood_top").withSideTexture("wood_side").build();
    public static final Block LEAVES = SimpleBlock.create("leaves", TRANSPARENT | OCCLUDING | COLLIDABLE).withTexture("leaves").build();
}
