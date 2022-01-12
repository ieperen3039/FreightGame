package NG.Entities;

import NG.Core.Coloring;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Freight.Cargo;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTool.AbstractMouseTool;
import NG.Mods.CargoType;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.AssetHandling.Asset;
import org.joml.Vector3fc;

import java.util.*;

/**
 * @author Geert van Ieperen created on 21-6-2020.
 */
public class Industry extends Storage {
    private static final double CARGO_SPAWN_FREQUENCY = 0.25f; // updates per second

    private transient Properties properties;
    private final String typeName;
    private final Coloring coloring = new Coloring(Color4f.WHITE);

    private double nextCargoSpawn;

    public Industry(
            Game game, Vector3fc position, double spawnTime, Properties properties
    ) {
        super(game, position, spawnTime);
        this.nextCargoSpawn = game.timer().getGameTime();
        this.properties = properties;
        this.typeName = properties.name;
    }

    @Override
    public void update() {
        double now = game.timer().getGameTime();
        while (now > nextCargoSpawn) {
            Rule rule = properties.cargoRule;
            int ruleExecutions = (int) (rule.rulesPerSecond / CARGO_SPAWN_FREQUENCY);

            if (rule.in.isEmpty()) {
                // short procedure for production
                for (CargoType type : rule.out.keySet()) {
                    int chunkSize = ruleExecutions * rule.out.get(type);
                    getContents().add(new Cargo(type, chunkSize, nextCargoSpawn, this));
                }

            } else {
                int numRuleExecutions = ruleExecutions;
                // collect maximum number of executable rules
                for (CargoType type : rule.in.keySet()) {
                    int amountFound = getContents().getAmountOf(type);
                    int maxRules = amountFound / rule.in.get(type);
                    numRuleExecutions = Math.min(numRuleExecutions, maxRules);
                }
                if (numRuleExecutions == 0) return;
                // take cargo
                for (CargoType type : rule.in.keySet()) {
                    int amount = rule.in.get(type) * numRuleExecutions;
                    getContents().take(type, amount);
                }
                // generate new cargo
                // if rule.out is empty (consume only), this is not executed
                for (CargoType type : rule.out.keySet()) {
                    int chunkSize = numRuleExecutions * rule.out.get(type);
                    getContents().add(new Cargo(type, chunkSize, nextCargoSpawn, this));
                }
            }

            nextCargoSpawn += 1.0 / CARGO_SPAWN_FREQUENCY;
        }
    }

    @Override
    public void draw(SGL gl) {
        MaterialShader.ifPresent(gl, _gl -> _gl.setMaterial(Material.ROUGH, coloring.getColor()));

        gl.pushMatrix();
        {
            gl.translate(getPosition());
            gl.render(properties.mesh.get(), this);
        }

        gl.popMatrix();
    }

    @Override
    public void reactMouse(AbstractMouseTool.MouseAction action, KeyControl keys) {

    }

    @Override
    public void setMarking(Coloring.Marking mark) {
        coloring.addMark(mark);
    }

    public Collection<CargoType> getAcceptedCargo() {
        Collection<CargoType> accepted = new ArrayList<>(8);

        Map<CargoType, Integer> in = properties.cargoRule.in;
        accepted.addAll(in.keySet());

        return accepted;
    }

    public static List<Industry> getNearbyIndustries(Game game, Vector3fc position, float range) {
        float rangeSq = range * range;
        List<Industry> nearby = new ArrayList<>();

        for (Entity entity : game.state()) {
            if (entity instanceof Industry) {
                Industry industry = (Industry) entity;
                if (industry.getPosition().distanceSquared(position) < rangeSq) {
                    nearby.add(industry);
                }
            }
        }

        return nearby;
    }

    @Override
    public String toString() {
        return "Industry " + properties.name;
    }

    @Override
    public void restoreFields(Game game) {
        super.restoreFields(game);
        properties = game.objectTypes().getIndustryByName(typeName);
    }

    public static class Rule {
        public final Map<CargoType, Integer> in;
        public final Map<CargoType, Integer> out;
        public final float rulesPerSecond;

        public static Rule generate(CargoType out, float amountPerSecond) {
            return new Rule(Map.of(out, 1), Collections.emptyMap(), amountPerSecond);
        }

        public static Rule accept(CargoType in, float amountPerSecond) {
            return new Rule(Collections.emptyMap(), Map.of(in, 1), amountPerSecond);
        }

        public Rule(
                Map<CargoType, Integer> in, Map<CargoType, Integer> out, float rulesPerSecond
        ) {
            assert in != null;
            assert out != null;

            this.in = in;
            this.out = out;
            this.rulesPerSecond = rulesPerSecond;
        }
    }

    public static class Properties {
        public final String name;
        public final Rule cargoRule;
        public final Asset<Mesh> mesh;
        public final int maxCargo;
        public final Color4f color; // debug

        public Properties(
                String name, Asset<Mesh> mesh, Rule cargoRule, int maxCargo
        ) {
            this.name = name;
            this.cargoRule = cargoRule;
            this.mesh = mesh;
            this.maxCargo = maxCargo;
            this.color = Color4f.WHITE;
        }
    }
}
