package NG.ScreenOverlay.Frames.Components;

import NG.ActionHandling.MouseAnyButtonClickListener;
import NG.Engine.Game;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import org.joml.Vector2i;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SFrame extends SContainer implements MouseAnyButtonClickListener {
    static final int FRAME_TITLE_BAR_SIZE = 50;
    private final Game game;
    private boolean minimized;

    public SFrame(Game game, int width, int height) {
        super(5, 5, false);
        this.game = game;
        setVisibleFlag(false);
        game.callbacks().onMouseButtonClick(this);

        SPanel upperBar = new SPanel(5, 1);
        SComponent exit = new SFrameCloseButton(this);
        upperBar.add(exit, new Vector2i(4, 0));
        SComponent minimize = new SButton("M", () -> setMinimized(true), FRAME_TITLE_BAR_SIZE, FRAME_TITLE_BAR_SIZE);
        upperBar.add(minimize, new Vector2i(3, 0));
        SComponent title = new STextArea("Title", true);
        upperBar.add(title, new Vector2i(1, 0));

        add(upperBar, new Vector2i(2, 0));

        setSize(width, height);
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

    public void requestFocus() {
        game.frameManager().focus(this);
    }

    @Override
    public void draw(SFrameLookAndFeel design) {
        if (!isVisible()) return;
        if (minimized) {
            // todo minimized panel

        } else {
            design.drawRectangle(position, dimensions);
            drawChildren(design);
        }
    }

    @Override
    public void onClick(int button, int x, int y) {
        for (SComponent c : children()) {
            if (c instanceof MouseAnyButtonClickListener && c.contains(x, y)) {
                MouseAnyButtonClickListener clickListener = (MouseAnyButtonClickListener) c;
                clickListener.onClick(button, x - c.getX(), y - c.getY());
                return;
            }
        }

        requestFocus();
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
        game.callbacks().removeListener(this);
        setVisibleFlag(false);
    }
}
