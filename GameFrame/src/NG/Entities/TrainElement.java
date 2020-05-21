package NG.Entities;

import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Resources.Resource;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

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
        public final float length;
        public final float mass;
        private final Resource<Mesh> mesh;

        public Properties(float length, float mass, Resource<Mesh> mesh) {
            this.length = length;
            this.mass = mass;
            this.mesh = mesh;
        }
    }
}
