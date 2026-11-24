package io.github.ieperen3039.trainsinspace.Settings;

import io.github.ieperen3039.ngn.DataStructures.Generic.Color4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.Serializable;

/**
 * A class that collects a number of settings. It is the only class whose fields are always initialized upon creation.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Settings extends io.github.ieperen3039.ngn.Settings.Settings implements Serializable {
    public int TARGET_TPS = 10;

    public final float CLICK_BOX_WIDTH = 1.5f;
    public final float CLICK_BOX_HEIGHT = 0.1f;
    public final float CLICK_BOX_RESOLUTION = 1f;

    public final float TRACK_HEIGHT_ABOVE_GROUND = 0.2f;
    public int STATION_RANGE = 10;
    public final float TRACK_WIDTH = CLICK_BOX_WIDTH / 4;
    public final float TRACK_HEIGHT_SPACE = 1.0f;
    public final float TRACK_COLLISION_BOX_LENGTH = 2.0f;

    public float PARTICLE_SIZE = 1;
    public Color4f SUNLIGHT_COLOR = Color4f.rgb(255, 255, 200);
    public float SUNLIGHT_INTENSITY = 0.4f;
    public Vector3fc SUNLIGHT_POSITION = new Vector3f(-1, 1, 1.5f);

    // TODO this is a global variable, not a setting
    public boolean RENDER_COLLISION_BOX;
}
