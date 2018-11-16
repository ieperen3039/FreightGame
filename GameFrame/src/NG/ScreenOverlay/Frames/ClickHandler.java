package NG.ScreenOverlay.Frames;

/**
 * @author Geert van Ieperen. Created on 16-11-2018.
 */
public interface ClickHandler {
    /**
     * Determine whether this object can handle the mouse click, and when possible does so.
     * @param button the mouse button used for clicking
     * @param xSc    the x screen position in pixels from top-left
     * @param ySc    the y screen position in pixels form top-left
     * @return false iff the click was not processed, and the underlying visuals can receive the click.
     */
    boolean processClick(int button, int xSc, int ySc);
}
