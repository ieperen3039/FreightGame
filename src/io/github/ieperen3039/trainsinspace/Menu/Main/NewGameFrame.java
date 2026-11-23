package io.github.ieperen3039.trainsinspace.Menu.Main;


import io.github.ieperen3039.ngn.Camera.Camera;
import io.github.ieperen3039.ngn.DataStructures.Generic.PairList;
import io.github.ieperen3039.ngn.Tools.Logger;
import io.github.ieperen3039.ngn.UserInterface.Components.*;
import io.github.ieperen3039.ngn.UserInterface.LayoutManagers.ListLayoutManager;
import io.github.ieperen3039.trainsinspace.Camera.TycoonCamera;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Core.ModLoader;
import io.github.ieperen3039.trainsinspace.GameMap.DefaultMapGenerator;
import io.github.ieperen3039.trainsinspace.GameMap.MapGeneratorMod;
import io.github.ieperen3039.trainsinspace.Mods.Mod;
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
    private final STextComponent notice;

    public NewGameFrame(final Game game, final ModLoader loader) {
        super("New Game Frame");
        modLoader = loader;
        List<Mod> modList = modLoader.allMods();
        mapGeneratorList = new ArrayList<>();
        int nOfMods = modList.size();
        toggleList = new PairList<>(nOfMods);

        SPanel mainPanel = new SPanel(new ListLayoutManager(false));

        // message
        notice = new STextComponent("Select which mods to load", MainMenu.TEXT_PROPERTIES);
        mainPanel.add(notice);

        // size selection
        SPanel sizeSelection = new SPanel(new ListLayoutManager(false), false);
        sizeSelection.add(new STextComponent("Size", MainMenu.TEXT_PROPERTIES));
        this.game = game;
        xSizeSelector = new SDropDown(game.gui(), 1, "100", "200", "500", "1000");
        sizeSelection.add(xSizeSelector);
        sizeSelection.add(new STextComponent("X", MainMenu.TEXT_PROPERTIES));
        ySizeSelector = new SDropDown(game.gui(), 1, "100", "200", "500", "1000");
        sizeSelection.add(ySizeSelector);
        mainPanel.add(sizeSelection);

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
                modPanel.add(button);
            }
        }
        mainPanel.add(modPanel);

        // generator selection
        mapGeneratorList.add(new DefaultMapGenerator(0));
        mapGeneratorSelector = new SDropDown(game.gui(), MainMenu.BUTTON_PROPERTIES_STATIC, 0, mapGeneratorList, Mod::getModName);
        mainPanel.add(mapGeneratorSelector);

        SButton propertiesComponent = new SButton("Set Generator Properties", () -> {
            // collect map generator properties
            MapGeneratorMod generator = mapGeneratorList.get(mapGeneratorSelector.getSelectedIndex());
            Collection<MapGeneratorMod.Property> properties = generator.getProperties();
            SPanel propPanel = createPropertiesPanel(properties);
            SFrame frame = new SFrame(generator.getModName() + " Properties");
            frame.setMainPanel(SContainer.column(
                    propPanel,
                    new SButton("Accept", frame::dispose)
            ));
            mapGeneratorSelector.addStateChangeListener(i -> frame.dispose());

            game.gui().addFrame(frame, this.getX(), this.getY());
        });

        mainPanel.add(propertiesComponent);

        // generate button
        mainPanel.add(new SFiller(0, 50));
        SButton generate = new SButton("Generate", MainMenu.BUTTON_PROPERTIES_STATIC);
        mainPanel.add(generate);

        setMainPanel(mainPanel);
        pack();

        // start game action
        generate.addLeftClickListener(this::generate);
    }

    private static SPanel createPropertiesPanel(Collection<MapGeneratorMod.Property> properties) {
        SPanel propPanel = new SPanel(2, properties.size());

        for (MapGeneratorMod.Property property : properties) {
            SSlider slider = new SSlider(property.minimum, property.maximum, property.current, MainMenu.BUTTON_PROPERTIES_STRETCH);
            slider.addChangeListener(newValue -> property.current = newValue);

            int precision = 3 - (int) (Math.log10(property.maximum - property.minimum));
            SActiveTextComponent value = new SActiveTextComponent(
                    () -> String.format("%6.0" + precision + "f", property.current),
                    MainMenu.TEXT_PROPERTIES
            );

            propPanel.add(slider);
            propPanel.add(value);
        }
        return propPanel;
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
            game.map().generateNew(generatorMod);

            // set camera to middle of map
            Vector2f size = game.map().getSize();
            Vector3f cameraFocus = new Vector3f(size.x / 2, size.y / 2, 0);
            Camera cam = game.camera();

            if (cam instanceof TycoonCamera tCam) {
                Vector3f cameraEye = new Vector3f(cameraFocus).add(-20, -20, 20);
                tCam.set(cameraFocus, cameraEye);
            } else {
                cam.set(cameraFocus);
            }

            // start
            modLoader.startGame();
            this.setVisible(false);

        } catch (Exception e) {
            notice.setText(e.getMessage());
            Logger.WARN.print(e);
        }
    }
}
