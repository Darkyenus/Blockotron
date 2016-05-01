package darkyenus.blockotron.world.components;

import com.github.antag99.retinazer.Component;
import darkyenus.blockotron.world.World;

/**
 *
 */
public final class RandomBlockBehavior implements Component {

    public float halfLife = Float.MAX_VALUE;
    public Behavior behavior;

    public interface Behavior {
        void act(World world, int worldX, int worldY, int worldZ, int entity);
    }

}
