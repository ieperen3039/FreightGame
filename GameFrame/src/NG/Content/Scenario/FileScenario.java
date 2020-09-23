package NG.Content.Scenario;

import NG.Core.Game;
import NG.Core.ModLoader;
import NG.GameMap.ImageMapGenerator;
import NG.GameMap.MapGeneratorMod;
import NG.Mods.Mod;
import NG.Settings.Settings;
import NG.Tools.Directory;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Geert van Ieperen created on 20-9-2020.
 */
public class FileScenario extends Scenario {
    public final Path scenariosPath;

    public FileScenario(ModLoader modLoader, String scenarioName) {
        super(modLoader);
        scenariosPath = Directory.scenarios.getPath(scenarioName);
    }

    @Override
    protected void setEntities(Game game, Settings settings) {

    }

    @Override
    protected void setMap(Game game, Settings settings) {
        MapGeneratorMod generator = new ImageMapGenerator(scenariosPath.resolve("heightmap.png"));
        game.map().generateNew(game, generator);
    }

    @Override
    protected List<Mod> getMods(ModLoader modLoader) {
        return modLoader.allMods();
    }
}
