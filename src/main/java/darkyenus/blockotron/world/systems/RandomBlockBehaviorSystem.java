package darkyenus.blockotron.world.systems;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.IntArray;
import com.github.antag99.retinazer.*;
import darkyenus.blockotron.world.World;
import darkyenus.blockotron.world.components.BlockPosition;
import darkyenus.blockotron.world.components.RandomBlockBehavior;

/**
 *
 */
@SkipWire
public class RandomBlockBehaviorSystem extends EntityProcessorSystem {

    /** Blocks will be tested for random events in this interval. */
    private static final float TRIGGER_TIMESTEP_SEC = 1f / 20f;
    /** Each trigger will only serve a fraction of the entities, for performance. */
    private static final int TRIGGER_PROCESS_JUMP = 8;
    /** Probability counts with this TRIGGER_TIMESTEP_SEC, which is corrected to amount for TRIGGER_PROCESS_JUMP. */
    private static final float JUMP_CORRECTED_TRIGGER_TIMESTEP_SEC = TRIGGER_PROCESS_JUMP * TRIGGER_TIMESTEP_SEC;

    @Wire
    private World world;

    @Wire
    private Mapper<RandomBlockBehavior> randomBlockBehaviorMapper;

    @Wire
    private Mapper<BlockPosition> blockPositionMapper;

    private final RandomXS128 random = new RandomXS128(System.nanoTime());

    private float nextTickCountdown = 0f;
    private int jumpOffset = 0;

    public RandomBlockBehaviorSystem() {
        super(Family.with(BlockPosition.class, RandomBlockBehavior.class));
    }

    @Override
    protected void processEntities(float delta) {
        final IntArray indices = getEntities().getIndices();
        final int[] items = indices.items;
        final int size = indices.size;

        nextTickCountdown -= delta;
        while(nextTickCountdown < 0){
            for (int i = jumpOffset; i < size; i += TRIGGER_PROCESS_JUMP) {
                process(items[i], delta);
            }
            jumpOffset = (jumpOffset + 1) % TRIGGER_PROCESS_JUMP;
            nextTickCountdown += TRIGGER_TIMESTEP_SEC;
        }
    }

    @Override
    protected void process(int entity, float delta) {
        final RandomBlockBehavior behavior = randomBlockBehaviorMapper.get(entity);
        if(random.nextFloat() < JUMP_CORRECTED_TRIGGER_TIMESTEP_SEC / behavior.halfLife){
            final BlockPosition position = blockPositionMapper.get(entity);
            behavior.behavior.act(world, position.x, position.y, position.z, entity);
        }
    }
}
