package NG.ScreenOverlay.Frames.Components;

import NG.ActionHandling.MouseRelativeClickListener;
import NG.Engine.Game;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.List;

/**
 * @author Geert van Ieperen. Created on 5-10-2018.
 */
public class SDropDown extends SComponent implements MouseRelativeClickListener {
    private final String[] values;
    private final DropDownOptions optionPane;
    private final Game game;

    private int current = 0;
    private boolean isOpened = false;
    private int minHeight;
    private int minWidth;

    private int dropOptionHeight = 50;

    public SDropDown(Game game, int current, String... values) {
        this(game, current, 50, 100, values);
    }

    public SDropDown(Game game, int current, int minWidth, int minHeight, String... values) {
        super();
        this.game = game;
        this.values = values;
        this.current = current;
        this.minHeight = minHeight;
        this.minWidth = minWidth;
        this.optionPane = new DropDownOptions();
    }

    public SDropDown(Game game, List<String> values) {
        this(game, 150, 50, values);
    }

    public SDropDown(Game game, int minWidth, int minHeight, List<String> values) {
        this(game, 0, minWidth, minHeight, values.toArray(new String[0]));
    }

    public SDropDown(Game game, Object[] values) {
        this.game = game;
        String[] arr = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            arr[i] = values[i].toString();
        }
        this.values = arr;
        this.minHeight = 50;
        this.minWidth = 50;
        this.optionPane = new DropDownOptions();
    }

    public int getSelectedIndex() {
        return current;
    }

    public String getSelected() {
        return values[current];
    }

    public void setMinimumSize(int width, int height) {
        minWidth = width;
        minHeight = height;
    }

    public void setDropOptionHeight(int dropOptionHeight) {
        this.dropOptionHeight = dropOptionHeight;
    }

    @Override
    public void setParent(SContainer parent) {
        super.setParent(parent);
        optionPane.setParent(parent);
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
        return false;
    }

    @Override
    public boolean wantVerticalGrow() {
        return false;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        design.drawDropDown(screenPosition, dimensions, values[current], isOpened);
        // modal dialogs are drawn separately
    }

    @Override
    public void onClick(int button, int xSc, int ySc) {
        if (isOpened) {
            close();

        } else {
            optionPane.setPosition(position.x, position.y + dimensions.y);
            optionPane.setSize(dimensions.x, 0);
            optionPane.setVisible(true);
            game.gui().setModalListener(optionPane);
        }
    }

    private void close() {
        optionPane.setVisible(false);
    }

    private class DropDownOptions extends SPanel implements MouseRelativeClickListener {

        private DropDownOptions() {
            super(1, values.length);
            setVisible(false);

            for (int i = 0; i < values.length; i++) {
                final int index = i;
                SExtendedTextArea option = new SExtendedTextArea(values[index], dropOptionHeight, true);
                option.setClickListener((b, x, y) -> {
                    current = index;
                    close();
                });

                add(option, new Vector2i(0, i));
            }
        }

        @Override
        public void onClick(int button, int xRel, int yRel) {
            if (xRel < 0 || yRel < 0 || xRel > getWidth() || yRel > getHeight()) {
                close();
                return;
            }

            SComponent target = getComponentAt(xRel, yRel);
            if (target instanceof SExtendedTextArea) {
                SExtendedTextArea option = (SExtendedTextArea) target;
                option.onClick(button, xRel, yRel);
            }
        }
    }
}
