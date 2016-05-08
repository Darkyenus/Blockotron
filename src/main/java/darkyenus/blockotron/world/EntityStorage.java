package darkyenus.blockotron.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntIntMap;
import com.github.antag99.retinazer.Component;
import com.github.antag99.retinazer.Engine;
import com.github.antag99.retinazer.Mapper;
import darkyenus.blockotron.world.components.BlockPosition;

/**
 * Storage of unloaded entities, ready to be
 */
public final class EntityStorage {

    private final Array<Component[]> storedEntities = new Array<>(false, 128, Component[].class);

    private final Array<Component> storeEntity_TMP = new Array<>(false, 16, Component.class);

    @SuppressWarnings("unchecked")
    public void storeEntities(Engine engine, int[] entities, int entitiesSize){
        final Mapper<?>[] mappers = engine.getMappers();
        final Array<Component> entityComponents = storeEntity_TMP;

        for (int i = 0; i < entitiesSize; i++) {
            final int entity = entities[i];

            for (Mapper<?> mapper : mappers) {
                //We care only about persistent components
                if(!PersistentComponent.class.isAssignableFrom(mapper.type)) continue;

                final PersistentComponent component = (PersistentComponent) mapper.get(entity);
                if(component != null){
                    if(mapper.isPooled()){
                        final PersistentComponent persistentComponent = (PersistentComponent) mapper.createComponent();
                        component.copyTo(persistentComponent);
                        entityComponents.add(persistentComponent);
                    } else {
                        entityComponents.add(component);
                    }
                }
            }

            if(entityComponents.size != 0){
                storedEntities.add(entityComponents.toArray());
                entityComponents.size = 0;//Faster clear
            }

            engine.destroyEntity(entity);
        }
    }

    @SuppressWarnings("unchecked")
    public void storeEntities(Engine engine, IntIntMap.Values entities){
        final Mapper<?>[] mappers = engine.getMappers();
        final Array<Component> entityComponents = storeEntity_TMP;

        while(entities.hasNext){
            final int entity = entities.next();

            for (Mapper<?> mapper : mappers) {
                //We care only about persistent components
                if(!PersistentComponent.class.isAssignableFrom(mapper.type)) continue;

                final PersistentComponent component = (PersistentComponent) mapper.get(entity);
                if(component != null){
                    if(mapper.isPooled()){
                        final PersistentComponent persistentComponent = (PersistentComponent) mapper.createComponent();
                        component.copyTo(persistentComponent);
                        entityComponents.add(persistentComponent);
                    } else {
                        entityComponents.add(component);
                    }
                }
            }

            if(entityComponents.size != 0){
                storedEntities.add(entityComponents.toArray());
                entityComponents.size = 0;//Faster clear
            }

            engine.destroyEntity(entity);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadEntities(Engine engine) {
        for (Component[] storedEntity : storedEntities) {
            final int entity = engine.createEntity();
            for (Component component : storedEntity) {
                ((Mapper) engine.getMapper(component.getClass())).add(entity, component);
            }
        }
        storedEntities.clear();
    }
    
    public boolean removeBlockEntity(int x, int y, int z){
        final Component[][] entities = storedEntities.items;
        final int entitiesSize = storedEntities.size;
        for (int i = 0; i < entitiesSize; i++) {
            final Component[] entity = entities[i];
            for (Component component : entity) {
                if(component instanceof BlockPosition){
                    final BlockPosition blockPosition = (BlockPosition) component;
                    if(blockPosition.x == x && blockPosition.y == y && blockPosition.z == z){
                        storedEntities.removeIndex(i);
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public interface PersistentComponent<Self> extends Component {
        void copyTo(Self c);
    }
}
