package io.github.ieperen3039.trainsinspace.GUIMenu.Components;

import io.github.ieperen3039.trainsinspace.GUIMenu.Rendering.NGFonts;
import io.github.ieperen3039.trainsinspace.GUIMenu.Rendering.SFrameLookAndFeel;
import io.github.ieperen3039.trainsinspace.GUIMenu.SComponentProperties;

import java.util.function.Supplier;

/**
 * @author Geert van Ieperen created on 22-5-2020.
 */
public class SActiveTextArea extends STextComponent {
    private final Supplier<String> supplier;

    public SActiveTextArea(Supplier<String> supplier, SComponentProperties props) {
        super("", props);
        this.supplier = supplier;
    }

    public SActiveTextArea(Supplier<String> supplier, int minHeight) {
        this(supplier, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT_MIDDLE, 0, minHeight);
    }

    public SActiveTextArea(
            Supplier<String> supplier, NGFonts.TextType textType,
            SFrameLookAndFeel.Alignment alignment,
            int width, int height
    ) {
        super("", textType, alignment, width, height);
        this.supplier = supplier;
    }

    @Override
    public String getText() {
        return supplier.get();
    }
}
