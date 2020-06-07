package NG.DataStructures.Generic;

import NG.Tools.AutoLock;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * A {@link TimedQueue} that uses ArrayDeque for implementation. Includes synchronized adding and deletion. Items added
 * to the queue with a timestamp less than the previous addition will cause the previous value to be removed
 * @author Geert van Ieperen created on 13-12-2017.
 */
public class BlockingTimedArrayQueue<T> implements TimedQueue<T>, Serializable {

    /** prevents race-conditions upon adding and removing */
    protected transient final AutoLock changeLock = new AutoLock.Instance();

    /** timestamps in seconds. Private, as semaphore must be handled */
    protected final Deque<Double> timeStamps;
    protected final Deque<T> elements;

    /**
     * @param capacity the initial expected maximum number of entries
     */
    public BlockingTimedArrayQueue(int capacity) {
        timeStamps = new ArrayDeque<>(capacity);
        elements = new ArrayDeque<>(capacity);
    }

    @Override
    public void add(T element, double timeStamp) {
        try (AutoLock.Section section = changeLock.open()) {
            // act as refinement
            while (!timeStamps.isEmpty() && timeStamps.peekLast() > timeStamp) {
                timeStamps.removeLast();
                elements.removeLast();
            }

            timeStamps.add(timeStamp);
            elements.add(element);
        }
    }

    @Override
    public T getNext(double timeStamp) {
        try (AutoLock.Section section = changeLock.open()) {
            if (timeStamps.isEmpty()) return null;

            Iterator<Double> times = timeStamps.iterator();
            Iterator<T> things = elements.iterator();

            T element = things.next();
            double nextElementStart = times.next();

            while (nextElementStart <= timeStamp) {
                if (!times.hasNext()) return things.next();

                element = things.next();
                nextElementStart = times.next();
            }

            return element;
        }
    }

    @Override
    public T getPrevious(double timeStamp) {
        try (AutoLock.Section section = changeLock.open()) {
            if (timeStamps.isEmpty()) return null;

            Iterator<Double> times = timeStamps.iterator();
            Iterator<T> things = elements.iterator();

            // there is no action until the first timestamp
            T element = null;
            double nextElementStart = times.next();

            while (nextElementStart < timeStamp) {
                if (!times.hasNext()) return things.next();

                element = things.next();
                nextElementStart = times.next();
            }

            return element;
        }
    }

    @Override
    public double timeOfNext(double timeStamp) {
        try (AutoLock.Section section = changeLock.open()) {
            if (timeStamps.isEmpty()) throw new IllegalStateException("empty");

            Iterator<Double> times = timeStamps.iterator();
            double nextActionStart = times.next();

            while (nextActionStart < timeStamp && times.hasNext()) {
                nextActionStart = times.next();
            }

            return nextActionStart;
        }
    }

    @Override
    public double timeOfPrevious(double timeStamp) {
        try (AutoLock.Section section = changeLock.open()) {
            if (timeStamps.isEmpty()) throw new IllegalStateException("empty");

            Iterator<Double> times = timeStamps.iterator();
            double previousActionStart = times.next();

            if (!times.hasNext()) {
                return timeStamp - previousActionStart;
            }

            double next = times.next();
            while (times.hasNext() && next < timeStamp) {
                previousActionStart = next;
                next = times.next();
            }
            return previousActionStart;
        }
    }

    @Override
    public void removeUntil(double timeStamp) {
        try (AutoLock.Section section = changeLock.open()) {
            while ((timeStamps.size() > 1) && (timeStamp > nextTimeStamp())) {
                progress();
            }
        }
    }

    /**
     * unsafe progression of the queue
     */
    protected void progress() {
        timeStamps.remove();
        elements.remove();
    }

    /** returns the next queued timestamp in seconds or null if there is none */
    public Double nextTimeStamp() {
        return timeStamps.peek();
    }

    @Override
    public String toString() {
        Iterator<Double> times = timeStamps.iterator();
        Iterator<T> elts = elements.iterator();

        StringBuilder s = new StringBuilder();
        s.append("TimedArray:");
        while (times.hasNext()) {
            s.append("\n");
            s.append(String.format("%1.04f", times.next()));
            s.append(" > ");
            s.append(elts.next());
        }

        return s.toString();
    }
}
