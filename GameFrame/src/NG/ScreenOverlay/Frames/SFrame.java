package NG.ScreenOverlay.Frames;

import NG.ActionHandling.MouseAnyButtonClickListener;
import NG.Engine.Game;
import NG.ScreenOverlay.ScreenOverlay;
import NG.Tools.Logger;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SFrame extends SContainer implements MouseAnyButtonClickListener {
    private final Game game;
    private boolean minimized;
    private boolean isVisible = false;

    public SFrame(Game game, int width, int height) {
        super();
        this.game = game;
        setSize(width, height);
        game.callbacks().onMouseButtonClick(this);
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
    public void draw(ScreenOverlay.Painter painter) {
        if (!isVisible) return;
        if (minimized) Logger.ASSERT.print("Drawing a minimized panel");

        lookFeel.drawRectangle(position, dimensions);
        drawChildren(painter);
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
    public boolean wantGrow() {
        return false;
    }

    public void dispose() {
        game.callbacks().removeListener(this);
    }

    public void show() {
        isVisible = true;
    }
}
