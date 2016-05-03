package darkyenus.blockotron.world.systems;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.LongArray;
import com.badlogic.gdx.utils.LongMap;
import com.github.antag99.retinazer.*;
import darkyenus.blockotron.world.Dimensions;
import darkyenus.blockotron.world.World;
import darkyenus.blockotron.world.components.BlockPosition;
import darkyenus.blockotron.world.components.ChunkLoading;
import darkyenus.blockotron.world.components.Position;

/**
 * System for loading chunks around entities that request it
 */
@SkipWire
public class ChunkLoadingSystem extends EntityProcessorSystem {

    private final boolean serverMode;
    /** key is entity */
    private final IntMap<Anchor> anchors = new IntMap<>();
    /** key is chunkColumnKey */
    private final LongMap<Integer> chunkUsageLevels = new LongMap<>();

    /** Columns which are loaded, but not needed anymore and kept because they will probably be needed soon. */
    private final LongArray inactiveChunks = new LongArray(true, 512 + 256);

    private static final int INACTIVE_CHUNKS_THRESHOLD = 512;
    private static final int INACTIVE_CHUNKS_KEEP = 128;

    @Wire
    private World world;

    @Wire
    private Mapper<ChunkLoading> worldLoadingMapper;

    @Wire
    private Mapper<Position> positionMapper;

    @Wire
    private Mapper<BlockPosition> blockPositionMapper;

    public ChunkLoadingSystem(boolean serverMode) {
        super(Family.with(ChunkLoading.class, Position.class));
        this.serverMode = serverMode;
    }

    @Override
    public void setup() {
        super.setup();

        final Family family = engine.getFamily(Family.with(ChunkLoading.class));
        family.addListener(new EntityListener() {
            @Override
            public void inserted(EntitySet entities) {
                final IntArray indices = entities.getIndices();
                final int[] items = indices.items;
                final int size = indices.size;

                for (int i = 0; i < size; i++) {
                    final int entity = items[i];

                    final ChunkLoading chunkLoading = worldLoadingMapper.get(entity);

                    if(!serverMode && !chunkLoading.rendering) return;

                    final Position position = positionMapper.get(entity);
                    final BlockPosition blockPosition = blockPositionMapper.get(entity);

                    final int x, y;
                    if(position != null){
                        x = (int)position.x;
                        y = (int)position.y;
                    } else if (blockPosition != null) {
                        x = blockPosition.x;
                        y = blockPosition.y;
                    } else {
                        return;
                    }

                    final Anchor anchor = new Anchor();
                    anchors.put(entity, anchor);
                    anchor.addTo(x, y, chunkLoading.radius);
                }
            }

            @Override
            public void removed(EntitySet entities) {
                final IntArray indices = entities.getIndices();
                final int[] items = indices.items;
                final int size = indices.size;

                for (int i = 0; i < size; i++) {
                    final int entity = items[i];

                    final Anchor remove = anchors.remove(entity);
                    if(remove != null){
                        remove.remove();
                    }
                }
            }
        });
    }

    @Override
    protected void process(int entity, float delta) {
        final Position position = positionMapper.get(entity);
        anchors.get(entity).moveTo((int)position.x, (int)position.y);
    }

    private final class Anchor {

        private int chunkX, chunkY;
        private int radius;

        private void add(){
            final LongMap<Integer> chunkUsageLevels = ChunkLoadingSystem.this.chunkUsageLevels;
            final LongArray inactiveChunks = ChunkLoadingSystem.this.inactiveChunks;
            final World world = ChunkLoadingSystem.this.world;

            final int chunkX = this.chunkX;
            final int chunkY = this.chunkY;
            final int radius = this.radius;

            for (int x = chunkX - radius; x <= chunkX + radius; x++) {
                for (int y = chunkY - radius; y <= chunkY + radius; y++) {
                    final long key = Dimensions.chunkColumnKey(x, y);
                    final Integer level = chunkUsageLevels.get(key, 0);
                    if(level == 0){
                        if (!inactiveChunks.removeValue(key)) {
                            //Was not inactive, load
                            for (int z = 0; z < Dimensions.CHUNK_LAYERS; z++) {
                                world.loadChunk(x, y, z);
                            }
                        }
                    }
                    chunkUsageLevels.put(key, level + 1);
                }
            }
        }

        public void remove(){
            final LongMap<Integer> chunkUsageLevels = ChunkLoadingSystem.this.chunkUsageLevels;
            final LongArray inactiveChunks = ChunkLoadingSystem.this.inactiveChunks;
            final World world = ChunkLoadingSystem.this.world;

            final int chunkX = this.chunkX;
            final int chunkY = this.chunkY;
            final int radius = this.radius;

            for (int x = chunkX - radius; x <= chunkX + radius; x++) {
                for (int y = chunkY - radius; y <= chunkY + radius; y++) {
                    final long key = Dimensions.chunkColumnKey(x, y);
                    final Integer newLevel = chunkUsageLevels.get(key, 1) - 1;
                    if(newLevel == 0){
                        chunkUsageLevels.remove(key);
                        inactiveChunks.add(key);
                    } else {
                        chunkUsageLevels.put(key, newLevel);
                    }
                }
            }

            if(inactiveChunks.size >= INACTIVE_CHUNKS_THRESHOLD){
                final int toRemove = inactiveChunks.size - INACTIVE_CHUNKS_KEEP;
                final long[] items = inactiveChunks.items;
                for (int item = 0; item < toRemove; item++) {
                    final long key = items[item];

                    final int x = Dimensions.chunkKeyToX(key);
                    final int y = Dimensions.chunkKeyToY(key);

                    for (int z = 0; z < Dimensions.CHUNK_LAYERS; z++) {
                        world.unloadChunk(x, y, z);
                    }
                }

                inactiveChunks.removeRange(0, toRemove - 1);
            }
        }

        public void moveTo(int worldX, int worldY) {
            final int newX = Dimensions.worldToChunk(worldX);
            final int newY = Dimensions.worldToChunk(worldY);
            if(newX != chunkX || newY != chunkY){
                remove();
                this.chunkX = newX;
                this.chunkY = newY;
                add();
            }
        }

        public void addTo(int worldX, int worldY, int radius){
            this.chunkX = Dimensions.worldToChunk(worldX);
            this.chunkY = Dimensions.worldToChunk(worldY);
            this.radius = radius;
            add();
        }
    }
}
