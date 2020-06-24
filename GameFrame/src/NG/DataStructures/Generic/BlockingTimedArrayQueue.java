package NG.DataStructures.Generic;

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
    public synchronized void add(T element, double timeStamp) {
        // act as refinement
        while (!timeStamps.isEmpty() && timeStamps.peekLast() > timeStamp) {
            timeStamps.removeLast();
            elements.removeLast();
        }

        timeStamps.add(timeStamp);
        elements.add(element);

    }

    @Override
    public synchronized T getNext(double timeStamp) {
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

    @Override
    public synchronized T getPrevious(double timeStamp) {
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

    @Override
    public synchronized double timeOfNext(double timeStamp) {
        if (timeStamps.isEmpty()) throw new IllegalStateException("empty");

        Iterator<Double> times = timeStamps.iterator();
        double nextActionStart = times.next();

        while (nextActionStart < timeStamp && times.hasNext()) {
            nextActionStart = times.next();
        }

        return Math.max(nextActionStart, timeStamp);
    }

    @Override
    public synchronized double timeOfPrevious(double timeStamp) {
        if (timeStamps.isEmpty()) throw new IllegalStateException("empty");

        Iterator<Double> times = timeStamps.iterator();
        double previousActionStart = times.next();

        if (!times.hasNext()) {
            return Math.min(timeStamp, previousActionStart);
        }

        double next = times.next();
        while (times.hasNext() && next < timeStamp) {
            previousActionStart = next;
            next = times.next();
        }
        return previousActionStart;
    }

    @Override
    public synchronized void removeUntil(double timeStamp) {
        while ((timeStamps.size() > 1) && (timeStamp > nextTimeStamp())) {
            progress();
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
