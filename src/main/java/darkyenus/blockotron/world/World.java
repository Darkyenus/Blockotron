package darkyenus.blockotron.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;

/**
 *
 */
public class World {

    private final LongMap<Chunk> chunks = new LongMap<>();
    private final ChunkProvider chunkProvider;
    private final Array<WorldObserver> observers = new Array<>(false, 8, WorldObserver.class);

    public World(ChunkProvider chunkProvider) {
        this.chunkProvider = chunkProvider;
        chunkProvider.initialize(this);
    }

    public static long chunkCoordKey(int x, int y){
        return ((long)x << 32) | ((long)y & 0xFFFF_FFFFL);
    }

    /** Return chunk at given chunk-coordinates. */
    public Chunk getChunk(int x, int y){
        final long key = chunkCoordKey(x, y);
        final Chunk existing = chunks.get(key);
        if(existing == null){
            final Chunk newChunk = chunkProvider.getChunk(x, y);
            chunks.put(key, newChunk);
            for (WorldObserver observer : observers()) {
                observer.chunkLoaded(newChunk);
            }
            return newChunk;
        } else {
            return existing;
        }
    }

    public void addObserver(WorldObserver observer){
        observers.add(observer);
    }

    public Iterable<WorldObserver> observers() {
        return observers;
    }

    public interface WorldObserver {
        default void chunkLoaded(Chunk chunk){}
        default void chunkChanged(Chunk chunk, boolean staticBlocks){}
        default void chunkUnloaded(Chunk chunk){}
    }
}
