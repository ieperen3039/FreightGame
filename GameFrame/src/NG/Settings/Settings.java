package NG.Settings;

/**
 * A class that collects a number of settings. It is the only class whose fields are always initialized upon creation.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Settings {
    public static final String GAME_NAME = "Freight Game"; // laaaaame
    public boolean DEBUG = true;

    public static int TOOL_BAR_HEIGHT = 80;


    public int TARGET_FPS = 60;
    public boolean V_SYNC = true;
    public int WINDOW_WIDTH = 1200;
    public int WINDOW_HEIGHT = 800;
    public static float FOV = 0.3f;
    public static float Z_NEAR = 1f;
    public static float Z_FAR = 2000;
    public int MAX_POINT_LIGHTS = 20;
    public int ANTIALIAS_LEVEL = 1;

    public float TRACK_SPACING = 0.1f;
    public int TARGET_TPS = 10;
}
