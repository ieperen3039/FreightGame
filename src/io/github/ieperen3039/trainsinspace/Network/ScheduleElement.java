package io.github.ieperen3039.trainsinspace.Network;

/**
 * @author Geert van Ieperen created on 28-6-2020.
 */
public class ScheduleElement {
    public final NetworkPosition target;

    public ScheduleElement(NetworkPosition target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return target.toString();
    }
}
