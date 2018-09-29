package NG.ScreenOverlay.Frames.Components;

import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import NG.ScreenOverlay.Frames.SFrameManager;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SFrame extends SContainer {
    static final int FRAME_TITLE_BAR_SIZE = 50;
    private final String title;
    private boolean minimized;
    private SFrameManager frameManager;
    private boolean isDisposed = false;
    private final SPanel middlePanel;

    /**
     * creates a new SFrame
     * @param title
     * @param width
     * @param height
     */
    public SFrame(String title, int width, int height) {
        this.title = title;
        setVisibleFlag(false);

        SPanel upperBar = makeUpperBar(title);
        add(upperBar, NORTH);

        middlePanel = new SPanel();
        add(middlePanel, MIDDLE);

        setSize(width, height);
    }

    /**
     * creates the main menu panel, which has a fixed size, does not have panel manipulation and
     * @param title
     */
    public SFrame(String title) {
        this.title = title;
        STextArea titleBar = new STextArea(title, true);
        add(titleBar, NORTH);

        middlePanel = new SPanel();
        add(middlePanel, MIDDLE);
    }

    private SPanel makeUpperBar(String frameTitle) {
        SPanel upperBar = new SPanel(5, 1);
        SComponent exit = new SCloseButton(this);
        upperBar.add(exit, new Vector2i(4, 0));
        SComponent minimize = new SButton("M", () -> setMinimized(true), FRAME_TITLE_BAR_SIZE, FRAME_TITLE_BAR_SIZE);
        upperBar.add(minimize, new Vector2i(3, 0));
        SExtendedTextArea title = new SExtendedTextArea(frameTitle, true);
        title.setDragListener(this::addToPosition);
        upperBar.add(title, new Vector2i(1, 0));

        return upperBar;
    }

    @Override
    public void add(SComponent comp, Object prop) {
        middlePanel.add(comp, prop);
    }

    /**
     * sets the visibility of this frame. This also invalidates the layout of this frame
     * @param visible if false, the frame is not rendered. If true, the frame is rendered at its position (possibly
     *                outside the viewport though).
     */
    public void setVisible(boolean visible) {
        setVisibleFlag(visible);
    }

    /**
     * sets the visibility of the specified component in this frame. If the component is not part of this frame, the
     * results are undetermined.
     * @param component  a component in this frame
     * @param setVisible if false, it will appear as if the component was removed from this frame. If true, it will be
     *                   there as if it always have been.
     */
    public void setVisibilityOf(SComponent component, boolean setVisible) {
        component.setVisibleFlag(setVisible);
        invalidateLayout();
    }

    public void setMinimized(boolean minimized) {
        this.minimized = minimized;
    }

    public boolean isMinimized() {
        return minimized;
    }

    /**
     * requests focus of the frameManager.
     * @throws NullPointerException if no frame-manager has been set
     */
    public void requestFocus() {
        frameManager.focus(this);
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic offset) {
        if (!isVisible()) return;
        if (minimized) {
            // todo minimized panel
//            design.drawMinimized();

        } else {
            Vector2i scPos = new Vector2i(position).add(offset);
            design.drawRectangle(scPos, dimensions);
            drawChildren(design, scPos);
        }
    }

    @Override
    public int minWidth() {
        return dimensions.x;
    }

    @Override
    public int minHeight() {
        return dimensions.y;
    }

    @Override
    public boolean wantHorizontalGrow() {
        return false;
    }

    public void dispose() {
        isDisposed = true;
        setVisibleFlag(false);
    }

    public void setManager(SFrameManager frameManager) {
        this.frameManager = frameManager;
    }

    public SComponent getComponentAt(int x, int y) {
        return locate(x, y);
    }

    @Override
    public String toString() {
        return "SFrame (" + title + ")";
    }

    public boolean isDisposed() {
        return isDisposed;
    }
}
