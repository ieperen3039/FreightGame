package Tools;

import nl.NG.Jetfightergame.Assets.Shapes.GeneralShapes;
import nl.NG.Jetfightergame.EntityGeneral.TemporalEntity;
import nl.NG.Jetfightergame.Rendering.Material;
import nl.NG.Jetfightergame.Rendering.MatrixStack.GL2;
import nl.NG.Jetfightergame.Settings.ServerSettings;
import nl.NG.Jetfightergame.Tools.Vectors.Color4f;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;
import org.joml.Quaternionf;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;
import java.util.function.IntConsumer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Created by Geert van Ieperen on 31-1-2017. a class with various tools
 */
public final class Toolbox {

    // universal random to be used everywhere
    public static final Random random = new Random();
    public static final double PHI = 1.6180339887498948;

    private static final float ROUNDINGERROR = 1E-6F;
    public static final float CURSOR_WIDTH = 0.05f;

    /**
     * Draws the x-axis (red), y-axis (green), z-axis (blue), and origin (yellow).
     */
    public static void drawAxisFrame(GL2 gl) {
        if (!ServerSettings.DEBUG) return;

        String source = Logger.getCallingMethod(1);
        if (!Logger.callerBlacklist.contains(source)) {
            Logger.DEBUG.printFrom(2, " - draws axis frame on " + gl.getPosition(new PosVector(0, 0, 0)));
            Logger.callerBlacklist.add(source);
        }

        Material mat = Material.GLOWING;
        gl.pushMatrix();
        {
            gl.setMaterial(mat, Color4f.BLUE);
            gl.draw(GeneralShapes.ARROW);
            gl.rotate((float) Math.toRadians(90), 0f, 1f, 0f);
            gl.setMaterial(mat, Color4f.RED);
            gl.draw(GeneralShapes.ARROW);
            gl.rotate((float) Math.toRadians(-90), 1f, 0f, 0f);
            gl.setMaterial(mat, Color4f.GREEN);
            gl.draw(GeneralShapes.ARROW);
            gl.scale(0.2f);
            gl.setMaterial(Material.ROUGH, Color4f.WHITE);
            gl.draw(GeneralShapes.CUBE);
        }
        gl.popMatrix();
    }

    public static void draw3DPointer(GL2 gl) {
        Material mat = Material.GLOWING;

        gl.setMaterial(mat, Color4f.BLUE);
        gl.pushMatrix();
        {
            gl.scale(1, CURSOR_WIDTH, CURSOR_WIDTH);
            gl.draw(GeneralShapes.CUBE);
        }
        gl.popMatrix();

        gl.setMaterial(mat, Color4f.RED);
        gl.pushMatrix();
        {
            gl.scale(CURSOR_WIDTH, 1, CURSOR_WIDTH);
            gl.draw(GeneralShapes.CUBE);
        }
        gl.popMatrix();

        gl.setMaterial(mat, Color4f.GREEN);
        gl.pushMatrix();
        {
            gl.scale(CURSOR_WIDTH, CURSOR_WIDTH, 1);
            gl.draw(GeneralShapes.CUBE);
        }
        gl.popMatrix();
    }

    public static void checkGLError() {
        if (!ServerSettings.DEBUG) return;
        int error;
        int i = 0;
        while ((error = glGetError()) != GL_NO_ERROR) {
            Logger.ERROR.printFrom(2, "glError " + asHex(error) + ": " + getMessage(error));
            if (++i == 10) throw new IllegalStateException("Context is probably not current for this thread");
        }
    }

    private static String getMessage(int error) {
        switch (error) {
            case GL_INVALID_ENUM:
                return "Invalid Enum";
            case GL_INVALID_VALUE:
                return "Invalid Value";
            case GL_INVALID_OPERATION:
                return "Invalid Operation";
            case GL_STACK_OVERFLOW:
                return "Stack Overflow";
            case GL_STACK_UNDERFLOW:
                return "Stack Underflow";
            case GL_OUT_OF_MEMORY:
                return "Out of Memory";
        }
        return "Unknown Error";
    }

    public static String asHex(int decimal) {
        return "0x" + Integer.toHexString(decimal).toUpperCase();
    }


    public static void checkALError() {
        checkALError("");
    }

    public static void checkALError(String args) {
        if (!ServerSettings.DEBUG) return;
        int error;
        int i = 0;
        while ((error = alGetError()) != AL_NO_ERROR) {
            Logger.WARN.printFrom(2, "alError " + asHex(error) + ": " + alGetString(error), args);
            if (++i == 10) {
                throw new IllegalStateException("Context is probably not current for this thread");
            }
        }
    }

    /**
     * call System.exit and tells who did it, unless DEBUG is false
     */
    public static void exitJava() {
        if (!ServerSettings.DEBUG) {
            final StackTraceElement caller = new Exception().getStackTrace()[1];
            Logger.WARN.print(caller + ": Tried to exit JVM while DEBUG mode is false.");
        }

        try {
            Logger.ERROR.newLine();
            Logger.DEBUG.printFrom(2, "Ending JVM");
            Thread.sleep(10);
            Thread.dumpStack();
            System.exit(-1);
        } catch (InterruptedException e) {
            System.exit(-1);
        }
    }

    public static boolean almostZero(float number) {
        return (((number + ROUNDINGERROR) >= 0.0f) && ((number - ROUNDINGERROR) <= 0.0f));
    }

    /**
     * performs an incremental insertion-sort on (preferably nearly-sorted) the given array
     * @param items the array to sort
     * @param map   maps a moving source to the value to be sorted upon
     * @modifies items
     */
    public static <Type> void insertionSort(Type[] items, Function<Type, Float> map) {
        // iterate incrementally over the array
        for (int head = 1; head < items.length; head++) {
            Type subject = items[head];

            // decrement for the right position
            int empty = head;

            while (empty > 0) {
                Type target = items[empty - 1];

                if (map.apply(target) > map.apply(subject)) {
                    items[empty] = target;
                    empty--;
                } else break;
            }
            items[empty] = subject;
        }
    }

    /**
     * merges a joining array into this array, and removes {@link TemporalEntity} entities that are overdue as in {@link
     * TemporalEntity#isOverdue()}
     * @param host the sorted largest non-empty of the arrays to merge, entities in this array will be checked for
     *             relevance.
     * @param join the sorted other non-empty array to merge
     * @param map  maps a moving source to the value to be sorted upon
     * @return a sorted array of living entities from both host and join combined.
     */
    public static <Type> Type[] mergeArrays(Type[] host, Type[] join, Function<Type, Float> map) {
        int hLength = host.length;
        int jLength = join.length;

        Type[] results = Arrays.copyOf(host, hLength + jLength);
        // current indices
        int hIndex = 0;
        int jIndex = 0;

        for (int i = 0; i < results.length; i++) {
            if (jIndex >= jLength) {
                results[i] = host[hIndex];
                hIndex++;

            } else if (hIndex >= hLength) {
                results[i] = join[jIndex];
                jIndex++;

            } else {
                Type hostItem = host[hIndex];
                Type joinItem = join[jIndex];

                // select the smallest
                if (map.apply(hostItem) < map.apply(joinItem)) {
                    results[i] = hostItem;
                    hIndex++;

                } else {
                    results[i] = joinItem;
                    jIndex++;
                }
            }
        }

        // loop automatically ends after at most (i = alpha.length + beta.length) iterations
        return results;
    }

    /** @return a rotation that maps the x-vector to the given direction, with up in direction of z */
    public static Quaternionf xTo(DirVector direction) {
        return new Quaternionf().rotateTo(DirVector.xVector(), direction);
    }

    /** returns a uniformly distributed random value between val1 and val2 */
    public static float randomBetween(float val1, float val2) {
        return val1 + ((val2 - val1) * random.nextFloat());
    }

    /** transforms a double to an int, by drawing a random variable for the remainder */
    public static int randomToInt(float value) {
        return (int) (value + random.nextFloat());
    }

    /**
     * executes the given function {@code size} times in parallel, with as parameters the numbers [0 ... size]
     * @param function
     * @param size
     * @throws InterruptedException
     */
    public static void inParallel(IntConsumer function, int size) {
        Thread[] waitList = new Thread[size];

        for (int i = 0; i < size; i++) {
            int n = i;
            Thread thread = new Thread(() -> function.accept(n));
            thread.start();
            waitList[i] = thread;
        }

        try {
            for (Thread thread : waitList) {
                thread.join();
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static float instantPreserveFraction(float rotationPreserveFactor, float deltaTime) {
        return (float) (StrictMath.pow(rotationPreserveFactor, deltaTime));
    }

    public static void waitFor(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public static <Type> int binarySearch(Type[] array, Function<Type, Float> map, float value) {
        int low = 0;
        int high = array.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Type e = array[mid];

            float cmp = map.apply(e);
            if (cmp < value)
                low = mid + 1;
            else if (cmp > value)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    public static boolean isValidQuaternion(Quaternionf rotation) {
        return !(Float.isNaN(rotation.x) || Float.isNaN(rotation.y) || Float.isNaN(rotation.z) || Float.isNaN(rotation.w));
    }

    public static String[] toStringArray(Object[] values) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].toString();
        }
        return result;
    }

    public static String findClosest(String target, String[] options) {
        int max = 0;
        int lengthOfMax = Integer.MAX_VALUE;
        String best = "";

        for (String candidate : options) {
            int wordLength = Math.abs(candidate.length() - target.length());
            int dist = hammingDistance(target, candidate);

            if (dist > max || (dist == max && wordLength < lengthOfMax)) {
                max = dist;
                lengthOfMax = wordLength;
                best = candidate;
            }
        }

        return best;
    }

    public static <T extends Enum> T findClosest(String target, T[] options) {
        int max = 0;
        int lengthOfMax = Integer.MAX_VALUE;
        T best = null;

        for (T candidate : options) {
            String asString = candidate.toString();
            int wordLength = Math.abs(asString.length() - target.length());
            int dist = hammingDistance(target, asString);

            if (dist > max || (dist == max && wordLength < lengthOfMax)) {
                max = dist;
                lengthOfMax = wordLength;
                best = candidate;
            }
        }

        return best;
    }

    /**
     * computes the hamming distance between string a and b as: {@code LCSLength(X[1..m], Y[1..n]) C = array(0..m, 0..n)
     * for i := 1..m for j := 1..n if X[i] = Y[j] C[i,j] := C[i-1,j-1] + 1 else C[i,j] := max(C[i,j-1], C[i-1,j]) return
     * C[m,n] }
     */
    public static int hammingDistance(String a, String b) {
        int m = a.length();
        int n = b.length();
        int[][] cMat = new int[m + 1][n + 1]; // initialized at 0

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char ca = a.charAt(i - 1);
                char cb = b.charAt(j - 1);
                if (ca == cb) {
                    cMat[i][j] = cMat[i - 1][j - 1] + 1;
                } else {
                    cMat[i][j] = Math.max(cMat[i][j - 1], cMat[i - 1][j]);
                }
            }
        }

        return cMat[m][n];
    }
}
