package darkyenus.blockotron.client;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.github.antag99.retinazer.EngineConfig;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.WorldRenderer;
import darkyenus.blockotron.utils.Profiler;
import darkyenus.blockotron.utils.SelectionWireResolver;
import darkyenus.blockotron.world.*;
import darkyenus.blockotron.world.components.*;
import darkyenus.blockotron.world.debug.DebugWorldGenerator;
import darkyenus.blockotron.world.systems.KinematicSystem;
import darkyenus.blockotron.world.systems.PlayerInputSystem;
import darkyenus.blockotron.world.systems.RandomBlockBehaviorSystem;

/**
 *
 */
public class GameScreen extends Screen {

    private WorldRenderer renderer;
    private World world;

    private ShapeRenderer shapeRenderer;
    private boolean debugOverlay = false;

    private int playerEntity;

    @Override
    public void show() {
        if(renderer == null){
            renderer = new WorldRenderer();
            world = new World(
                    new GeneratorChunkProvider(new DebugWorldGenerator()),
                    new EngineConfig()
                            .addSystem(new RandomBlockBehaviorSystem())
                            .addSystem(new PlayerInputSystem())
                            .addSystem(new KinematicSystem())
                            .addWireResolver(new SelectionWireResolver(renderer)));
            world.addObserver(renderer);

            shapeRenderer = new ShapeRenderer();

            playerEntity = world.entityEngine().createEntity();
            world.entityEngine().getMapper(Played.class).create(playerEntity);
            world.entityEngine().getMapper(Position.class).create(playerEntity).set(0, 0, 30);
            world.entityEngine().getMapper(Kinematic.class).create(playerEntity).setup(10f, true).setupHitbox(0.4f, 1.8f);
            world.entityEngine().getMapper(Orientation.class).create(playerEntity);
            world.entityEngine().getMapper(SelfMotionCapable.class).create(playerEntity).setup(45f, 6f);
        }
    }

    @Override
    public void render(float delta) {
        Profiler.beginFrame();
        update();
        draw();
        Profiler.endFrame();
    }

    private void update(){
        world.update(Gdx.graphics.getDeltaTime());
        if(Gdx.input.isKeyJustPressed(Input.Keys.F3)){
            debugOverlay = !debugOverlay;
        }
    }

    private void draw(){
        BlockFaces.update();

        Gdx.gl.glClearColor(0.2f, 0.2f, 0.9f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderer.render();

        if(debugOverlay){
            final SpriteBatch batch = Game.uiBatch();
            final BitmapFont font = Game.debugFont();

            final StringBuilder sb = new StringBuilder();
            sb.append("FPS: ").append(Gdx.graphics.getFramesPerSecond()).append('\n');
            sb.append("X: ").append(renderer.camera.position.x).append('\n');
            sb.append("Y: ").append(renderer.camera.position.y).append('\n');
            sb.append("Z: ").append(renderer.camera.position.z).append('\n');

            final Kinematic playerKinematic = world.entityEngine().getMapper(Kinematic.class).get(playerEntity);
            sb.append("VX: ").append(playerKinematic.velX).append('\n');
            sb.append("VY: ").append(playerKinematic.velY).append('\n');
            sb.append("VZ: ").append(playerKinematic.velZ).append('\n');

            sb.append("Chunk X: ").append(World.chunkCoord(renderer.camera.position.x)).append(" ").append(World.inChunkCoordXY(renderer.camera.position.x)).append('\n');
            sb.append("Chunk Y: ").append(World.chunkCoord(renderer.camera.position.y)).append(" ").append(World.inChunkCoordXY(renderer.camera.position.y)).append('\n');
            sb.append("Dir X: ").append(renderer.camera.direction.x).append('\n');
            sb.append("Dir Y: ").append(renderer.camera.direction.y).append('\n');
            sb.append("Dir Z: ").append(renderer.camera.direction.z).append('\n');
            sb.append("Direction: ").append(Side.matchDirection(renderer.camera.direction)).append('\n');
            sb.append('\n');
            final World.RayCastResult blockOnRay = world.getBlockOnRay(renderer.camera.position, renderer.camera.direction, 200, BlockFilter.NO_AIR);
            if(blockOnRay != null){
                sb.append("Looking at ").append(blockOnRay.getBlock()).append('\n');
                sb.append("   X: ").append(blockOnRay.getX()).append('\n');
                sb.append("   Y: ").append(blockOnRay.getY()).append('\n');
                sb.append("   Z: ").append(blockOnRay.getZ()).append('\n');
                sb.append(" Side: ").append(blockOnRay.getSide()).append('\n');
                final byte occlusionMask = world.getChunk(World.chunkCoord(blockOnRay.getX()), World.chunkCoord(blockOnRay.getY()))
                        .getOcclusionMask(
                                World.inChunkCoordXY(blockOnRay.getX()),
                                World.inChunkCoordXY(blockOnRay.getY()),
                                World.inChunkCoordZ(blockOnRay.getZ()));
                if(occlusionMask != 0){
                    sb.append(" Visible from: ");
                    for (Side side : Side.values()) {
                        if((occlusionMask & side.flag) != 0){
                            sb.append(side).append(" ");
                        }
                    }
                    sb.append('\n');
                }
            }

            batch.begin();
            font.draw(batch, sb, 5, Gdx.graphics.getHeight() - 5);
            batch.end();

            Profiler.renderGraph(shapeRenderer);
        }

        { //DEBUG: Draw crosshair
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.WHITE);
            float hW = Gdx.graphics.getWidth() * 0.5f;
            float hH = Gdx.graphics.getHeight() * 0.5f;
            float thickness = 1f;
            float size = 6f;
            shapeRenderer.rect(hW - thickness, hH - size, thickness * 2f, size * 2f);
            shapeRenderer.rect(hW - size, hH - thickness, size * 2f, thickness * 2f);
            shapeRenderer.end();
        }
    }
}
