package darkyenus.blockotron.utils;

import com.badlogic.gdx.utils.reflect.Field;
import com.github.antag99.retinazer.Engine;
import com.github.antag99.retinazer.WireResolver;

/**
 * Wire resolver that tries to fill the fields from given possibilities.
 */
public class SelectionWireResolver implements WireResolver {

    private final Object[] possibilities;

    public SelectionWireResolver(Object...possibilities) {
        this.possibilities = possibilities;
    }

    @Override
    public boolean wire(Engine engine, Object object, Field field) throws Throwable {
        final Class type = field.getType();
        for (Object possibility : possibilities) {
            if(type.isInstance(possibility)){
                field.set(object, possibility);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean unwire(Engine engine, Object object, Field field) throws Throwable {
        final Object contained = field.get(object);
        for (Object possibility : possibilities) {
            if(possibility == contained){
                field.set(object, null);
                return true;
            }
        }
        return false;
    }
}
