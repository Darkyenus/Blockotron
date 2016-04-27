package darkyenus.blockotron.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 *
 */
public class MenuScreen extends StageScreen {

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

        final Label title = new Label("Blockotron", new Label.LabelStyle(Game.debugFont(), Color.WHITE));
        title.setFontScale(8f);
        table.add(title).padLeft(Value.zero).row();

        final TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = Game.debugFont();
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = Color.CHARTREUSE;

        table.add().expand(0, 1).row();

        table.add(createButton("Singleplayer", buttonStyle, ()-> game.setScreen(new GameScreen()))).row();
        table.add(createButton("Multiplayer", buttonStyle, ()->{
            //TODO
        })).row();
        table.add(createButton("Options", buttonStyle, ()-> game.setScreen(new OptionsScreen(this)))).row();

        table.add().expand(0, 3).row();

        table.add(createButton("Exit", buttonStyle, ()-> Gdx.app.exit())).row();

        table.add().expand(0, 2).row();

        stage.addActor(table);
    }

    private TextButton createButton(String label, TextButton.TextButtonStyle style, Runnable listener){
        final TextButton button = new TextButton(label, style);
        button.getLabel().setFontScale(2f);
        button.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                listener.run();
            }
        });
        return button;
    }
}
