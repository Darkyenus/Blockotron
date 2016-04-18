package darkyenus.blockotron.render;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.DefaultRenderableSorter;

/**
 * Extended version of basic renderable sorter which supports multiple sorting levels denoted by
 * Integer instance passed as userData. Positive levels will be drawn on top.
 */
public class BiasedRenderableSorter extends DefaultRenderableSorter {

    private int getBias(Object biasOrNull){
        if(biasOrNull instanceof Integer){
            return (Integer)biasOrNull;
        } else {
            return 0;
        }
    }

    @Override
    public int compare(Renderable o1, Renderable o2) {
        final int bias1 = getBias(o1.userData);
        final int bias2 = getBias(o2.userData);
        if(bias1 == bias2){
            return super.compare(o1, o2);
        } else {
            return bias1 - bias2;
        }
    }
}
