package darkyenus.blockotron.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.function.Consumer;

/**
 *
 */
public class OptionsScreen extends StageScreen {

    private final Screen backScreen;

    public OptionsScreen(Screen backScreen) {
        this.backScreen = backScreen;
    }

    @Override
    public void createStage() {
        final Table table = new Table();
        table.setFillParent(true);
        table.left();

        final Value hPadding = new Value() {
            @Override
            public float get(Actor context) {
                return 0.05f * Gdx.graphics.getWidth();
            }
        };

        table.padLeft(hPadding);
        table.defaults().left().pad(5f).padLeft(40f);

        table.add().expand(0, 1).row();

        final Label title = new Label("Options", new Label.LabelStyle(Game.debugFont(), Color.WHITE));
        title.setFontScale(4f);
        table.add(title).padLeft(Value.zero).row();

        table.add().expand(0, 1).row();

        table.add(createBooleanOption("Fullscreen", "Windowed", Gdx.graphics.isFullscreen(), fullscreen -> {
            final Graphics.DisplayMode desktopDisplayMode = Gdx.graphics.getDisplayMode(Gdx.graphics.getMonitor());
            if(fullscreen){
                Gdx.graphics.setFullscreenMode(desktopDisplayMode);
            } else {
                Gdx.graphics.setWindowedMode((int)(desktopDisplayMode.width * 0.7f), (int)(desktopDisplayMode.height * 0.7f));
            }
        })).row();

        //TODO Default vSync may not be "true"
        table.add(createBooleanOption("V-Sync: On", "V-Sync: Off", true, vSync -> Gdx.graphics.setVSync(vSync))).row();

        table.add().expand(0, 3).row();

        final TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = Game.debugFont();
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = Color.CHARTREUSE;
        table.add(createButton("Back", buttonStyle, (button)-> game.setScreen(backScreen))).row();

        table.add().expand(0, 2).row();

        stage.addActor(table);
    }

    private TextButton createButton(String label, TextButton.TextButtonStyle style, Consumer<TextButton> listener){
        final TextButton button = new TextButton(label, style);
        button.getLabel().setFontScale(2f);
        button.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                listener.accept(button);
            }
        });
        return button;
    }

    private TextButton createBooleanOption(String trueLabel, String falseLabel, boolean current, Consumer<Boolean> changeListener){
        final TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = Game.debugFont();
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = Color.CHARTREUSE;

        final boolean[] status = {current};

        final TextButton textButton = new TextButton(current ? trueLabel : falseLabel, buttonStyle);
        textButton.getLabel().setFontScale(2f);
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                status[0] = !status[0];
                changeListener.accept(status[0]);
                textButton.getLabel().setText(status[0] ? trueLabel : falseLabel);
            }
        });

        return textButton;
    }

}
