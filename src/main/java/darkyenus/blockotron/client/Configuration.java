package darkyenus.blockotron.client;

import com.badlogic.gdx.math.MathUtils;

import java.io.*;

/**
 * Global game configuration and its de/serialization.
 */
public final class Configuration {

    public static final BooleanConfig fullscreen = new BooleanConfig("fullscreen", false);
    public static final BooleanConfig vSync = new BooleanConfig("v-sync", false);

    public static final IntegerConfig aaSamples = new IntegerConfig("aa-samples", 0);

    public static final IntegerConfig windowWidth = new IntegerConfig("width", 0);
    public static final IntegerConfig windowHeight = new IntegerConfig("height", 0);

    public static final PercentConfig masterVolume = new PercentConfig("master-volume", 1f);

    private static final AbstractConfig<?>[] all = {fullscreen, vSync, aaSamples, windowWidth, windowHeight, masterVolume};

    //TODO Better persistence location
    public static final File GAME_ROOT = new File("_root_");
    static {
        //noinspection ResultOfMethodCallIgnored
        GAME_ROOT.mkdirs();
    }

    private static final File configFile = new File(GAME_ROOT, "config.txt");

    public static void loadConfiguration(){
        try(BufferedReader reader = new BufferedReader(new FileReader(configFile))){
            while(true){
                final String line = reader.readLine();
                if(line == null) return;
                final int eqIndex = line.indexOf('=');
                if(eqIndex == -1) continue;
                final String key = line.substring(0, eqIndex);
                for (AbstractConfig<?> config : all) {
                    if(config.name.equalsIgnoreCase(key)){
                        config.load(line.subSequence(eqIndex+1, line.length()));
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e){
            System.out.println("Exception file does not exist");
            saveConfiguration();
        } catch (Exception e){
            System.err.println("Failed to load configuration");
            e.printStackTrace(System.err);
        }
    }

    public static void saveConfiguration(){
        final StringBuilder sb = new StringBuilder();
        for (AbstractConfig<?> config : all) {
            sb.append(config.name).append("=");
            config.save(sb);
            sb.append('\n');
        }

        try(FileWriter writer = new FileWriter(configFile, false)){
            writer.append(sb);
        } catch (Exception e){
            System.err.println("Failed to save configuration");
            e.printStackTrace(System.err);
        }
    }


    public static abstract class AbstractConfig<T> {

        public final String name;
        public final T defaultValue;
        public T value;

        protected AbstractConfig(String name, T defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public abstract void load(CharSequence from);

        public abstract void save(StringBuilder sb);
    }

    public static final class BooleanConfig extends AbstractConfig<Boolean> {

        protected BooleanConfig(String name, boolean defaultValue) {
            super(name, defaultValue);
        }

        @Override
        public void load(CharSequence from) {
            if(ignoreCaseMatches("true", from) || ignoreCaseMatches("1", from)){
                value = true;
            }
        }

        @Override
        public void save(StringBuilder sb) {
            sb.append(value ? "true" : "false");
        }
    }

    public static final class PercentConfig extends AbstractConfig<Float> {

        protected PercentConfig(String name, float defaultValue) {
            super(name, defaultValue);
        }

        @Override
        public void load(CharSequence from) {
            try {
                final int percent = MathUtils.clamp(Integer.parseUnsignedInt(from.toString()), 0, 100);
                value = percent / 100f;
            } catch (NumberFormatException ignored) {
            }
        }

        @Override
        public void save(StringBuilder sb) {
            sb.append(MathUtils.clamp(MathUtils.roundPositive(value * 100f), 0, 100));
        }
    }

    public static final class IntegerConfig extends AbstractConfig<Integer> {

        protected IntegerConfig(String name, int defaultValue) {
            super(name, defaultValue);
        }

        @Override
        public void load(CharSequence from) {
            try {
                value = Integer.parseInt(from.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        @Override
        public void save(StringBuilder sb) {
            sb.append(value);
        }
    }

    private static boolean ignoreCaseMatches(CharSequence one, CharSequence two) {
        if(one == null && two == null) return true;
        if(one == null || two == null) return false;
        if(one.length() != two.length()) return false;
        for (int i = 0; i < one.length(); i++) {
            final char c1 = Character.toLowerCase(one.charAt(i));
            final char c2 = Character.toLowerCase(two.charAt(i));
            if(c1 != c2)return false;
        }
        return true;
    }

}
