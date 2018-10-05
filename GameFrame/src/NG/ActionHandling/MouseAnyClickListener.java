package NG.ActionHandling;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public interface MouseAnyClickListener {
    /**
     * whenever the user clicks, this event is fired
     * @param button the button number
     * @param x      the x position on the screen
     * @param y      the y position on the screen
     */
    void onClick(int button, int x, int y);
}