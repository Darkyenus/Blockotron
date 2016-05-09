package darkyenus.blockotron.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.antag99.retinazer.Component;
import com.github.antag99.retinazer.Engine;
import com.github.antag99.retinazer.Mapper;

import java.io.IOException;

import static darkyenus.blockotron.world.Registry.idForComponent;

/**
 * Storage of unloaded entities, ready to be
 */
public final class EntityStorage implements Pool.Poolable {

    private static final Pool<EntityStorage> ENTITY_STORAGE_POOL = new Pool<EntityStorage>() {
        @Override
        protected EntityStorage newObject() {
            return new EntityStorage();
        }
    };

    public static EntityStorage obtain(){
        return ENTITY_STORAGE_POOL.obtain();
    }

    public static void free(EntityStorage entityStorage){
        ENTITY_STORAGE_POOL.free(entityStorage);
    }

    private final ResizableOutput storedEntities = new ResizableOutput(1 << 10);

    private final Array<Component> storeEntity_TMP = new Array<>(false, 16, Component.class);
    private final Input loadEntities_TMP = new Input();

    private EntityStorage() {
    }

    @SuppressWarnings("unchecked")
    public void storeEntities(World world, int[] entities, int entitiesSize){
        final Engine engine = world.entityEngine();
        final Mapper<?>[] mappers = engine.getMappers();
        final Kryo kryo = world.kryo();

        for (int i = 0; i < entitiesSize; i++) {
            final int entity = entities[i];
            storeEntity(engine, mappers, kryo, entity);
        }
    }

    @SuppressWarnings("unchecked")
    public void storeEntities(World world, IntIntMap.Values entities){
        final Engine engine = world.entityEngine();
        final Mapper<?>[] mappers = engine.getMappers();
        final Kryo kryo = world.kryo();

        while(entities.hasNext){
            final int entity = entities.next();
            storeEntity(engine, mappers, kryo, entity);
        }
    }

    @SuppressWarnings("unchecked")
    private void storeEntity(Engine engine, Mapper<?>[] mappers, Kryo kryo, int entity){
        final Array<Component> entityComponents = storeEntity_TMP;

        for (Mapper<?> mapper : mappers) {
            final Component component = mapper.get(entity);
            if(component != null){
                entityComponents.add(component);
            }
        }

        if(entityComponents.size != 0){
            final Output storedEntities = this.storedEntities;
            storedEntities.writeInt(entityComponents.size, true);
            for (Component component : entityComponents) {
                final int id = idForComponent(component.getClass());
                storedEntities.writeInt(id, true);
                kryo.writeObject(storedEntities, component);
            }

            entityComponents.size = 0;//Faster clear
        }

        engine.destroyEntity(entity);
    }

    @SuppressWarnings("unchecked")
    public void loadEntities(World world) {
        final Engine engine = world.entityEngine();
        final Kryo kryo = world.kryo();

        final Output storedEntities = this.storedEntities;
        final Input in = loadEntities_TMP;
        in.setBuffer(storedEntities.getBuffer(), 0, storedEntities.position());

        try {
            while(in.available() != 0){
                final int entity = engine.createEntity();

                final int componentCount = in.readInt(true);
                for (int i = 0; i < componentCount; i++) {
                    final int componentID = in.readInt(true);
                    final Class component = Registry.componentForID(componentID);

                    final Mapper<Component> mapper = engine.getMapper(component);

                    try {
                        final Component componentInstance = kryo.readObject(in, Registry.componentForID(componentID));
                        mapper.add(entity, componentInstance);
                    } catch (Exception ex) {
                        Gdx.app.error("EntityStorage", "Exception while reading "+componentID+" ("+Registry.componentForID(componentID)+")", ex);
                    }
                }
            }
        } catch (IOException e) {
            throw new Error("Reading from byte array, this should never happen!", e);
        }
        storedEntities.clear();
    }

    public static void saveAndFreeStorage(EntityStorage storage, Output out){
        if(storage == null){
            out.writeInt(-1);
        } else {
            final Output storedEntities = storage.storedEntities;
            out.writeInt(storedEntities.position());
            out.write(storedEntities.getBuffer(), 0, storedEntities.position());
            free(storage);
        }
    }

    public static EntityStorage obtainAndLoadStorage(Input in){
        final int length = in.readInt();
        if(length < 0) return null;
        final EntityStorage storage = obtain();

        final ResizableOutput storedEntities = storage.storedEntities;
        storedEntities.clear();
        storedEntities.read(in, length);

        return storage;
    }

    @Override
    public void reset() {
        storedEntities.clear();
    }

    private static final class ResizableOutput extends Output {

        public ResizableOutput(int bufferSize) {
            super(bufferSize);
        }

        @Override
        public boolean require(int required) throws KryoException {
            return super.require(required);
        }

        public void read(Input from, int bytes){
            require(bytes);
            final byte[] buffer = getBuffer();
            final int read = from.read(buffer, position, bytes);
            if(read != bytes) throw new KryoException("Attempted to read "+bytes+" bytes, but did load "+read+" bytes");
            position += bytes;
        }
    }
}
