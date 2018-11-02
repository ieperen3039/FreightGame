package NG.ScreenOverlay;

import NG.ActionHandling.MouseClickListener;
import NG.ActionHandling.MouseReleaseListener;
import NG.Engine.Game;
import NG.ScreenOverlay.Frames.Components.SButton;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import NG.Settings.Settings;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

/**
 * @author Geert van Ieperen. Created on 1-11-2018.
 */
public class ToolBar implements MouseClickListener, MouseReleaseListener {
    private static final Vector2i ZERO = new Vector2i(0, 0);
    private static int BUTTON_SIZE = Settings.TOOL_BAR_HEIGHT;

    private Game game;
    private List<Optional<SButton>> buttons;
    private Vector2i toolbarDim = new Vector2i();

    private boolean validLayout = false;
    private Optional<SButton> clickedButton;

    /**
     * creates the toolbar on the top of the GUI. an instance of this should be passed to the {@link
     * NG.ScreenOverlay.Frames.GUIManager}.
     * @param game a reference to the game itself.
     */
    public ToolBar(Game game) {
        this.game = game;
        buttons = new ArrayList<>();
    }

    /**
     * adds a button to the end of the toolbar. The button is square and should display only a few characters TODO:
     * create an icon button constructor
     * @param text   characters displayed on the button.
     * @param action the action to occur when this button is pressed.
     */
    public void addButton(String text, Runnable action) {
        SButton newButton = new SButton(text, action, 0, 0);
        newButton.setSize(BUTTON_SIZE, BUTTON_SIZE);
        buttons.add(Optional.of(newButton));
        validLayout = false;
    }

    /**
     * adds an empty space between the buttons previously added and those to be added later. The size of these
     * separators are all equal.
     */
    public void addSeparator() {
        buttons.add(Optional.empty());
        validLayout = false;
    }

    public void draw(SFrameLookAndFeel design) {
        validate();

        toolbarDim.set(game.window().getWidth(), Settings.TOOL_BAR_HEIGHT);
        design.drawRectangle(ZERO, toolbarDim);

        for (Optional<SButton> button : buttons) {
            button.ifPresent(b -> b.draw(design, b.getPosition()));
        }
    }

    /**
     * ensures that the buttons are positioned correctly
     */
    private void validate() {
        if (validLayout) return;

        int fillerSize = game.window().getWidth();
        int fillers = 2;
        for (Optional<SButton> button : buttons) {
            if (button.isPresent()) {
                fillerSize -= button.get().getWidth();
            } else {
                fillers++;
            }
        }
        assert fillerSize > 0 : "Too many buttons (" + buttons.size() + ")";

        fillerSize /= fillers;

        int x = fillerSize;
        for (Optional<SButton> button : buttons) {
            if (button.isPresent()) {
                button.get().setPosition(x, 0);
                x += BUTTON_SIZE;
            } else {
                x += fillerSize;
            }
        }

        validLayout = true;
    }

    public boolean contains(int x, int y) {
        return y < Settings.TOOL_BAR_HEIGHT;
    }

    @Override
    public void onClick(int button, int xSc, int ySc) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            clickedButton = buttons.stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(b -> b.contains(xSc, ySc))
                    .findAny();

            clickedButton.ifPresent(b -> b.onClick(button, xSc, ySc));
        }
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        clickedButton.ifPresent(b -> b.onRelease(button, xSc, ySc));
    }
}
