package darkyenus.blockotron.world.systems;

import com.badlogic.gdx.math.RandomXS128;
import com.github.antag99.retinazer.*;
import darkyenus.blockotron.world.World;
import darkyenus.blockotron.world.components.BlockPosition;
import darkyenus.blockotron.world.components.RandomBlockBehavior;

/**
 *
 */
@SkipWire
public class RandomBlockBehaviorSystem extends EntityProcessorSystem {

    private static final float TRIGGER_TIMESTEP_SEC = 1f / 20f;

    @Wire
    private World world;

    @Wire
    private Mapper<RandomBlockBehavior> randomBlockBehaviorMapper;

    @Wire
    private Mapper<BlockPosition> blockPositionMapper;

    private final RandomXS128 random = new RandomXS128(System.nanoTime());

    private float nextTickCountdown = 0f;

    public RandomBlockBehaviorSystem() {
        super(Family.with(BlockPosition.class, RandomBlockBehavior.class));
    }

    @Override
    protected void processEntities(float delta) {
        nextTickCountdown -= delta;
        while(nextTickCountdown < 0){
            super.processEntities(delta);
            nextTickCountdown += TRIGGER_TIMESTEP_SEC;
        }
    }

    @Override
    protected void process(int entity, float delta) {
        final RandomBlockBehavior behavior = randomBlockBehaviorMapper.get(entity);
        if(random.nextFloat() < TRIGGER_TIMESTEP_SEC / behavior.halfLife){
            final BlockPosition position = blockPositionMapper.get(entity);
            behavior.behavior.act(world, position.x, position.y, position.z, entity);
        }
    }
}
