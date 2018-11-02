package NG.ActionHandling;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public interface MouseClickListener {
    /**
     * whenever the user clicks, this event is fired
     * @param button the button number
     * @param xSc    the x position on the screen
     * @param ySc    the y position on the screen
     */
    void onClick(int button, int xSc, int ySc);
}
