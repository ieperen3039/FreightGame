package NG.Core;

import NG.DataStructures.Generic.Color4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Geert van Ieperen created on 3-9-2020.
 */
public class Coloring {
    List<Marking> markings = new ArrayList<>(4);

    public enum Priority {
        MAXIMUM, MOUSE_HOVER, OCCUPIED_TRACK, MINIMUM
    }

    public Coloring(Color4f baseColor) {
        markings.add(new Marking(baseColor, Priority.MINIMUM));
    }

    public void addMark(Marking mark) {
        markings.add(mark);
        markings.sort(Comparator.comparing(m -> m.priority));
    }

    public Marking addMark(Color4f color, Priority priority) {
        Marking newMark = new Marking(color, priority);
        addMark(newMark);
        return newMark;
    }

    public void removeMark(Priority priority) {
        if (priority == Priority.MINIMUM) {
            throw new IllegalArgumentException("Priority to remove can not be " + Priority.MINIMUM);
        }

        markings.removeIf(c -> c.priority == priority);
    }

    public Color4f getColor() {
        markings.removeIf(c -> !c.isValid());
        return markings.get(0).color;
    }

    public static class Marking {
        public final Color4f color;
        public final Priority priority;
        private boolean isValid = true;

        public Marking(Color4f color, Priority priority) {
            this.color = color;
            this.priority = priority;
        }

        public Marking() {
            this.priority = Priority.MINIMUM;
            color = Color4f.WHITE;
            isValid = false;
        }

        public boolean isValid() {
            return isValid;
        }

        public void invalidate() {
            isValid = false;
        }
    }
}
