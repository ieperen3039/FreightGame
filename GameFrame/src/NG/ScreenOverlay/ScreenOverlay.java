package NG.ScreenOverlay;

import NG.DataStructures.Color4f;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.Tools.Logger;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import static NG.ScreenOverlay.Menu.MenuStyleSettings.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Jorren & Geert
 */
public final class ScreenOverlay implements GameAspect {
    private long vg;
    private NVGColor color;
    private NVGPaint paint;

    /** fontbuffer MUST be a field */
    @SuppressWarnings("FieldCanBeLocal")
    private final ByteBuffer[] fontBuffer = new ByteBuffer[JFGFonts.values().length];
    private Map<Path, Integer> imageBuffer = new HashMap<>();

    private final Collection<Consumer<Painter>> drawBuffer = new ArrayList<>();
    private final Lock drawBufferLock = new ReentrantLock();

    /**
     * @param game the game this plays in
     * @throws IOException If an error occures during the setup of the Hud.
     */
    public void init(Game game) throws IOException {

        if (game.settings().ANTIALIAS > 0) {
            vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        } else {
            vg = nvgCreate(NVG_STENCIL_STROKES);
        }

        if (vg == NULL) {
            throw new IOException("Could not initialize NanoVG");
        }

        JFGFonts[] fonts = JFGFonts.values();
        for (int i = 0; i < fonts.length; i++) {
            fontBuffer[i] = fonts[i].asByteBuffer();
            if (nvgCreateFontMem(vg, fonts[i].name, fontBuffer[i], 1) == -1) {
                Logger.ERROR.print("Could not create font " + fonts[i].name);
            }
        }

        color = NVGColor.create();
        paint = NVGPaint.create();
    }

    @Override
    public void cleanup() {
        removeHudItem();
    }

    public void addHudItem(Consumer<Painter> render) {
        if (render == null) return;

        drawBufferLock.lock();
        try {
            drawBuffer.add(render);
        } finally {
            drawBufferLock.unlock();
        }
    }

    public void removeHudItem(Consumer<Painter> render) {
        if (render == null) return;

        drawBufferLock.lock();
        try {
            drawBuffer.remove(render);
        } finally {
            drawBufferLock.unlock();
        }
    }

    /** clear the hud drawBuffer */
    public void removeHudItem() {
        drawBufferLock.lock();
        try {
            drawBuffer.clear();
        } finally {
            drawBufferLock.unlock();
        }
    }

    public class Painter {
        private final int printRollSize;
        private final int yPrintRoll;
        private final int xPrintRoll;
        private int printRollEntry = 0;
        /** maps a position in world-space to a position on the screen */
        private final Function<Vector3f, Vector2f> mapper;

        private final Color4f strokeColor = MENU_STROKE_COLOR;
        private final int strokeWidth = MENU_STROKE_WIDTH;
        private final Color4f textColor = Color4f.WHITE;
        private final Color4f fillColor = MENU_FILL_COLOR;

        public final int windowWidth;
        public final int windowHeight;
        public final Vector3f cameraPosition;

        /**
         * @param windowWidth    width of this hud display iteration
         * @param windowHeight   height of ''
         * @param mapper         maps a world-space vector to relative position ([-1, 1], [-1, 1]) in the view.
         * @param cameraPosition renderposition of camera in worldspace
         * @param xPrintRoll     x position of where to start the printRoll
         * @param yPrintRoll     y position of where to start the printRoll
         * @param printRollSize  fontsize of printRoll
         */
        public Painter(
                int windowWidth, int windowHeight, Function<Vector3f, Vector2f> mapper, Vector3f cameraPosition,
                int xPrintRoll, int yPrintRoll, int printRollSize
        ) {
            this.windowWidth = windowWidth;
            this.windowHeight = windowHeight;
            this.mapper = mapper;
            this.cameraPosition = cameraPosition;
            this.printRollSize = printRollSize;
            this.yPrintRoll = printRollSize + yPrintRoll;
            this.xPrintRoll = xPrintRoll;
        }


        /**
         * @param worldPosition a position in world-space
         * @return the coordinates of this position as where they appear on the screen, possibly outside the borders.
         */
        public Vector2i positionOnScreen(Vector3f worldPosition) {
            final Vector2f relativePosition = mapper.apply(worldPosition);
            if (relativePosition == null) return null;

            relativePosition.add(1f, -1f).mul(0.5f, -0.5f);

            int x = (int) (relativePosition.x() * windowWidth);
            int y = (int) (relativePosition.y() * windowHeight);
            return new Vector2i(x, y);
        }

        /**
         * Get an instance of NVGColor with the correct values. All color values are floating point numbers supposed to
         * be between 0f and 1f.
         * @param red   The red component.
         * @param green The green component.
         * @param blue  The blue component.
         * @param alpha The alpha component.
         * @return an instance of NVGColor.
         */
        public NVGColor rgba(float red, float green, float blue, float alpha) {
            color.r(red);
            color.g(green);
            color.b(blue);
            color.a(alpha);

            return color;
        }

        /** @see #rgba(float, float, float, float) */
        private NVGColor rgba(Color4f color) {
            return rgba(color.red, color.green, color.blue, color.alpha);
        }

        public void rectangle(int x, int y, int width, int height) {
            rectangle(x, y, width, height, fillColor, strokeColor, strokeWidth);
        }

        public void rectangle(int x, int y, int width, int height, Color4f fillColor, Color4f strokeColor, int strokeWidth) {
            nvgBeginPath(vg);
            nvgRect(vg, x, y, width, height);

            setFill(fillColor);
            setStroke(strokeWidth, strokeColor);
        }

        public void roundedRectangle(Vector2ic pos, Vector2ic dim, int indent) {
            roundedRectangle(pos.x(), pos.y(), dim.x(), dim.y(), indent);
        }

        public void roundedRectangle(int x, int y, int width, int height, int indent) {
            roundedRectangle(x, y, width, height, indent, fillColor, strokeColor, strokeWidth);
        }

        public void roundedRectangle(int x, int y, int width, int height, int indent, Color4f fillColor, Color4f strokeColor, int strokeWidth) {
            int xMax = x + width;
            int yMax = y + height;

            polygon(
                    fillColor, strokeColor, strokeWidth,
                    new Vector2i(x + indent, y),
                    new Vector2i(xMax - indent, y),
                    new Vector2i(xMax, y + indent),
                    new Vector2i(xMax, yMax - indent),
                    new Vector2i(xMax - indent, yMax),
                    new Vector2i(x + indent, yMax),
                    new Vector2i(x, yMax - indent),
                    new Vector2i(x, y + indent)
            );
        }

        /** @see #circle(int, int, int, Color4f, int, Color4f) */
        public void circle(int x, int y, int radius) {
            circle(x, y, radius, fillColor, strokeWidth, strokeColor);
        }

        /**
         * draws a circle. x and y are the circle middle. x, y and radius are in screen coordinates.
         */
        public void circle(int x, int y, int radius, Color4f fillColor, int strokeWidth, Color4f strokeColor) {
            nvgBeginPath(vg);
            nvgCircle(vg, x, y, radius);

            setFill(fillColor);
            setStroke(strokeWidth, strokeColor);
        }

        public void polygon(Vector2i... points) {
            polygon(fillColor, strokeColor, strokeWidth, points);
        }

        public void polygon(Color4f fillColor, Color4f strokeColor, int strokeWidth, Vector2i... points) {
            nvgBeginPath(vg);

            nvgMoveTo(vg, points[points.length - 1].x, points[points.length - 1].y);
            for (Vector2i point : points) {
                nvgLineTo(vg, point.x, point.y);
            }

            setFill(fillColor);
            setStroke(strokeWidth, strokeColor);
        }

        /**
         * draw a line along the coordinates, when supplied in (x, y) pairs
         * @param points (x, y) pairs of screen coordinates
         */
        public void line(int strokeWidth, Color4f strokeColor, int... points) {
            nvgBeginPath(vg);

            int i = 0;
            nvgMoveTo(vg, points[i++], points[i++]);
            while (i < points.length) {
                nvgLineTo(vg, points[i++], points[i++]);
            }

            setStroke(strokeWidth, strokeColor);
        }

        // non-shape defining functions

        public void text(int x, int y, float size, JFGFonts font, int align, Color4f color, String text) {
            nvgFontSize(vg, size);
            nvgFontFace(vg, font.name);
            nvgTextAlign(vg, align);
            nvgFillColor(vg, rgba(color));
            nvgText(vg, x, y, text);
        }

        public void printRoll(String text) {
            int y = yPrintRoll + ((printRollSize + 5) * printRollEntry);

            text(xPrintRoll, y, printRollSize, JFGFonts.LUCIDA_CONSOLE, NVG_ALIGN_LEFT, textColor, text);
            printRollEntry++;
        }

        public void setFill(Color4f color) {
            nvgFillColor(vg, rgba(color));
            nvgFill(vg);
        }

        public void setStroke(int width, Color4f color) {
            nvgStrokeWidth(vg, width);
            nvgStrokeColor(vg, rgba(color));
            nvgStroke(vg);
        }

        public void image(Path filename, int x, int y, int width, int height, float alpha) throws IOException {
            image(filename, x, y, width, height, 0f, alpha, NVG_IMAGE_GENERATE_MIPMAPS);
        }

        public void image(Path fileName, int x, int y, int width, int height, float angle, float alpha, int imageFlags) throws IOException {
            int img = getImage(fileName, imageFlags);
            NVGPaint p = nvgImagePattern(vg, x, y, width, height, angle, img, alpha, paint);

            rectangle(x, y, width, height);

            nvgFillPaint(vg, p);
            nvgFill(vg);
        }

        private int getImage(Path filePath, int imageFlags) throws IOException {
            if (imageBuffer.containsKey(filePath)) {
                return imageBuffer.get(filePath);
            }
            ByteBuffer image = Toolbox.toByteBuffer(filePath);
            int img = nvgCreateImageMem(vg, imageFlags, image);
            imageBuffer.put(filePath, img);
            return img;
        }
    }

    /**
     * @param windowWidth    width of the current window drawn on
     * @param windowHeight   height of the current window
     * @param mapper         maps a world-space vector to relative position ([-1, 1], [-1, 1]) in the view.
     * @param cameraPosition position of camera
     */
    public void draw(int windowWidth, int windowHeight, Function<Vector3f, Vector2f> mapper, Vector3f cameraPosition) {
        Painter vanGogh = new Painter(windowWidth, windowHeight, mapper, cameraPosition, 5, 5, 24);
        draw(windowWidth, windowHeight, vanGogh);
    }

    /**
     * @param windowWidth  width of the current window drawn on
     * @param windowHeight height of the current window
     * @param xRoll        x position of the debug printroll screen
     * @param yRoll        y position of ''
     * @param rollSize     font size of ''
     */
    public void draw(int windowWidth, int windowHeight, int xRoll, int yRoll, int rollSize) {
        Painter bobRoss = new Painter(windowWidth, windowHeight, (v) -> new Vector2f(), Vectors.zVector(), xRoll, yRoll, rollSize);
        draw(windowWidth, windowHeight, bobRoss);
    }

    /**
     * draw using the given painter
     */
    private synchronized void draw(int windowWidth, int windowHeight, Painter painter) {
        // Begin NanoVG frame
        nvgBeginFrame(vg, windowWidth, windowHeight, 1);

        // Draw the right drawhandlers
        drawBufferLock.lock();
        try {
            drawBuffer.forEach(m -> m.accept(painter));
        } finally {
            drawBufferLock.unlock();
        }

        // End NanoVG frame
        nvgEndFrame(vg);

        // restore window state
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_STENCIL_TEST);
    }
}
