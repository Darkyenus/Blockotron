package darkyenus.blockotron.client;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import darkyenus.blockotron.render.BlockFaces;
import darkyenus.blockotron.render.WorldRenderer;
import darkyenus.blockotron.world.*;
import darkyenus.blockotron.world.debug.DebugWorldGenerator;

/**
 *
 */
public class GameScreen extends Screen {

    private WorldRenderer renderer;
    private World world;

    private FirstPersonCameraController controller;
    private ShapeRenderer shapeRenderer;
    private boolean debugOverlay = false;

    @Override
    public void show() {
        if(renderer == null){
            world = new World(new GeneratorChunkProvider(new DebugWorldGenerator()));
            renderer = new WorldRenderer();
            world.addObserver(renderer);

            controller = new FirstPersonCameraController(renderer.camera);
            Gdx.input.setInputProcessor(controller);
            shapeRenderer = new ShapeRenderer();
        }
    }

    @Override
    public void render(float delta) {
        update(delta);
        draw();
    }

    private void update(float delta){
        controller.update(delta);
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
