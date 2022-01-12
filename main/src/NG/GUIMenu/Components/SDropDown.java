package NG.GUIMenu.Components;

import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.InputHandling.MouseClickListener;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.DROP_DOWN_HEAD_CLOSED;
import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.DROP_DOWN_HEAD_OPEN;

/**
 * A menu item that may assume different options, where the player can choose from using a drop-down selection.
 * @author Geert van Ieperen. Created on 5-10-2018.
 */
public class SDropDown extends SComponent implements MouseClickListener {
    public static final SComponentProperties DEFAULT_PROPERTIES = new SComponentProperties(
            250, 80, true, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT_MIDDLE
    );

    private final String[] values;
    private final DropDownOptions optionPane;
    private final UIFrameManager gui;
    private final boolean doFoldDown;
    private List<Consumer<Integer>> stateChangeListeners = new ArrayList<>();

    private int current;
    private boolean isOpened = false;
    private int minHeight;
    private int minWidth;
    private int textWidth;

    private int dropOptionHeight = 50;
    private final SFrameLookAndFeel.Alignment alignment;
    private final NGFonts.TextType textType;

    /**
     * create a dropdown menu with the given possible values, with a minimum width of 150 and height of 50
     * @param gui     a reference to the gui in which this is displayed
     * @param initial the initial selected item, such that {@code values[initial]} is shown
     * @param values  a list of possible values for this dropdown menu
     */
    public SDropDown(UIFrameManager gui, int initial, String... values) {
        this(gui, DEFAULT_PROPERTIES, initial, true, values);
    }

    /**
     * create a dropdown menu with the given possible values
     * @param gui             a reference to the gui in which this is displayed
     * @param initial         the initial selected item, such that {@code values[initial]} is shown
     * @param directionIsDown if false, the options will appear above the element
     * @param values          a list of possible values for this dropdown menu
     */
    public SDropDown(
            UIFrameManager gui, SComponentProperties properties, int initial, boolean directionIsDown, String... values
    ) {
        assert values.length > 0;
        this.values = values;
        this.current = initial;
        this.gui = gui;
        this.doFoldDown = directionIsDown;
        this.minWidth = properties.minWidth;
        this.minHeight = properties.minHeight;
        this.alignment = properties.alignment;
        this.textType = properties.textType;
        setGrowthPolicy(properties.wantHzGrow, properties.wantVtGrow);

        this.optionPane = new DropDownOptions(values);
    }

    /**
     * create a dropdown menu with the string representation of the given object array as values. To obtain the selected
     * values, one must retrieve the selected index with {@link #getSelectedIndex()} and access the original array.
     * @param gui     a reference to the gui in which this is displayed
     * @param initial the initial selected item, such that {@code values[initial]} is shown
     * @param values  a list of possible values for this dropdown menu
     */
    public <T> SDropDown(UIFrameManager gui, SComponentProperties properties, int initial, List<? extends T> values) {
        this(gui, properties, initial, values, true, String::valueOf);
    }

    /**
     * create a dropdown menu with the string representation of the given object array as values. To obtain the selected
     * values, one must retrieve the selected index with {@link #getSelectedIndex()} and access the original array.
     * @param gui       a reference to the gui in which this is displayed
     * @param initial   the initial selected item, such that {@code values[initial]} is shown
     * @param values    a list of possible values for this dropdown menu
     * @param directionIsDown if false, the options will appear above the element
     * @param stringExtractor a function to turn a value into its string representation
     */
    public <T> SDropDown(
            UIFrameManager gui, SComponentProperties properties, int initial, List<? extends T> values,
            boolean directionIsDown, Function<T, String> stringExtractor
    ) {
        assert !values.isEmpty();
        this.minHeight = properties.minHeight;
        this.minWidth = properties.minWidth;

        int candidate = 0;
        String[] arr = new String[values.size()];
        for (int i = 0; i < values.size(); i++) {
            T elt = values.get(i);
            arr[i] = stringExtractor.apply(elt);

            if (elt.equals(initial)) {
                candidate = i;
            }
        }

        this.current = candidate;
        this.values = arr;
        this.gui = gui;
        this.doFoldDown = directionIsDown;
        this.alignment = properties.alignment;
        this.textType = properties.textType;
        setGrowthPolicy(properties.wantHzGrow, properties.wantVtGrow);

        this.optionPane = new DropDownOptions(arr);
    }

    /** @return the index of the currently selected item in the original array */
    public int getSelectedIndex() {
        return current;
    }

    /** @return the currently selected item */
    public String getSelected() {
        return values.length == 0 ? null : values[current];
    }

    /** hints the layout manager the minimum size of this component */
    public void setMinimumSize(int width, int height) {
        minWidth = width;
        minHeight = height;
    }

    /** sets the height of a single option in the drop-down section of the component. */
    public void setDropOptionHeight(int dropOptionHeight) {
        this.dropOptionHeight = dropOptionHeight;
    }

    public void addStateChangeListener(Consumer<Integer> action) {
        stateChangeListeners.add(action);
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
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        String text = values[current];

        int textWidth = design.getTextWidth(text, textType);
        if (this.textWidth != textWidth) {
            this.textWidth = textWidth;
            invalidateLayout();
        }

        design.draw(isOpened ? DROP_DOWN_HEAD_OPEN : DROP_DOWN_HEAD_CLOSED, screenPosition, getSize());
        Vector2i textPosition = new Vector2i(screenPosition).add(4, 0); // 4 is the virtual component border
        design.drawText(textPosition, getSize(), text, textType, alignment);
        // modal dialogs are drawn separately
    }

    @Override
    public void onClick(int button, int xSc, int ySc) {
        if (isOpened) {
            close();

        } else {
            optionPane.setSize(getWidth(), 0);
            optionPane.validateLayout();

            Vector2i scPos = getScreenPosition();
            int yPos = doFoldDown ? scPos.y + getHeight() : scPos.y - optionPane.getHeight();
            optionPane.setPosition(scPos.x, yPos);
            optionPane.setVisible(true);

            gui.setModalListener(optionPane);
        }
    }

    public void setCurrent(int index) {
        current = index;
        stateChangeListeners.forEach(c -> c.accept(current));
    }

    private void close() {
        optionPane.setVisible(false);
    }

    private class DropDownOptions extends SDecorator implements MouseClickListener {
        private DropDownOptions(String[] values) {
            SPanel panel = new SPanel(1, values.length);
            setVisible(false);

            assert textType != null;
            assert alignment != null;

            for (int i = 0; i < values.length; i++) {
                final int index = i;
                SExtendedTextArea option = new SExtendedTextArea(
                        values[index], minWidth, dropOptionHeight, true, textType, alignment
                );

                option.setClickListener((b, x, y) -> {
                    setCurrent(index);
                    close();
                });

                panel.add(option, new Vector2i(0, i));
            }

            setContents(panel);
        }

        @Override
        public void onClick(int button, int xRel, int yRel) {
            SComponent target = getComponentAt(xRel, yRel);
            assert (target instanceof SExtendedTextArea);

            SExtendedTextArea option = (SExtendedTextArea) target;
            option.onClick(button, 0, 0);
        }
    }
}
