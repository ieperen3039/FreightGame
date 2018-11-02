package NG.ScreenOverlay;

import NG.DataStructures.PairList;
import NG.Engine.Game;
import NG.Engine.ModLoader;
import NG.Engine.ModLoader.IllegalNumberOfModulesException;
import NG.GameState.MapGeneratorMod;
import NG.Mods.Mod;
import NG.ScreenOverlay.Frames.Components.*;
import NG.Tools.Logger;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Geert van Ieperen. Created on 28-9-2018.
 */
public class MainMenu extends SFrame {
    // these are upper bounds
    private static final int NUM_TOP_BUTTONS = 10;
    private static final int NUM_BOT_BUTTONS = 10;
    public static final int BUTTON_MIN_WIDTH = 300;
    public static final int BUTTON_MIN_HEIGHT = 50;

    private final Vector2i topButtonPos;
    private final Vector2i bottomButtonPos;
    public static final int NUM_BUTTONS = NUM_TOP_BUTTONS + NUM_BOT_BUTTONS + 1;
    private final Game game;
    private final ModLoader modLoader;
    private final SFrame newGameFrame;

    public MainMenu(Game game, ModLoader modManager, Runnable exitGameAction) {
        super("Main Menu", 400, 500, false);
        this.game = game;
        this.modLoader = modManager;
        topButtonPos = new Vector2i(1, -1);
        bottomButtonPos = new Vector2i(1, NUM_BUTTONS);
        SContainer buttons = new SPanel(3, NUM_BUTTONS);

        newGameFrame = getNewGameFrame();

        SButton newGame = new SButton("Start new game", this::showNewGame, BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
        buttons.add(newGame, onTop());
        SButton exitGame = new SButton("Exit game", exitGameAction, BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
        buttons.add(exitGame, onBot());

        Vector2i mid = onTop();
        buttons.add(new SFiller(), new Vector2i(0, mid.y));
        buttons.add(new SFiller(), new Vector2i(1, mid.y));
        buttons.add(new SFiller(), new Vector2i(2, mid.y));

        setMainPanel(buttons);
    }

    private void showNewGame() {
        newGameFrame.setVisible(true);
        game.gui().addFrame(newGameFrame);
    }

    private SFrame getNewGameFrame() {
        List<Mod> modList = modLoader.allMods();
        int nOfMods = modList.size();
        PairList<SToggleButton, Mod> toggleList = new PairList<>(nOfMods);

        SFrame newGameFrame = new SFrame("New Game Parameters");
        final int ROWS = 10;
        final int COLS = 3;
        Vector2i mpos = new Vector2i(1, 0);

        SPanel mainPanel = new SPanel(COLS, ROWS);
        mainPanel.add(new SFiller(100, 100), new Vector2i(0, 0));
        mainPanel.add(new SFiller(100, 100), new Vector2i(COLS - 1, ROWS - 1));

        // message
        STextArea notice = new STextArea("Select which mods to load", 50, false);
        mainPanel.add(notice, mpos.add(0, 1));

        // size selection
        SPanel sizeSelection = new SPanel(0, 0, 4, 1, false, false);
        sizeSelection.add(new STextArea("Size", 0, true), new Vector2i(0, 0));
        final SDropDown xSizeSelector = new SDropDown(game, 1, 100, 60, "100", "200", "500", "1000");
        sizeSelection.add(xSizeSelector, new Vector2i(1, 0));
        sizeSelection.add(new STextArea("X", 0, true), new Vector2i(2, 0));
        final SDropDown ySizeSelector = new SDropDown(game, 1, 100, 60, "100", "200", "500", "1000");
        sizeSelection.add(ySizeSelector, new Vector2i(3, 0));
        mainPanel.add(sizeSelection, mpos.add(0, 1));

        // add mod buttons
        SContainer modPanel = new SPanel(1, nOfMods);
        Vector2i pos = new Vector2i(0, -1);
        for (Mod mod : modList) {
            if (mod instanceof MapGeneratorMod) continue;
            SToggleButton button = new SToggleButton(mod.getModName(), BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
            toggleList.add(button, mod);
            modPanel.add(button, pos.add(0, 1));
        }
        mainPanel.add(modPanel, mpos.add(0, 1));

        // generator selection
        List<String> generatorNames = modList.stream()
                .filter(m -> m instanceof MapGeneratorMod)
                .map(Mod::getModName)
                .collect(Collectors.toList());
        SDropDown generatorSelector = new SDropDown(game, generatorNames);
        mainPanel.add(generatorSelector, mpos.add(0, 1));

        // generate button
        mainPanel.add(new SFiller(0, 50), mpos.add(0, 1));
        SButton generate = new SButton("Generate", BUTTON_MIN_WIDTH, BUTTON_MIN_HEIGHT);
        mainPanel.add(generate, mpos.add(0, 1));

        newGameFrame.setMainPanel(mainPanel);
        newGameFrame.pack();

        // start game action
        generate.addLeftClickListener(() -> {
            try {
                int selected = generatorSelector.getSelectedIndex();
                MapGeneratorMod generatorMod = (MapGeneratorMod) modList.get(selected);

                final String xSize = xSizeSelector.getSelected();
                generatorMod.setXSize(Integer.parseInt(xSize));
                final String ySize = ySizeSelector.getSelected();
                generatorMod.setYSize(Integer.parseInt(ySize));

                List<Mod> targets = new ArrayList<>();
                for (int i = 0; i < toggleList.size(); i++) {
                    if (toggleList.left(i).getState()) {
                        Mod mod = toggleList.right(i);

                        if (mod instanceof MapGeneratorMod) {
                            Logger.ASSERT.print("map generator mod found in modlist");

                        } else {
                            targets.add(mod);
                        }
                    }
                }

                modLoader.initMods(targets);

                if (targets.isEmpty()) throw new IllegalNumberOfModulesException("No mods selected");
                game.map().generateNew(generatorMod);

                modLoader.startGame();
                newGameFrame.setVisible(false);

            } catch (IllegalNumberOfModulesException e) {
                notice.setText(e.getMessage());
                Logger.WARN.print(e);
            }
        });

        return newGameFrame;
    }

    private Vector2i onTop() {
        return topButtonPos.add(0, 1);
    }

    private Vector2i onBot() {
        return bottomButtonPos.sub(0, 1);
    }

}
