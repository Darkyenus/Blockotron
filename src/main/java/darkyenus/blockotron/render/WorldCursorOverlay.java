package darkyenus.blockotron.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import darkyenus.blockotron.utils.BlockBoundingBox;
import darkyenus.blockotron.world.BlockFilter;
import darkyenus.blockotron.world.World;
import darkyenus.blockotron.world.blocks.Air;
import darkyenus.blockotron.world.blocks.Glass;
import darkyenus.blockotron.world.blocks.Grass;

/**
 * Draws the wireframe cursor over the selected block.
 */
public class WorldCursorOverlay implements RenderableProvider {

    private final Mesh cursorMesh;
    private final Material cursorMaterial;

    private boolean visible = false;
    private final Vector3 position = new Vector3();
    private final Vector3 scale = new Vector3();

    private static final Integer CURSOR_RENDERABLE_BIAS = 100;

    public WorldCursorOverlay() {
        final MeshBuilder builder = new MeshBuilder();
        builder.begin(new VertexAttributes(VertexAttribute.Position()), GL20.GL_LINES);
        builder.box(0.5f, 0.5f, 0.5f, 1f, 1f, 1f);
        cursorMesh = builder.end();

        cursorMaterial = new Material(ColorAttribute.createDiffuse(Color.BLACK), new DepthTestAttribute(0));
    }

    /** Update cursor's position based on parameters.
     * @param distance max block distance that will be still drawn */
    public void update(World world, Camera camera, float distance){
        final World.RayCastResult target = world.getBlockOnRay(camera.position, camera.direction, distance, BlockFilter.NO_AIR);
        if(target == null){
            visible = false;
        }else{
            visible = true;
            BlockBoundingBox hitBox = target.getBlock().hitBox;
            position.set(target.getX(), target.getY(), target.getZ()).add(hitBox.offsetX, hitBox.offsetY, hitBox.offsetZ);
            scale.set(hitBox.sizeX, hitBox.sizeY, hitBox.sizeZ);

            //DEBUG world editing
            if(Gdx.input.isKeyJustPressed(Input.Keys.C)){
                world.setBlock(target.getX() + target.getSide().offX, target.getY() + target.getSide().offY, target.getZ() + target.getSide().offZ, Grass.GRASS);
            } else if(Gdx.input.isKeyJustPressed(Input.Keys.R)){
                world.setBlock(target.getX() + target.getSide().offX, target.getY() + target.getSide().offY, target.getZ() + target.getSide().offZ, Glass.GLASS);
            } else if(Gdx.input.isKeyJustPressed(Input.Keys.F)){
                world.setBlock(target.getX(), target.getY(), target.getZ(), Air.AIR);
            }
        }
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        if(visible){
            final Renderable r = pool.obtain();
            r.meshPart.mesh = cursorMesh;
            r.meshPart.primitiveType = GL20.GL_LINES;
            r.meshPart.offset = 0;
            r.meshPart.size = cursorMesh.getNumIndices();
            r.worldTransform.setToTranslation(position).scl(scale);
            r.material = cursorMaterial;
            r.userData = CURSOR_RENDERABLE_BIAS;
            renderables.add(r);
        }
    }
}
