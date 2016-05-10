package darkyenus.blockotron.world;

import com.badlogic.gdx.utils.IntArray;
import darkyenus.blockotron.world.blocks.Air;

import static darkyenus.blockotron.world.Dimensions.*;

/**
 *
 */
final class LightUpdater {
    private final Chunk[] chunks = new Chunk[9];
    private final byte[][] lights = new byte[9][];
    private final IntArray updateStack = new IntArray(false, 512);

    private static final int MASK = 0b11_1111;
    private static final int SHIFT = 6;

    private static final int CENTER = 13;
    private static final int X = 1;
    private static final int Y = 3;
    private static final int Z = 9;

    /** Entry point, updates the whole chunk */
    public void update(Chunk chunk){
        setup(chunk);
        for (int x = CHUNK_SIZE; x < CHUNK_SIZE + CHUNK_SIZE; x++) {
            for (int y = CHUNK_SIZE; y < CHUNK_SIZE + CHUNK_SIZE; y++) {
                queue(x, y, CHUNK_SIZE + CHUNK_SIZE - 1);
            }
        }
        process();
    }

    private void queue(int x, int y, int z){
        if(x < 0 || x >= 9*3 || y < 0 || y >= 9*3 || z < 0 || z >= 9*3) return;
        updateStack.add(((x & MASK) << (SHIFT + SHIFT)) | ((y & MASK) << (SHIFT)) | (z & MASK));
    }

    private void setup(Chunk center) {
        updateStack.clear();
        for (int xO = -1; xO <= 1; xO++) {
            for (int yO = -1; yO <= 1; yO++) {
                for (int zO = -1; zO <= 1; zO++) {
                    final Chunk loadedChunk = center.world.getLoadedChunk(center.x + xO, center.y + yO, center.z + zO);
                    final int key = CENTER + xO * X + yO * Y + zO * Z;
                    chunks[key] = loadedChunk;
                    lights[key] = loadedChunk == null ? null : loadedChunk.light;
                }
            }
        }
    }

    private byte lightValue(int x, int y, int z, byte def){
        final int chunkKey = CENTER + ((x & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT) * X
                + ((y & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT) * Y
                + ((z & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT) * Z;
        if(chunkKey < 0 || chunkKey >= 9)return def;
        final byte[] light = lights[chunkKey];
        if(light == null) return def;
        else return light[inChunkKey(x, y, z)];
    }

    private Block block(int x, int y, int z){
        final int chunkKey = CENTER + ((x & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT) * X
                + ((y & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT) * Y
                + ((z & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT) * Z;
        if(chunkKey < 0 || chunkKey >= 9)return Air.AIR;

        final Chunk chunk = chunks[chunkKey];
        if(chunk == null) return Air.AIR;
        else return chunk.blocks[inChunkKey(x, y, z)];
    }

    private byte skyLightValue(int x, int y, int z){
        return (byte) (lightValue(x, y, z, (byte)-1) & 0b1111);
    }

    private void setLightValue(int x, int y, int z, byte lightValue){
        final int chunkKey = CENTER + ((x & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT) * X
                + ((y & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT) * Y
                + ((z & ~CHUNK_SIZE_MASK) >> CHUNK_SIZE_SHIFT) * Z;
        if(chunkKey < 0 || chunkKey >= 9)return;
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
    }
}
