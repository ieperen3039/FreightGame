package NG.ScreenOverlay.Frames.Components;

import NG.ActionHandling.MouseAnyClickListener;
import NG.ActionHandling.MouseReleaseListener;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import NG.Tools.Logger;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.Collection;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen. Created on 22-9-2018.
 */
public class SButton extends SComponent implements MouseAnyClickListener, MouseReleaseListener {
    private Collection<Runnable> leftClickListeners = new ArrayList<>();
    private Collection<Runnable> rightClickListeners = new ArrayList<>();
    private final int minHeight;
    private final int minWidth;

    private String text;
    private boolean isPressed = false;
    private boolean vtGrow = false;
    private boolean hzGrow = false;

    public SButton(int minWidth, int minHeight, String text) {
        this.minHeight = minHeight;
        this.minWidth = minWidth;
        this.text = text;
    }

    public SButton(String text, Runnable action, int minWidth, int minHeight) {
        this(minWidth, minHeight, text);
        leftClickListeners.add(action);
    }

    public SButton(String text, Runnable onLeftClick, Runnable onRightClick, int minWidth, int minHeight) {
        this(text, onLeftClick, minWidth, minHeight);
        rightClickListeners.add(onRightClick);
    }

    public void setGrowthPolicy(boolean horizontal, boolean vertical) {
        hzGrow = horizontal;
        vtGrow = vertical;
    }

    @Override
    public int minWidth() {
        return minWidth;
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    @Override
    public boolean wantHorizontalGrow() {
        return hzGrow;
    }

    @Override
    public boolean wantVerticalGrow() {
        return vtGrow;
    }

    public void addLeftClickListener(Runnable action) {
        leftClickListeners.add(action);
    }

    public void addRightClickListeners(Runnable action) {
        rightClickListeners.add(action);
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic offset) {
        if (dimensions.x == 0 || dimensions.y == 0) return;

        Vector2i scPos = new Vector2i(position).add(offset);
        design.drawButton(scPos, dimensions, text, isPressed);
    }

    @Override
    public void onClick(int button, int x, int y) {
        isPressed = true;
    }

    @Override
    public void onRelease(int button, int x, int y) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            leftClickListeners.forEach(Runnable::run);

        } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            rightClickListeners.forEach(Runnable::run);
        } else {
            Logger.DEBUG.print("button clicked with " + button + " which has no action");
        }
        isPressed = false;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (" + getText() + ")";
    }
}