package darkyenus.blockotron.world;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Interface for items which can serialize themselves
 */
public interface SelfSerializable {
    void serialize(Output out, Kryo kryo);
    void deserialize(Input input, Kryo kryo);
}
