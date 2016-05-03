package darkyenus.blockotron.world.systems;

import com.badlogic.gdx.utils.IntArray;
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
    private final LongMap<Anchor> anchors = new LongMap<>();
    private final LongMap<Integer> chunkUsageLevels = new LongMap<>();

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

                    final int x, y, z;
                    if(position != null){
                        x = (int)position.x;
                        y = (int)position.y;
                        z = (int)position.z;
                    } else if (blockPosition != null) {
                        x = blockPosition.x;
                        y = blockPosition.y;
                        z = blockPosition.z;
                    } else {
                        return;
                    }

                    final Anchor anchor = new Anchor();
                    anchors.put(entity, anchor);
                    anchor.addTo(x, y, z, chunkLoading.radius);
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
        anchors.get(entity).moveTo((int)position.x, (int)position.y, (int)position.z);
    }

    private final class Anchor {

        private int chunkX, chunkY, chunkZ;
        private int radius;

        private void add(){
            final int chunkX = this.chunkX;
            final int chunkY = this.chunkY;
            final int chunkZ = this.chunkZ;
            final int radius = this.radius;

            for (int x = chunkX - radius; x <= chunkX + radius; x++) {
                for (int y = chunkY - radius; y <= chunkY + radius; y++) {
                    for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
                        if(z < 0 || z >= Dimensions.CHUNK_LAYERS)continue;
                        final long key = Dimensions.chunkKey(x, y, z);
                        final Integer level = chunkUsageLevels.get(key, 0);
                        if(level == 0){
                            world.loadChunk(x, y, z);
                        }
                        chunkUsageLevels.put(key, level + 1);
                    }
                }
            }
        }

        public void remove(){
            final int chunkX = this.chunkX;
            final int chunkY = this.chunkY;
            final int chunkZ = this.chunkZ;
            final int radius = this.radius;

            for (int x = chunkX - radius; x <= chunkX + radius; x++) {
                for (int y = chunkY - radius; y <= chunkY + radius; y++) {
                    for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
                        if(z < 0 || z >= Dimensions.CHUNK_LAYERS)continue;
                        final long key = Dimensions.chunkKey(x, y, z);
                        final Integer newLevel = chunkUsageLevels.get(key, 1) - 1;
                        if(newLevel == 0){
                            chunkUsageLevels.remove(key);
                            world.unloadChunk(x, y, z);
                        } else {
                            chunkUsageLevels.put(key, newLevel);
                        }
                    }
                }
            }
        }

        public void moveTo(int worldX, int worldY, int worldZ) {
            final int newX = Dimensions.worldToChunk(worldX);
            final int newY = Dimensions.worldToChunk(worldY);
            final int newZ = Dimensions.worldToChunk(worldZ);
            if(newX != chunkX || newY != chunkY || newZ != chunkZ){
                remove();
                this.chunkX = newX;
                this.chunkY = newY;
                this.chunkZ = newZ;
                add();
            }
        }

        public void addTo(int worldX, int worldY, int worldZ, int radius){
            this.chunkX = Dimensions.worldToChunk(worldX);
            this.chunkY = Dimensions.worldToChunk(worldY);
            this.chunkZ = Dimensions.worldToChunk(worldZ);
            this.radius = radius;
            add();
        }
    }
}
