package NG.Tools;

import org.joml.Vector2ic;
import org.joml.Vector3fc;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A logger class that has its levels reflected in enum constants
 * The order of the constants reflect the order of level
 * @author Geert van Ieperen created on 2-6-2018.
 */
public enum Logger {
    // order matters!
    /** for any information about the internal functioning */
    DEBUG,
    /** for errors caused by a break of assumptions in internal, where the game still could continue */
    ASSERT,
    /** for general information about the game state */
    INFO,
    /** for errors that allow the game to continue */
    WARN,
    /** for errors that probably cause the game to crash at some point */
    ERROR;

    public static boolean doPrintCallsites = true;

    /** prevents spamming the chat */
    protected static Set<String> callerBlacklist = new HashSet<>();
    private static List<Supplier<String>> onlinePrints = new CopyOnWriteArrayList<>();
    private static Consumer<String> out = null;
    private static Consumer<String> err = null;

    private boolean enabled = true;
    private String codeName = String.format("[%-5s]", this);

    static {
        setOutputReceiver(null, null);
    }

    private static String concatenate(Object[] x) {
        if (x.length == 0) return "";
        for (int i = 0; i < x.length; i++) {
            if (x[i] == null)
                x[i] = "null";
            if (x[i] instanceof Vector3fc) {
                Vector3fc v = (Vector3fc) x[i];
                x[i] = String.format("(%1.3f, %1.3f, %1.3f),", v.x(), v.y(), v.z());
            }
            if (x[i] instanceof Vector2ic) {
                Vector2ic v = (Vector2ic) x[i];
                x[i] = String.format("(%d, %d),", v.x(), v.y());
            }
        }
        StringBuilder s = new StringBuilder(x[0].toString());
        for (int i = 1; i < x.length; i++) {
            s.append(" | ").append(x[i]);
        }
        return s.toString();
    }

    /**
     * sets the debug output of the given print method to the specified output. If both regular and error is null, reset
     * to the default outputs
     * @param regular the new output
     * @param error   the error output
     */
    public static void setOutputReceiver(Consumer<String> regular, Consumer<String> error) {
        if (regular == null && error == null) {
            // default
            out = System.out::println;
            err = System.err::println;
            return;
        }

        if (regular != null) out = regular;
        if (error != null) err = error;
    }

    /**
     * adds a line to the online output roll
     * @param source
     */
    public static void printOnline(Supplier<String> source) {
        if (source == null) {
            Logger.ERROR.print("source is null");
        }
        onlinePrints.add(source);
    }

    /**
     * puts a message on the debug screen, which is updated every frame
     * @param accepter a method that prints the given string, on the same position as a previous call to this method
     */
    public static void setOnlineOutput(Consumer<String> accepter) {
        for (Supplier<String> source : onlinePrints) {
            String message = source.get();
            accepter.accept(message);
        }
    }

    /**
     * DEBUG method to get the calling method name
     * @param level the stack depth to receive. -1 = this method {@code getCallingMethod(int)} 0 = the calling method
     *              (yourself) 1 = the caller of the method this is called in
     * @return a string that completely describes the path to the file, the method and line number where this is called
     *         If DEBUG == false, return an empty string
     */
    public static String getCallingMethod(int level) {
        StackTraceElement caller;
        Exception exception = new Exception();
        StackTraceElement[] stackTrace = exception.getStackTrace();
        level++;
        do {
            caller = stackTrace[level++]; // level + 1
        } while (caller.isNativeMethod() && level < stackTrace.length);

        return String.format("%-100s ", caller);
    }

    /**
     * removes the specified updater off the debug screen
     * @param source an per-frame updated debug message that has previously added to the debug screen
     */
    public static void removeOnlineUpdate(Supplier<String> source) {
        onlinePrints.remove(source);
    }

    /**
     * Set the logger to display only the text supplied by loggers that came after the supplied logger. A call to {@code
     * setLoggerLevel(Logger.values()[0])} will enable all loggers
     * @param minimum the minimum logger level that IS displayed
     */
    public static void setLoggingLevel(Logger minimum) {
        Logger[] levels = values();
        for (int i = 0; i < levels.length; i++) {
            levels[i].enabled = (i >= minimum.ordinal());
        }
    }

    public static Logger getLoggingLevel() {
        Logger[] values = values();
        for (Logger logger : values) {
            if (logger.enabled) return logger;
        }
        return null; // no logging is enabled
    }

    /**
     * prints the result of {@link Object#toString()} of the given objects to the output, preceded with calling method.
     * Every unique callside will only be allowed to print once. For recursive calls, every level will be regarded as a
     * new level, thus print once for every unique depth
     * @param identifier the string that identifies this call as unique
     * @param s          the strings to print
     */
    public synchronized void printSpamless(String identifier, Object... s) {
        if (!callerBlacklist.contains(identifier)) {
            printFrom(2, s);
            callerBlacklist.add(identifier);
        }
    }

    /**
     * prints the toString method of the given objects to the debug output, preceded with the method caller specified by
     * the given call depth
     * @param depth 0 = this method, 1 = the calling method (yourself)
     */
    public synchronized void printFrom(int depth, Object... s) {
        if (!enabled) return;

        String prefix = codeName;
        if (doPrintCallsites) prefix = getCallingMethod(depth) + prefix;

        switch (this) {
            case DEBUG:
            case INFO:
                out.accept(prefix + ": " + concatenate(s));
                break;
            case ASSERT:
            case WARN:
            case ERROR:
                err.accept(prefix + ": " + concatenate(s));

                // let's do a fancy stream
                if (this == ERROR) Arrays.stream(s)
                        .filter(e -> e instanceof Exception)
                        .map(e -> (Exception) e)
                        .flatMap(e -> Arrays.stream(e.getStackTrace()))
                        .map(StackTraceElement::toString)
                        .forEach(err);
                break;
        }
    }

    /**
     * prints the toString method of the given objects to System.out, preceded with calling method
     */
    public void print(Object... s) {
        printFrom(2, s);
    }

    public void printf(String format, Object... arguments) {
        printFrom(2, String.format(Locale.US, format, arguments));
    }

    public void newLine() {
        if (enabled) out.accept("");
    }

    public PrintStream getPrintStream() {
        return System.err;
    }
}