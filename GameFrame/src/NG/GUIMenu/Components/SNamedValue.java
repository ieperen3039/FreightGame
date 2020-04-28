package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import org.joml.Vector2ic;

import java.util.function.Supplier;

/**
 * @author Geert van Ieperen created on 24-2-2019.
 */
public class SNamedValue extends STextComponent {
    private final Supplier<Object> producer;

    public SNamedValue(String name, Supplier<Object> producer, int minComponentHeight) {
        super(name, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT, 0, minComponentHeight);
        this.producer = producer;

        setGrowthPolicy(true, false);
    }

    @Override
    public String getText() {
        return text + ": " + producer.get();
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        design.draw(SFrameLookAndFeel.UIComponent.PANEL, screenPosition, getSize());
        super.draw(design, screenPosition);
    }

    @Override
    public String toString() {
        String text = getText();
        String substring = text.length() > 25 ? text.substring(0, 20) + "..." : text;
        return this.getClass().getSimpleName() + " (" + substring + ")";
    }
}
