package io.github.ieperen3039.trainsinspace.Content.Scenario;

import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Core.ModLoader;
import io.github.ieperen3039.trainsinspace.Entities.Industry;
import io.github.ieperen3039.trainsinspace.GameMap.GameMap;
import io.github.ieperen3039.trainsinspace.GameMap.ImageMapGenerator;
import io.github.ieperen3039.trainsinspace.GameMap.MapGeneratorMod;
import io.github.ieperen3039.trainsinspace.Mods.Mod;
import io.github.ieperen3039.trainsinspace.Mods.TypeCollection;
import io.github.ieperen3039.trainsinspace.AssetHandling.Resource;
import io.github.ieperen3039.trainsinspace.Settings.Settings;
import io.github.ieperen3039.trainsinspace.Tools.Logger;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen created on 20-9-2020.
 */
public class FileScenario extends Scenario {
    // god knows why we need a *factory builder*
    private static final ObjectMapper JSON_PARSER_FACTORY = new ObjectMapper(
            new JsonFactory(new JsonFactoryBuilder().configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true))
    );
    public static final Resource.Path SCENARIOS_DIRECTORY = Resource.Path.get("scenarios");

    public final Resource.Path scenarioPath;
    private final Resource<JsonNode> scenarioDescription;

    public FileScenario(ModLoader modLoader, String scenarioName) throws IOException {
        super(modLoader);
        scenarioPath = SCENARIOS_DIRECTORY.resolve(scenarioName);

        Resource.Path descriptionFile = scenarioPath.resolve("description.json");
        scenarioDescription = Resource.get(path -> JSON_PARSER_FACTORY.readTree(path.asStream()), descriptionFile);
    }

    @Override
    protected void setEntities(Game game, Settings settings) {
        TypeCollection entityTypes = game.objectTypes();
        GameMap map = game.map();

        JsonNode industriesNode = scenarioDescription.get().get("industries");

        if (industriesNode != null) {
            assert industriesNode.isArray();

            for (JsonNode industryNode : industriesNode) {
                String type = industryNode.get("type").textValue();
                Industry.Properties industryProps = entityTypes.getIndustryByName(type);

                JsonNode positionNode = industryNode.get("position");
                Vector2f position2f = parseVector2f(positionNode);
                Vector3f position = toPosition(map, position2f);

                Industry industry = new Industry(game, position, 0, industryProps);
                game.state().addEntity(industry);
            }
        }
    }

    @Override
    protected void setMap(Game game, Settings settings) {
        float edgeLength = scenarioDescription.get().get("map_edge_length").floatValue();
        Resource.Path imagePath = scenarioPath.resolve("heightmap.png");

        MapGeneratorMod generator = new ImageMapGenerator(imagePath, edgeLength);
        game.map().generateNew(game, generator);
    }

    @Override
    protected List<Mod> getMods(ModLoader modLoader) {
        JsonNode modNode = scenarioDescription.get().get("mods");
        assert modNode.isArray();

        List<Mod> modList = new ArrayList<>();

        for (JsonNode elt : modNode) {
            String modName = elt.textValue();
            Mod mod = modLoader.getModByName(modName);

            if (mod == null) {
                Logger.ERROR.print("Could not find mod " + modName);
            } else {
                modList.add(mod);
            }
        }

        return modList;
    }

    private Vector3f parseVector3f(JsonNode vectorNode) {
        assert vectorNode.isArray();
        assert vectorNode.size() == 3;

        return new Vector3f(
                vectorNode.get(0).floatValue(),
                vectorNode.get(1).floatValue(),
                vectorNode.get(2).floatValue()
        );
    }

    private Vector2f parseVector2f(JsonNode vectorNode) {
        assert vectorNode.isArray();
        assert vectorNode.size() == 2;

        return new Vector2f(
                vectorNode.get(0).floatValue(),
                vectorNode.get(1).floatValue()
        );
    }

    private Vector3f toPosition(GameMap map, Vector2f position2f) {
        return new Vector3f(position2f.x, position2f.y, map.getHeightAt(position2f.x, position2f.y));
    }
}
