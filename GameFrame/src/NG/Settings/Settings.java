package NG.Settings;

import NG.DataStructures.Generic.Color4f;

/**
 * A class that collects a number of settings. It is the only class whose fields are always initialized upon creation.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Settings {
    public static final String GAME_NAME = "Trains in SPAAAACE"; // laaaaame
    public boolean DEBUG = true;

    // game engine settings
    public int TARGET_TPS = 20;

    // video settings
    public int TARGET_FPS = 60;
    public boolean V_SYNC = true;
    public int WINDOW_WIDTH = 1200;
    public int WINDOW_HEIGHT = 800;
    public int ANTIALIAS_LEVEL = 1;
    public int STATIC_SHADOW_RESOLUTION = 1024;
    public int DYNAMIC_SHADOW_RESOLUTION = 0;
    public float PARTICLE_SIZE = 1;

    // camera settings
    public boolean ISOMETRIC_VIEW = false;
    public float CAMERA_ZOOM_SPEED = 0.1f;
    public static float Z_NEAR = 1f;
    public static float Z_FAR = 250;
    public float MAX_CAMERA_DIST = Z_FAR / 2f;
    public float MIN_CAMERA_DIST = Z_NEAR * 2f;
    public static float FOV = (float) Math.toRadians(40);

    // UI settings
    public static int TOOL_BAR_HEIGHT = 80;
    public static final float CLICK_BOX_WIDTH = 1.5f;
    public static final float CLICK_BOX_HEIGHT = 0.1f;
    public static final float CLICK_BOX_RESOLUTION = 1f;

    // in-game functional settings
    public static final float TRACK_HEIGHT_ABOVE_GROUND = 0.2f;
    public static int STATION_RANGE = 10;
    public static final float TRACK_WIDTH = CLICK_BOX_WIDTH / 4;
    public static final float TRACK_HEIGHT_SPACE = 1.0f;
    public static final float TRACK_COLLISION_BOX_LENGTH = 2.0f;

    // in-game appearance settings
    public Color4f AMBIENT_LIGHT = new Color4f(1, 1, 1, 0.2f);
    public Color4f SUNLIGHT_COLOR = Color4f.WHITE;
    public float SUNLIGHT_INTENSITY = 0.8f;
    public boolean RENDER_COLLISION_BOX = true;
}
