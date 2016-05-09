package darkyenus.blockotron.world.generator;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import darkyenus.blockotron.world.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 *
 */
public final class ChunkSaving {

    private static File getChunkFile(File worldBase, int chunkX, int chunkY, int chunkZ){
        //noinspection ResultOfMethodCallIgnored
        worldBase.mkdirs();
        return new File(worldBase, "chunk." + chunkX + "." + chunkY + "." + chunkZ + ".bin");
    }

    private static final Output chunkOutput = new Output(1<<10);
    private static final Input chunkInput = new Input(1<<10);

    public static void saveChunk(File worldBase, Chunk chunk) throws Exception {
        final File chunkFile = getChunkFile(worldBase, chunk.x, chunk.y, chunk.z);
        final Output out = ChunkSaving.chunkOutput;
        out.clear();
        out.setOutputStream(new FileOutputStream(chunkFile, false));

        for (Block block : chunk.blocks) {
            out.writeInt(block.getRegistryID(), true);
        }

        final EntityStorage entityStorage = chunk.getEntityStorage();
        EntityStorage.saveAndFreeStorage(entityStorage, out);

        out.close();
    }

    public static boolean loadChunk(File worldBase, Chunk chunk) {
        final File chunkFile = getChunkFile(worldBase, chunk.x, chunk.y, chunk.z);
        final Input in = ChunkSaving.chunkInput;
        try {
            in.setInputStream(new FileInputStream(chunkFile));
        } catch (FileNotFoundException e) {
            return false;
        }

        for (int i = 0; i < chunk.blocks.length; i++) {
            final int blockID = in.readInt(true);
            final int x = Dimensions.inChunkKeyToX(i);
            final int y = Dimensions.inChunkKeyToY(i);
            final int z = Dimensions.inChunkKeyToZ(i);
            chunk.setLocalBlock(x, y, z, Registry.block(blockID));
        }

        final EntityStorage entityStorage = EntityStorage.obtainAndLoadStorage(in);
        chunk.endPopulating(entityStorage);

        return true;
    }
}
