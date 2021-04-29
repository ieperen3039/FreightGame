package NG.GUIMenu.Components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Geert van Ieperen created on 19-9-2020.
 */
public class STabPanel extends SDecorator {
    public <T> STabPanel(List<T> elements, Function<T, String> nameExtractor, Function<T, SComponent> tabCreator) {
        int numElements = elements.size();

        SComponent[] tabContents = new SComponent[numElements];
        for (int i = 0; i < numElements; i++) {
            tabContents[i] = tabCreator.apply(elements.get(i));
        }

        String[] tabLabels = new String[numElements];
        for (int i = 0; i < numElements; i++) {
            tabLabels[i] = nameExtractor.apply(elements.get(i));
        }

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

        setContents(SContainer.column(
                tabButtons, tabArea
        ));
    }

    public STabPanel(String[] tabLabels, SComponent[] tabContents) {
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

        setContents(SContainer.column(
                tabButtons, tabArea
        ));
    }

    public static class Builder {
        List<String> labels = new ArrayList<>();
        List<SComponent> elements = new ArrayList<>();

        public Builder add(String tabLabel, SComponent element) {
            labels.add(tabLabel);
            elements.add(element);
            return this;
        }

        public STabPanel get() {
            return new STabPanel(labels.toArray(new String[0]), elements.toArray(new SComponent[0]));
        }
    }
}
