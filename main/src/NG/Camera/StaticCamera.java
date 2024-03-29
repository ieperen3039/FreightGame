package NG.Camera;


import NG.Core.Game;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.Serializable;

/**
 * @author Geert van Ieperen created on 22-12-2017. a camera that doesn't move
 */
public class StaticCamera implements Camera, Serializable {
    private Vector3fc eye, focus;
    private Vector3fc up;

    public StaticCamera(Vector3fc eye, Vector3fc focus, Vector3fc up) {
        this.eye = eye;
        this.focus = focus;
        this.up = up;
    }

    @Override
    public void init(Game game) throws Exception {

    }

    @Override
    public Vector3f vectorToFocus() {
        return new Vector3f(focus).sub(eye);
    }

    @Override
    public void updatePosition(float deltaTime) {

    }

    @Override
    public Vector3fc getEye() {
        return eye;
    }

    @Override
    public Vector3fc getFocus() {
        return focus;
    }

    @Override
    public Vector3fc getUpVector() {
        return up;
    }

    @Override
    public void set(Vector3fc focus, Vector3fc eye) {
        this.focus = new Vector3f(focus);
        this.eye = new Vector3f(eye);
    }

    @Override
    public void cleanup() {

    }

    @Override
    public boolean isIsometric() {
        return true;
    }

    @Override
    public void onMouseMove(int xDelta, int yDelta, float xPos, float yPos) {

    }

    @Override
    public void onScroll(float value) {

    }

    @Override
    public void onClick(int button, int xRel, int yRel) {

    }

    @Override
    public void onRelease(int button) {

    }
}
