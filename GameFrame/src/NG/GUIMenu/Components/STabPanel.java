package NG.GUIMenu.Components;

import java.util.List;
import java.util.function.Function;

/**
 * @author Geert van Ieperen created on 19-9-2020.
 */
public class STabPanel extends SDecorator {
    public <T> STabPanel(List<T> elements, Function<T, String> nameExtractor, Function<T, SComponent> tabCreator) {
        super(create(elements, nameExtractor, tabCreator));
    }

    public <T> STabPanel(String[] tabLabels, SComponent[] tabContents) {
        super(create(tabLabels, tabContents));
    }

    private static <T> SContainer create(
            List<T> elements, Function<T, String> nameExtractor, Function<T, SComponent> tabCreator
    ) {
        int numElements = elements.size();

        SComponent[] tabContents = new SComponent[numElements];
        for (int i = 0; i < numElements; i++) {
            tabContents[i] = tabCreator.apply(elements.get(i));
        }

        String[] tabLabels = new String[numElements];
        for (int i = 0; i < numElements; i++) {
            tabLabels[i] = nameExtractor.apply(elements.get(i));
        }

        return create(tabLabels, tabContents);
    }

    private static SContainer create(String[] tabLabels, SComponent[] tabContents) {
        int minWidth = 0;
        int minHeight = 0;
        for (SComponent tab : tabContents) {
            minWidth = Math.max(minWidth, tab.minWidth());
            minHeight = Math.max(minHeight, tab.minHeight());
        }

        SComponentArea tabArea = new SComponentArea(minWidth, minHeight);

        SExclusiveButtonRow tabButtons = new SExclusiveButtonRow(true, tabLabels)
                .addSelectionListener(i -> tabArea.show(tabContents[i]));

        tabArea.show(new SFiller());

        return SContainer.column(
                tabButtons, tabArea
        );
    }
}
