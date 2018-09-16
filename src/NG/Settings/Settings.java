package NG.Settings;

/**
 * A class that collects a number of settings. It is the only class whose fields are always initialized upon creation.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Settings {
    public static final String GAME_NAME = "Freight Game"; // laaaaame
    public boolean DEBUG = true;

    public int ANTIALIAS = 1;
    public boolean V_SYNC = true;
    public int TARGET_FPS = 60;
    public int WINDOW_WIDTH = 1200;
    public int WINDOW_HEIGHT = 800;
    public int MAX_POINT_LIGHTS = 20;
}
