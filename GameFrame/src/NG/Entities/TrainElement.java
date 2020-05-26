package NG.Entities;

import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Resources.Resource;
import NG.Tracks.TrackType;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import java.util.List;

/**
 * @author Geert van Ieperen created on 19-5-2020.
 */
public interface TrainElement {
    default void draw(SGL gl, Vector3fc position, Quaternionfc rotation, Entity sourceEntity) {
        ShaderProgram shader = gl.getShader();

        if (shader instanceof MaterialShader) {
            ((MaterialShader) shader).setMaterial(Material.METAL);
        }

        gl.pushMatrix();
        {
            gl.translate(position);
            gl.rotate(rotation);

            gl.render(getProperties().mesh.get(), sourceEntity);
        }
        gl.popMatrix();
    }

    Properties getProperties();

    class Properties {
        private final String name;
        public final float length;
        public final float mass;
        public final float linearResistance;
        public final float quadraticResistance;

        private final Resource<Mesh> mesh;
        private final List<String> trackTypes;

        public Properties(
                String name, float length, float mass, float linearResistance, float quadraticResistance,
                Resource<Mesh> mesh, List<String> trackTypes
        ) {
            this.name = name;
            this.length = length;
            this.mass = mass;
            this.linearResistance = linearResistance;
            this.quadraticResistance = quadraticResistance;
            this.mesh = mesh;
            this.trackTypes = trackTypes;
        }

        @Override
        public String toString() {
            return name;
        }

        boolean isCompatibleWith(TrackType type) {
            return trackTypes.contains(type.toString());
        }
    }
}
