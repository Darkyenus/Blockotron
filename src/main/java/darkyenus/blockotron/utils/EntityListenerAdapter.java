package darkyenus.blockotron.utils;

import com.badlogic.gdx.utils.IntArray;
import com.github.antag99.retinazer.EntityListener;
import com.github.antag99.retinazer.EntitySet;

/**
 * Entity listener which iterates through all entities and calls methods separately
 */
public abstract class EntityListenerAdapter implements EntityListener {

    @Override
    public final void inserted(EntitySet entities) {
        final IntArray indices = entities.getIndices();
        final int[] entityIDs = indices.items;
        final int size = indices.size;

        for (int i = 0; i < size; i++) {
            final int entity = entityIDs[i];
            inserted(entity);
        }
    }

    @Override
    public final void removed(EntitySet entities) {
        final IntArray indices = entities.getIndices();
        final int[] entityIDs = indices.items;
        final int size = indices.size;

        for (int i = 0; i < size; i++) {
            final int entity = entityIDs[i];
            removed(entity);
        }
    }

    protected abstract void inserted(int entity);
    protected abstract void removed(int entity);
}
