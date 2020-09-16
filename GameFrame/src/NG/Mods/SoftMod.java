package NG.Mods;

import NG.Core.Game;
import NG.Core.Version;
import NG.Entities.Industry;
import NG.Entities.Locomotive;
import NG.Entities.Wagon;
import NG.Rendering.MeshLoading.Mesh;
import NG.Resources.Resource;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * A mod that encapsulates any softmod. In contrary to regular mods, this one has a constructor as it isn't loaded by
 * the modloader. In case a hard-mod wants to incorporate a soft-mod, it must supply a no-arg constructor.
 * @author Geert van Ieperen created on 21-5-2020.
 */
public class SoftMod implements Mod {
    // god knows why we need a *factory builder*
    private static final ObjectMapper JSON_PARSER_FACTORY = new ObjectMapper(
            new JsonFactory(new JsonFactoryBuilder().configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true))
    );

    public final String name;
    public final Version version;

    private final JsonNode fileRoot;
    private final Path path;

    /** constructor that wraps a directory containing a {@code description.json} file into a soft mod */
    public SoftMod(Path path) throws IOException {
        this.path = path;
        File file = path.resolve("description.json").toFile();
        fileRoot = JSON_PARSER_FACTORY.readTree(file);

        JsonNode nameNode = fileRoot.findValue("set_name");
        name = nameNode != null ? nameNode.textValue() : path.getFileName().toString();
        JsonNode versionNode = fileRoot.findValue("version");
        this.version = versionNode != null ? new Version(versionNode.textValue()) : new Version(0, 0);
    }

    private JsonNode findOrThrow(JsonNode locoNode, String element) throws IOException {
        JsonNode node = locoNode.get(element);
        if (node == null) throw new IOException("Could not find required field \"" + element + "\"");
        return node;
    }

    @Override
    public String getModName() {
        return name;
    }

    @Override
    public void init(Game game) throws IOException {
        TypeCollection types = game.objectTypes();

        List<Locomotive.Properties> locomotives = types.locomotiveTypes;
        List<Wagon.Properties> wagons = types.wagonTypes;
        List<CargoType> cargoTypes = types.cargoTypes;
        List<Industry.Properties> industries = types.industryTypes;

        Map<String, CargoType> cargoTypeMap = new HashMap<>();
        cargoTypes.forEach(type -> cargoTypeMap.put(type.name(), type));

        /* cargo types */
        JsonNode cargoesNode = fileRoot.get("cargo");
        if (cargoesNode != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = cargoesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> fieldNode = fields.next();
                String cargoName = fieldNode.getKey();
                JsonNode cargoNode = fieldNode.getValue();

                JsonNode prices = findOrThrow(cargoNode, "price");
                float[] pricePerDay = new float[prices.size()];
                Iterator<JsonNode> eltItr = prices.elements();
                for (int i = 0; i < prices.size(); i++) {
                    pricePerDay[i] = eltItr.next().floatValue();
                }

                float minimum = findOrThrow(cargoNode, "minimum_price").floatValue();

                CargoType cargoType = new CargoType(cargoName, pricePerDay, minimum);
                cargoTypes.add(cargoType);
                cargoTypeMap.put(cargoName, cargoType);
            }
        }

        /* locomotives */
        JsonNode locosNode = fileRoot.get("locomotives");
        if (locosNode != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = locosNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> fieldNode = fields.next();
                String name = fieldNode.getKey();
                JsonNode locoNode = fieldNode.getValue();

                String meshFileName = findOrThrow(locoNode, "mesh").textValue();
                Resource<Mesh> mesh = Mesh.createResource(path.resolve(meshFileName));

                float mass = findOrThrow(locoNode, "mass").floatValue();
                float length = findOrThrow(locoNode, "length").floatValue();
                float linearResistance = findOrThrow(locoNode, "r1").floatValue();
                float quadraticResistance = findOrThrow(locoNode, "r2").floatValue();
                float tractiveEffort = findOrThrow(locoNode, "tractive_effort").floatValue();

                List<String> railTypes = readTextArray(locoNode.get("railtypes"));

                int buildCost = findOrThrow(locoNode, "build_cost").intValue();
                float maintenance = findOrThrow(locoNode, "maintenance_ps").floatValue();

                locomotives.add(new Locomotive.Properties(
                        name, length, mass, linearResistance, quadraticResistance, mesh, railTypes, buildCost,
                        maintenance, tractiveEffort
                ));
            }
        }

        /* wagons */
        JsonNode wagonsNode = fileRoot.get("wagons");
        if (wagonsNode != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = wagonsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> fieldNode = fields.next();
                String name = fieldNode.getKey();
                JsonNode wagonNode = fieldNode.getValue();

                String meshFileName = findOrThrow(wagonNode, "mesh").textValue();
                Resource<Mesh> mesh = Mesh.createResource(path.resolve(meshFileName));

                float mass = findOrThrow(wagonNode, "mass").floatValue();
                float length = findOrThrow(wagonNode, "length").floatValue();
                float maxSpeed = findOrThrow(wagonNode, "max_speed").floatValue();
                float linearResistance = findOrThrow(wagonNode, "r1").floatValue();
                float loadTime = findOrThrow(wagonNode, "load_time").floatValue();

                List<String> railtypes = readTextArray(wagonNode.get("railtypes"));

                JsonNode cargoTypeNode = findOrThrow(wagonNode, "cargo_types");
                Map<CargoType, Integer> capacity = readIntMap(cargoTypeNode, cargoTypeMap);

                int buildCost = findOrThrow(wagonNode, "build_cost").intValue();
                float maintenance = findOrThrow(wagonNode, "maintenance_ps").floatValue();
                wagons.add(new Wagon.Properties(
                        name, length, mass, linearResistance, maxSpeed, mesh, railtypes, buildCost, maintenance,
                        capacity, loadTime
                ));
            }
        }

        JsonNode industriesNode = fileRoot.get("industries");
        if (industriesNode != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = industriesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> fieldNode = fields.next();
                String name = fieldNode.getKey();
                JsonNode industryNode = fieldNode.getValue();

                String meshFileName = findOrThrow(industryNode, "mesh").textValue();
                Resource<Mesh> mesh = Mesh.createResource(path.resolve(meshFileName));

                int maxCargo = findOrThrow(industryNode, "max_cargo").intValue();

                JsonNode ruleNode = findOrThrow(industryNode, "rule");
                float rulesPerSecond = findOrThrow(ruleNode, "rate").floatValue();

                JsonNode inRuleNode = ruleNode.get("in");
                Map<CargoType, Integer> ruleIn = readIntMap(inRuleNode, cargoTypeMap);

                JsonNode outRuleNode = ruleNode.get("out");
                Map<CargoType, Integer> ruleOut = readIntMap(outRuleNode, cargoTypeMap);

                Industry.Rule rule = new Industry.Rule(ruleIn, ruleOut, rulesPerSecond);
                industries.add(new Industry.Properties(name, mesh, rule, maxCargo));
            }
        }
    }

    /** reads an array of strings. If node is null, return an unmodifiable empty list */
    private List<String> readTextArray(JsonNode node) {
        if (node == null) return Collections.emptyList();
        assert node.isArray();

        List<String> results = new ArrayList<>();
        Iterator<JsonNode> elements = node.elements();
        while (elements.hasNext()) {
            results.add(elements.next().textValue());
        }

        return results;
    }

    /**
     * parses ruleNode as a map of strings to integers, and maps these string fields to objects using the given map. If
     * ruleNode is null, returns an empty unmodifiable map
     */
    public <T> Map<T, Integer> readIntMap(JsonNode ruleNode, Map<String, T> cargoTypeMap) {
        if (ruleNode == null) return Collections.emptyMap();

        Map<T, Integer> ruleMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> elements = ruleNode.fields();
        while (elements.hasNext()) {
            Map.Entry<String, JsonNode> eltNode = elements.next();

            String type = eltNode.getKey();
            int amount = eltNode.getValue().intValue();
            T cargoType = cargoTypeMap.get(type);
            ruleMap.put(cargoType, amount);
        }

        return ruleMap;
    }

    @Override
    public void cleanup() {

    }

    @Override
    public Version getVersionNumber() {
        return version;
    }
}
