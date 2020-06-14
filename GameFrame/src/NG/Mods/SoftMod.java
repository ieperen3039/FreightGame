package NG.Mods;

import NG.Core.Game;
import NG.Core.Version;
import NG.Entities.Locomotive;
import NG.Entities.StationImpl;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    public final List<Locomotive.Properties> locomotives = new ArrayList<>();
    public final List<Wagon.Properties> wagons = new ArrayList<>();

    /** constructor that wraps a directory containing a {@code description.json} file into a soft mod */
    public SoftMod(Path path) throws IOException {
        File file = path.resolve("description.json").toFile();
        JsonNode root = JSON_PARSER_FACTORY.readTree(file);

        JsonNode nameNode = root.findValue("set_name");
        name = nameNode != null ? nameNode.textValue() : path.getFileName().toString();
        JsonNode versionNode = root.findValue("version");
        this.version = versionNode != null ? new Version(versionNode.textValue()) : new Version(0, 0);

        /* locomotives */
        JsonNode locosNode = root.findValue("locomotives");
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
                float tractiveEffort = findOrThrow(locoNode, "tractiveEffort").floatValue();

                List<String> railTypes = getRailTypes(locoNode, "railtypes");

                locomotives.add(new Locomotive.Properties(name, length, mass, linearResistance, quadraticResistance, mesh, railTypes, tractiveEffort));
            }
        }

        /* wagons */
        JsonNode wagonsNode = root.findValue("wagons");
        if (wagonsNode != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = wagonsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> fieldNode = fields.next();
                String name = fieldNode.getKey();
                JsonNode wagonNode = fieldNode.getValue();

                JsonNode meshNode = findOrThrow(wagonNode, "mesh");
                Resource<Mesh> mesh = Mesh.createResource(path.resolve(meshNode.textValue()));

                float mass = findOrThrow(wagonNode, "mass").floatValue();
                float length = findOrThrow(wagonNode, "length").floatValue();
                float maxSpeed = findOrThrow(wagonNode, "max_speed").floatValue();
                float linearResistance = findOrThrow(wagonNode, "r1").floatValue();

                List<String> railtypes = getRailTypes(wagonNode, "railtypes");
                Map<CargoType, Integer> capacity = Map.of(StationImpl.DEBUG_CUBES, 10);

                wagons.add(new Wagon.Properties(name, length, mass, linearResistance, mesh, railtypes, maxSpeed, capacity));
            }
        }
    }

    private List<String> getRailTypes(JsonNode node, String fieldName) {
        JsonNode railTypeNode = node.findValue(fieldName);
        List<String> railTypes = new ArrayList<>();
        if (railTypeNode != null) {
            Iterator<JsonNode> elements = railTypeNode.elements();
            while (elements.hasNext()) {
                railTypes.add(elements.next().textValue());
            }
        }
        return railTypes;
    }

    private JsonNode findOrThrow(JsonNode locoNode, String element) throws IOException {
        JsonNode node = locoNode.findValue(element);
        if (node == null) throw new IOException("Could not find required field " + element);
        return node;
    }

    @Override
    public String getModName() {
        return name;
    }

    @Override
    public void init(Game game) {
        TypeCollection types = game.objectTypes();
        types.locomotiveTypes.addAll(locomotives);
        types.wagonTypes.addAll(wagons);
    }

    @Override
    public void cleanup() {

    }

    @Override
    public Version getVersionNumber() {
        return version;
    }
}
