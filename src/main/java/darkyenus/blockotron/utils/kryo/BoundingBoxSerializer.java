package darkyenus.blockotron.utils.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import darkyenus.blockotron.utils.BoundingBox;

/**
 * Serializes bounding boxes.
 * Takes special care to preserve identity of UNIT_BOUNDING_BOX references.
 */
public class BoundingBoxSerializer extends Serializer<BoundingBox> {

    public static final BoundingBoxSerializer INSTANCE = new BoundingBoxSerializer();

    private BoundingBoxSerializer() {
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, BoundingBox object) {
        if(object == BoundingBox.UNIT_BOUNDING_BOX) {
            output.writeFloat(Float.NaN);
            return;
        }
        output.writeFloat(object.offsetX);
        output.writeFloat(object.offsetY);
        output.writeFloat(object.offsetZ);
        output.writeFloat(object.sizeX);
        output.writeFloat(object.sizeY);
        output.writeFloat(object.sizeZ);
    }

    @Override
    public BoundingBox read(Kryo kryo, Input input, Class<BoundingBox> type) {
        final float offsetX = input.readFloat();
        if(Float.isNaN(offsetX)){
            return BoundingBox.UNIT_BOUNDING_BOX;
        }
        return new BoundingBox(offsetX, input.readFloat(), input.readFloat(),
                input.readFloat(), input.readFloat(), input.readFloat());
    }
}
