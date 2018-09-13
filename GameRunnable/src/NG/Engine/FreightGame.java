package NG.Engine;

import NG.Camera.Camera;
import NG.Camera.SimpleKeyCamera;

/**
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class FreightGame {
    public final Camera camera;

    public FreightGame() {
        camera = new SimpleKeyCamera(this);
    }

    private void init() {
        // init all fields
    }

    public void root() {
        init();
    }
}
