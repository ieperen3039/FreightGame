package NG.Settings;

/**
 * A class that collects a number of settings. It is the only class whose fields are always initialized upon creation.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Settings {
    public static final String GAME_NAME = "Freight Game"; // laaaaame
    public boolean DEBUG = true;
    public int TARGET_TPS = 10;

    public static int TOOL_BAR_HEIGHT = 80;

    // video settings
    public int TARGET_FPS = 60;
    public boolean V_SYNC = true;
    public int WINDOW_WIDTH = 1200;
    public int WINDOW_HEIGHT = 800;
    public static float FOV = 0.3f;
    public static float Z_NEAR = 1f;
    public static float Z_FAR = 5000;
    public int MAX_POINT_LIGHTS = 16;
    public int ANTIALIAS_LEVEL = 1;
    public static boolean ISOMETRIC_VIEW = false;
    public final float CAMERA_ZOOM_SPEED = -0.1f;
    public final int MAX_CAMERA_DIST = 1000;

    public static int STATION_RANGE = 5;

    public float TRACK_SPACING = 0.1f;
}
