package NG.ScreenOverlay.Frames.Components;

import NG.ScreenOverlay.Frames.GUIManager;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SFrame extends SComponent {
    public static final int INNER_BORDER = 4;
    public static final int OUTER_BORDER = 4;

    public static final int FRAME_TITLE_BAR_SIZE = 50;
    private final String title;
    private boolean minimized;
    private GUIManager frameManager;
    private boolean isDisposed = false;
    private SContainer middlePanel;
    private SComponent upperBar;

    /**
     * creates a new SFrame
     * @param title
     * @param width
     * @param height
     */
    public SFrame(String title, int width, int height) {
        this(title, width, height, true);
    }

    /**
     * @param title
     * @param width
     * @param height
     * @param editable
     */
    public SFrame(String title, int width, int height, boolean editable) {
        this.title = title;
        setVisibleFlag(false);

        upperBar = editable ? makeUpperBar(title) : new STextArea(title, FRAME_TITLE_BAR_SIZE, true);
        middlePanel = new SPanel();

        setSize(width, height);
    }

    public SFrame(String title) {
        this(title, 0, 0);
    }

    private SPanel makeUpperBar(String frameTitle) {
        SPanel upperBar = new SPanel(0, FRAME_TITLE_BAR_SIZE, 5, 1, true, false);
        SComponent exit = new SCloseButton(this);
        upperBar.add(exit, new Vector2i(4, 0));
        SComponent minimize = new SButton("M", () -> setMinimized(true), FRAME_TITLE_BAR_SIZE, FRAME_TITLE_BAR_SIZE);
        upperBar.add(minimize, new Vector2i(3, 0));
        SExtendedTextArea title = new SExtendedTextArea(frameTitle, 20, true);
        title.setDragListener(this::addToPosition);
        upperBar.add(title, new Vector2i(1, 0));

        return upperBar;
    }

    /**
     * sets the area below the title bar to contain the given component. The size of this component is determined by
     * this frame.
     * @param comp the new middle component
     */
    public void setMainPanel(SContainer comp) {
        middlePanel = comp;
        middlePanel.setPosition(0, FRAME_TITLE_BAR_SIZE);
    }

    /**
     * sets the visibility of this frame. This also invalidates the layout of this frame
     * @param visible if false, the frame is not rendered. If true, the frame is rendered at its position (possibly
     *                outside the viewport though).
     */
    public void setVisible(boolean visible) {
        setVisibleFlag(visible);
        invalidateLayout();
    }

    public void pack() {
        setSize(minWidth(), minHeight());
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
            // take offset into account for consistency.
            Vector2i scPos = new Vector2i(position).add(offset);

            design.drawRectangle(scPos, dimensions);
            upperBar.draw(design, scPos);

            if (middlePanel.isVisible()) {
                middlePanel.draw(design, scPos);
            }
        }
    }

    @Override
    public int minWidth() {
        return Math.max(middlePanel.minWidth(), upperBar.minWidth());
    }

    @Override
    public int minHeight() {
        return middlePanel.minHeight() + upperBar.minHeight();
    }

    public void dispose() {
        isDisposed = true;
        setVisibleFlag(false);
    }

    public void setManager(GUIManager frameManager) {
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

    @Override
    SComponent locate(int x, int y) {
        if (upperBar.contains(x, y)) {
            return upperBar.locate(x, y);
        }
        if (middlePanel.isVisible() && middlePanel.contains(x, y)) {
            int yr = y - middlePanel.getY();
            return middlePanel.locate(x, yr);
        }
        return this;
    }

    @Override
    protected void setVisibleFlag(boolean doVisible) {
        super.setVisibleFlag(doVisible);
        if (doVisible) invalidateLayout();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        invalidateDimensions();
    }

    @Override
    public void addToSize(int xDelta, int yDelta) {
        super.addToSize(xDelta, yDelta);
        invalidateDimensions();
    }

    private void invalidateDimensions() {
        upperBar.setSize(getWidth(), FRAME_TITLE_BAR_SIZE);
        middlePanel.setSize(getWidth(), getHeight() - FRAME_TITLE_BAR_SIZE);
        middlePanel.invalidateDimensions();
    }

    public void invalidateLayout() {
        upperBar.setSize(getWidth(), FRAME_TITLE_BAR_SIZE);
        middlePanel.setSize(getWidth(), getHeight() - FRAME_TITLE_BAR_SIZE);
        middlePanel.invalidateLayout();
    }

    @Override
    public boolean wantVerticalGrow() {
        return false;
    }

    @Override
    public boolean wantHorizontalGrow() {
        return false;
    }
}
