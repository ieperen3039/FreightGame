package NG.GUIMenu.FrameManagers;

import NG.Core.GameAspect;
import NG.GUIMenu.Components.SComponent;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Components.SToolBar;
import NG.GUIMenu.Rendering.NVGOverlay;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.InputHandling.MouseTools.MouseToolListener;

/**
 * A class that manages frames of a game. New {@link SFrame} objects can be added using {@link #addFrame(SFrame)}
 * @author Geert van Ieperen. Created on 29-9-2018.
 */
public interface FrameGUIManager extends GameAspect, MouseToolListener {

    /**
     * draws the elements of this HUD
     * @param painter
     */
    void draw(NVGOverlay.Painter painter);

    /**
     * adds the given frame at a position that the frame manager assumes to be optimal
     * @param frame
     * @see #addFrame(SFrame, int, int)
     */
    void addFrame(SFrame frame);

    /**
     * adds a fame on the given position, and focusses it.
     * @param frame the frame to be added.
     * @param x     screen x coordinate in pixels from left
     * @param y     screen y coordinate in pixels from top
     */
    void addFrame(SFrame frame, int x, int y);

    /**
     * brings the given from to the front-most position
     * @param frame a frame that has been added to this manager
     * @throws java.util.NoSuchElementException if the given frame has not been added or has been disposed.
     */
    void focus(SFrame frame);

    /**
     * sets the toolbar of the screen to the given object. Overwrites the current setting.
     * @param toolBar any toolbar, or null to remove the toolbar
     */
    void setToolBar(SToolBar toolBar);

    /** @return the toolbar set with {@link #setToolBar(SToolBar)}, or null if none has been set */
    SToolBar getToolBar();

    /**
     * adds a component to the hud. The position of the component may be changed as a result of this call.
     * @param component any new component
     */
    default void addElement(SComponent component) {
        if (!(component instanceof SFrame)) {
            component = new SFrame(component.toString(), component);
        }

        addFrame((SFrame) component);
    }

    boolean checkMouseScroll(int xSc, int ySc, float value);

    /**
     * removes a component from the hud
     * @param component a component previously added
     * @return
     */
    boolean removeElement(SComponent component);

    /**
     * sets the appearance of the frames on the next drawing cycles to the given object. This overrides any previous
     * setting.
     * @param lookAndFeel any look-and-feel provider.
     */
    void setLookAndFeel(SFrameLookAndFeel lookAndFeel);

    /**
     * @return false iff no call to {@link #setLookAndFeel(SFrameLookAndFeel)} has occurred.
     */
    boolean hasLookAndFeel();

    SComponent getComponentAt(int xSc, int ySc);

    /**
     * @param xSc screen x coordinate in pixels from left
     * @param ySc screen y coordinate in pixels from top
     * @return the SFrame covering the given coordinate
     */
    boolean covers(int xSc, int ySc);

    /**
     * The next click action is redirected to the given listener instead of being processed by the frames. This is reset
     * after such click occurs.
     * @param listener a listener that receives the button and screen positions of the next click exactly once.
     */
    void setModalListener(SComponent listener);
}
