package NG.DataStructures.Generic;

/**
 * A queue that allows a producer to queue timed objects (e.g. positions) while a consumer takes the next item from a
 * specified timestamp onwards.
 * @author Geert van Ieperen created on 13-12-2017.
 */
public interface TimedQueue<T> {

    /**
     * add an element to be accessible in the interval [the timeStamp of the previous item, the given timestamp] Items
     * added to the queue with a timestamp less than the previous addition will cause the previous value to be removed
     * @param element   the element that will be returned upon calling {@link #getNext(double)}
     * @param timeStamp the timestamp in seconds from where this element becomes active
     */
    void add(T element, double timeStamp);

    /**
     * @param timeStamp the timestamp to consider
     * @return the earliest element with a timestamp equal or after the given timestamp.
     */
    T getNext(double timeStamp);

    /**
     * @param timeStamp the timestamp to consider
     * @return the latest element with a timestamp strictly before the given timestamp.
     */
    T getPrevious(double timeStamp);

    /**
     * @param timeStamp the timestamp in seconds to measure from
     * @return the number of seconds until {@link #getNext(double)} returns a different value. If timeStamp is exactly
     * an element of this queue, it measures to that element, and returns 0 (as {@code getNext(double)} would do}. If
     * timeStamp is later than any action, it returns a negative number representing the difference between this
     * timestamp and the last element in this queue.
     */
    double timeUntilNext(double timeStamp);

    /**
     * @param timeStamp the timestamp in seconds to measure from
     * @return the number of seconds since {@link #getPrevious(double)} returned a different value. If timeStamp is
     * exactly an element of this queue, it measures to the element before that (as {@code getPrevious(double)} would
     * do}. If timeStamp is earlier than any action, it returns a negative number representing the difference between
     * the first element in this queue and this timestamp.
     */
    double timeSincePrevious(float timeStamp);

    /**
     * upon returning, nextTimeStamp > timeStamp or there exist no item with such timestamp.
     * @param timeStamp the time until where the state of the queue should be updated.
     */
    void removeUntil(double timeStamp);
}
