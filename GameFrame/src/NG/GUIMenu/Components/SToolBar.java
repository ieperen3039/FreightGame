package NG.GUIMenu.Components;

import NG.Core.Game;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseReleaseListener;
import NG.Menu.Main.MainMenu;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.TOOLBAR_BACKGROUND;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

/**
 * @author Geert van Ieperen. Created on 1-11-2018.
 */
public class SToolBar extends SContainer implements MouseReleaseListener, MouseClickListener {
    public static final int MAX_BAR_ICONS = 20; // TODO look for opportunity of calculating this
    public static final int TOOL_BAR_HEIGHT = MainMenu.MAIN_BUTTON_PROPERTIES.minHeight;
    private static final int BUTTON_SIZE = TOOL_BAR_HEIGHT - (4 + 4); // where did the 4 come from? / where did it go? / where did you come from, cotton-eyed joe
    private final SComponentProperties buttonProps = new SComponentProperties(200, BUTTON_SIZE, false, false);

    private final Game game;
    private int nextButtonIndex = 0;
    private SButton clickedButton;

    /**
     * creates the toolbar on the top of the GUI.
     * @param game    a reference to the game itself.
     * @param doSides if true, separators are added to each size of the bar
     */
    public SToolBar(Game game, boolean doSides) {
        super(MAX_BAR_ICONS, 1);
        this.game = game;
        if (doSides) {
            add(SFiller.horizontal(), null);
            super.add(SFiller.horizontal(), new Vector2i(MAX_BAR_ICONS - 1, 0));
        }
    }

    @Override
    public void add(SComponent comp, Object prop) {
        assert prop == null;
        super.add(comp, new Vector2i(nextButtonIndex++, 0));
    }

    /**
     * adds a button to the end of the toolbar. The button is square and should display only a few characters
     * @param text   characters displayed on the button.
     * @param action the action to occur when this button is pressed.
     *///TODO create an icon button constructor
    public void addButton(String text, Runnable action) {
        SButton newButton = new SButton(text, action, buttonProps);
        newButton.setSize(0, 0);
        newButton.setXBorder(BUTTON_SIZE / 2);
        add(newButton, null);
    }

    /**
     * adds an empty space between the buttons previously added and those to be added later. The size of these
     * separators are all equal.
     */
    public void addSeparator() {
        add(SFiller.horizontal(), null);
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        validateLayout();
        int scWidth = game.window().getWidth();
        design.draw(TOOLBAR_BACKGROUND, screenPosition, new Vector2i(scWidth, TOOL_BAR_HEIGHT));
        drawChildren(design, screenPosition);
    }

    @Override
    public void onClick(int button, int xSc, int ySc) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            SComponent clicked = getComponentAt(xSc, ySc);

            if (clicked instanceof SButton) {
                clickedButton = (SButton) clicked;
                clickedButton.onClick(button, xSc, ySc);
            }
        }
    }

    @Override
    public void onRelease(int button) {
        if (clickedButton != null) {
            clickedButton.onRelease(button);
        }
    }

    @Override
    public void doValidateLayout() {
        setSize(game.window().getWidth(), TOOL_BAR_HEIGHT);
        super.doValidateLayout();
    }
}
