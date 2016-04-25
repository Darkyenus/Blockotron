package darkyenus.blockotron.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 *
 */
public final class Profiler {

    private static long frameStart;

    private static final float[] frameTimes = new float[256];
    private static int lastAddedFrameTime = -1;

    public static void beginFrame() {
        frameStart = System.nanoTime();
    }

    public static void endFrame() {
        lastAddedFrameTime++;
        if(lastAddedFrameTime == frameTimes.length){
            lastAddedFrameTime = 0;
        }
        frameTimes[lastAddedFrameTime] = (System.nanoTime() - frameStart) / 1000_000_000f;
    }

    public static void renderGraph(ShapeRenderer shapeRenderer) {
        //Draw fps log
        final int scaleX = 3;
        final int width = frameTimes.length * scaleX;
        final int height = 400;
        final int offX = Gdx.graphics.getWidth() - width - 10;
        final int offY = Gdx.graphics.getHeight() - height - 10;

        final float levelScaleY = 20000f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(offX, offY, width, height);

        float prevX = offX;
        float prevY = offY + frameTimes[lastAddedFrameTime]*levelScaleY;

        for (int i = 0; i < frameTimes.length; i++) {
            final float level = frameTimes[(lastAddedFrameTime + i) % frameTimes.length];
            if(level > 1f/30f){
                shapeRenderer.setColor(Color.RED);
            } else if(level > 1f/60f){
                shapeRenderer.setColor(Color.YELLOW);
            } else {
                shapeRenderer.setColor(Color.GREEN);
            }
            final int newX = offX + i * scaleX;
            final float newY = offY + level * levelScaleY;
            shapeRenderer.line(prevX, prevY, newX, newY);
            prevX = newX;
            prevY = newY;
        }
        shapeRenderer.end();
    }
}
