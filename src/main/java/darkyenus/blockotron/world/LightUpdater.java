package darkyenus.blockotron.world;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Pool;
import darkyenus.blockotron.world.blocks.Air;

import static darkyenus.blockotron.world.Dimensions.*;

/**
 *
 */
final class LightUpdater {
    private static final int CHUNK_COUNT = 3*3*3;
    private static final int BLOCK_COUNT = 3*CHUNK_SIZE;

    private final Chunk[] chunks = new Chunk[CHUNK_COUNT];
    private final byte[][] lights = new byte[CHUNK_COUNT][];
    private final IntArray updateStack = new IntArray(false, 512);
    private boolean updateChunkBelow = false;

    private static final int MASK = 0b11_1111;
    private static final int SHIFT = 6;

    private static final int CENTER = 13;
    private static final int X = 1;
    private static final int Y = 3;
    private static final int Z = 9;

    private LightUpdater() {
    }

    public static void updateChunk(Chunk chunk){
        final LightUpdater updater = LIGHT_UPDATER_POOL.obtain();
        updater.update(chunk);
        LIGHT_UPDATER_POOL.free(updater);
    }

    public static void updateChunk(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ){
        final LightUpdater updater = LIGHT_UPDATER_POOL.obtain();
        updater.update(chunk, inChunkX, inChunkY, inChunkZ);
        LIGHT_UPDATER_POOL.free(updater);
    }

    /** Entry point, updates the whole chunk */
    private void update(Chunk chunk){
        setup(chunk);
        for (int x = CHUNK_SIZE; x < CHUNK_SIZE + CHUNK_SIZE; x++) {
            for (int y = CHUNK_SIZE; y < CHUNK_SIZE + CHUNK_SIZE; y++) {
                queue(x, y, CHUNK_SIZE + CHUNK_SIZE - 1);
            }
        }
        process();
    }

    private void update(Chunk chunk, int inChunkX, int inChunkY, int inChunkZ){
        setup(chunk);
        queue(CHUNK_SIZE + inChunkX, CHUNK_SIZE + inChunkY, CHUNK_SIZE + inChunkZ);
        process();
    }

    private void queue(int x, int y, int z){
        if(x < 0 || x >= BLOCK_COUNT || y < 0 || y >= BLOCK_COUNT || z >= BLOCK_COUNT) return;
        if(z < 0){
            updateChunkBelow = true;
            return;
        }
        updateStack.add(((x & MASK) << (SHIFT + SHIFT)) | ((y & MASK) << (SHIFT)) | (z & MASK));
    }

    private void setup(Chunk center) {
        updateChunkBelow = false;
        updateStack.clear();
        for (int xO = -1; xO <= 1; xO++) {
            for (int yO = -1; yO <= 1; yO++) {
                for (int zO = -1; zO <= 1; zO++) {
                    final Chunk loadedChunk = center.world.getLoadedChunk(center.x + xO, center.y + yO, center.z + zO);
                    final int key = CENTER + xO * X + yO * Y + zO * Z;
                    chunks[key] = loadedChunk;
                    lights[key] = loadedChunk == null ? null : loadedChunk.getLight();
                }
            }
        }
    }

    private int chunkKey(int x, int y, int z){
        return CENTER + ((x-CHUNK_SIZE) >> CHUNK_SIZE_SHIFT) * X
                + ((y-CHUNK_SIZE) >> CHUNK_SIZE_SHIFT) * Y
                + ((z-CHUNK_SIZE) >> CHUNK_SIZE_SHIFT) * Z;
    }

    private byte lightValue(int x, int y, int z, byte def){
        final int chunkKey = chunkKey(x, y, z);
        if(chunkKey < 0 || chunkKey >= CHUNK_COUNT) return def;

        final byte[] light = lights[chunkKey];
        if(light == null) return def;
        else return light[inChunkKey(x, y, z)];
    }

    private Block block(int x, int y, int z){
        final int chunkKey = chunkKey(x, y, z);
        if(chunkKey < 0 || chunkKey >= CHUNK_COUNT) return Air.AIR;

        final Chunk chunk = chunks[chunkKey];
        if(chunk == null) return Air.AIR;
        else return chunk.blocks[inChunkKey(x, y, z)];
    }

    private byte skyLightValue(int x, int y, int z){
        return (byte) (lightValue(x, y, z, (byte)-1) & 0b1111);
    }

    private void setLightValue(int x, int y, int z, byte lightValue){
        final int chunkKey = chunkKey(x, y, z);
        if(chunkKey < 0 || chunkKey >= CHUNK_COUNT) return;

        final byte[] light = lights[chunkKey];
        if(light == null) return;
        final int key = inChunkKey(x, y, z);
        final byte old = light[key];
        if(old != lightValue){
            light[key] = lightValue;
            queue(x+1, y, z);
            queue(x-1, y, z);
            queue(x, y+1, z);
            queue(x, y-1, z);
            queue(x, y, z+1);
            queue(x, y, z-1);
        }
    }

    private int max(int b1, int b2, int b3, int b4, int b5, int b6){
        if(b1 >= b2 && b1 >= b3 && b1 >= b4 && b1 >= b5 && b1 >= b6){
            return b1;
        }
        if(b2 >= b3 && b2 >= b4 && b2 >= b5 && b2 >= b6){
            return b2;
        }
        if(b3 >= b4 && b3 >= b5 && b3 >= b6){
            return b3;
        }
        if(b4 >= b5 && b4 >= b6){
            return b4;
        }
        if(b5 >= b6){
            return b5;
        }
        return b6;
    }

    private void process(){
        final IntArray updateStack = this.updateStack;
        while(updateStack.size != 0){
            final int key = updateStack.pop();
            final int x = (key >> SHIFT + SHIFT) & MASK;
            final int y = (key >> SHIFT) & MASK;
            final int z = key & MASK;

            final Block block = block(x, y, z);
            if(block.isTransparent()){
                final int sky = max(
                        skyLightValue(x + 1, y, z) - 1,
                        skyLightValue(x - 1, y, z) - 1,
                        skyLightValue(x, y + 1, z) - 1,
                        skyLightValue(x, y - 1, z) - 1,
                        skyLightValue(x, y, z - 1) - 1,
                        skyLightValue(x, y, z + 1));
                setLightValue(x, y, z, (byte)sky);
            } else {
                setLightValue(x, y, z, (byte)0);
            }
        }

        if(updateChunkBelow){
            final Chunk below = chunks[CENTER - Z];
            if(below != null){
                update(below);
            }
        }
    }

    private static final Pool<LightUpdater> LIGHT_UPDATER_POOL = new Pool<LightUpdater>() {
        @Override
        protected LightUpdater newObject() {
            return new LightUpdater();
        }
    };
}
