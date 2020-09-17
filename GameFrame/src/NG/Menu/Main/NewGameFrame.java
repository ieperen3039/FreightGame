package NG.Menu.Main;


import NG.Camera.Camera;
import NG.Core.Game;
import NG.Core.ModLoader;
import NG.DataStructures.Generic.PairList;
import NG.GUIMenu.Components.*;
import NG.GameMap.DefaultMapGenerator;
import NG.GameMap.MapGeneratorMod;
import NG.Mods.Mod;
import NG.Tools.Logger;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 21-11-2018.
 */
public class NewGameFrame extends SFrame {
    private final SDropDown mapGeneratorSelector;
    private final List<MapGeneratorMod> mapGeneratorList;
    private final SDropDown xSizeSelector;
    private final SDropDown ySizeSelector;
    private final PairList<SToggleButton, Mod> toggleList;
    private final ModLoader modLoader;
    private final Game game;
    private final STextArea notice;

    public NewGameFrame(final Game game, final ModLoader loader) {
        super("New Game Frame");
        modLoader = loader;
        List<Mod> modList = modLoader.allMods();
        mapGeneratorList = new ArrayList<>();
        int nOfMods = modList.size();
        toggleList = new PairList<>(nOfMods);

        final int ROWS = 10;
        final int COLS = 3;
        Vector2i mpos = new Vector2i(1, 0);

        SPanel mainPanel = new SPanel(COLS, ROWS);
        mainPanel.add(new SFiller(10, 10), new Vector2i(0, 0));
        mainPanel.add(new SFiller(10, 10), new Vector2i(COLS - 1, ROWS - 1));

        // message
        notice = new STextArea("Select which mods to load", MainMenu.TEXT_PROPERTIES);
        mainPanel.add(notice, mpos.add(0, 1));

        // size selection
        SPanel sizeSelection = new SPanel(4, 1, false, false);
        sizeSelection.add(new STextArea("Size", MainMenu.TEXT_PROPERTIES), new Vector2i(0, 0));
        this.game = game;
        xSizeSelector = new SDropDown(game.gui(), 1, "100", "200", "500", "1000");
        sizeSelection.add(xSizeSelector, new Vector2i(1, 0));
        sizeSelection.add(new STextArea("X", MainMenu.TEXT_PROPERTIES), new Vector2i(2, 0));
        ySizeSelector = new SDropDown(game.gui(), 1, "100", "200", "500", "1000");
        sizeSelection.add(ySizeSelector, new Vector2i(3, 0));
        mainPanel.add(sizeSelection, mpos.add(0, 1));

        // add mod buttons
        SContainer modPanel = new SPanel(1, nOfMods);
        Vector2i pos = new Vector2i(0, -1);
        for (Mod mod : modList) {
            if (mod instanceof MapGeneratorMod) {
                mapGeneratorList.add((MapGeneratorMod) mod);

            } else {
                SToggleButton button = new SToggleButton(mod.getModName(), MainMenu.BUTTON_PROPERTIES_STRETCH);
                button.setActive(true);
                toggleList.add(button, mod);
                modPanel.add(button, pos.add(0, 1));
            }
        }
        mainPanel.add(modPanel, mpos.add(0, 1));

        // generator selection
        mapGeneratorList.add(new DefaultMapGenerator(0));
        mapGeneratorSelector = new SDropDown(game.gui(), MainMenu.BUTTON_PROPERTIES_STATIC, 0, mapGeneratorList, Mod::getModName);
        mainPanel.add(mapGeneratorSelector, mpos.add(0, 1));

        SButton propertiesComponent = new SButton("Set Generator Properties", () -> {
            // collect map generator properties
            MapGeneratorMod generator = mapGeneratorList.get(mapGeneratorSelector.getSelectedIndex());
            Collection<MapGeneratorMod.Property> properties = generator.getProperties();
            SPanel propPanel = new SPanel(2, properties.size());
            Vector2i propertyButtonPos = new Vector2i(0, -1);

            for (MapGeneratorMod.Property property : properties) {
                SSlider slider = new SSlider(property.minimum, property.maximum, property.current, MainMenu.BUTTON_PROPERTIES_STRETCH);
                slider.addChangeListener(newValue -> property.current = newValue);

                int precision = 3 - (int) (Math.log10(property.maximum - property.minimum));
                SActiveTextArea value = new SActiveTextArea(
                        () -> String.format("%6.0" + precision + "f", property.current),
                        MainMenu.TEXT_PROPERTIES
                );

                propPanel.add(slider, propertyButtonPos.add(0, 1));
                propPanel.add(value, propertyButtonPos.add(1, 0));

                propertyButtonPos.add(-1, 0);
            }
            SFrame frame = new SFrame(generator.getModName() + " Properties");
            frame.setMainPanel(SContainer.column(
                    propPanel,
                    new SButton("Accept", frame::dispose)
            ));
            mapGeneratorSelector.addStateChangeListener(i -> frame.dispose());

            game.gui().addFrame(frame, this.getX(), this.getY());
        });

        mainPanel.add(propertiesComponent, mpos.add(0, 1));

        // generate button
        mainPanel.add(new SFiller(0, 50), mpos.add(0, 1));
        SButton generate = new SButton("Generate", MainMenu.BUTTON_PROPERTIES_STATIC);
        mainPanel.add(generate, mpos.add(0, 1));

        setMainPanel(mainPanel);
        pack();

        // start game action
        generate.addLeftClickListener(this::generate);
    }

    public void generate() {
        try {
            // get and install map generator
            int selected = mapGeneratorSelector.getSelectedIndex();
            MapGeneratorMod generatorMod = mapGeneratorList.get(selected);

            int xSize = Integer.parseInt(xSizeSelector.getSelected());
            int ySize = Integer.parseInt(ySizeSelector.getSelected());
            generatorMod.setXSize(xSize);
            generatorMod.setYSize(ySize);

            // install selected mods
            List<Mod> targets = new ArrayList<>();
            for (int i = 0; i < toggleList.size(); i++) {
                if (toggleList.left(i).isActive()) {
                    Mod mod = toggleList.right(i);

                    if (mod instanceof MapGeneratorMod) {
                        Logger.ASSERT.print("map generator mod found in modlist");

                    } else {
                        targets.add(mod);
                    }
                }
            }

            modLoader.initMods(targets);

            if (targets.isEmpty()) throw new ModLoader.IllegalNumberOfModulesException("No mods selected");
            game.map().generateNew(game, generatorMod);

            // set camera to middle of map
            Vector2f size = game.map().getSize();
            Vector3f cameraFocus = new Vector3f(size.x / 2, size.y / 2, 0);
            Camera cam = game.camera();
            Vector3f cameraEye = new Vector3f(cameraFocus).add(-20, -20, 20);
            cam.set(cameraFocus, cameraEye);

            // start
            modLoader.startGame();
            this.setVisible(false);

        } catch (Exception e) {
            notice.setText(e.getMessage());
            Logger.WARN.print(e);
        }
    }
}
