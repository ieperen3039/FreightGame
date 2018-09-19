package NG.ScreenOverlay;

import org.joml.Vector2i;

/**
 * Generic UI element that can be drawn in the hud.
 * @author Jorren Hendriks.
 */
public abstract class UIElement implements HudElement {

    protected int x = 0;
    protected int y = 0;
    protected int width;
    protected int height;

    public UIElement(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean contains(Vector2i v) {
        return contains(v.x, v.y);
    }

    public boolean contains(int x, int y) {
        return this.x <= x && x <= this.x + width &&
                this.y <= y && y <= this.y + height;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Vector2i getPosition() {
        return new Vector2i(x, y);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setPosition(Vector2i position) {
        this.x = position.x;
        this.y = position.y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
