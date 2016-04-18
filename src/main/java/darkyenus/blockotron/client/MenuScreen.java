package darkyenus.blockotron.client;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 *
 */
public class MenuScreen extends StageScreen {

    @Override
    public void createStage() {
        final Table table = new Table();
        table.setFillParent(true);
        table.center();

        final TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = Game.debugFont();
        final TextButton createWorld = new TextButton("Create world", style);
        createWorld.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen());
            }
        });
        table.add(createWorld);
        stage.addActor(table);
    }
}
