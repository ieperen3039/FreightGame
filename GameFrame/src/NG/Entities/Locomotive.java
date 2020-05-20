package NG.Entities;

import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Resources.Resource;
import NG.Tools.Directory;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 5-5-2020.
 */
public class Locomotive implements TrainElement {
    private final Resource<Mesh> mesh;

    public Locomotive() {
        this.mesh = Mesh.createResource(Directory.meshes, "locos", "LittleRedDiesel.ply");
    }

    @Override
    public void draw(SGL gl, Vector3fc position, Quaternionfc rotation, Entity sourceEntity) {
        ShaderProgram shader = gl.getShader();

        if (shader instanceof MaterialShader) {
            ((MaterialShader) shader).setMaterial(Material.METAL);
        }

        gl.pushMatrix();
        {
            gl.translate(position);
            gl.rotate(rotation);

            gl.translate(-0.5f, 0, 0); // TODO set standard for loco mesh
            gl.render(mesh.get(), sourceEntity);
        }
        gl.popMatrix();
    }

    @Override
    public float realLength() {
        return 2;
    }
}
