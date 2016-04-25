import com.badlogic.gdx.math.MathUtils;
import darkyenus.blockotron.utils.BoundingBox;
import static darkyenus.blockotron.utils.BoundingBox.UNIT_BOUNDING_BOX;
import darkyenus.blockotron.world.Side;

public class BoundingBoxTest {

    private static void testRay(String name, BoundingBox target, BoundingBox box, float oX, float oY, float oZ, float dX, float dY, float dZ, boolean correctResult, Side correctSide){
        testRay(name, target, box, oX, oY, oZ, dX, dY, dZ, correctResult, correctSide, Float.NaN);
    }

    private static void testRay(String name, BoundingBox target, BoundingBox box, float oX, float oY, float oZ, float dX, float dY, float dZ, boolean correctResult, Side correctSide, float expectedT){
        final BoundingBox.BoundingBoxIntersectResult result = new BoundingBox.BoundingBoxIntersectResult();
        final boolean collided;
        if(box == null){
            collided = target.intersectsRay(oX, oY, oZ, dX, dY, dZ, result);
        } else {
            collided = target.intersectsBox(box, oX, oY, oZ, dX, dY, dZ, result);
        }

        if(collided && correctResult && result.getSide() == correctSide && (Float.isNaN(expectedT) || MathUtils.isEqual(expectedT, result.getT()))){
            System.out.println(name+" collided to correct side");
            System.out.flush();
        }else if(!collided && !correctResult) {
            System.out.println(name+" correctly did not collide");
            System.out.flush();
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append(name).append("\n\tExpected: ");
            if(correctResult){
                sb.append("Collision with ").append(correctSide);
            } else {
                sb.append("No collision");
            }
            if(!Float.isNaN(expectedT))sb.append(" at T = ").append(expectedT);
            sb.append("\n\tGot: ");
            if(collided){
                sb.append("Collision with ").append(result.getSide());
            } else {
                sb.append("No collision");
            }
            sb.append(" at T = ").append(result.getT());
            System.err.println(sb);
            System.err.flush();
        }
    }

    public static void main(String[] args){
        testRay("X", UNIT_BOUNDING_BOX, null, -1f, 0.5f, 0.5f, 1f, 0f, 0f, true, Side.WEST, 1f);
        testRay("Z", UNIT_BOUNDING_BOX, UNIT_BOUNDING_BOX, 0.5f, 0.5f, 10f, 0f, 0f, -1f, true, Side.TOP, 9f);
        testRay("-Z", UNIT_BOUNDING_BOX, UNIT_BOUNDING_BOX, 0.0f, 0.0f, -10f, 0f, 0f, 1f, true, Side.BOTTOM, 9f);

        testRay("1D", UNIT_BOUNDING_BOX, null, 0.5f, 0.5f, 5f, 0f, 0f, -1f, true, Side.TOP);
        testRay("2D", UNIT_BOUNDING_BOX, null, 0.5f, 5f, 5f, 0f, -0.9f, -1f, true, Side.NORTH);
        testRay("3D", UNIT_BOUNDING_BOX, null, -1, -1, -1, 0.9f, 1, 0.9f, true, Side.WEST);

        testRay("!1D", UNIT_BOUNDING_BOX, null, 0.5f, 0.5f, 5f, 0f, -1f, 0f, false, null);
        testRay("!2D", UNIT_BOUNDING_BOX, null, 0.5f, 5f, 5f, -1f, 0f, -1f, false, null);
        testRay("!3D", UNIT_BOUNDING_BOX, null, -1, -1, -1, 1, 0, 0, false, null);

        testRay("-1D", UNIT_BOUNDING_BOX, null, 0.5f, 0.5f, 5f, 0f, 0f, 1f, false, null);
        testRay("-2D", UNIT_BOUNDING_BOX, null, 0.5f, 5f, 5f, 0f, 1f, 1f, false, null);
        testRay("-3D", UNIT_BOUNDING_BOX, null, -1, -1, -1, -1, -1, -1, false, null);
    }
}
